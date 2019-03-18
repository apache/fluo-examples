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

package webindex.data.fluo;

import java.net.MalformedURLException;

import org.junit.Test;

import webindex.core.models.Page;
import webindex.core.models.URL;


public class PageLoaderTest {

	@Test
	public void testUpdatePageWithNullPage() {
		PageLoader.updatePage((Page) null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testUpdatePageWithEmptyPage() {
		PageLoader.updatePage(Page.EMPTY);
	}
	
	@Test(expected = NullPointerException.class)
	public void testDeletePageThrowsNPEifNullURL() throws MalformedURLException{
		PageLoader.deletePage((URL)null);
	}
	
	@Test
	public void testDeletePageWithBlankURL() throws MalformedURLException{
			URL url = new URL("","","",0,false,false);
			PageLoader.deletePage(url);
	}
	
	@Test
	public void testLoadWithNullTxCtx() throws MalformedURLException{
		try {
			URL url = new URL("www.apache.org", "www.apache.org", "http://www.apache.org", 80, false, false);
			PageLoader loader = PageLoader.deletePage(url);
			loader.load(null, null);
		} catch (Exception e) {
			if(e instanceof NullPointerException)
				throw new NullPointerException(e.getMessage());
		}
	}
}
