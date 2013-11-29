package se.uu.it.jdooms.objectspace;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import mpi.MPI;
import mpi.MPIException;
import mpi.Status;
import org.apache.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static se.uu.it.jdooms.objectspace.DSObjectSpace.*;

/**
 * Receiver class for MPI communication  between nodes
 */
public class DSObjectReceiver {
    private static final Logger logger = Logger.getLogger(DSObjectReceiver.class);
    private DSObjectSpaceImpl dsObjectSpace;
    private boolean receiving;

    public DSObjectReceiver(DSObjectSpaceImpl dsObjectSpace) {
        logger.info("Initiated");
        receiving = true;
        this.dsObjectSpace = dsObjectSpace;
    }

    /**
     * Finalize receiver
     */
    public void done() {
        receiving = false;
    }

    /**
     * Receive loop
     */
    public void receive() {
        Object[] receiveBuffer = new Object[1];
        Object[] sendBuffer = new Object[1];

        // @TODO Make non-blocking
        while(receiving) {
            Status mps = null;
            try {
                mps = MPI.COMM_WORLD.Recv(receiveBuffer, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, MPI.ANY_TAG);
            } catch (MPIException e) {
                e.printStackTrace();
            }
            switch (mps.getTag()) {
                case 10:
                    logger.debug("Got tag: " + mps.getTag() + " (putLocal)");
                    Object object = receiveBuffer[0];
                    dsObjectSpace.putLocalObject(object, Classifier.Shared);
                    break;
                case 20:
                    logger.debug("Got tag " + mps.getTag() + " from node " + mps.getSource() + " (getObject)");
                    int objectID = (Integer) receiveBuffer[0];
                    Object localObject = dsObjectSpace.getLocalObject(objectID, Permission.ReadWrite);
                    sendBuffer[0] = localObject;
                    if (localObject != null) {
                        logger.debug("Sending " + localObject + " to node " + mps.getSource());
                        try {
                            MPI.COMM_WORLD.Send(sendBuffer, 0, 1, MPI.OBJECT, mps.getSource(), 10);
                        } catch (MPIException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case 30:
                    break;
                default:
                    break;
            }
        }
    }

    private void loadDSClass(String clazz) {
        Method findLoadedClass = null;
        try {
            findLoadedClass = ClassLoader.class.getDeclaredMethod("findLoadedClass", new Class[] { String.class });
            findLoadedClass.setAccessible(true);
            ClassLoader cl = this.getClass().getClassLoader();
            Class tmp_cl = (Class) findLoadedClass.invoke(cl, clazz);
            if (tmp_cl == null) {
                ClassPool classPool = ClassPool.getDefault();
                try {
                    String path = System.getProperty("user.dir");
                    classPool.appendClassPath(path + "/out/production/jdooms-worker");
                    classPool.appendClassPath(path + "/../jdooms/bin");
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
                try {
                    CtClass ctClass = classPool.get(clazz);
                    CtClass superCtClass = classPool.get("se.uu.it.jdooms.objectspace.DSObjectBase");
                    CtClass ctSerializable = classPool.get("java.io.Serializable");

                    if (ctClass.isFrozen()) { ctClass.defrost(); }
                    ctClass.setSuperclass(superCtClass);
                    ctClass.addInterface(ctSerializable);
                    ctClass.toClass();
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
