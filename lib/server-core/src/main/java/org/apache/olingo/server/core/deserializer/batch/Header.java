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
package org.apache.olingo.server.core.deserializer.batch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Header implements Iterable<HeaderField>, Cloneable {
  private Map<String, HeaderField> headers = new HashMap<>();
  private int lineNumber;

  public Header(int lineNumer) {
    lineNumber = lineNumer;
  }

  public void addHeader(String name, String value, int lineNumber) {
    HeaderField headerField = getHeaderFieldOrDefault(name, lineNumber);
    List<String> headerValues = headerField.getValues();

    if (!headerValues.contains(value)) {
      headerValues.add(value);
    }
  }

  public void addHeader(String name, List<String> values, int lineNumber) {
    HeaderField headerField = getHeaderFieldOrDefault(name, lineNumber);
    List<String> headerValues = headerField.getValues();

    for (String value : values) {
      if (!headerValues.contains(value)) {
        headerValues.add(value);
      }
    }
  }

  public void replaceHeaderField(HeaderField headerField) {
    headers.put(headerField.getFieldName().toLowerCase(Locale.ENGLISH), headerField);
  }

  public boolean exists(String name) {
    HeaderField field = headers.get(name.toLowerCase(Locale.ENGLISH));

    return field != null && !field.getValues().isEmpty();
  }

  public void removeHeader(String name) {
    headers.remove(name.toLowerCase(Locale.ENGLISH));
  }

  public String getHeader(String name) {
    HeaderField headerField = getHeaderField(name);

    return (headerField == null) ? null : headerField.getValue();
  }

  public List<String> getHeaders(String name) {
    HeaderField headerField = getHeaderField(name);

    return (headerField == null) ? new ArrayList<>() : headerField.getValues();
  }

  public HeaderField getHeaderField(String name) {
    return headers.get(name.toLowerCase(Locale.ENGLISH));
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public Map<String, String> toSingleMap() {
    Map<String, String> singleMap = new HashMap<>();

    for (Map.Entry<String, HeaderField> entries : headers.entrySet()) {
      HeaderField field = entries.getValue();
      singleMap.put(field.getFieldName(), getHeader(entries.getKey()));
    }

    return singleMap;
  }

  public Map<String, List<String>> toMultiMap() {
    Map<String, List<String>> singleMap = new HashMap<>();

    for (Map.Entry<String, HeaderField> entries : headers.entrySet()) {
      HeaderField field = entries.getValue();
      singleMap.put(field.getFieldName(), field.getValues());
    }

    return singleMap;
  }

  private HeaderField getHeaderFieldOrDefault(String name, int lineNumber) {
    HeaderField headerField = headers.get(name.toLowerCase(Locale.ENGLISH));
    if (headerField == null) {
      headerField = new HeaderField(name, lineNumber);
      headers.put(name.toLowerCase(Locale.ENGLISH), headerField);
    }

    return headerField;
  }

  @Override
  public Header clone() throws CloneNotSupportedException{
    Header clone = (Header) super.clone();
    clone.lineNumber = lineNumber;
    clone.headers = new HashMap<>();
    for (Map.Entry<String, HeaderField> entries : headers.entrySet()) {
      clone.headers.put(entries.getKey(), entries.getValue().clone());
    }

    return clone;
  }

  @Override
  public Iterator<HeaderField> iterator() {
    return new Iterator<HeaderField>() {
      Iterator<String> keyIterator = headers.keySet().iterator();

      @Override
      public boolean hasNext() {
        return keyIterator.hasNext();
      }

      @Override
      public HeaderField next() {
        return headers.get(keyIterator.next());
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public static List<String> splitValuesByComma(String headerValue) {
    List<String> singleValues = new ArrayList<>();

    String[] parts = headerValue.split(",");
    for (String value : parts) {
      singleValues.add(value.trim());
    }

    return singleValues;
  }
}