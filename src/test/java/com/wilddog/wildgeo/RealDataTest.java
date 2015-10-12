package com.wilddog.wildgeo;

import com.wilddog.client.*;
import com.wilddog.wildgeo.WildGeo;
import com.wilddog.wildgeo.GeoLocation;
import com.wilddog.wildgeo.core.SimpleFuture;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RealDataTest {

    Wilddog wilddog;

    @Before
    public void setup() {
        Config cfg = Wilddog.getDefaultConfig();
        if (!cfg.isFrozen()) {
            cfg.setLogLevel(Logger.Level.DEBUG);
        }
       /* this.wilddog = new Wilddog(String.format("https://%s.wilddogio-demo.com",
                TestHelpers.randomAlphaNumericString(16)));*/
        this.wilddog = new Wilddog("https://myoffice1.wilddogio.com/");
    }

    public WildGeo newTestWildGeo() {
        return new WildGeo(this.wilddog.child(TestHelpers.randomAlphaNumericString(16)));
    }

    protected void setLoc(WildGeo wildGeo, String key, double latitude, double longitude) {
        setLoc(wildGeo, key, latitude, longitude, false);
    }

    protected void removeLoc(WildGeo wildGeo, String key) {
        removeLoc(wildGeo, key, false);
    }

    protected void setValueAndWait(Wilddog wilddog, Object value) {
        final SimpleFuture<WilddogError> futureError = new SimpleFuture<WilddogError>();
        wilddog.setValue(value, new Wilddog.CompletionListener() {
            @Override
            public void onComplete(WilddogError error, Wilddog wilddog) {
                futureError.put(error);
            }
        });
        try {
            Assert.assertNull(futureError.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            Assert.fail("Timeout occured!");
        }
    }

    protected void setLoc(WildGeo wildGeo, String key, double latitude, double longitude, boolean wait) {
        final SimpleFuture<WilddogError> futureError = new SimpleFuture<WilddogError>();
        wildGeo.setLocation(key, new GeoLocation(latitude, longitude), new WildGeo.CompletionListener() {
            @Override
            public void onComplete(String key, WilddogError wilddogError) {
                futureError.put(wilddogError);
            }
        });
        if (wait) {
            try {
                Assert.assertNull(futureError.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Assert.fail("Timeout occured!");
            }
        }
    }

    protected void removeLoc(WildGeo wildGeo, String key, boolean wait) {
        final SimpleFuture<WilddogError> futureError = new SimpleFuture<WilddogError>();
        wildGeo.removeLocation(key, new WildGeo.CompletionListener() {
            @Override
            public void onComplete(String key, WilddogError wilddogError) {
                futureError.put(wilddogError);
            }
        });
        if (wait) {
            try {
                Assert.assertNull(futureError.get(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                Assert.fail("Timeout occured!");
            }
        }
    }

    protected void waitForWildGeoReady(WildGeo wildGeo) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        wildGeo.getWilddog().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                semaphore.release();
            }

            @Override
            public void onCancelled(WilddogError wilddogError) {
                Assert.fail("Wilddog error: " + wilddogError);
            }
        });
        Assert.assertTrue("Timeout occured!", semaphore.tryAcquire(TestHelpers.TIMEOUT_SECONDS, TimeUnit.SECONDS));
    }

    @After
    public void teardown() {
        this.wilddog.setValue(null);
        this.wilddog = null;
    }
}
