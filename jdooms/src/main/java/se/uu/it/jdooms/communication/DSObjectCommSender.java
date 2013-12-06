package se.uu.it.jdooms.communication;

import mpi.MPI;
import mpi.MPIException;

import static se.uu.it.jdooms.communication.DSObjectComm.*;

public class DSObjectCommSender {
    private final int nodeID;
    private final int clusterSize;

    public DSObjectCommSender(DSObjectComm DSObjectComm) {
        nodeID = DSObjectComm.getNodeID();
        clusterSize = DSObjectComm.getClusterSize();
    }
    /**
     * Sends an DSObjectCommMessage via MPI
     * @param message the message to send
     * @throws mpi.MPIException
     */
    public void send(DSObjectCommMessage message) throws MPIException {
        if (message.tag == REQ_OBJECT_R || message.tag == REQ_OBJECT_RW) {
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) MPI.COMM_WORLD.send(message.objectID, 1, MPI.INT, node, message.tag);
            }
        } else if (message.tag == RET_OBJECT_R || message.tag == RET_OBJECT_RW) {
            MPI.COMM_WORLD.send(message.obj, message.obj.length, MPI.BYTE, message.destination, message.tag);
        } else if (message.tag == LOAD_DSCLASS) {
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) MPI.COMM_WORLD.send(message.clazz, message.clazz.length, MPI.CHAR, node, message.tag);
            }
        } else if (message.tag == SYNCHRONIZE) {
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) MPI.COMM_WORLD.send(message.synchronize, 1, MPI.BOOLEAN, node, message.tag);
            }
        }
    }
}
