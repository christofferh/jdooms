package se.uu.it.jdooms.communication;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

public class DSObjectCommFinalizer extends DSObjectCommSynchronize {
    private static final Logger logger = Logger.getLogger(DSObjectCommFinalizer.class);
    public DSObjectCommFinalizer(DSObjectComm dsObjectComm, DSObjectSpaceMap<Integer, Object> cache, DSObjectSpaceMap<Integer, Object> tmp_cache) {
        super(dsObjectComm, cache, tmp_cache);
    }

    @Override
    public void run() {
        synchronize();
        logger.debug("Finalized");
        dsObjectComm.dsFinalize();
    }
}
