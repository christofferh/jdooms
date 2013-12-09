package se.uu.it.jdooms.objectspace;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static se.uu.it.jdooms.communication.DSObjectComm.*;

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
        if (observers.size() > 0) {
            logger.debug("Notify observers");
        }
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
        logger.debug("self invalidating");
        for (Entry<K, V> entry : super.entrySet()) {
            if (((DSObjectBase)entry.getValue()).getPermission() == DSObjectSpace.Permission.Read) {
                V obj = entry.getValue();
                ((DSObjectBase)obj).setValid(false);
                super.put(entry.getKey(), obj);
            }
        }

        for (V value : super.values()) {
            logger.debug("object " + ((DSObjectBase)value).getID() + " is valid: " + ((DSObjectBase)value).isValid());
        }
    }

    /**
     *
     * @param key
     * @param tag
     * @return
     */
    public V get(K key, int tag) {
        V obj = super.get(key);
        if (obj != null && tag == REQ_OBJECT_RW) {
            ((DSObjectBase)obj).setPermission(DSObjectSpace.Permission.Read);
            super.put(key, obj);
        }
        return obj;
    }
}