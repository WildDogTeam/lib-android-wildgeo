/*
 * Wilddog WildGeo Java Library
 *
 * Copyright © 2014 Wilddog - All Rights Reserved
 * https://www.wilddog.com
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binaryform must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY WILDDOG AS IS AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL WILDDOG BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.wilddog.wildgeo;

import com.wilddog.client.DataSnapshot;
import com.wilddog.client.SyncError;
import com.wilddog.client.SyncReference;
import com.wilddog.client.ValueEventListener;
import com.wilddog.wildgeo.core.GeoHash;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A WildGeo instance is used to store geo location data in Wilddog.
 */
public class WildGeo {

    /**
     * A listener that can be used to be notified about a successful write or an error on writing.
     */
    public static interface CompletionListener {
        /**
         * Called once a location was successfully saved on the server or an error occurred. On success, the parameter
         * error will be null; in case of an error, the error will be passed to this method.
         * @param key The key whose location was saved
         * @param error The error or null if no error occurred
         */
        public void onComplete(String key, SyncError error);
    }

    /**
     * A small wrapper class to forward any events to the LocationEventListener.
     */
    private static class LocationValueEventListener implements ValueEventListener {

        private final LocationCallback callback;

        LocationValueEventListener(LocationCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.getValue() == null) {
                this.callback.onLocationResult(dataSnapshot.getKey(), null);
            } else {
                GeoLocation location = WildGeo.getLocationValue(dataSnapshot);
                if (location != null) {
                    this.callback.onLocationResult(dataSnapshot.getKey(), location);
                } else {
                    String message = "WildGeo data has invalid format: " + dataSnapshot.getValue();
                    this.callback.onCancelled(SyncError.fromException(new Throwable(message)));
                }
            }
        }

        @Override
        public void onCancelled(SyncError syncError) {
            this.callback.onCancelled(syncError);
        }
    }

    static GeoLocation getLocationValue(DataSnapshot dataSnapshot) {
        try {
            Map data = (Map) dataSnapshot.getValue(Map.class);
            List<?> location = (List<?>) data.get("l");
            Number latitudeObj = (Number)location.get(0);
            Number longitudeObj = (Number)location.get(1);
            double latitude = latitudeObj.doubleValue();
            double longitude = longitudeObj.doubleValue();
            if (location.size() == 2 && GeoLocation.coordinatesValid(latitude, longitude)) {
                return new GeoLocation(latitude, longitude);
            } else {
                return null;
            }
        } catch (NullPointerException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    private final SyncReference syncReference;
    private final EventRaiser eventRaiser;

    /**
     * Creates a new WildGeo instance at the given Wilddog reference.
     * @param syncReference The Wilddog reference this WildGeo instance uses
     */
    public WildGeo(SyncReference syncReference) {
        this.syncReference = syncReference;
        EventRaiser eventRaiser;
        try {
            eventRaiser = new AndroidEventRaiser();
        } catch (Throwable e) {
            throw new RuntimeException("This sdk needs Android environment.");
        }
        this.eventRaiser = eventRaiser;
    }

    /**
     * @return The Wilddog reference this WildGeo instance uses
     */
    public SyncReference getSyncReference() {
        return this.syncReference;
    }

    SyncReference wilddogRefForKey(String key) {
        return this.syncReference.child(key);
    }

    /**
     * Sets the location for a given key.
     * @param key The key to save the location for
     * @param location The location of this key
     */
    public void setLocation(String key, GeoLocation location) {
        this.setLocation(key, location, null);
    }

    /**
     * Sets the location for a given key.
     * @param key The key to save the location for
     * @param location The location of this key
     * @param completionListener A listener that is called once the location was successfully saved on the server or an
     *                           error occurred
     */
    public void setLocation(final String key, final GeoLocation location, final CompletionListener completionListener) {
        if (key == null) {
            throw new NullPointerException();
        }
        SyncReference keyRef = this.wilddogRefForKey(key);
        GeoHash geoHash = new GeoHash(location);
        Map<String, Object> updates = new HashMap<String, Object>();
        updates.put("g", geoHash.getGeoHashString());
        updates.put("l", new double[]{location.latitude, location.longitude});
        if (completionListener != null) {
            keyRef.setValue(updates, geoHash.getGeoHashString(), new SyncReference.CompletionListener() {
                @Override
                public void onComplete(SyncError error, SyncReference syncReference) {
                    if (completionListener != null) {
                        completionListener.onComplete(key, error);
                    }
                }
            });
        } else {
            keyRef.setValue(updates, geoHash.getGeoHashString());
        }
    }

    /**
     * Removes the location for a key from this WildGeo.
     * @param key The key to remove from this WildGeo
     */
    public void removeLocation(String key) {
        this.removeLocation(key, null);
    }

    /**
     * Removes the location for a key from this WildGeo.
     * @param key The key to remove from this WildGeo
     * @param completionListener A completion listener that is called once the location is successfully removed
     *                           from the server or an error occurred
     */
    public void removeLocation(final String key, final CompletionListener completionListener) {
        if (key == null) {
            throw new NullPointerException();
        }
        SyncReference keyRef = this.wilddogRefForKey(key);
        if (completionListener != null) {
            keyRef.setValue(null, new SyncReference.CompletionListener() {
                @Override
                public void onComplete(SyncError error, SyncReference wilddog) {
                    completionListener.onComplete(key, error);
                }
            });
        } else {
            keyRef.setValue(null);
        }
    }

    /**
     * Gets the current location for a key and calls the callback with the current value.
     *
     * @param key The key whose location to get
     * @param callback The callback that is called once the location is retrieved
     */
    public void getLocation(String key, LocationCallback callback) {
        SyncReference keyWilddog = this.wilddogRefForKey(key);
        LocationValueEventListener valueListener = new LocationValueEventListener(callback);
        keyWilddog.addListenerForSingleValueEvent(valueListener);
    }

    /**
     * Returns a new Query object centered at the given location and with the given radius.
     * @param center The center of the query
     * @param radius The radius of the query, in kilometers
     * @return The new GeoQuery object
     */
    public GeoQuery queryAtLocation(GeoLocation center, double radius) {
        return new GeoQuery(this, center, radius);
    }

    void raiseEvent(Runnable r) {
        this.eventRaiser.raiseEvent(r);
    }
}
