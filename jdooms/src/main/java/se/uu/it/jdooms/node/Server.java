package se.uu.it.jdooms.node;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceImpl;
import se.uu.it.jdooms.workerdispatcher.DSObjectDispatcher;

/**
 * JDOOMS Server
 */
public class Server {
    private final DSObjectDispatcher dsObjectDispatcher;
    private final DSObjectSpaceImpl dsObjectSpace;
    private String[] args;
    private static final Logger logger = Logger.getLogger(Server.class);

	/**
     * Start the JDOOMS server
	 * @param args contains MPI parameters and Worker class
	 */
	public static void main(String[] args)   {
        Server server = new Server(args);
        server.start();
	}

    /**
     * JDOOMS server constructor
     * @param args program arguments
     */
    private Server(String[] args) {
        this.args = args;
        dsObjectSpace = new DSObjectSpaceImpl(args);
        dsObjectDispatcher = new DSObjectDispatcher(dsObjectSpace);
    }

    /**
     * Start the Worker classes
     */
    private void start(){
        if (dsObjectSpace.getNodeID() == 0)
        {
            dsObjectDispatcher.startWorkers(args);
        } else {
            dsObjectDispatcher.startWorkers(args);
        }
    }
}
