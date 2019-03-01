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

package stresso.trie;

import org.apache.fluo.api.data.Column;
import org.apache.fluo.recipes.core.types.StringEncoder;
import org.apache.fluo.recipes.core.types.TypeLayer;

/**
 *
 */
public class Constants {

  public static final TypeLayer TYPEL = new TypeLayer(new StringEncoder());

  public static final Column COUNT_SEEN_COL = new Column("count", "seen");
  public static final Column COUNT_WAIT_COL = new Column("count", "wait");

  public static final String NODE_SIZE_PROP = "trie.nodeSize";
  public static final String STOP_LEVEL_PROP = "trie.stopLevel";
}
