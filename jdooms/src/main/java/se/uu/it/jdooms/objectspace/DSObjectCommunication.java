package se.uu.it.jdooms.objectspace;

import org.apache.log4j.Logger;
import java.io.IOException;

/**
 * Communication class for MPI communication
 */
public class DSObjectCommunication implements Runnable {
    private static final Logger logger = Logger.getLogger(DSObjectCommunication.class);
    private DSObjectSpaceImpl dsObjectSpace;
    private DSObjectReceiver receiver;
    private DSObjectSender sender;

    public DSObjectCommunication(DSObjectSpaceImpl dsObjectSpace) {
        this.dsObjectSpace = dsObjectSpace;
        receiver = new DSObjectReceiver(this.dsObjectSpace);
        sender = new DSObjectSender(this.dsObjectSpace);
    }

    /**
     * Start the communication
     */
    @Override
    public void run() {
        receiver.receive();
    }

    /**
     * Finalize the receiving
     */
    public void done() {
        receiver.done();
    }

    /**
     * Send out object to nodes
     * @param obj the object to store
     */
    public void putObject(Object obj) {
        try {
            sender.broadcastPut(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Request object from the nodes
     * @param ID the ID of the object
     */
    public void getObject(int ID) {
        sender.getObject(ID);
    }
}
