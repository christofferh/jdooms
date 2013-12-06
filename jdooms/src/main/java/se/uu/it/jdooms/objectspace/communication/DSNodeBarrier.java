package se.uu.it.jdooms.objectspace.communication;

import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 * Barrier class for workers
 */
public class DSNodeBarrier {
    private static final Logger logger = Logger.getLogger(DSNodeBarrier.class);
    private ArrayList<Integer> waitingNodes;
    private Object localSynchronizer;
    private int clusterSize;
    private int nodeID;

    public DSNodeBarrier(int nodeID, int clusterSize) {
        this.clusterSize = clusterSize;
        this.nodeID = nodeID;
        waitingNodes = new ArrayList<Integer>(clusterSize);
    }

    /**
     * Adds a remote node to the list of nodes waiting at a barrier. When add is called and the list of workers
     * is full, the barrier is released and the local worker threads are notified.
     * @param nodeID the nodeID of the reported node
     */
    public void add(int nodeID) {
        waitingNodes.add(nodeID);
        if (waitingNodes.size() >= clusterSize) {
            waitingNodes.clear();
            synchronized (localSynchronizer) {
                localSynchronizer.notify();
            }
        }
    }

    /**
     * Initializes a barrier at the local node and registers the DSObjectSynchronize instance to be notified.
     * @param obj reference to the DSObjectSynchronize instance
     * @return false if the barrier was immediately released. Otherwise true
     */
    public boolean barrier(Object obj) {
        localSynchronizer = obj;
        waitingNodes.add(nodeID);
        if (waitingNodes.size() >= clusterSize) {
            waitingNodes.clear();
            synchronized (localSynchronizer) {
                localSynchronizer.notify();
            }
            return false;
        }
        return true;
    }
}
