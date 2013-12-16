package se.uu.it.jdooms.workerdispatcher;

import se.uu.it.jdooms.objectspace.DSObjectSpace;

/**
 * Interface for user to implement in each distributed object.
 */
interface DSObject extends Runnable {
    void Init(DSObjectSpace objSpace);
}
