package se.uu.it.jdooms.objectspace;

import org.apache.log4j.Logger;
import java.io.IOException;

import se.uu.it.jdooms.objectspace.DSObjectSpace.Permission;

/**
 * Communication class for MPI communication
 */
public class DSObjectCommunication implements Runnable {
    private static final Logger logger = Logger.getLogger(DSObjectCommunication.class);
    private DSObjectSpaceImpl dsObjectSpace;
    private DSObjectSpaceMap<Integer, Object> dsObjectSpaceMap;
    private DSObjectReceiver receiver;
    private DSObjectSender sender;

    public DSObjectCommunication(DSObjectSpaceImpl dsObjectSpace, DSObjectSpaceMap dsObjectSpaceMap) {
        this.dsObjectSpace = dsObjectSpace;
        this.dsObjectSpaceMap = dsObjectSpaceMap;
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
    public Object getObject(int ID, Permission permission) {
        sender.getObject(ID, permission);
        Object obj = null;
        dsObjectSpaceMap.addObserver(this);

        while (obj == null) {
            obj = dsObjectSpaceMap.get(ID);
            if (obj == null) {
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return  obj;
    }

    /**
     * Sends class to load on other nodes
     * @param clazz
     */
    public void loadDSClass(String clazz) {
        sender.loadDSClass(clazz);
    }

    /**
     * Synchronize call
     */
    public void synchronize() {
        logger.debug(dsObjectSpace.getNodeID() + " sent MPI Barrier");
        sender.barrier();
    }
}
