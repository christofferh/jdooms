package se.uu.it.jdooms.communication;

import org.apache.log4j.Logger;

/**
 * Last worker on the node sends a synchronization messsage to the cluster and waits
 * for the other nodes to synchronize
 */
public class DSObjectCommSynchronize implements Runnable {
    private static final Logger logger = Logger.getLogger(DSObjectCommSynchronize.class);
    private final DSObjectComm DSObjectComm;
    private final DSObjectNodeBarrier DSObjectNodeBarrier;

    public DSObjectCommSynchronize(DSObjectComm DSObjectComm) {
        this.DSObjectComm = DSObjectComm;
        DSObjectNodeBarrier = DSObjectComm.getDSObjectNodeBarrier();
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
    }
}
