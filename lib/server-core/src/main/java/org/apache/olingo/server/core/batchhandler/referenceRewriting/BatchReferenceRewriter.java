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
package org.apache.olingo.server.core.batchhandler.referenceRewriting;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.deserializer.batch.BatchDeserializerException;
import org.apache.olingo.server.api.deserializer.batch.BatchDeserializerException.MessageKeys;

public class BatchReferenceRewriter {
  private static final String REG_EX_REFERENCE = "\\$(.*)(/.*)?";
  private static final Pattern REFERENCE_PATTERN = Pattern.compile(REG_EX_REFERENCE);

  private Map<String, String> contentIdMapping = new HashMap<>();

  public String getReferenceInURI(ODataRequest request) {
    Matcher matcher = REFERENCE_PATTERN.matcher(removeSlash(removeSlash(request.getRawODataPath(), true), false));

    return (matcher.matches()) ? matcher.group(1) : null;
  }

  public void replaceReference(ODataRequest request) {
    String reference = getReferenceInURI(request);

    if (reference != null) {
      String replacement = contentIdMapping.get(reference);

      if (replacement != null) {
        replaceContentIdReference(request, reference, replacement);
      } else {
        throw new ODataRuntimeException("Required Content-Id for reference \"" + reference + "\" not found.");
      }
    }
  }

  private void replaceContentIdReference(ODataRequest request, String contentId, String resourceUri) {
    String newUri = request.getRawODataPath().replace("/$" + contentId, resourceUri);
    request.setRawODataPath(newUri);
    request.setRawRequestUri(request.getRawBaseUri() + "/" + newUri);
  }

  public void addMapping(ODataRequest request, ODataResponse response)
      throws BatchDeserializerException {
    String resourceUri = getODataPath(request, response);
    String contentId = request.getHeader(HttpHeader.CONTENT_ID);

    contentIdMapping.put(contentId, resourceUri);
  }

  private String getODataPath(ODataRequest request, ODataResponse response)
      throws BatchDeserializerException {
    String resourceUri = null;

    if (request.getMethod() == HttpMethod.POST) {
      // Create entity
      // The URI of the new resource will be generated by the server and published in the location header
      String locationHeader = response.getHeader(HttpHeader.LOCATION);
      resourceUri = locationHeader == null ? null : parseODataPath(locationHeader, request.getRawBaseUri());
    } else {
      // Update, Upsert (PUT, PATCH, Delete)
      // These methods still addresses a given resource, so we use the URI given by the request
      resourceUri = request.getRawODataPath();
    }

    return resourceUri;
  }

  private String parseODataPath(String uri, String rawBaseUri) throws BatchDeserializerException {
    if (uri.indexOf(rawBaseUri) == 0) {
      return uri.substring(rawBaseUri.length());
    } else {
      throw new BatchDeserializerException("Invalid base uri or uri", MessageKeys.INVALID_URI, "0");
    }
  }

  private String removeSlash(String rawODataPath, boolean first) {
    int indexOfSlash = rawODataPath.indexOf('/');
    if (first) {
      return (indexOfSlash == 0) ? rawODataPath.substring(1) : rawODataPath;
    } else {
      return (indexOfSlash != -1) ? rawODataPath.substring(0, indexOfSlash) : rawODataPath;
    }
  }
}
