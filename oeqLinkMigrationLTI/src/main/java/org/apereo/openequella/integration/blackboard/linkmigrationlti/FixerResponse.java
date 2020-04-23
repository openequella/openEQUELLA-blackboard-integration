/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0, (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apereo.openequella.integration.blackboard.linkmigrationlti;

public class FixerResponse {
  private boolean validResponse = false;
  private String newUrl;
  private String newName;
  private String newDescription;
  private String newBody;

  public boolean isValidResponse() {
	return validResponse;
  }

  public void setValidResponse(boolean validResponse) {
	this.validResponse = validResponse;
  }

  public String getNewUrl() {
	return newUrl;
  }

  public void setNewUrl(String newUrl) {
	this.newUrl = newUrl;
  }

  public String getNewName() {
	return newName;
  }

  public void setNewName(String newName) {
	this.newName = newName;
  }

  public String getNewDescription() {
	return newDescription;
  }

  public void setNewDescription(String newDescription) {
	this.newDescription = newDescription;
  }

  public String getNewBody() {
	return newBody;
  }

  public void setNewBody(String newBody) {
	this.newBody = newBody;
  }
}
