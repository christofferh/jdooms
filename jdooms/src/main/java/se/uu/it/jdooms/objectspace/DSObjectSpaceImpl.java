package se.uu.it.jdooms.objectspace;

import javassist.*;
import mpi.MPI;
import org.apache.log4j.Logger;
import java.util.HashMap;

/**
 * Implementation of the Distributed Object Space
 */
public class DSObjectSpaceImpl implements DSObjectSpace {
    private static final Logger logger = Logger.getLogger(DSObjectSpaceImpl.class);
    private HashMap<Integer, Object> objectSpace;

    private int nodeId;
    private int clusterSize;
    private DSObjectCommunication dsObjectCommunication;
    private Thread dsObjectCommThread;

    public DSObjectSpaceImpl(String[] args) {
        objectSpace = new HashMap<Integer, Object>();

        MPI.Init(args);
        nodeId = MPI.COMM_WORLD.Rank();
        clusterSize = MPI.COMM_WORLD.Size();

        dsObjectCommunication = new DSObjectCommunication(this);
        dsObjectCommThread = new Thread(dsObjectCommunication);
        dsObjectCommThread.start();
    }

    /**
     * Return the node ID
     * @return the node Id
     */
    @Override
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Returns the cluster size
     * @return the cluster size
     */
    public int getClusterSize() {
        return clusterSize;
    }

    /**
     * Tries to get an object from the local object space, if unsuccessful, request it from the cluster.
     *
     * @param objectID the ID of the requested object
     */
    @Override
    public Object getObject(int objectID, Permission permission) {
        if (objectSpace.containsKey(objectID)) {
            return objectSpace.get(objectID);
        } else {
            return null;
        }

        /*else {
            dsObjectCommunication.getObject(objectID);
            while (!objectSpace.containsKey(objectID)) {}
            return objectSpace.get(objectID);
        }*/
    }

    /**
     * Stores an object in the local object space
     * @param obj
     * @param classifier
     */
    @Override
    public void putObject(Object obj, DSObjectSpace.Classifier classifier) {
        if (obj != null) {
            objectSpace.put(((DSObjectBase) obj).getID(), obj);
            dsObjectCommunication.putObject(obj);
        } else {
            //return DSObjectSender.broadcastPut(obj, classifier);
        }
    }

    /**
     * Distributed new
     * @param clazz class to be instantiated
     * @param ID uniquely identifiable number to the returning object
     * @return new instance of the input clazz
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    @Override
    public Object dsNew(String clazz, int ID) throws IllegalAccessException, InstantiationException {
        Object obj = null;
        obj = getObject(ID, Permission.ReadWrite); //What kind of permission should we use here.

        if(obj != null ) {
            logger.debug("fetched object");
            return obj;
        } else {
            logger.debug("creating object");
            ClassPool classPool = ClassPool.getDefault();
            try {
                classPool.appendClassPath("/Users/Christoffer/Code/project-it/jdooms-worker/out/production/jdooms-worker");
                classPool.appendClassPath("/Users/Christoffer/Code/project-it/jdooms/bin");
            } catch (NotFoundException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            logger.debug(classPool.toString());

            try {
                logger.debug("Trying to get ctclass");
                CtClass ctClass = classPool.get(clazz);

                logger.debug("Trying to get new superclass");
                CtClass superCtClass = classPool.get("se.uu.it.jdooms.objectspace.DSObjectBase");

                CtClass ctSerializable = classPool.get("java.io.Serializable");

                if (ctClass.isFrozen()) {
                    ctClass.defrost();
                }

                logger.debug("Trying to set new superclass");
                ctClass.setSuperclass(superCtClass); //check if alredy exists
                ctClass.addInterface(ctSerializable);//check if alredy exists


            /*logger.debug("filed printing");
            for (CtField field : ctClass.getDeclaredFields()) {
                logger.debug("field :" + field.getName());

            }*/


                logger.debug("Trying to create class from CtClass");
                Class cl = ctClass.toClass();

                logger.debug("Trying to create instance from class");
                obj = cl.newInstance();

                ((DSObjectBase)obj).setClassifier(Classifier.Shared);
                ((DSObjectBase)obj).setID(ID);

                putObject(obj, Classifier.Shared);
            } catch (NotFoundException e) {
                e.printStackTrace();
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }

            return obj;
        }
    }

    /**
     * For receiver to store objects
     * @param obj
     */
    public void storeObject(Object obj, DSObjectSpace.Classifier classifier) {
        if (obj != null) {
            objectSpace.put(((DSObjectBase) obj).getID(), obj);
        }
    }

    /**
     * Get local object
     * @param objectID
     * @param permission
     * @return
     */
    public Object getLocalObject(int objectID, Permission permission) {
        return objectSpace.get(objectID);
    }

    /**
     * Put local object
     * @param obj
     * @param classifier
     */
    public void putLocalObject(Object obj, Classifier classifier) {
        if (obj != null) {
            objectSpace.put(((DSObjectBase) obj).getID(), obj);
        }
    }

    /**
     * Bytecode injection test
     */
    public void beforeMethodTest() {
        logger.debug("Before Method");
    }
}
