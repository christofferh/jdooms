package se.uu.it.jdooms.workerdispatcher;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceImpl;

/**
 * Distributed object dispatcher
 */
public class DSObjectDispatcher {
    private static final Logger logger = Logger.getLogger(DSObjectDispatcher.class);
    private DSObjectSpaceImpl dsObjectSpace;
    private int workersPerNode;
    private int beginWorkerID;

    public DSObjectDispatcher(DSObjectSpaceImpl dsObjectSpace){
        this.dsObjectSpace = dsObjectSpace;
        this.workersPerNode = dsObjectSpace.getWorkerCount() / dsObjectSpace.getClusterSize();
        this.beginWorkerID = dsObjectSpace.getNodeID() * workersPerNode;
    }

    /**
     * Creates an instance of a DSObject and creates threads for every available processor
     * @param className Fully qualified name of a class implementing DSObject
     */
    public void startWorkers(String className) {
        DSObject worker;
        try {
            worker = instantiate(className, DSObject.class);
            worker.Init(dsObjectSpace);
            for (int i = beginWorkerID; i < beginWorkerID + workersPerNode; i++)
            {
                String workerID = Integer.toString(i);
                new Thread(worker, workerID).start();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates an instance of the fully qualified class name className
     * @param className fully qualified class name
     * @param type the return type
     * @param <T> the return type
     * @return A new instance of 'className' of type <T>
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws IllegalAccessException
     */
    private <T> T instantiate(final String className, final Class<T> type) throws InstantiationException, ClassNotFoundException, IllegalAccessException {
        ClassLoader loader = this.getClass().getClassLoader();
        Class worker = loader.loadClass(className);

        return (T)worker.newInstance();
    }
}
