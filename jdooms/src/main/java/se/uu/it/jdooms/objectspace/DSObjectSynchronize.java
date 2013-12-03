package se.uu.it.jdooms.objectspace;


import org.apache.log4j.Logger;

public class DSObjectSynchronize implements Runnable {
    private static final Logger logger = Logger.getLogger(DSObjectSynchronize.class);
    private DSObjectCommunication dsObjectCommunication;
    private DSNodeBarrier dsNodeBarrier;

    public DSObjectSynchronize(DSObjectCommunication dsObjectCommunication) {
        this.dsObjectCommunication = dsObjectCommunication;
        dsNodeBarrier = dsObjectCommunication.getDsNodeBarrier();
    }

    @Override
    public void run() {
        /* DO SYNC STUFF */
        dsObjectCommunication.synchronize();
        if (dsNodeBarrier.barrier(this)) {
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
