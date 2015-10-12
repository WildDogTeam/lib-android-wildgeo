package com.wilddog.wildgeo.example;

import com.wilddog.client.Wilddog;
import com.wilddog.client.WilddogError;
import com.wilddog.wildgeo.WildGeo;
import com.wilddog.wildgeo.GeoLocation;
import com.wilddog.wildgeo.GeoQuery;
import com.wilddog.wildgeo.GeoQueryEventListener;

public class Example {

    public static void main(String[] args) throws InterruptedException {
        Wilddog wilddog = new Wilddog("https://geofire-v3.wilddogio.com/geofire");
        WildGeo wildGeo = new WildGeo(wilddog);
        GeoQuery query = wildGeo.queryAtLocation(new GeoLocation(37.7, -122.4), 10);
        query.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                System.out.println(String.format("%s entered at [%f, %f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onKeyExited(String key) {
                System.out.println(String.format("%s exited", key));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                System.out.println(String.format("%s moved to [%f, %f]", key, location.latitude, location.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                System.out.println("All initial key entered events have been fired!");
            }

            @Override
            public void onGeoQueryError(WilddogError error) {
                System.err.println("There was an error querying locations: " + error.getMessage());
            }
        });
        // run for another 60 seconds
        Thread.sleep(60000);
    }
}
