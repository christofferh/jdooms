package se.uu.it.jdooms.workerdispatcher;

import se.uu.it.jdooms.objectspace.DSObjectSpace;

/**
 * Interface for user to implement in each distributed object.
 */
public interface DSObject extends Runnable {
    void Init(String[] args, DSObjectSpace objSpace);
}
