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

import com.google.common.base.Objects;

public class Counts {
  // number of documents a phrase was seen in
  public final long docPhraseCount;
  // total times a phrase was seen in all documents
  public final long totalPhraseCount;

  public Counts() {
    docPhraseCount = 0;
    totalPhraseCount = 0;
  }

  public Counts(long docPhraseCount, long totalPhraseCount) {
    this.docPhraseCount = docPhraseCount;
    this.totalPhraseCount = totalPhraseCount;
  }

  public Counts add(Counts other) {
    return new Counts(this.docPhraseCount + other.docPhraseCount,
        this.totalPhraseCount + other.totalPhraseCount);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Counts) {
      Counts opc = (Counts) o;
      return opc.docPhraseCount == docPhraseCount && opc.totalPhraseCount == totalPhraseCount;
    }

    return false;
  }

  @Override
  public int hashCode() {
    return (int) (993 * totalPhraseCount + 17 * docPhraseCount);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).add("documents", docPhraseCount)
        .add("total", totalPhraseCount).toString();
  }
}
