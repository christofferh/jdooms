package se.uu.it.jdooms.objectspace;

import mpi.MPI;
import mpi.MPIException;
import org.apache.log4j.Logger;
import java.io.*;
import static se.uu.it.jdooms.objectspace.DSObjectSpace.*;

/**
 * Sender class for MPI communication between nodes.
 */
public class DSObjectSender {
    private static final Logger logger = Logger.getLogger(DSObjectSender.class);
    private DSObjectSpaceImpl dsObjectSpace;

    public DSObjectSender(DSObjectSpaceImpl dsObjectSpace) {
        logger.debug("Initiated");

        this.dsObjectSpace = dsObjectSpace;
    }

    /**
     * Broadcasts a object to the cluster
     * @param object the object to be sent
     * @throws IOException
     */
    protected void broadcastPut(Object object) throws IOException {
        Object[] sendBuffer = new Object[1];
        sendBuffer[0] = object;

        /* @TODO: Make non-blocking */
        for (int node = 0; node < dsObjectSpace.getClusterSize(); node++) {
            if (node != dsObjectSpace.getNodeID()) {
                try {
                    MPI.COMM_WORLD.Send(sendBuffer, 0, 1, MPI.OBJECT, node, 10);
                } catch (MPIException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Requests the object with a certain hashcode from the cluster.
     * @param ID the ID of the object
     */
    protected void getObject(int ID) {
        Object[] sendBuffer = new Object[1];
        sendBuffer[0] = (Integer) ID;
        /* @TODO: Make non-blocking */
        for (int node = 0; node < dsObjectSpace.getClusterSize(); node++) {
            if (node != dsObjectSpace.getNodeID()) {
                try {
                    MPI.COMM_WORLD.Send(sendBuffer, 0, 1, MPI.OBJECT, node, 20);
                } catch (MPIException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Send a MPI barrier call to all the other nodes
     */
    protected void barrier() {
        try {
            MPI.COMM_WORLD.Barrier();
        } catch (MPIException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param clazz
     */
    protected void loadDSClass(String clazz) {
        Object[] sendBuffer = new Object[1];
        sendBuffer[0] = clazz;
        for (int node = 0; node < dsObjectSpace.getClusterSize(); node++) {
            if (node != dsObjectSpace.getNodeID()) {
                try {
                    MPI.COMM_WORLD.Send(sendBuffer, 0, 1, MPI.OBJECT, node, 30);
                } catch (MPIException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sends an object with a permission to a specific node
     * @param nodeId        the receiver node ID
     * @param object        the object to send
     * @param permission    the permission of the object
     */
    protected void sendObject(int nodeId, Object object, Permission permission) {

    }

    /**
     * Deletes an object with a specific hashcode on the specified node
     * @param nodeId    the node ID to delete from
     * @param hashId    the hashcode of the object
     */
    protected void deleteObject(int nodeId, int hashId) {

    }

    /**
     * Gets permission for an object with the specified hashcode
     * @param hashCode      the hashcode of the object
     * @return Permission
     */
    protected Permission getPermission(int hashCode) {
        return null;
    }

    /**
     * Sets the permision for an object with the specified hashcode
     * @param hashCode      the hashcode of the object
     * @param permission    the permission to set
     */
    protected void setPermission(int hashCode, Permission permission){

    }

    /**
     * Gets the classifier for an object with the specified hashcode
     * @param hashCode      the hashcode of the object
     * @return Classifier
     */
    protected Classifier getClassifier(int hashCode) {
        return null;
    }

    /**
     * Sets the classifier for and object with the specified hashcode
     * @param hashCode      the hashcode of the object
     * @param classifier    the classifier to set
     */
    protected void setClassifier(int hashCode, Classifier classifier) {
    }
}


