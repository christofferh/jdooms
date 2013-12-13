package se.uu.it.jdooms.objectspace;

import javassist.*;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;

import se.uu.it.jdooms.communication.DSObjectCommMessage;
import se.uu.it.jdooms.communication.DSObjectComm;

import se.uu.it.jdooms.communication.DSObjectCommSynchronize;

/**
 * Implementation of the Distributed Object Space
 */
public class DSObjectSpaceImpl implements DSObjectSpace {
    private static final Logger logger = Logger.getLogger(DSObjectSpaceImpl.class);
    private static final String CLASSLOADER_METHOD = "findLoadedClass";
    private static final String USER_DIR = System.getProperty("user.dir");
    private static final String JDOOMS_BIN_DIR = "/../jdooms/bin";
    private static final String DSOBJECT_BINARY_NAME = "se.uu.it.jdooms.objectspace.DSObjectBaseImpl";
    private static final String SERIALIZABLE_BINARY_NAME = "java.io.Serializable";

    private final DSObjectSpaceMap<Integer, Object> objectSpaceMap;
    private final CyclicBarrier barrier;
    private final DSObjectComm DSObjectComm;

    public DSObjectSpaceImpl(String[] args) {
        Queue<DSObjectCommMessage> queue = new ConcurrentLinkedQueue<DSObjectCommMessage>();

        objectSpaceMap = new DSObjectSpaceMap<Integer, Object>(Integer.valueOf(args[1]));
        DSObjectComm = new DSObjectComm(args, objectSpaceMap, queue);
        Thread dsObjectCommThread = new Thread(DSObjectComm);
        dsObjectCommThread.start();

        barrier = new CyclicBarrier(Integer.valueOf(args[1]), new DSObjectCommSynchronize(DSObjectComm, objectSpaceMap));
    }

    /**
     * Return the node ID
     * @return the node Id
     */
    @Override
    public int getNodeID() {
        return DSObjectComm.getNodeID();
    }

    /**
     * Return the worker ID.
     * @return the workerID
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
        return DSObjectComm.getWorkerCount();
    }

    /**
     * Returns the cluster size
     * @return the cluster size
     */
    public int getClusterSize() {
        return DSObjectComm.getClusterSize();
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
        logger.debug("getObject() id " + objectID);
        if (obj != null && !((DSObjectBase) obj).isValid()) {
            logger.debug("getObject() remote id " + objectID);
            obj = DSObjectComm.getObject(objectID, permission);
        }
        return obj;
    }

    public void reserveObject(int objectID) {
        DSObjectComm.reserveObject(objectID);
    }
    /**
     * Barrier call
     */
    @Override
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
     * Finalize call
     */
    @Override
    public void dsFinalize() {
        DSObjectComm.dsFinalize();
    }

    /**
     * Distributed new
     * @param clazz class to be instantiated
     * @param objectID uniquely identifiable number to the returning object
     * @return new instance of the input clazz
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Override
    public Object dsNew(String clazz, int objectID) throws IllegalAccessException, InstantiationException {
        /* Make sure the ds class is loaded before trying to get it */
        loadDSClass(clazz);
        Object obj = objectSpaceMap.get(objectID);
        if (obj != null && ((DSObjectBase) obj).isValid() ) {
            logger.debug("Fetched object from local cache");
            return obj;
        } else {
            DSObjectComm.reserveObject(objectID);
            try {
                logger.debug("Creating object and putting in local cache");
                Class tmp_clazz = this.getClass().getClassLoader().loadClass(clazz);
                obj = tmp_clazz.newInstance();

                ((DSObjectBase)obj).setPermission(Permission.ReadWrite);
                ((DSObjectBase)obj).setClassifier(Classifier.Shared);
                ((DSObjectBase)obj).setID(objectID);
                ((DSObjectBase)obj).setValid(true);
                putObject(obj);
            } catch (ClassNotFoundException e) {
                logger.debug("This should never happen, because we forced the class to be loaded before running this");
                e.printStackTrace();
            }
            return obj;
        }
    }

    /**
     * Loads a specified DSClass in the class loader
     * @param clazz fully qualified name of the class to load
     */
    public static void loadDSClass(String clazz) {
        Method findLoadedClass;
        try {
            findLoadedClass = ClassLoader.class.getDeclaredMethod(CLASSLOADER_METHOD, new Class[] { String.class });
            findLoadedClass.setAccessible(true);
            ClassLoader cl = DSObjectSpaceImpl.class.getClassLoader();
            Class dsClazz = (Class) findLoadedClass.invoke(cl, clazz);
            if (dsClazz == null) {
                ClassPool classPool = ClassPool.getDefault();
                try {
                    classPool.appendClassPath(USER_DIR + JDOOMS_BIN_DIR);
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    CtClass ctClass = classPool.get(clazz);
                    CtClass superCtClass = classPool.get(DSOBJECT_BINARY_NAME);
                    CtClass ctSerializable = classPool.get(SERIALIZABLE_BINARY_NAME);
                    ctClass.setSuperclass(superCtClass);
                    ctClass.addInterface(ctSerializable);
                    ctClass.toClass();
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
