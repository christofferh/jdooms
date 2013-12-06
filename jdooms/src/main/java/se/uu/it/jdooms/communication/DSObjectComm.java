package se.uu.it.jdooms.communication;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import mpi.*;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.log4j.Logger;

import se.uu.it.jdooms.objectspace.DSObjectBaseImpl;
import se.uu.it.jdooms.objectspace.DSObjectSpace.Permission;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;

/**
 * Communication class for MPI communication
 */
public class DSObjectComm implements Runnable {
    private static final Logger logger      = Logger.getLogger(DSObjectComm.class);
    public static final int REQ_OBJECT_R    = 10;
    public static final int REQ_OBJECT_RW   = 15;
    public static final int RET_OBJECT_R    = 20;
    public static final int RET_OBJECT_RW   = 25;
    public static final int LOAD_DSCLASS    = 30;
    public static final int SYNCHRONIZE     = 40;

    private int nodeID;
    private int clusterSize;
    private int workerCount;
    private boolean receiving;

    private DSObjectSpaceMap<Integer, Object> dsObjectSpaceMap;
    private Queue<DSObjectCommMessage> queue;
    private DSObjectCommSender DSObjectCommSender;
    private DSObjectNodeBarrier DSObjectNodeBarrier;

    public DSObjectComm(String[] args, DSObjectSpaceMap<Integer, Object> dsObjectSpaceMap, Queue<DSObjectCommMessage> queue) {
        this.dsObjectSpaceMap = dsObjectSpaceMap;
        this.queue = queue;

        try {
            MPI.Init(args);
            nodeID = MPI.COMM_WORLD.getRank();
            clusterSize = MPI.COMM_WORLD.getSize();
            workerCount = Integer.valueOf(args[1]) * clusterSize;
        } catch (MPIException e) {
            e.printStackTrace();
        }

        DSObjectCommSender = new DSObjectCommSender(this);
        DSObjectNodeBarrier = new DSObjectNodeBarrier(nodeID, clusterSize);
        receiving = true;
    }

    /**
     * Returns the local nodeID
     * @return the nodeID
     */
    public int getNodeID() {
        return nodeID;
    }

    /**
     * Returns the total cluster size
     * @return the clusterSize
     */
    public int getClusterSize() {
        return clusterSize;
    }

    /**
     * Returns the total worker count
     * @return the worker count
     */
    public int getWorkerCount() {
        return workerCount;
    }

    /**
     * Return the node barrier
     * @return the node barrier
     */
    public DSObjectNodeBarrier getDSObjectNodeBarrier() {
        return DSObjectNodeBarrier;
    }
    /**
     * Start the communication
     */
    @Override
    public void run() {
        Thread.currentThread().setName("COMM-" + nodeID);
        DSObjectCommMessage message;
        Status status;
        while (receiving) {
            message = queue.poll();
            if (message != null) {
                try {
                    DSObjectCommSender.send(message);
                } catch (MPIException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    status = MPI.COMM_WORLD.iProbe(MPI.ANY_SOURCE, MPI.ANY_TAG);
                    if (status != null) {
                        int tag = status.getTag();

                        if (tag == RET_OBJECT_R || tag == RET_OBJECT_RW) {
                            logger.debug("RET");
                            byte[] byte_buffer = new byte[status.getCount(MPI.BYTE)];
                            DSObjectBaseImpl obj;
                            MPI.COMM_WORLD.recv(byte_buffer, byte_buffer.length, MPI.BYTE, MPI.ANY_SOURCE, tag);
                            obj = (DSObjectBaseImpl) SerializationUtils.deserialize(byte_buffer);
                            dsObjectSpaceMap.put(obj.getID(), obj);
                        } else if (tag == REQ_OBJECT_R || tag == REQ_OBJECT_RW) {
                            logger.debug("GET");
                            int[] int_buffer = new int[1];
                            MPI.COMM_WORLD.recv(int_buffer, 1, MPI.INT, MPI.ANY_SOURCE, tag);
                            int objectId = int_buffer[0];
                            Object obj = dsObjectSpaceMap.get(objectId);
                            if (obj != null) {
                                queue.offer(new DSObjectCommMessage(((tag == REQ_OBJECT_R) ? RET_OBJECT_R : RET_OBJECT_RW),
                                                                status.getSource(),
                                                                obj));
                            }
                        } else if (tag == LOAD_DSCLASS) {
                            char[] byte_buffer = new char[status.getCount(MPI.CHAR)];
                            String clazz;
                            MPI.COMM_WORLD.recv(byte_buffer, byte_buffer.length, MPI.CHAR, MPI.ANY_SOURCE, tag);
                            clazz = new String(byte_buffer);
                            loadDSClass(clazz);
                        } else if (tag == SYNCHRONIZE) {
                            logger.debug("SYNC");
                            boolean[] bool_buffer = new boolean[1];
                            MPI.COMM_WORLD.recv(bool_buffer, 1,  MPI.BOOLEAN, MPI.ANY_SOURCE, tag);
                            DSObjectNodeBarrier.add(status.getSource());
                        }
                    }
                } catch (MPIException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Puts a synchronize message on the queue and self-invalidates its local cache
     */
    public void synchronize() {
        queue.offer(new DSObjectCommMessage(SYNCHRONIZE));
        dsObjectSpaceMap.selfInvalidate();
    }

    /**
     * Puts a loadDSClass message on the queue
     * @param clazz the class to load
     */
    public void enqueueloadDSClass(String clazz) {
        queue.offer(new DSObjectCommMessage(LOAD_DSCLASS, clazz));
    }

    /**
     * Request object from the nodes
     * @param objectID the ID of the object
     */
    public Object getObject(int objectID, Permission permission) {
        if (permission == Permission.Read) {
            queue.offer(new DSObjectCommMessage(REQ_OBJECT_R, objectID));
        } else {
            queue.offer(new DSObjectCommMessage(REQ_OBJECT_RW, objectID));
        }

        Object obj = null;
        dsObjectSpaceMap.addObserver(this);

        while (obj == null) {
            obj = dsObjectSpaceMap.get(objectID);
            if (obj == null) {
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        dsObjectSpaceMap.removeObserver(this);
        ((DSObjectBaseImpl)obj).setPermission(permission);
        ((DSObjectBaseImpl)obj).setValid(true);

        return obj;
    }

    /**
     * Loads a specified DSClass in the class loader
     * @param clazz fully qualified name of the class to load
     */
    private void loadDSClass(String clazz) {
        Method findLoadedClass;
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
                    CtClass superCtClass = classPool.get("se.uu.it.jdooms.objectspace.DSObjectBaseImpl");
                    CtClass ctSerializable = classPool.get("java.io.Serializable");

                    if (ctClass.isFrozen()) ctClass.defrost();

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



