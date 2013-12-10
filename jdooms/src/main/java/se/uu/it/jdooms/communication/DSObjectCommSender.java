package se.uu.it.jdooms.communication;


import mpi.MPI;
import mpi.MPIException;
import mpi.Request;
import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static se.uu.it.jdooms.communication.DSObjectComm.*;

public class DSObjectCommSender {
    private static final Logger logger = Logger.getLogger(DSObjectCommSender.class);
    private final int nodeID;
    private final int clusterSize;
    private int syncCounter = 0;

    public DSObjectCommSender(DSObjectComm DSObjectComm) {
        nodeID = DSObjectComm.getNodeID();
        clusterSize = DSObjectComm.getClusterSize();
    }
    /**
     * Sends an DSObjectCommMessage via MPI
     *
     * @param message the message to send
     * @throws mpi.MPIException
     */
    public Request send(DSObjectCommMessage message) throws MPIException {
        Request request = null;
        if (message.tag == REQ_OBJECT_R || message.tag == REQ_OBJECT_RW) {
            for (int node = 0; node < clusterSize; node++)
                if (node != nodeID) {
                    logger.debug("Sent Request tag: " + message.tag + " to " + node);
                    request = MPI.COMM_WORLD.iSend(message.objectID, message.objectID.capacity(), MPI.BYTE, node, message.tag);
                }
        } else if (message.tag == RES_OBJECT_R || message.tag == RES_OBJECT_RW) {
            logger.debug("Sent Response");
            request = MPI.COMM_WORLD.iSend(message.obj, message.obj.capacity(), MPI.BYTE, message.destination, message.tag);
        } else if (message.tag == LOAD_DSCLASS) {
            logger.debug("Sent LOAD_DSCLASS");
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) {
                    request = MPI.COMM_WORLD.iSend(message.clazz, message.clazz.capacity(), MPI.BYTE, node, message.tag);
                }
            }
        } else if (message.tag == RESERVE_OBJECT) {
            logger.debug("Sent RESERVE_OBJECT");
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) {
                    request = MPI.COMM_WORLD.iSend(message.objectID, message.objectID.capacity(), MPI.BYTE, node, message.tag);
                }
            }
        } else if (message.tag == SYNCHRONIZE) {
            logger.debug("Sent SYNC " + syncCounter);
            syncCounter++;
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) {
                    request = MPI.COMM_WORLD.iSend(ByteBuffer.allocateDirect(0), 0, MPI.BYTE, node, message.tag);
                }
            }
        } else if (message.tag == FINALIZE) {
            logger.debug("Sent FINALIZE");
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) {
                    request = MPI.COMM_WORLD.iSend(ByteBuffer.allocateDirect(0), 0, MPI.BYTE, node, message.tag);
                }
            }
        }
        return request;
    }
}
