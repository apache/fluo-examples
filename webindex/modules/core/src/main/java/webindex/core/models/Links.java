/*
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

package webindex.core.models;

import java.util.ArrayList;
import java.util.List;

public class Links {

  private String url;
  private String linkType;
  private String next = "";
  private Integer pageNum;
  private Long total;
  private List<Link> links = new ArrayList<>();

  public Links() {
    // Jackson deserialization
  }

  public Links(String url, String linkType, Integer pageNum) {
    this.url = url;
    this.linkType = linkType;
    this.pageNum = pageNum;
  }

  public Long getTotal() {
    return total;
  }

  public void setTotal(Long total) {
    this.total = total;
  }

  public String getUrl() {
    return url;
  }

  public List<Link> getLinks() {
    return links;
  }

  public void addLink(Link link) {
    links.add(link);
  }

  public String getLinkType() {
    return linkType;
  }

  public Integer getPageNum() {
    return pageNum;
  }

  public String getNext() {
    return next;
  }

  public void setNext(String next) {
    this.next = next;
  }
}
