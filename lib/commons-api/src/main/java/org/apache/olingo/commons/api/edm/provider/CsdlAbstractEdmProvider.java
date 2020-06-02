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
package org.apache.olingo.commons.api.edm.provider;

import java.util.List;

import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataException;

/**
 * Dummy implementation of {@link CsdlEdmProvider}
 */
public abstract class CsdlAbstractEdmProvider implements CsdlEdmProvider {

  @Override
  public CsdlEnumType getEnumType(FullQualifiedName enumTypeName) throws ODataException {
    return null;
  }

  @Override
  public CsdlTypeDefinition getTypeDefinition(FullQualifiedName typeDefinitionName) throws ODataException {
    return null;
  }

  @Override
  public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
    return null;
  }

  @Override
  public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) throws ODataException {
    return null;
  }

  @Override
  public List<CsdlAction> getActions(FullQualifiedName actionName) throws ODataException {
    return null;
  }

  @Override
  public List<CsdlFunction> getFunctions(FullQualifiedName functionName) throws ODataException {
    return null;
  }

  @Override
  public CsdlTerm getTerm(FullQualifiedName termName) throws ODataException {
    return null;
  }

  @Override
  public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName)
      throws ODataException {
    return null;
  }

  @Override
  public CsdlSingleton getSingleton(FullQualifiedName entityContainer, String singletonName)
      throws ODataException {
    return null;
  }

  @Override
  public CsdlActionImport getActionImport(FullQualifiedName entityContainer, String actionImportName)
      throws ODataException {
    return null;
  }

  @Override
  public CsdlFunctionImport getFunctionImport(FullQualifiedName entityContainer, String functionImportName)
      throws ODataException {
    return null;
  }

  @Override
  public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName)
      throws ODataException {
    return null;
  }

  @Override
  public List<CsdlAliasInfo> getAliasInfos() throws ODataException {
    return null;
  }

  @Override
  public List<CsdlSchema> getSchemas() throws ODataException {
    return null;
  }

  @Override
  public CsdlEntityContainer getEntityContainer() throws ODataException {
    return null;
  }

  @Override
  public CsdlAnnotations getAnnotationsGroup(FullQualifiedName targetName, String qualifier)
      throws ODataException {
    return null;
  }

}