package hudson.plugins.erlangcover;

import static org.junit.Assert.*;

import org.junit.Test;

public class CoverPublisherTest {

	@Test
	public void testGetOnlyStable() {
		CoverPublisher testObjectTrue = new CoverPublisher(null, true, false, false, false, false, null);
		CoverPublisher testObjectFalse = new CoverPublisher(null, false, false, false, false, false, null);
		assertTrue(testObjectTrue.getOnlyStable());
		assertTrue(!testObjectFalse.getOnlyStable());
	}
	
	@Test
	public void testGetFailUnhealthy() {
		CoverPublisher testObjectTrue = new CoverPublisher(null, false, true, false, false, false, null);
		CoverPublisher testObjectFalse = new CoverPublisher(null, false, false, false, false, false, null);
		assertTrue(testObjectTrue.getFailUnhealthy());
		assertTrue(!testObjectFalse.getFailUnhealthy());
	}

	@Test
	public void testGetFailUnstable() {
		CoverPublisher testObjectTrue = new CoverPublisher(null, false, false, true, false, false, null);
		CoverPublisher testObjectFalse = new CoverPublisher(null, false, false, false, false, false, null);
		assertTrue(testObjectTrue.getFailUnstable());
		assertTrue(!testObjectFalse.getFailUnstable());
	}

	@Test
	public void testGetAutoUpdateHealth() {
		CoverPublisher testObjectTrue = new CoverPublisher(null, false, false, false, true, false, null);
		CoverPublisher testObjectFalse = new CoverPublisher(null, false, false, false, false, false, null);
		assertTrue(testObjectTrue.getAutoUpdateHealth());
		assertTrue(!testObjectFalse.getAutoUpdateHealth());
	}

	@Test
	public void testGetAutoUpdateStability() {
		CoverPublisher testObjectTrue = new CoverPublisher(null, false, false, false, false, true, null);
		CoverPublisher testObjectFalse = new CoverPublisher(null, false, false, false, false, false, null);
		assertTrue(testObjectTrue.getAutoUpdateStability());
		assertTrue(!testObjectFalse.getAutoUpdateStability());
	}

}
