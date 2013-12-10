package se.uu.it.jdooms.communication;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import mpi.*;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.log4j.Logger;

import se.uu.it.jdooms.objectspace.DSObjectBase;
import se.uu.it.jdooms.objectspace.DSObjectBaseImpl;
import se.uu.it.jdooms.objectspace.DSObjectSpace.Permission;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Queue;

/**
 * Communication class for MPI communication
 */
public class DSObjectComm implements Runnable {
    private static final Logger logger      = Logger.getLogger(DSObjectComm.class);
    public static final int REQ_OBJECT_R    = 10;
    public static final int REQ_OBJECT_RW   = 15;
    public static final int RES_OBJECT_R    = 20;
    public static final int RES_OBJECT_RW   = 25;
    public static final int LOAD_DSCLASS    = 30;
    public static final int SYNCHRONIZE     = 40;
    public static final int FINALIZE        = 50;
    public static final int RESERVE_OBJECT  = 60;


    private int nodeID;
    private int clusterSize;
    private int workerCount;
    private boolean receiving;
    private int syncCounter = 0;

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
        Request request = null;
        while (receiving) {
            message = queue.poll();
            if (message != null) {
                try {
                    request = DSObjectCommSender.send(message);
                } catch (MPIException e) {
                    e.printStackTrace();
                }
            }
            try {
                status = MPI.COMM_WORLD.iProbe(MPI.ANY_SOURCE, MPI.ANY_TAG);
                if (status != null) {
                    int tag = status.getTag();
                    if (tag == RES_OBJECT_R || tag == RES_OBJECT_RW) {
                        logger.debug("Got Response");
                        byte[] byte_buffer = new byte[status.getCount(MPI.BYTE)];
                        MPI.COMM_WORLD.recv(byte_buffer, byte_buffer.length, MPI.BYTE, MPI.ANY_SOURCE, tag);
                        Object obj = SerializationUtils.deserialize(byte_buffer);
                        if (tag == RES_OBJECT_R) {
                            ((DSObjectBase)obj).setPermission(Permission.Read);
                        } else {
                            ((DSObjectBase)obj).setPermission(Permission.ReadWrite);
                        }
                        ((DSObjectBase)obj).setValid(true);
                        dsObjectSpaceMap.put(((DSObjectBase)obj).getID(), obj);
                    } else if (tag == REQ_OBJECT_R || tag == REQ_OBJECT_RW) {
                        byte[] byte_buffer = new byte[status.getCount(MPI.BYTE)];
                        MPI.COMM_WORLD.recv(byte_buffer, byte_buffer.length, MPI.BYTE, MPI.ANY_SOURCE, tag);
                        int objectID = ByteBuffer.wrap(byte_buffer).getInt();
                        logger.debug("Got Request, objectid " + objectID);
                        Object obj = dsObjectSpaceMap.get(objectID);

                        if (obj != null && ((DSObjectBase)obj).getPermission() == Permission.ReadWrite) { //TODO:kanske kolla om objektet är valid?
                            queue.offer(new DSObjectCommMessage(((tag == REQ_OBJECT_R) ? RES_OBJECT_R : RES_OBJECT_RW),
                                                            status.getSource(),
                                                            obj));
                            if (tag == REQ_OBJECT_RW) {
                                dsObjectSpaceMap.setPermission(objectID, Permission.Read);
                            }
                        }
                    } else if (tag == RESERVE_OBJECT) {
                        byte[] byte_buffer = new byte[status.getCount(MPI.BYTE)];
                        MPI.COMM_WORLD.recv(byte_buffer, byte_buffer.length, MPI.BYTE, MPI.ANY_SOURCE, tag);
                        int objectID = ByteBuffer.wrap(byte_buffer).getInt();
                        logger.debug("Got RESERVE_OBJECT " + objectID);
                        DSObjectBaseImpl dsObjectBase = new DSObjectBaseImpl();
                        dsObjectBase.setReserved(true);
                        dsObjectBase.setPermission(Permission.Read);
                        dsObjectBase.setValid(false);
                        dsObjectSpaceMap.put(objectID, dsObjectBase);
                    } else if (tag == LOAD_DSCLASS) {
                        logger.debug("Got LOAD_DSCLASS");
                        byte[] byteBuffer = new byte[status.getCount(MPI.BYTE)];
                        MPI.COMM_WORLD.recv(byteBuffer, byteBuffer.length, MPI.BYTE, MPI.ANY_SOURCE, tag);
                        String clazz = new String(byteBuffer);
                        loadDSClass(clazz);
                    } else if (tag == SYNCHRONIZE) {
                        logger.debug("Got SYNC " + syncCounter);
                        syncCounter++;
                        byte[] byte_buffer = new byte[status.getCount(MPI.BYTE)];
                        MPI.COMM_WORLD.recv(byte_buffer, 1,  MPI.BYTE, MPI.ANY_SOURCE, tag);
                        DSObjectNodeBarrier.add(status.getSource());
                    } else if (tag == FINALIZE) {
                        logger.debug("Got FINALIZE");
                        byte[] byte_buffer = new byte[status.getCount(MPI.BYTE)];
                        MPI.COMM_WORLD.recv(byte_buffer, 1,  MPI.BYTE, MPI.ANY_SOURCE, tag);
                        receiving = false;
                    }
                }
            } catch (MPIException e) {
                e.printStackTrace();
            }

            if (message != null && request != null) { //if the cluster size is 1 request will be null since it wont be sent anywhere
                try {
                    request.test();
                } catch (MPIException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            MPI.Finalize();
        } catch (MPIException e) {
            e.printStackTrace();
        }
    }

    /**
     * Puts a synchronize message on the queue and self-invalidates its local cache
     */
    public void synchronize() {
        queue.offer(new DSObjectCommMessage(SYNCHRONIZE));
    }

    /**
     * Puts a dsFinalize message on the queue
     */
    public void dsFinalize() {
        queue.offer(new DSObjectCommMessage(FINALIZE));
    }

    /**
     * Puts a reserveObject on the queue
     * @param objectID
     */
    public void reserveObject(int objectID) {
        queue.offer(new DSObjectCommMessage(RESERVE_OBJECT, objectID));
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

        Object obj;
        dsObjectSpaceMap.addObserver(this);

        logger.debug("getting here");

        obj = dsObjectSpaceMap.get(objectID);
        while (!((DSObjectBase)obj).isValid()) {
            logger.debug("but not here");
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            obj = dsObjectSpaceMap.get(objectID);
        }
        dsObjectSpaceMap.removeObserver(this);
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



