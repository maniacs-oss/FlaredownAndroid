package com.flaredown.flaredownApp.Helpers.APIv2.EndPoints.CheckIns;

import java.util.Collection;

/**
 * Used for storing a collection of unique Trackables.
 */
public class TrackableCollection <T extends Trackable> extends ObservableHashSet<T> {
    @Override
    public boolean add(T object) {
        if(this.contains(object)) throw new IllegalStateException("Collection already has a Trackable with this id (" + object.getTrackableId() + ")."); // Force unique trackables.
        if(object.getTrackableId() == null)
            throw new IllegalStateException("Trackable ID cannot be null"); // Trackables cannot have a null id.
        return super.add(object);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        for (T object : collection) {
            if(this.contains(object) || collection.contains(object)) throw new IllegalStateException("Collection already has a Trackable with this id(" + object.getTrackableId() + ").");
            if(object.getTrackableId() == null) throw new IllegalStateException("Trackable ID cannot be null");
        }
        return super.addAll(collection);
    }
}