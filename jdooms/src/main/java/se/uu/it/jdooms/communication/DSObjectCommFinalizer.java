package se.uu.it.jdooms.communication;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

public class DSObjectCommFinalizer extends DSObjectCommSynchronize {
    private static final Logger logger = Logger.getLogger(DSObjectCommFinalizer.class);
    public DSObjectCommFinalizer(DSObjectComm dsObjectComm, DSObjectSpaceMap<Integer, Object> objectSpaceMap) {
        super(dsObjectComm, objectSpaceMap);
    }

    @Override
    public void run() {
        super.run();
        logger.debug("Finalized");
        dsObjectComm.dsFinalize();
    }
}
