package se.uu.it.jdooms.objectspace;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Object store, extends ConcurrentHashMap to observe changes to objects
 * @param <K>
 * @param <V>
 */
public class DSObjectSpaceMap<K, V> extends ConcurrentHashMap<K, V> {
    private static final Logger logger = Logger.getLogger(DSObjectSpaceMap.class);
    private final ArrayList<Object> observers = new ArrayList<Object>();

    public DSObjectSpaceMap() {
        super();
    }

    /**
     * Notifies the registered observers
     */
    private void notifyObservers() {
        logger.debug("Notify observers");
        for (Object obj : observers) {
                synchronized (obj) {
                    obj.notify();
                }
        }
    }

    /**
     * Adds an observer
     * @param obj the reference to the observer
     */
    public void addObserver(Object obj) {
        logger.debug("addObserver: " + obj);
        observers.add(obj);
    }

    /**
     * Removes an observer
     * @param obj the reference to the observer
     */
    public void removeObserver(Object obj) {
        logger.debug("removeObserver: " + obj);
        observers.remove(obj);
    }

    /**
     * Puts an object to the object store and notifies the observers
     * @param key       the objectID
     * @param value     the object
     * @return          the previous value associated with key, or null if there was no mapping for key
     */
    public V put(K key, V value) {
        logger.debug("Put key: " + key + " objectspace");
        V result = super.put(key, value);
        notifyObservers();
        return result;
    }

    /**
     * Invalidates all objects with Permission.Read in the object store
     */
    public void selfInvalidate() {
        for (Object obj : super.values()) {
            if (((DSObjectBaseImpl)obj).getPermission() == DSObjectSpace.Permission.Read) {
                ((DSObjectBaseImpl)obj).setValid(false);
            }
        }
    }
}