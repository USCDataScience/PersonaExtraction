/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.usc.ir;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.list.SetUniqueList;

public class Persona {

  private List<String> usernames;

  private String pageId;

  @SuppressWarnings("unchecked")
  public Persona() {
    this.pageId = null;
    this.usernames = (List<String>)SetUniqueList.decorate(new ArrayList<String>());
  }

  /**
   * @return the usernames
   */
  public List<String> getUsernames() {
    return usernames;
  }

  /**
   * @param usernames
   *          the usernames to set
   */
  public void setUsernames(List<String> usernames) {
    this.usernames = usernames;
  }

  /**
   * @return the pageId
   */
  public String getPageId() {
    return pageId;
  }

  /**
   * @param pageId the pageId to set
   */
  public void setPageId(String pageId) {
    this.pageId = pageId;
  }



  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[pageId=");
    builder.append(this.pageId);
    builder.append(",personas=");
    builder.append(this.usernames.toString());
    builder.append("]");
    return builder.toString();
    
  }
 
}
