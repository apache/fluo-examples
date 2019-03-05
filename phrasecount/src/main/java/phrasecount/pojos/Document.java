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

package phrasecount.pojos;

import java.util.HashMap;
import java.util.Map;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class Document {
  // the location where the document came from. This is needed inorder to detect when a document
  // changes.
  private String uri;

  // the text of a document.
  private String content;

  private String hash = null;

  public Document(String uri, String content) {
    this.content = content;
    this.uri = uri;
  }

  public String getURI() {
    return uri;
  }

  public String getHash() {
    if (hash != null) {
      return hash;
    }

    Hasher hasher = Hashing.sha1().newHasher();
    String[] tokens = content.toLowerCase().split("[^\\p{Alnum}]+");

    for (String token : tokens) {
      hasher.putString(token);
    }

    return hash = hasher.hash().toString();
  }

  public Map<String, Integer> getPhrases() {
    String[] tokens = content.toLowerCase().split("[^\\p{Alnum}]+");

    Map<String, Integer> phrases = new HashMap<>();
    for (int i = 3; i < tokens.length; i++) {
      String phrase = tokens[i - 3] + " " + tokens[i - 2] + " " + tokens[i - 1] + " " + tokens[i];
      Integer old = phrases.put(phrase, 1);
      if (old != null) {
        phrases.put(phrase, 1 + old);
      }
    }

    return phrases;
  }

  public String getContent() {
    return content;
  }
}
