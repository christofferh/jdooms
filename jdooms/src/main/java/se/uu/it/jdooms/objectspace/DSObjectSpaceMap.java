package se.uu.it.jdooms.objectspace;

import com.rits.cloning.Cloner;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Object store, extends ConcurrentHashMap to observe changes to objects
 * @param <K>
 * @param <V>
 */
public class DSObjectSpaceMap<K, V> extends ConcurrentHashMap<K, V> {
    private static final Logger logger = Logger.getLogger(DSObjectSpaceMap.class);
    private AtomicReferenceArray<Object> observers;
    private final Cloner cloner;
    private int nodeWorkerCount;
    private ArrayList<Integer> writeList = new ArrayList<Integer>();

    public DSObjectSpaceMap(int nodeWorkerCount) {
        super();
        this.cloner = new Cloner();
        this.nodeWorkerCount = nodeWorkerCount;
        observers = new AtomicReferenceArray<Object>(nodeWorkerCount);
    }

    public void addWrite(int objectID) {
        logger.debug("added ID:" + objectID);
        writeList.add(objectID);
    }

    public void updateWrite() {
        for (int objectID : writeList) {
            logger.debug("updateWrite ID:" + objectID);
            V obj = super.get(objectID);
            ((DSObjectBase)obj).setPermission(DSObjectSpace.Permission.Read);
        }
        writeList.clear();
    }

    /**
     * Notifies the registered observers
     */
    private synchronized void notifyObservers() {
        for (int i = 0; i < observers.length(); i++) {
            if (observers.get(i) != null) {
                synchronized (observers.get(i)) {
                    if (observers.get(i) != null) {
                        observers.get(i).notify();
                    }
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
        observers.set(Integer.parseInt(Thread.currentThread().getName()) % nodeWorkerCount, null);
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
            }
        }
    }

    /**
     * Sets the permission permission on the object at index key
     * @param objectID object ID
     * @param permission Read, ReadWrite
     */
    public void setPermission(int objectID, DSObjectSpace.Permission permission) {
        V obj = super.get(objectID);
        if (obj != null && ((DSObjectBase)obj).getPermission() == DSObjectSpace.Permission.ReadWrite) {
            ((DSObjectBase)obj).setPermission(permission);
        }
    }

    public void clone(DSObjectSpaceMap<K, V> map) {
        map.clear();
        for (Entry<K, V> entry : super.entrySet()) {
            //map.put(cloner.deepClone(entry.getKey()), cloner.deepClone(entry.getValue()));
            if (((DSObjectBase)entry.getValue()).getPermission() == DSObjectSpace.Permission.ReadWrite) {
                map.put(cloner.deepClone(entry.getKey()), cloner.deepClone(entry.getValue()));
            }
            //map.put(entry.getKey(), entry.getValue());
        }
    }
}