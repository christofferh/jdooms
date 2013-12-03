package se.uu.it.jdooms.objectspace;

import org.apache.log4j.Logger;

import java.util.ArrayList;

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

    public void add(int nodeID) {
        waitingNodes.add(nodeID);
        if (waitingNodes.size() >= clusterSize) {
            waitingNodes.clear();
            synchronized (localSynchronizer) {
                localSynchronizer.notify();
            }
        }
    }

    public int getSize() {
        return waitingNodes.size();
    }

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
