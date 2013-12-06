package se.uu.it.jdooms.communication;

import org.apache.log4j.Logger;
import java.util.ArrayList;

/**
 * Barrier class for workers
 */
public class DSObjectNodeBarrier {
    private static final Logger logger = Logger.getLogger(DSObjectNodeBarrier.class);
    private final ArrayList<Integer> waitingNodes;
    private Object localSynchronizer;
    private final int clusterSize;
    private final int nodeID;

    public DSObjectNodeBarrier(int nodeID, int clusterSize) {
        this.clusterSize = clusterSize;
        this.nodeID = nodeID;
        waitingNodes = new ArrayList<Integer>(clusterSize);
    }

    /**
     * Adds a remote node to the list of nodes waiting at a barrier. When add is called and the list of workers
     * is full, the barrier is released and the local worker threads are notified.
     * @param nodeID the nodeID of the reported node
     */
    public synchronized void add(int nodeID) {
        waitingNodes.add(nodeID);
        if (waitingNodes.size() >= clusterSize) {
            waitingNodes.clear();
            localSynchronizer.notify();
        }
    }

    /**
     * Initializes a barrier at the local node and registers the DSObjectCommSynchronize instance to be notified.
     * @param obj reference to the DSObjectCommSynchronize instance
     * @return false if the barrier was immediately released. Otherwise true
     */
    public synchronized boolean barrier(Object obj) {
        localSynchronizer = obj;
        waitingNodes.add(nodeID);
        if (waitingNodes.size() >= clusterSize) {
            waitingNodes.clear();
            localSynchronizer.notify();
            return false;
        }
        return true;
    }
}
