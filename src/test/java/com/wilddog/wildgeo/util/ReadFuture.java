package com.wilddog.wildgeo.util;

import com.wilddog.client.DataSnapshot;
import com.wilddog.client.WilddogError;
import com.wilddog.client.Query;
import com.wilddog.client.ValueEventListener;
import com.wilddog.wildgeo.core.SimpleFuture;

public class ReadFuture extends SimpleFuture<Object> {

    public ReadFuture(Query ref) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ReadFuture.this.put(dataSnapshot);
            }

            @Override
            public void onCancelled(WilddogError wilddogError) {
                ReadFuture.this.put(wilddogError);
            }
        });
    }
}
