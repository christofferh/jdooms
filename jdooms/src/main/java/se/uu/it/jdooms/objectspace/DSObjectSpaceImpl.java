package se.uu.it.jdooms.objectspace;

import javassist.*;
import mpi.MPI;
import mpi.MPIException;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Implementation of the Distributed Object Space
 */
public class DSObjectSpaceImpl implements DSObjectSpace {
    private static final Logger logger = Logger.getLogger(DSObjectSpaceImpl.class);
    private DSObjectSpaceMap<Integer, Object> objectSpaceMap;
    private CyclicBarrier barrier;

    private int nodeID;
    private int clusterSize;
    private int workerCount;

    private DSObjectCommunication dsObjectCommunication;
    private Thread dsObjectCommThread;

    public DSObjectSpaceImpl(String[] args) {
        objectSpaceMap = new DSObjectSpaceMap<Integer, Object>();
        try {
            MPI.Init(args);
            nodeID = MPI.COMM_WORLD.Rank();
            clusterSize = MPI.COMM_WORLD.Size();
            workerCount = Integer.valueOf(args[1]);
        } catch (MPIException e) {
            e.printStackTrace();
        }

        dsObjectCommunication = new DSObjectCommunication(this, objectSpaceMap);
        dsObjectCommThread = new Thread(dsObjectCommunication, "COMM-" + nodeID);
        dsObjectCommThread.start();
        barrier = new CyclicBarrier(workerCount, new DSObjectSynchronize(dsObjectCommunication));
    }

    /**
     * Return the node ID
     * @return the node Id
     */
    @Override
    public int getNodeID() {
        return nodeID;
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
        return workerCount * clusterSize;
    }

    /**
     * Returns the cluster size
     * @return the cluster size
     */
    public int getClusterSize() {
        return clusterSize;
    }

    /**
     * Stores an object in the local object space
     * @param obj
     * @param classifier
     */
    @Override
    public void putObject(Object obj, Classifier classifier) {
        if (obj != null) {
            objectSpaceMap.put(((DSObjectBase) obj).getID(), obj);
            //dsObjectCommunication.putObject(obj);
        }
    }

    /**
     * Tries to get an object from the local object space, if unsuccessful, request it from the cluster.
     *
     * @param objectID the ID of the requested object
     */
    @Override
    public Object getObject(int objectID, Permission permission) {
        Object obj = objectSpaceMap.get(objectID);
        if (obj == null) {
            obj = dsObjectCommunication.getObject(objectID, permission); // LÅS
        }
        return obj;
    }

    /**
     * Put local object
     * @param obj
     * @param classifier
     */
    public void putLocalObject(Object obj, Classifier classifier) {
        if (obj != null) {
            objectSpaceMap.put(((DSObjectBase) obj).getID(), obj);
        }
    }
    /**
     * Get local object
     * @param objectID
     * @param permission
     * @return
     */
    public Object getLocalObject(int objectID, Permission permission) {
        return objectSpaceMap.get(objectID);
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
                logger.debug("Creating and sending DSclass");
                dsObjectCommunication.loadDSClass(clazz);
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

                ((DSObjectBase)obj).setClassifier(Classifier.Shared);
                ((DSObjectBase)obj).setID(ID);

                putLocalObject(obj, Classifier.Shared);
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return obj;
        }
    }
}
