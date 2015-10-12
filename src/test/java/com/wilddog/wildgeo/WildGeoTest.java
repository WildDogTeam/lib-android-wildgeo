package com.wilddog.wildgeo;

import com.wilddog.client.DataSnapshot;
import com.wilddog.client.Wilddog;
import com.wilddog.client.WilddogError;
import com.wilddog.wildgeo.WildGeo;
import com.wilddog.wildgeo.GeoLocation;
import com.wilddog.wildgeo.LocationCallback;
import com.wilddog.wildgeo.util.ReadFuture;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@RunWith(JUnit4.class)
public class WildGeoTest extends RealDataTest {
    @Rule
    public org.junit.rules.ExpectedException exception = ExpectedException.none();

    @Test
    public void wildGeoSetsLocations() throws InterruptedException, ExecutionException, TimeoutException {
        WildGeo wildGeo = newTestWildGeo();
        setLoc(wildGeo, "loc1", 0, 0);
        setLoc(wildGeo, "loc2", 50, 50);
        setLoc(wildGeo, "loc3", -90, -90, true);

        Future<Object> future = new ReadFuture(wildGeo.getWilddog());
        Map<String, Object> expected = new HashMap<String, Object>();
        expected.put("loc1", new HashMap<String, Object>() {{
            put("l", Arrays.asList(0, 0));
            put("g", "7zzzzzzzzz");
        }});
        expected.put("loc2", new HashMap<String, Object>() {{
            put("l", Arrays.asList(50, 50));
            put("g", "v0gs3y0zh7");
        }});
        expected.put("loc3", new HashMap<String, Object>() {{
            put("l", Arrays.asList(-90, -90));
            put("g", "1bpbpbpbpb");
        }});
        Object result = future.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS);
        Assert.assertEquals(expected, ((DataSnapshot)result).getValue());
    }

    @Test
    public void getLocationReturnsCorrectLocation() throws InterruptedException, ExecutionException, TimeoutException {
        WildGeo wildGeo = newTestWildGeo();

        TestCallback testCallback1 = new TestCallback();
        wildGeo.getLocation("loc1", testCallback1);
        Assert.assertEquals(TestCallback.noLocation("loc1"), testCallback1.getCallbackValue());

        TestCallback testCallback2 = new TestCallback();
        setLoc(wildGeo, "loc1", 0, 0, true);
        wildGeo.getLocation("loc1", testCallback2);
        Assert.assertEquals(TestCallback.location("loc1", 0, 0), testCallback2.getCallbackValue());

        TestCallback testCallback3 = new TestCallback();
        setLoc(wildGeo, "loc2", 1, 1, true);
        wildGeo.getLocation("loc2", testCallback3);
        Assert.assertEquals(TestCallback.location("loc2", 1, 1), testCallback3.getCallbackValue());

        TestCallback testCallback4 = new TestCallback();
        setLoc(wildGeo, "loc1", 5, 5, true);
        wildGeo.getLocation("loc1", testCallback4);
        Assert.assertEquals(TestCallback.location("loc1", 5, 5), testCallback4.getCallbackValue());

        TestCallback testCallback5 = new TestCallback();
        removeLoc(wildGeo, "loc1");
        wildGeo.getLocation("loc1", testCallback5);
        Assert.assertEquals(TestCallback.noLocation("loc1"), testCallback5.getCallbackValue());
    }

    @Test
    public void getLocationOnWrongDataReturnsError() throws InterruptedException {
        WildGeo wildGeo = newTestWildGeo();
        setValueAndWait(wildGeo.wilddogRefForKey("loc1"), "NaN");

        final Semaphore semaphore = new Semaphore(0);
        wildGeo.getLocation("loc1", new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                Assert.fail("This should not be a valid location!");
            }

            @Override
            public void onCancelled(WilddogError wilddogError) {
                semaphore.release();
            }
        });
        semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS);

        setValueAndWait(wildGeo.wilddogRefForKey("loc2"), new HashMap<String, Object>() {{
           put("l", 10);
           put("g", "abc");
        }});

        wildGeo.getLocation("loc2", new LocationCallback() {
            @Override
            public void onLocationResult(String key, GeoLocation location) {
                Assert.fail("This should not be a valid location!");
            }

            @Override
            public void onCancelled(WilddogError wilddogError) {
                semaphore.release();
            }
        });
        semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    @Test
    public void invalidCoordinatesThrowException() {
        WildGeo wildGeo = newTestWildGeo();
        try {
            wildGeo.setLocation("test", new GeoLocation(-91, 90));
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            wildGeo.setLocation("test", new GeoLocation(0, -180.1));
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
        try {
            wildGeo.setLocation("test", new GeoLocation(0, 181.1));
            Assert.fail("Did not throw illegal argument exception!");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void locationWorksWithLongs() throws InterruptedException, ExecutionException, TimeoutException {
        WildGeo wildGeo = newTestWildGeo();
        Wilddog wilddog = wildGeo.wilddogRefForKey("loc");

        final Semaphore semaphore = new Semaphore(0);
        wilddog.setValue(new HashMap<String, Object>() {{
            put("l", Arrays.asList(1L, 2L));
            put("g", "7zzzzzzzzz"); // this is wrong but we don't care in this test
        }}, "7zzzzzzzzz", new Wilddog.CompletionListener() {
            @Override
            public void onComplete(WilddogError error, Wilddog wilddog) {
                semaphore.release();
            }
        });
        semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS);

        TestCallback testCallback = new TestCallback();
        wildGeo.getLocation("loc", testCallback);
        Assert.assertEquals(TestCallback.location("loc", 1, 2), testCallback.getCallbackValue());
    }
}
