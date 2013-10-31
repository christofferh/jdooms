package se.uu.it.jdooms.workerdispatcher;

import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectSpaceImpl;

/**
 * Distributed object dispatcher.
 */
public class DSObjectDispatcher {
    private static final Logger logger = Logger.getLogger(DSObjectDispatcher.class);
    private DSObjectSpaceImpl dsObjectSpace;
    private Thread[] threadArray;

    public DSObjectDispatcher(DSObjectSpaceImpl dsObjectSpace){
        this.dsObjectSpace = dsObjectSpace;
    }

    /**
     * Creates an instance of a DSObject and creates threads for every available processor
     * @param className Fully qualified name of a class implementing DSObject
     */
    public void startWorkers(String className) {
        DSObject worker = null;
        try {
            worker = instantiate(className, DSObject.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        assert worker != null;
        worker.Init(dsObjectSpace);
        for(int i = 0; i < 1/*Runtime.getRuntime().availableProcessors()*/; i++)
        {
            new Thread(worker).start();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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

    /**
     * Interface method for DSObjects to finalize themselves
     * @param obj The object to be finalized
     */
    /*@Override
    public void done(DSObject obj) {
        logger.debug("Finalizer");
        //To change body of implemented methods use File | Settings | File Templates.
        for (int i = 0; i < threadArray.length; i++) {
            if () {
                logger.debug("Removing object from thread array");
                threadArray[i] = null;
            }
        }
    }*/
}
