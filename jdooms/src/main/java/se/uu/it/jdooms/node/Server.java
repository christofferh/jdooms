package se.uu.it.jdooms.node;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceImpl;
import se.uu.it.jdooms.workerdispatcher.DSObjectDispatcher;

import static se.uu.it.jdooms.objectspace.DSObjectSpace.*;

/**
 * JDOOMS server class
 */
public class Server {
    private DSObjectDispatcher dsObjectDispatcher;
    private DSObjectSpaceImpl dsObjectSpace;
    private static String workerName;
    private static final Logger logger = Logger.getLogger(Server.class);

	/**
     * Start the JDOOMS server
	 * @param args contains MPI parameters and Worker class
	 */
	public static void main(String[] args) {
        // Take string input of where to find the jar or class to load and execute
        workerName = args[0];
        Server server = new Server(args);
        server.start();
        //server.test();
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
     * Simple test method for storing an object local and then requesting it from other nodes
     */
    private void test() {
        Dummy apa = new Dummy(3, "Monkey");
        if (dsObjectSpace.getRank() == 0) {
            dsObjectSpace.putLocalObject(apa, Classifier.Shared);
        }

        if (dsObjectSpace.getRank() != 0) {
            Dummy apaNode1 = (Dummy) dsObjectSpace.getObject(apa.getID(), Permission.ReadWrite);
            logger.debug(apaNode1.getName());
        }
    }

    /**
     * Start the Worker classes
     */
    public void start(){
        if (dsObjectSpace.getRank() == 0)
        {
            dsObjectDispatcher.startWorkers(workerName);
        } else {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            dsObjectDispatcher.startWorkers(workerName);
        }
    }
}
