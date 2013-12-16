package se.uu.it.jdooms.communication;

import mpi.*;
import org.apache.log4j.Logger;

import se.uu.it.jdooms.objectspace.DSObjectBase;
import se.uu.it.jdooms.objectspace.DSObjectSpace.Permission;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

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

    private static Queue<DSObjectCommMessage> queue = new ConcurrentLinkedQueue<DSObjectCommMessage>();

    private int nodeID;
    private int clusterSize;
    private int workerCount;
    private boolean receiving;

    private DSObjectSpaceMap<Integer, Object> dsObjectSpaceMap;
    private DSObjectCommSender dsObjectCommSender;
    private DSObjectCommReceiver dsObjectCommReceiver;
    private DSObjectNodeBarrier dsObjectNodeBarrier;

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

        dsObjectNodeBarrier = new DSObjectNodeBarrier(nodeID, clusterSize);
        dsObjectCommSender = new DSObjectCommSender(this);
        dsObjectCommReceiver = new DSObjectCommReceiver(dsObjectSpaceMap, dsObjectNodeBarrier);
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
    public DSObjectNodeBarrier getDsObjectNodeBarrier() {
        return dsObjectNodeBarrier;
    }
    /**
     * Puts a synchronize message on the queue
     */
    public void synchronize() {
        queue.offer(new DSObjectCommMessage(SYNCHRONIZE));
    }

    /**
     * Puts a dsFinalize message on the queue
     */
    public void dsFinalize() {
        receiving = false;
    }

    /**
     * Puts a message on the send queue
     * @param dsObjectCommMessage the message to enqueue
     */
    public static void enqueuMessage(DSObjectCommMessage dsObjectCommMessage) {
        queue.offer(dsObjectCommMessage);
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
                Collections.addAll(requests, dsObjectCommSender.send(message));
            }
            try {
                status = MPI.COMM_WORLD.iProbe(MPI.ANY_SOURCE, MPI.ANY_TAG);
                if (status != null) {
                    dsObjectCommReceiver.receive(status);
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


}



