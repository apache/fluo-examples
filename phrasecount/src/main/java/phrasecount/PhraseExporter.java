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

package phrasecount;

import java.util.function.Consumer;

import org.apache.accumulo.core.data.Mutation;
import org.apache.fluo.recipes.accumulo.export.AccumuloExporter;
import org.apache.fluo.recipes.core.export.SequencedExport;
import phrasecount.pojos.Counts;
import phrasecount.query.PhraseCountTable;

/**
 * Export code that converts {@link Counts} objects from the export queue to Mutations that are
 * written to Accumulo.
 */
public class PhraseExporter extends AccumuloExporter<String, Counts> {

  @Override
  protected void translate(SequencedExport<String, Counts> export, Consumer<Mutation> consumer) {
    String phrase = export.getKey();
    long seq = export.getSequence();
    Counts counts = export.getValue();
    consumer.accept(PhraseCountTable.createMutation(phrase, seq, counts));
  }
}
