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
import se.uu.it.jdooms.objectspace.DSObjectSpaceImpl;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private static Queue<DSObjectCommMessage> queue = new ConcurrentLinkedQueue<DSObjectCommMessage>();;

    private int nodeID;
    private int clusterSize;
    private int workerCount;
    private boolean receiving;
    private int syncCounter = 0;

    private DSObjectSpaceMap<Integer, Object> dsObjectSpaceMap;
    private DSObjectCommSender DSObjectCommSender;
    private DSObjectNodeBarrier DSObjectNodeBarrier;

    public DSObjectComm(String[] args, DSObjectSpaceMap<Integer, Object> dsObjectSpaceMap) {
        this.dsObjectSpaceMap = dsObjectSpaceMap;

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
        ArrayList<Request> requests = new ArrayList<Request>();
        while (receiving) {
            message = queue.poll();
            if (message != null) {
                Collections.addAll(requests, DSObjectCommSender.send(message));
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
                        dsObjectBase.setPermission(Permission.Read);
                        dsObjectBase.setValid(false);
                        dsObjectSpaceMap.put(objectID, dsObjectBase);
                    } else if (tag == LOAD_DSCLASS) {
                        logger.debug("Got LOAD_DSCLASS");
                        byte[] byteBuffer = new byte[status.getCount(MPI.BYTE)];
                        MPI.COMM_WORLD.recv(byteBuffer, byteBuffer.length, MPI.BYTE, MPI.ANY_SOURCE, tag);
                        String clazz = new String(byteBuffer);
                        DSObjectSpaceImpl.loadDSClass(clazz);
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
                    }
                }
            } catch (MPIException e) {
                e.printStackTrace();
            }

            if (requests.size() != 0) { //if the cluster size is 1 request will be null since it wont be sent anywhere
                for (int i = 0; i < requests.size(); i++) {
                    try {
                        if (requests.get(i).test()) {
                            requests.remove(i);
                        }
                    } catch (MPIException e) {
                        e.printStackTrace();
                    }
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
        synchronize();
        receiving = false;
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
    public static void enqueueloadDSClass(String clazz) {
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
        obj = dsObjectSpaceMap.get(objectID);
        while (!((DSObjectBase)obj).isValid()) {
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
}



