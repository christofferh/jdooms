package se.uu.it.jdooms.objectspace;

import mpi.MPI;
import mpi.Status;
import org.apache.log4j.Logger;
import static se.uu.it.jdooms.objectspace.DSObjectSpace.*;

/**
 * Receiver class for MPI communication  between nodes
 */
public class DSObjectReceiver {
    private static final Logger logger = Logger.getLogger(DSObjectReceiver.class);
    private DSObjectSpaceImpl dsObjectSpace;
    private boolean receiving;

    public DSObjectReceiver(DSObjectSpaceImpl dsObjectSpace) {
        logger.info("Initiated");
        receiving = true;
        this.dsObjectSpace = dsObjectSpace;
    }

    /**
     * Finalize receiver
     */
    public void done() {
        receiving = false;
    }

    /**
     * Receive loop
     */
    public void receive() {
        Object[] receiveBuffer = new Object[1];
        Object[] sendBuffer = new Object[1];

        // @TODO Make non-blocking
        while(receiving) {
            Status mps = MPI.COMM_WORLD.Recv(receiveBuffer, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, MPI.ANY_TAG);
            switch (mps.tag) {
                case 10:
                    logger.debug("Got tag: " + mps.tag + " (putLocal)");
                    Object object = receiveBuffer[0];
                    dsObjectSpace.putLocalObject(object, Classifier.Shared);
                    break;
                case 20:
                    logger.debug("Got tag " + mps.tag + " from node " + mps.source + " (getObject)");
                    int objectID = (Integer) receiveBuffer[0];
                    Object localObject = dsObjectSpace.getLocalObject(objectID, Permission.ReadWrite);
                    sendBuffer[0] = localObject;
                    if (localObject != null) {
                        logger.debug("Sending " + localObject + " to node " + mps.source);
                        MPI.COMM_WORLD.Send(sendBuffer, 0, 1, MPI.OBJECT, mps.source, 10);
                    }
                    break;
                case 30:
                    break;
                default:
                    break;
            }
        }
    }

}
