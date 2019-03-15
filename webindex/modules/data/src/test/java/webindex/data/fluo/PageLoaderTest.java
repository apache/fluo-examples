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

import org.junit.Assert;
import org.junit.Test;

import webindex.core.models.Page;
import webindex.core.models.URL;


public class PageLoaderTest {

	@Test
	public void testUpdatePageWithNullPage() {
		PageLoader.updatePage((Page) null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testEmptyPageThrowsIllegalArgument() {
		Page p = Page.EMPTY;
		PageLoader.updatePage(p);
	}
	
	@Test(expected = NullPointerException.class)
	public void testDeletePageThrowsNPEifNullURL() {
		try {
			PageLoader.deletePage((URL)null);
		} catch (MalformedURLException e) {
			
		}
		
	}
	
	@Test
	public void testDeletePageWithBlankURL() {
		URL url = new URL("","","",0,false,false);
		try {
			PageLoader.deletePage(url);
		} catch (MalformedURLException e) {
			
		}
	}
	
	//This test wont make sense until PageLoader.deletePage() handles non null empty URLS
	@Test
	public void testLoadWithNullTxCtx() {
		URL url = new URL("","","",0,false,false);
		PageLoader loader;
		try {
			loader = PageLoader.deletePage(url);
			loader.load(null, null);
		} catch (NullPointerException e) {	
			Assert.fail();
		} catch (MalformedURLException e) {
			// from PageLoader.deletePage()
		} catch (Exception e) {
			// from PageLoader.load()
		}
	}
}
