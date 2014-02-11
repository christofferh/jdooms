package se.uu.it.jdooms.communication;


import mpi.MPI;
import mpi.MPIException;
import mpi.Request;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static se.uu.it.jdooms.communication.DSObjectComm.*;

/**
 * Sender class
 */
class DSObjectCommSender {
    private static final Logger logger = Logger.getLogger(DSObjectCommSender.class);
    private final int nodeID;
    private final int clusterSize;
    private ByteBuffer sendBuffer;

    public DSObjectCommSender(DSObjectComm DSObjectComm) {
        nodeID = DSObjectComm.getNodeID();
        clusterSize = DSObjectComm.getClusterCount();
        sendBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        sendBuffer.order(ByteOrder.nativeOrder());
    }
    /**
     * Sends an DSObjectCommMessage via MPI
     * @param message the message to send
     */
    public Request[] send(DSObjectCommMessage message) {
        Request[] requests = null;
        switch (message.tag) {
            case REQ_OBJECT_R:
                sendBuffer.putInt(message.objectID);
                requests = broadcast(REQ_OBJECT_R);
                break;
            case REQ_OBJECT_RW:
                sendBuffer.putInt(message.objectID);
                requests = broadcast(REQ_OBJECT_RW);
                break;
            case RES_OBJECT_R:
                byte[] serialized = SerializationUtils.serialize((Serializable) message.obj);
                sendBuffer.put(serialized);
                requests = send(RES_OBJECT_R, message.destination);
                break;
            case RES_OBJECT_RW:
                serialized = SerializationUtils.serialize((Serializable) message.obj);
                sendBuffer.put(serialized);
                requests = send(RES_OBJECT_RW, message.destination);
                break;
            case RESERVE_OBJECT:
                sendBuffer.putInt(message.objectID);
                requests = broadcast(RESERVE_OBJECT);
                break;
            case LOAD_DSCLASS:
                sendBuffer.put(message.clazz.getBytes());
                requests = broadcast(LOAD_DSCLASS);
                break;
            case SYNCHRONIZE:
                requests = broadcast(SYNCHRONIZE);
                break;
            default:
        }
        return requests;
    }

    /**
     * Broadscasts data to all nodes
     * @param tag   the tag of the message
     * @return      the request objects
     */
    private Request[] broadcast(int tag) {
        Request request;
        int iter = 0;
        Request[] requests = new Request[clusterSize-1];
        for (int node = 0; node < clusterSize; node++) {
            if (node != nodeID) {
                try {
                    request = MPI.COMM_WORLD.iSend(sendBuffer, sendBuffer.position(), MPI.BYTE, node, tag);

                    requests[iter] = request;
                    iter++;
                } catch (MPIException e) {
                    e.printStackTrace();
                }

            }
        }
        sendBuffer.position(0);
        return requests;
    }

    /**
     * Send the data to a certain node
     * @param tag           the tag
     * @param destination   the destination nodeID
     * @return              the request object
     */
    private Request[] send(int tag, int destination) {
        Request request;
        Request[] requests = new Request[1];
        try {
            request = MPI.COMM_WORLD.iSend(sendBuffer, sendBuffer.position(), MPI.BYTE, destination, tag);
            sendBuffer.position(0);
            requests[0] = request;
        } catch (MPIException e) {
            e.printStackTrace();
        }
        return requests;
    }
}
