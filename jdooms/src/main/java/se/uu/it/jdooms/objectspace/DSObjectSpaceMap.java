package se.uu.it.jdooms.objectspace;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DSObjectSpaceMap<K, V> extends ConcurrentHashMap<K, V> {
    private static final Logger logger = Logger.getLogger(DSObjectSpaceMap.class);
    private final ArrayList<Object> observers = new ArrayList<Object>();

    public DSObjectSpaceMap() {
        super();
    }

    private void notifyObservers() {
        logger.debug("Notify observers");
        for (Object obj : observers) {
            synchronized (obj) {
                obj.notify();
            }
        }
    }

    public void addObserver(Object obj) {
        logger.debug("addObserver: " + obj);
        observers.add(obj);
    }

    public void removeObserver(Object obj) {
        logger.debug("removeObserver: " + obj);
        observers.remove(obj);
    }

    public V put(K key, V value) {
        logger.debug("Put key: " + key + " objectspace");
        V result = (V) super.put(key, value);
        notifyObservers();
        return result;
    }

    public void selfInvalidate() {
        for (Object obj : observers) {
            if (((DSObjectBase)obj).getPermission() == DSObjectSpace.Permission.Read) {
                ((DSObjectBase)obj).setValid(false);
            }
        }
    }
}