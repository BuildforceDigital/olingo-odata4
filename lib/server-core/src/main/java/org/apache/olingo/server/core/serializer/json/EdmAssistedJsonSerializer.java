/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.olingo.server.core.serializer.json;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.AbstractEntityCollection;
import org.apache.olingo.commons.api.data.AbstractODataObject;
import org.apache.olingo.commons.api.data.Annotatable;
import org.apache.olingo.commons.api.data.Annotation;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Linked;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.Valuable;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmComplexType;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.core.edm.EdmTypeInfo;
import org.apache.olingo.commons.core.edm.primitivetype.EdmPrimitiveTypeFactory;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.EdmAssistedSerializer;
import org.apache.olingo.server.api.serializer.EdmAssistedSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerException.MessageKeys;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.core.serializer.SerializerResultImpl;
import org.apache.olingo.server.core.serializer.utils.CircleStreamBuffer;
import org.apache.olingo.server.core.serializer.utils.ContentTypeHelper;
import org.apache.olingo.server.core.serializer.utils.ContextURLBuilder;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class EdmAssistedJsonSerializer implements EdmAssistedSerializer {

  private static final String IO_EXCEPTION_TEXT = "An I/O exception occurred.";

  protected final boolean isIEEE754Compatible;
  protected final boolean isODataMetadataNone;
  protected final boolean isODataMetadataFull;

  public EdmAssistedJsonSerializer(ContentType contentType) {
    isIEEE754Compatible = ContentTypeHelper.isODataIEEE754Compatible(contentType);
    isODataMetadataNone = ContentTypeHelper.isODataMetadataNone(contentType);
    isODataMetadataFull = ContentTypeHelper.isODataMetadataFull(contentType);
  }

  @Override
  public SerializerResult entityCollection(ServiceMetadata metadata, EdmEntityType entityType,
                                           AbstractEntityCollection entityCollection, EdmAssistedSerializerOptions options)
      throws SerializerException {
    return serialize(metadata, entityType, entityCollection, options == null ? null : options.getContextURL());
  }

  public SerializerResult entity(ServiceMetadata metadata, EdmEntityType entityType, Entity entity,
                                 EdmAssistedSerializerOptions options) throws SerializerException {
    return serialize(metadata, entityType, entity, options == null ? null : options.getContextURL());
  }

  protected SerializerResult serialize(ServiceMetadata metadata, EdmEntityType entityType,
                                       AbstractODataObject obj, ContextURL contextURL) throws SerializerException {
    String metadataETag =
        isODataMetadataNone || metadata == null || metadata.getServiceMetadataETagSupport() == null ? null : metadata
            .getServiceMetadataETagSupport().getMetadataETag();
    String contextURLString = isODataMetadataNone || contextURL == null ? null : ContextURLBuilder.create(
        contextURL).toASCIIString();
    OutputStream outputStream = null;
    SerializerException cachedException = null;
    
    CircleStreamBuffer buffer = new CircleStreamBuffer();
    outputStream = buffer.getOutputStream();
    try (JsonGenerator json = new JsonFactory().createGenerator(outputStream)) {
      if (obj instanceof AbstractEntityCollection) {
        doSerialize(entityType, (AbstractEntityCollection) obj, contextURLString, metadataETag, json);
      } else if (obj instanceof Entity) {
        doSerialize(entityType, (Entity) obj, contextURLString, metadataETag, json);
      } else {
        throw new SerializerException("Input type not supported.", MessageKeys.NOT_IMPLEMENTED);
      }
      json.flush();
      json.close();
      return SerializerResultImpl.with().content(buffer.getInputStream()).build();
    } catch (IOException e) {
      cachedException = new SerializerException(IO_EXCEPTION_TEXT, e, SerializerException.MessageKeys.IO_EXCEPTION);
      throw cachedException;
    } finally {
      if (outputStream != null) {
        try {
          outputStream.close();
        } catch (IOException e) {
          throw cachedException == null ? new SerializerException(IO_EXCEPTION_TEXT, e,
              SerializerException.MessageKeys.IO_EXCEPTION) : cachedException;
        }
      }
    }
  }

  protected void doSerialize(EdmEntityType entityType, AbstractEntityCollection entityCollection,
                             String contextURLString, String metadataETag, JsonGenerator json)
      throws IOException, SerializerException {

    json.writeStartObject();

    metadata(contextURLString, metadataETag, null, null, entityCollection.getId(), false, json);

    if (entityCollection.getCount() != null) {
      if (isIEEE754Compatible) {
        json.writeStringField(Constants.JSON_COUNT, Integer.toString(entityCollection.getCount()));
      } else {
        json.writeNumberField(Constants.JSON_COUNT, entityCollection.getCount());
      }
    }
    if (entityCollection.getDeltaLink() != null) {
      json.writeStringField(Constants.JSON_DELTA_LINK, entityCollection.getDeltaLink().toASCIIString());
    }

    for (Annotation annotation : entityCollection.getAnnotations()) {
      valuable(json, annotation, '@' + annotation.getTerm(), null, null);
    }

    json.writeArrayFieldStart(Constants.VALUE);
    for (Entity entity : entityCollection) {
      doSerialize(entityType, entity, null, null, json);
    }
    json.writeEndArray();

    if (entityCollection.getNext() != null) {
      json.writeStringField(Constants.JSON_NEXT_LINK, entityCollection.getNext().toASCIIString());
    }

    json.writeEndObject();
  }

  protected void doSerialize(EdmEntityType entityType, Entity entity,
                             String contextURLString, String metadataETag, JsonGenerator json)
      throws IOException, SerializerException {

    json.writeStartObject();

    String typeName = entity.getType() == null ? null : new EdmTypeInfo.Builder().setTypeExpression(entity
        .getType()).build().external();
    metadata(contextURLString, metadataETag, entity.getETag(), typeName, entity.getId(), true, json);

    for (Annotation annotation : entity.getAnnotations()) {
      valuable(json, annotation, '@' + annotation.getTerm(), null, null);
    }

    for (Property property : entity.getProperties()) {
      String name = property.getName();
      EdmProperty edmProperty = entityType == null || entityType.getStructuralProperty(name) == null ? null
          : entityType.getStructuralProperty(name);
      valuable(json, property, name, edmProperty == null ? null : edmProperty.getType(), edmProperty);
    }

    if (!isODataMetadataNone &&
        entity.getEditLink() != null && entity.getEditLink().getHref() != null) {
      json.writeStringField(Constants.JSON_EDIT_LINK, entity.getEditLink().getHref());

      if (entity.isMediaEntity()) {
        json.writeStringField(Constants.JSON_MEDIA_READ_LINK, entity.getEditLink().getHref() + "/$value");
      }
    }

    links(entity, entityType, json);

    json.writeEndObject();
  }

  private void metadata(String contextURLString, String metadataETag, String eTag,
                        String type, URI id, boolean writeNullId, JsonGenerator json)
      throws IOException, SerializerException {
    if (!isODataMetadataNone) {
      if (contextURLString != null) {
        json.writeStringField(Constants.JSON_CONTEXT, contextURLString);
      }
      if (metadataETag != null) {
        json.writeStringField(Constants.JSON_METADATA_ETAG, metadataETag);
      }
      if (eTag != null) {
        json.writeStringField(Constants.JSON_ETAG, eTag);
      }
      if(isODataMetadataFull){
        if (type != null) {
          json.writeStringField(Constants.JSON_TYPE, type);
        }
        if (id == null) {
          if (writeNullId) {
            json.writeNullField(Constants.JSON_ID);
          }
        } else {
          json.writeStringField(Constants.JSON_ID, id.toASCIIString());
        }
      }
    }
  }

  private void links(Linked linked, EdmEntityType entityType, JsonGenerator json)
      throws IOException, SerializerException {

    for (Link link : linked.getNavigationLinks()) {
      String name = link.getTitle();
      for (Annotation annotation : link.getAnnotations()) {
        valuable(json, annotation, name + '@' + annotation.getTerm(), null, null);
      }

      EdmEntityType targetType =
          entityType == null || name == null || entityType.getNavigationProperty(name) == null ? null : entityType
              .getNavigationProperty(name).getType();
      if (link.getInlineEntity() != null) {
        json.writeFieldName(name);
        doSerialize(targetType, link.getInlineEntity(), null, null, json);
      } else if (link.getInlineEntitySet() != null) {
        json.writeArrayFieldStart(name);
        for (Entity subEntry : link.getInlineEntitySet().getEntities()) {
          doSerialize(targetType, subEntry, null, null, json);
        }
        json.writeEndArray();
      }
    }
  }

  private void collection(JsonGenerator json, EdmType itemType, String typeName,
                          EdmProperty edmProperty, ValueType valueType, List<?> value)
      throws IOException, SerializerException {

    json.writeStartArray();

    for (Object item : value) {
      switch (valueType) {
      case COLLECTION_PRIMITIVE:
        primitiveValue(json, (EdmPrimitiveType) itemType, typeName, edmProperty, item);
        break;

      case COLLECTION_GEOSPATIAL:
      case COLLECTION_ENUM:
        throw new SerializerException("Geo and enum types are not supported.", MessageKeys.NOT_IMPLEMENTED);

      case COLLECTION_COMPLEX:
        complexValue(json, (EdmComplexType) itemType, typeName, (ComplexValue) item);
        break;

      default:
      }
    }

    json.writeEndArray();
  }

  protected void primitiveValue(JsonGenerator json, EdmPrimitiveType valueType, String typeName,
                                EdmProperty edmProperty, Object value) throws IOException, SerializerException {

    EdmPrimitiveType type = valueType;
    if (type == null) {
      EdmPrimitiveTypeKind kind =
          typeName == null ? EdmTypeInfo.determineTypeKind(value) : new EdmTypeInfo.Builder().setTypeExpression(
              typeName).build().getPrimitiveTypeKind();
      type = kind == null ? null : EdmPrimitiveTypeFactory.getInstance(kind);
    }

    if (value == null) {
      json.writeNull();
    } else if (type == null) {
      throw new SerializerException("The primitive type could not be determined.",
          MessageKeys.INCONSISTENT_PROPERTY_TYPE, "");
    } else if (type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Boolean)) {
      json.writeBoolean((Boolean) value);
    } else {
      String serialized = null;
      try {
        serialized = type.valueToString(value,
            edmProperty == null ? null : edmProperty.isNullable(),
            edmProperty == null ? null : edmProperty.getMaxLength(),
            edmProperty == null ? Constants.DEFAULT_PRECISION : edmProperty.getPrecision(),
            edmProperty == null ? Constants.DEFAULT_SCALE : edmProperty.getScale(),
            edmProperty == null ? null : edmProperty.isUnicode());
      } catch (EdmPrimitiveTypeException e) {
        String name = edmProperty == null ? "" : edmProperty.getName();
        throw new SerializerException("Wrong value for property '" + name + "'!", e,
            SerializerException.MessageKeys.WRONG_PROPERTY_VALUE, name, value.toString());
      }
      if (isIEEE754Compatible &&
          (type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int64)
              || type == EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Decimal))
          || type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Byte)
              && type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.SByte)
              && type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Single)
              && type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Double)
              && type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int16)
              && type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int32)
              && type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Int64)
              && type != EdmPrimitiveTypeFactory.getInstance(EdmPrimitiveTypeKind.Decimal)) {
        json.writeString(serialized);
      } else {
        json.writeNumber(serialized);
      }
    }
  }

  private void complexValue(JsonGenerator json, EdmComplexType valueType, String typeName,
                            ComplexValue value) throws IOException, SerializerException {
    json.writeStartObject();

    if (typeName != null && isODataMetadataFull) {
      json.writeStringField(Constants.JSON_TYPE, typeName);
    }

    for (Property property : value.getValue()) {
      String name = property.getName();
      EdmProperty edmProperty = valueType == null || valueType.getStructuralProperty(name) == null ? null
          : valueType.getStructuralProperty(name);
      valuable(json, property, name, edmProperty == null ? null : edmProperty.getType(), edmProperty);
    }
    links(value, null, json);

    json.writeEndObject();
  }

  private void value(JsonGenerator json, Valuable value, EdmType type, EdmProperty edmProperty)
      throws IOException, SerializerException {
    String typeName = value.getType() == null ? null : new EdmTypeInfo.Builder().setTypeExpression(value
        .getType()).build().external();

    if (value.isNull()) {
      json.writeNull();
    } else if (value.isCollection()) {
      collection(json, type, typeName, edmProperty, value.getValueType(), value.asCollection());
    } else if (value.isPrimitive()) {
      primitiveValue(json, (EdmPrimitiveType) type, typeName, edmProperty, value.asPrimitive());
    } else if (value.isComplex()) {
      complexValue(json, (EdmComplexType) type, typeName, value.asComplex());
    } else if (value.isEnum() || value.isGeospatial()) {
      throw new SerializerException("Geo and enum types are not supported.", MessageKeys.NOT_IMPLEMENTED);
    }
  }

  protected void valuable(JsonGenerator json, Valuable valuable, String name, EdmType type,
      EdmProperty edmProperty) throws IOException, SerializerException {

    if (isODataMetadataFull
        && !(valuable instanceof Annotation) && !valuable.isComplex()) {

      String typeName = valuable.getType();
      if (typeName == null && type == null && valuable.isPrimitive()) {
        if (valuable.isCollection()) {
          if (!valuable.asCollection().isEmpty()) {
            EdmPrimitiveTypeKind kind = EdmTypeInfo.determineTypeKind(valuable.asCollection().get(0));
            if (kind != null) {
              typeName = "Collection(" + kind.getFullQualifiedName().getFullQualifiedNameAsString() + ')';
            }
          }
        } else {
          EdmPrimitiveTypeKind kind = EdmTypeInfo.determineTypeKind(valuable.asPrimitive());
          if (kind != null) {
            typeName = kind.getFullQualifiedName().getFullQualifiedNameAsString();
          }
        }
      }
      
      if (typeName != null) {
        json.writeStringField(name + Constants.JSON_TYPE, constructTypeExpression(typeName));
      }
    }

    for (Annotation annotation : valuable.getAnnotations()) {
      valuable(json, annotation, name + '@' + annotation.getTerm(), null, null);
    }

    json.writeFieldName(name);
    value(json, valuable, type, edmProperty);
  }

  private String constructTypeExpression(String typeName) {
    EdmTypeInfo typeInfo = new EdmTypeInfo.Builder().setTypeExpression(typeName).build();
    StringBuilder stringBuilder = new StringBuilder();

    if (typeInfo.isCollection()) {
      stringBuilder.append("#Collection(");
    } else {
      stringBuilder.append('#');
    }

    stringBuilder.append(typeInfo.isPrimitiveType() ? typeInfo.getFullQualifiedName().getName() : typeInfo
        .getFullQualifiedName().getFullQualifiedNameAsString());

    if (typeInfo.isCollection()) {
      stringBuilder.append(')');
    }

    return stringBuilder.toString();
  }
}
