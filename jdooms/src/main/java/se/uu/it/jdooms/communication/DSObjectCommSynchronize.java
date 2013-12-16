package se.uu.it.jdooms.communication;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

/**
 * Last worker on the node sends a synchronization message to the cluster and waits
 * for the other nodes to synchronize
 */
public class DSObjectCommSynchronize implements Runnable {
    private static final Logger logger = Logger.getLogger(DSObjectCommSynchronize.class);
    protected final DSObjectComm dsObjectComm;
    private final DSObjectNodeBarrier dsObjectNodeBarrier;
    private final DSObjectSpaceMap<Integer, Object> objectSpaceMap;

    public DSObjectCommSynchronize(DSObjectComm dsObjectComm, DSObjectSpaceMap<Integer, Object> objectSpaceMap) {
        this.dsObjectComm = dsObjectComm;
        dsObjectNodeBarrier = dsObjectComm.getDsObjectNodeBarrier();
        this.objectSpaceMap = objectSpaceMap;
    }

    @Override
    public void run() {
        dsObjectComm.synchronize();
        if (dsObjectNodeBarrier.barrier(this)) {
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
