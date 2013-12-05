package se.uu.it.jdooms.objectspace;

import javassist.*;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;

import se.uu.it.jdooms.objectspace.DSObjectCommunication.*;

/**
 * Implementation of the Distributed Object Space
 */
public class DSObjectSpaceImpl implements DSObjectSpace {
    private static final Logger logger = Logger.getLogger(DSObjectSpaceImpl.class);
    private DSObjectSpaceMap<Integer, Object> objectSpaceMap;
    private CyclicBarrier barrier;
    private Queue<DSObjectMessage> queue;
    private DSObjectCommunication dsObjectCommunication;

    public DSObjectSpaceImpl(String[] args) {
        objectSpaceMap = new DSObjectSpaceMap<Integer, Object>();
        queue = new ConcurrentLinkedQueue<DSObjectMessage>();
        dsObjectCommunication = new DSObjectCommunication(args, this, objectSpaceMap, queue);
        Thread dsObjectCommThread = new Thread(dsObjectCommunication);
        dsObjectCommThread.start();
        barrier = new CyclicBarrier(Integer.valueOf(args[1]), new DSObjectSynchronize(dsObjectCommunication));
    }

    /**
     * Return the node ID
     * @return the node Id
     */
    @Override
    public int getNodeID() {
        return dsObjectCommunication.getNodeID();
    }

    /**
     * Return the worker ID.
     * @return
     */
    @Override
    public int getWorkerID() {
        return Integer.parseInt(Thread.currentThread().getName());
    }

    /**
     * Returns the worker count given as program parameter
     * @return the worker count
     */
    @Override
    public int getWorkerCount() {
        return dsObjectCommunication.getWorkerCount();
    }

    /**
     * Returns the cluster size
     * @return the cluster size
     */
    public int getClusterSize() {
        return dsObjectCommunication.getClusterSize();
    }

    /**
     * Stores an object in the local object space
     * @param obj the object to store
     */
    @Override
    public void putObject(Object obj) {
        if (obj != null) {
            objectSpaceMap.put(((DSObjectBase) obj).getID(), obj);
        }
    }

    /**
     * Tries to get an object from the local object space, if unsuccessful, request it from the cluster.
     * @param objectID the ID of the requested object
     */
    @Override
    public Object getObject(int objectID, Permission permission) {
        Object obj = objectSpaceMap.get(objectID);
        if (obj == null || !((DSObjectBase) obj).isValid()) {
            obj = dsObjectCommunication.getObject(objectID, permission);
        }
        return obj;
    }

    /**
     * Tries to get an object from the local object space. Needed for dsNew
     * @param objectID the ID of the requested object
     */
    public Object getLocalObject(int objectID, Permission permission) {
        Object obj = objectSpaceMap.get(objectID);
        if (obj == null || !((DSObjectBase) obj).isValid()) {
            return null;
        }
        return obj;
    }

    /**
     * Barrier call
     */
    public void synchronize() {
        try {
            barrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (BrokenBarrierException e) {
            e.printStackTrace();
        }
    }

    /**
     * Distributed new
     * @param clazz class to be instantiated
     * @param ID uniquely identifiable number to the returning object
     * @return new instance of the input clazz
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Override
    public Object dsNew(String clazz, int ID) throws IllegalAccessException, InstantiationException { //Should be synchronized
        //make sure the ds class is loaded before trying to get it
        Method findLoadedClass = null;
        try {
            findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
            findLoadedClass.setAccessible(true);
            ClassLoader cl = this.getClass().getClassLoader();
            Class tmp_cl = (Class) findLoadedClass.invoke(cl, clazz);
            if (tmp_cl == null) {
                logger.debug("Creating and sending DSclass: " + clazz);
                dsObjectCommunication.enqueueloadDSClass(clazz);
                ClassPool classPool = ClassPool.getDefault();
                try {
                    String path = System.getProperty("user.dir");
                    classPool.appendClassPath(path + "/out/production/jdooms-worker");
                    classPool.appendClassPath(path + "/../jdooms/bin");
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    CtClass ctClass = classPool.get(clazz);
                    CtClass superCtClass = classPool.get("se.uu.it.jdooms.objectspace.DSObjectBase");
                    CtClass ctSerializable = classPool.get("java.io.Serializable");

                    if (ctClass.isFrozen()) { ctClass.defrost(); }
                    ctClass.setSuperclass(superCtClass);
                    ctClass.addInterface(ctSerializable);
                    ctClass.toClass();
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            } else {

            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        Object obj = null;
        obj = getLocalObject(ID, Permission.ReadWrite); //What kind of permission should we use here.

        if(obj != null ) {
            logger.debug("Fetched object from local cache");
            return obj;
        } else {
            try {
                logger.debug("Creating object and putting in local cache");
                ClassLoader cl = this.getClass().getClassLoader();
                Class tmp_clazz = (Class) findLoadedClass.invoke(cl, clazz);
                obj = tmp_clazz.newInstance();

                ((DSObjectBase)obj).setPermission(Permission.ReadWrite);
                ((DSObjectBase)obj).setClassifier(Classifier.Shared);
                ((DSObjectBase)obj).setID(ID);
                ((DSObjectBase)obj).setValid(true);

                putObject(obj);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return obj;
        }
    }
}
