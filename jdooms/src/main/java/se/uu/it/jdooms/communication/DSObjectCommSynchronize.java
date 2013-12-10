package se.uu.it.jdooms.communication;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

/**
 * Last worker on the node sends a synchronization messsage to the cluster and waits
 * for the other nodes to synchronize
 */
public class DSObjectCommSynchronize implements Runnable {
    private static final Logger logger = Logger.getLogger(DSObjectCommSynchronize.class);
    private final DSObjectComm DSObjectComm;
    private final DSObjectNodeBarrier DSObjectNodeBarrier;
    private final DSObjectSpaceMap<Integer, Object> objectSpaceMap;

    public DSObjectCommSynchronize(DSObjectComm DSObjectComm, DSObjectSpaceMap<Integer, Object> objectSpaceMap) {
        this.DSObjectComm = DSObjectComm;
        DSObjectNodeBarrier = DSObjectComm.getDSObjectNodeBarrier();
        this.objectSpaceMap = objectSpaceMap;
    }

    @Override
    public void run() {
        DSObjectComm.synchronize();
        if (DSObjectNodeBarrier.barrier(this)) {
            try {
                synchronized (this) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        objectSpaceMap.selfInvalidate();
        logger.debug("Worker passed barrier");
    }
}
