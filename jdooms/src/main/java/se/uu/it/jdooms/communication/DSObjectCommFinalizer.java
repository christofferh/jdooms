package se.uu.it.jdooms.communication;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

/**
 * Finalizer class
 * Shuts JDOOMS down gracefully.
 * @TODO: Doesn't always shutdown gracefully, fix.
 */
public class DSObjectCommFinalizer extends DSObjectCommSynchronize {
    private static final Logger logger = Logger.getLogger(DSObjectCommFinalizer.class);
    public DSObjectCommFinalizer(DSObjectComm dsObjectComm, DSObjectSpaceMap<Integer, Object> cache) {
        super(dsObjectComm, cache);
    }

    @Override
    public void run() {
        synchronize();
        logger.debug("Finalized");
        dsObjectComm.dsFinalize();
    }
}
