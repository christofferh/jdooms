package se.uu.it.jdooms.communication;


import mpi.MPI;
import mpi.MPIException;
import mpi.Request;
import org.apache.log4j.Logger;

import java.nio.ByteBuffer;

import static se.uu.it.jdooms.communication.DSObjectComm.*;

/**
 * Sender class
 */
class DSObjectCommSender {
    private static final Logger logger = Logger.getLogger(DSObjectCommSender.class);
    private final int nodeID;
    private final int clusterSize;

    public DSObjectCommSender(DSObjectComm DSObjectComm) {
        nodeID = DSObjectComm.getNodeID();
        clusterSize = DSObjectComm.getClusterSize();
    }
    /**
     * Sends an DSObjectCommMessage via MPI
     *
     * @param message the message to send
     */
    public Request[] send(DSObjectCommMessage message) {
        Request[] requests = null;
        switch (message.tag) {
            case REQ_OBJECT_R:
                requests = broadcast(REQ_OBJECT_R, message.data);
                break;
            case REQ_OBJECT_RW:
                requests = broadcast(REQ_OBJECT_RW, message.data);
                break;
            case RES_OBJECT_R:
                requests = send(RES_OBJECT_R, message.data, message.destination);
                break;
            case RES_OBJECT_RW:
                requests = send(RES_OBJECT_RW, message.data, message.destination);
                break;
            case RESERVE_OBJECT:
                requests = broadcast(RESERVE_OBJECT, message.data);
                break;
            case LOAD_DSCLASS:
                requests = broadcast(LOAD_DSCLASS, message.data);
                break;
            case SYNCHRONIZE:
                requests = broadcast(SYNCHRONIZE, message.data);
                break;
            default:
        }
        return requests;
    }

    /**
     * Broadscasts data to all nodes
     * @param tag
     * @param data
     * @return
     */
    private Request[] broadcast(int tag, ByteBuffer data) {
        Request request;
        int iter = 0;
        Request[] requests = new Request[clusterSize-1];
        for (int node = 0; node < clusterSize; node++) {
            if (node != nodeID) {
                try {
                    request = MPI.COMM_WORLD.iSend(data, data.capacity(), MPI.BYTE, node, tag);
                    requests[iter] = request;
                    iter++;
                } catch (MPIException e) {
                    e.printStackTrace();
                }

            }
        }
        return requests;
    }

    /**
     * Send the data to a certain node
     * @param tag
     * @param data
     * @param destination
     * @return
     */
    private Request[] send(int tag, ByteBuffer data, int destination) {
        Request request;
        Request[] requests = new Request[1];
        try {
            request = MPI.COMM_WORLD.iSend(data, data.capacity(), MPI.BYTE, destination, tag);
            requests[0] = request;
        } catch (MPIException e) {
            e.printStackTrace();
        }
        return requests;
    }
}
