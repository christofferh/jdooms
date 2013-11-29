package se.uu.it.jdooms.objectspace;

import javassist.*;
import mpi.MPI;
import mpi.MPIException;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * Implementation of the Distributed Object Space
 */
public class DSObjectSpaceImpl implements DSObjectSpace {
    private static final Logger logger = Logger.getLogger(DSObjectSpaceImpl.class);
    private HashMap<Integer, Object> objectSpace;
    private CyclicBarrier barrier;

    private int nodeID;
    private int clusterSize;
    private int workerCount;

    private DSObjectCommunication dsObjectCommunication;
    private Thread dsObjectCommThread;

    public DSObjectSpaceImpl(String[] args) {
        objectSpace = new HashMap<Integer, Object>();
        try {
            MPI.Init(args);
            nodeID = MPI.COMM_WORLD.Rank();
            clusterSize = MPI.COMM_WORLD.Size();
            workerCount = Integer.valueOf(args[1]);
        } catch (MPIException e) {
            e.printStackTrace();
        }

        dsObjectCommunication = new DSObjectCommunication(this);
        dsObjectCommThread = new Thread(dsObjectCommunication);
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
     * Tries to get an object from the local object space, if unsuccessful, request it from the cluster.
     *
     * @param objectID the ID of the requested object
     */
    @Override
    public Object getObject(int objectID, Permission permission) {
        if (objectSpace.containsKey(objectID)) {
            return objectSpace.get(objectID);
        } else {
            return null;
        }

        /*else {
            dsObjectCommunication.getObject(objectID);
            while (!objectSpace.containsKey(objectID)) {}
            return objectSpace.get(objectID);
        }*/
    }

    /**
     * Stores an object in the local object space
     * @param obj
     * @param classifier
     */
    @Override
    public void putObject(Object obj, DSObjectSpace.Classifier classifier) {
        if (obj != null) {
            objectSpace.put(((DSObjectBase) obj).getID(), obj);
            dsObjectCommunication.putObject(obj);
        } else {
            //return DSObjectSender.broadcastPut(obj, classifier);
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
    /*@Override
    public Object dsNew(String clazz, int ID) throws IllegalAccessException, InstantiationException { //Should be synchronized
        //make sure the ds class is loaded before trying to get it
        Object obj = null;
        obj = getObject(ID, Permission.ReadWrite); //What kind of permission should we use here.

        if(obj != null ) {
            logger.debug("fetched object");
            return obj;
        } else {

            try {
                Method findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
                findLoadedClass.setAccessible(true);
                ClassLoader cl = this.getClass().getClassLoader();
                Class tmp_cl = (Class) findLoadedClass.invoke(cl, clazz);
                if (tmp_cl != null) {
                    obj = tmp_cl.newInstance();

                    ((DSObjectBase)obj).setClassifier(Classifier.Shared);
                    ((DSObjectBase)obj).setID(ID);

                    putObject(obj, Classifier.Shared);
                } else {

                    logger.debug("creating object");
                    ClassPool classPool = ClassPool.getDefault();
                    try {
                        String path = System.getProperty("user.dir");
                        classPool.appendClassPath(path + "/out/production/jdooms-worker");
                        classPool.appendClassPath(path + "/../jdooms/bin");
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    }

                    logger.debug(classPool.toString());

                    try {
                        logger.debug("Trying to get ctclass");
                        CtClass ctClass = classPool.get(clazz);

                        logger.debug("Trying to get new superclass");
                        CtClass superCtClass = classPool.get("se.uu.it.jdooms.objectspace.DSObjectBase");

                        CtClass ctSerializable = classPool.get("java.io.Serializable");

                        if (ctClass.isFrozen()) {
                            ctClass.defrost();
                        }

                        logger.debug("Trying to set new superclass");
                        ctClass.setSuperclass(superCtClass); //check if alredy exists
                        ctClass.addInterface(ctSerializable);//check if alredy exists


                        logger.debug("Trying to create class from CtClass");
                        Class tmp_clazz = ctClass.toClass();

                        //ClassDebug(tmp_clazz);

                        logger.debug("Trying to create instance from class");
                        obj = tmp_clazz.newInstance();

                        ((DSObjectBase)obj).setClassifier(Classifier.Shared);
                        ((DSObjectBase)obj).setID(ID);

                        putObject(obj, Classifier.Shared);

                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    } catch (CannotCompileException e) {
                        e.printStackTrace();
                        logger.debug(e.getCause());
                        logger.debug(e.getReason());
                        logger.debug(e.getMessage());
                    }
                }

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return obj;
        }
    }*/

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
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (NotFoundException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InvocationTargetException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        Object obj = null;
        obj = getObject(ID, Permission.ReadWrite); //What kind of permission should we use here.

        if(obj != null ) {
            logger.debug("fetched object");
            return obj;
        } else {
            try {
                ClassLoader cl = this.getClass().getClassLoader();
                Class tmp_clazz = (Class) findLoadedClass.invoke(cl, clazz);
                obj = tmp_clazz.newInstance();

                ((DSObjectBase)obj).setClassifier(Classifier.Shared);
                ((DSObjectBase)obj).setID(ID);

                putObject(obj, Classifier.Shared);
            } catch (InvocationTargetException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
            return obj;
        }
    }

    /**
     * For receiver to store objects
     * @param obj
     */
    public void storeObject(Object obj, DSObjectSpace.Classifier classifier) {
        if (obj != null) {
            objectSpace.put(((DSObjectBase) obj).getID(), obj);
        }
    }

    /**
     * Get local object
     * @param objectID
     * @param permission
     * @return
     */
    public Object getLocalObject(int objectID, Permission permission) {
        return objectSpace.get(objectID);
    }

    /**
     * Put local object
     * @param obj
     * @param classifier
     */
    public void putLocalObject(Object obj, Classifier classifier) {
        if (obj != null) {
            objectSpace.put(((DSObjectBase) obj).getID(), obj);
        }
    }

    /**
     * Bytecode injection test
     */
    public void beforeMethodTest() {
        logger.debug("Before Method");
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
}
