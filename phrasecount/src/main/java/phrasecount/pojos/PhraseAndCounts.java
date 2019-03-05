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

public class PhraseAndCounts extends Counts {
  public String phrase;

  public PhraseAndCounts(String phrase, int docPhraseCount, int totalPhraseCount) {
    super(docPhraseCount, totalPhraseCount);
    this.phrase = phrase;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof PhraseAndCounts) {
      PhraseAndCounts op = (PhraseAndCounts) o;
      return phrase.equals(op.phrase) && super.equals(op);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return super.hashCode() + 31 * phrase.hashCode();
  }
}
