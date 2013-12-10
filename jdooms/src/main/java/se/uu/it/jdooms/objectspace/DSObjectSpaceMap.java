package se.uu.it.jdooms.objectspace;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import se.uu.it.jdooms.communication.DSObjectComm;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static se.uu.it.jdooms.communication.DSObjectComm.*;

/**
 * Object store, extends ConcurrentHashMap to observe changes to objects
 * @param <K>
 * @param <V>
 */
public class DSObjectSpaceMap<K, V> extends ConcurrentHashMap<K, V> {
    private static final Logger logger = Logger.getLogger(DSObjectSpaceMap.class);
    //private final List<Object> observers = Collections.synchronizedList(new ArrayList<Object>());
    private AtomicReferenceArray<Object> observers;
    private int nodeWorkerCount;

    public DSObjectSpaceMap(int nodeWorkerCount) {
        super();
        this.nodeWorkerCount = nodeWorkerCount;
        observers = new AtomicReferenceArray<Object>(nodeWorkerCount);
    }

    /**
     * Notifies the registered observers
     */
    private void notifyObservers() {
        for (int i = 0; i < observers.length(); i++) {
            if (observers.get(i) != null) {
                synchronized (observers.get(i)) {
                    observers.get(i).notify();
                }
            }
        }
    }

    /**
     * Adds an observer
     * @param obj the reference to the observer
     */
    public void addObserver(Object obj) {
        logger.debug("addObserver: " + obj);
        observers.set(Integer.parseInt(Thread.currentThread().getName())%nodeWorkerCount, obj);
    }

    /**
     * Removes an observer
     * @param obj the reference to the observer
     */
    public void removeObserver(Object obj) {
        logger.debug("removeObserver: " + obj);
        observers.set(Integer.parseInt(Thread.currentThread().getName())%nodeWorkerCount, null);
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
                logger.debug("self invalidating, objectID " + ((DSObjectBase)obj).getID());
                //super.put(entry.getKey(), obj);
            }
        }

        for (V value : super.values()) {
            logger.debug("object " + ((DSObjectBase)value).getID() + " is valid: " + ((DSObjectBase)value).isValid() + ", permission: " + ((DSObjectBase)value).getPermission());
        }
    }

    /*public V get(K key, int tag) {
        V obj = super.get(key);
        if (obj != null && tag == REQ_OBJECT_RW) {
            //((DSObjectBase)obj).setPermission(DSObjectSpace.Permission.Read);
            //super.put(key, obj);
        }
        return obj;
    }*/

    public void setPermission(K key, DSObjectSpace.Permission permission) {
        V obj = super.get(key);
        if (obj != null && ((DSObjectBase)obj).getPermission() == DSObjectSpace.Permission.ReadWrite) {
            ((DSObjectBase)obj).setPermission(permission);
        }
    }
}