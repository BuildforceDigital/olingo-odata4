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
package org.apache.olingo.server.core.uri.queryoption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmStructuredType;
import org.apache.olingo.server.api.uri.queryoption.ApplyItem;
import org.apache.olingo.server.api.uri.queryoption.ApplyOption;
import org.apache.olingo.server.api.uri.queryoption.SystemQueryOptionKind;

public class ApplyOptionImpl extends SystemQueryOptionImpl implements ApplyOption {

  private List<ApplyItem> transformations = new ArrayList<ApplyItem>();
  private EdmStructuredType edmStructuredType;

  public ApplyOptionImpl() {
    setKind(SystemQueryOptionKind.APPLY);
  }

  @Override
  public List<ApplyItem> getApplyItems() {
    return Collections.unmodifiableList(transformations);
  }

  public ApplyOptionImpl add(ApplyItem transformation) {
    transformations.add(transformation);
    return this;
  }

  public void setEdmStructuredType(EdmStructuredType referencedType) {
      edmStructuredType = referencedType;
  }
  
  @Override
  public EdmStructuredType getEdmStructuredType() {
    return edmStructuredType;
  }
}
