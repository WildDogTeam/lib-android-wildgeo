package com.wilddog.wildgeo.example;

import com.wilddog.client.Wilddog;
import com.wilddog.client.WilddogError;
import com.wilddog.wildgeo.GeoLocation;
import com.wilddog.wildgeo.GeoQuery;
import com.wilddog.wildgeo.GeoQueryEventListener;
import com.wilddog.wildgeo.LocationCallback;
import com.wilddog.wildgeo.WildGeo;

public class Example {

    public static void main(String[] args) throws InterruptedException {
        // new Wilddog ref
        Wilddog wilddog = new Wilddog("https://geofire.wilddogio.com/beijing/_geofire");
        WildGeo wildGeo = new WildGeo(wilddog);
        
        GeoQuery query = wildGeo.queryAtLocation(new GeoLocation(40.897614,116.408032),1 );
        
        //add listener
        QueryListener listener1 = new QueryListener("listener1");
        query.addGeoQueryEventListener(listener1);
        query.addGeoQueryEventListener(new QueryListener("listener2"));
        
        // set a location
        wildGeo.setLocation("wildgeo-hq", new GeoLocation(37.7853999, -122.4056973),new WildGeo.CompletionListener() {
            public void onComplete(String key, WilddogError error) {
                if (error != null) {
                    System.err.println("There was an error saving the location to wildgeo: " + error);
                } else {
                    System.out.println("Location saved on server successfully!");
                }
            }
        });
        
        Thread.sleep(5000);
        
        wildGeo.getLocation("wildgeo-hq", new LocationCallback() {
            public void onLocationResult(String key, GeoLocation location) {
                if (location != null) {
                    System.out.println(String.format("The location for key %s is [%f,%f]", key, location.latitude, location.longitude));
                } else {
                    System.out.println(String.format("There is no location for key %s in wildgeo", key));
                }
            }
            public void onCancelled(WilddogError wilddogError) {
                System.err.println("There was an error getting the wildgeo location: " + wilddogError);
            }
        });
        
        //remove listener1
        query.removeGeoQueryEventListener(listener1);
        
        Thread.sleep(5000);
        
        System.out.println("update center location");
        query.setCenter(new GeoLocation(39.897614,116.408032));
        query.setRadius(1);
        
        
        Thread.sleep(5000);
        query.removeAllListeners();
        
        //run for another 60 seconds
        Thread.sleep(60000);
    }
   
}

class QueryListener implements GeoQueryEventListener{
    
    private String listenerName;

    public QueryListener(String listenerName) {
        this.listenerName = listenerName;
    }
    
    @Override
    public void onKeyEntered(String key, GeoLocation location) {
        System.out.println(String.format("%s %s entered at [%f, %f]", listenerName,key, location.latitude, location.longitude));
    }

    @Override
    public void onKeyExited(String key) {
        System.out.println(String.format("%s　%s exited", listenerName,key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation location) {
        System.out.println(String.format("%s　%s moved to [%f, %f]", listenerName,key, location.latitude, location.longitude));
    }

    @Override
    public void onGeoQueryReady() {
        System.out.println(listenerName+" All initial key entered events have been fired!");
    }

    @Override
    public void onGeoQueryError(WilddogError error) {
        System.err.println(listenerName+"　There was an error querying locations: " + error.getMessage());
    }
    
}

