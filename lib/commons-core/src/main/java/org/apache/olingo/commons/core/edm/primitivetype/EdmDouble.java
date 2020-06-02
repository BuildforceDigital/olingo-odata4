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
package org.apache.olingo.commons.core.edm.primitivetype;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;

/**
 * Implementation of the EDM primitive type Double.
 */
public final class EdmDouble extends SingletonPrimitiveType {

  protected static final String NEGATIVE_INFINITY = "-INF";

  protected static final String POSITIVE_INFINITY = "INF";

  protected static final String NaN = "NaN";

  private static final Pattern PATTERN = Pattern.compile(
      "[+-]?\\p{Digit}+(?:\\.\\p{Digit}+)?(?:[Ee][+-]?\\p{Digit}{1,3})?");

  private static final EdmDouble INSTANCE = new EdmDouble();

  public static EdmDouble getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean isCompatible(EdmPrimitiveType primitiveType) {
    return primitiveType instanceof EdmByte
        || primitiveType instanceof EdmSByte
        || primitiveType instanceof EdmInt16
        || primitiveType instanceof EdmInt32
        || primitiveType instanceof EdmInt64
        || primitiveType instanceof EdmSingle
        || primitiveType instanceof EdmDouble;
  }

  @Override
  public Class<?> getDefaultType() {
    return Double.class;
  }

  @Override
  protected <T> T internalValueOfString(String value,
                                        Boolean isNullable, Integer maxLength, Integer precision,
                                        Integer scale, Boolean isUnicode, Class<T> returnType) throws EdmPrimitiveTypeException {

    Double result;
    BigDecimal bigDecimalValue = null;
    // Handle special values first.
    switch (value) {
      case NEGATIVE_INFINITY:
        result = Double.NEGATIVE_INFINITY;
        break;
      case POSITIVE_INFINITY:
        result = Double.POSITIVE_INFINITY;
        break;
      case NaN:
        result = Double.NaN;
        break;
      default:
        // Now only "normal" numbers remain.
        if (!PATTERN.matcher(value).matches()) {
          throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.");
        }

        // The number format is checked above, so we don't have to catch NumberFormatException.
        bigDecimalValue = new BigDecimal(value);
        result = bigDecimalValue.doubleValue();
        // "Real" infinite values have been treated already above, so we can throw an exception
        // if the conversion to a double results in an infinite value.
        if (result.isInfinite() || BigDecimal.valueOf(result).compareTo(bigDecimalValue) != 0) {
          throw new EdmPrimitiveTypeException("The literal '" + value + "' has illegal content.");
        }
        break;
    }

    if (returnType.isAssignableFrom(Double.class)) {
      return returnType.cast(result);
    } else if (result.isInfinite() || result.isNaN()) {
      if (returnType.isAssignableFrom(Float.class)) {
        return returnType.cast(result.floatValue());
      } else {
        throw new EdmPrimitiveTypeException("The literal '" + value
            + "' cannot be converted to value type " + returnType + ".");
      }
    } else {
      try {
        return EdmDecimal.convertDecimal(bigDecimalValue, returnType);
      } catch (IllegalArgumentException e) {
        throw new EdmPrimitiveTypeException("The literal '" + value
            + "' cannot be converted to value type " + returnType + ".", e);
      } catch (ClassCastException e) {
        throw new EdmPrimitiveTypeException("The value type " + returnType + " is not supported.", e);
      }
    }
  }

  @Override
  protected <T> String internalValueToString(T value,
                                             Boolean isNullable, Integer maxLength, Integer precision,
                                             Integer scale, Boolean isUnicode) throws EdmPrimitiveTypeException {
    if (value instanceof Long) {
      if (Math.abs((Long) value) < 1L << 51) {
        return value.toString();
      } else {
        throw new EdmPrimitiveTypeException("The value '" + value + "' is not valid.");
      }
    } else if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
      return value.toString();
    } else if (value instanceof Double) {
      return (Double) value == Double.NEGATIVE_INFINITY ? NEGATIVE_INFINITY
          : (Double) value == Double.POSITIVE_INFINITY ? POSITIVE_INFINITY : value.toString();
    } else if (value instanceof Float) {
      return (Float) value == Float.NEGATIVE_INFINITY ? NEGATIVE_INFINITY
          : (Float) value == Float.POSITIVE_INFINITY ? POSITIVE_INFINITY : value.toString();
    } else if (value instanceof BigDecimal) {
      double doubleValue = ((BigDecimal) value).doubleValue();
      if (!Double.isInfinite(doubleValue) && BigDecimal.valueOf(doubleValue).compareTo((BigDecimal) value) == 0) {
        return value.toString();
      } else {
        throw new EdmPrimitiveTypeException("The value '" + value + "' is not valid.");
      }
    } else {
      throw new EdmPrimitiveTypeException("The value type " + value.getClass() + " is not supported.");
    }
  }
}