package webindex.data.fluo;

import java.net.MalformedURLException;

import org.junit.Assert;
import org.junit.Test;

import webindex.core.models.Page;
import webindex.core.models.URL;

@SuppressWarnings("unused")
public class PageLoaderTest {

	@Test
	public void testUpdatePageWithNullPage() {
		Page p = null;
		PageLoader loader = PageLoader.updatePage(p);
		loader = null;
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testEmptyPageThrowsIllegalArgument() {
		Page p = Page.EMPTY;
		PageLoader loader = PageLoader.updatePage(p);
		loader = null;
	}
	
	@Test(expected = NullPointerException.class)
	public void testDeleteFailsWithNullUrl() {
		try {
			PageLoader loader = PageLoader.deletePage(null);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testDeletePage() {
		URL url = new URL("","","",0,false,false);
		try {
			PageLoader loader = PageLoader.deletePage(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testLoadWithNullTxCtx() {
		URL url = new URL("","","",0,false,false);
		PageLoader loader;
		try {
			loader = PageLoader.deletePage(url);
			loader.load(null, null);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
			Assert.fail();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
}
