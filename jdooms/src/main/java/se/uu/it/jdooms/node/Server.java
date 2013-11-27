package se.uu.it.jdooms.node;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceImpl;
import se.uu.it.jdooms.workerdispatcher.DSObjectDispatcher;


/**
 * JDOOMS server class
 */
public class Server {
    private DSObjectDispatcher dsObjectDispatcher;
    private DSObjectSpaceImpl dsObjectSpace;
    private static String className;
    private static final Logger logger = Logger.getLogger(Server.class);

	/**
     * Start the JDOOMS server
	 * @param args contains MPI parameters and Worker class
	 */
	public static void main(String[] args)   {
        // Take string input of where to find the jar or class to load and execute
        className = args[0];
        Server server = new Server(args);
        server.start();
	}

    /**
     * JDOOMS server constructor
     * @param args
     */
    public Server(String[] args){
        dsObjectSpace = new DSObjectSpaceImpl(args);
        dsObjectDispatcher = new DSObjectDispatcher(dsObjectSpace);
    }

    /**
     * Start the Worker classes
     */
    public void start(){
        if (dsObjectSpace.getNodeID() == 0)
        {
            dsObjectDispatcher.startWorkers(className);
        } else {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dsObjectDispatcher.startWorkers(className);
        }
    }
}
