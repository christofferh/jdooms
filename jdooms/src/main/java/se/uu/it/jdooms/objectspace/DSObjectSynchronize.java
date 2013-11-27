package se.uu.it.jdooms.objectspace;

public class DSObjectSynchronize implements Runnable {
    private DSObjectCommunication dsObjectCommunication;

    public DSObjectSynchronize(DSObjectCommunication dsObjectCommunication) {
        this.dsObjectCommunication = dsObjectCommunication;
    }

    @Override
    public void run() {
        /* DO SYNC STUFF */
        dsObjectCommunication.synchronize();
    }
}
