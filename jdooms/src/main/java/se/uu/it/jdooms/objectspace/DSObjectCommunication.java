package se.uu.it.jdooms.objectspace;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import mpi.*;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.log4j.Logger;

import se.uu.it.jdooms.objectspace.DSObjectSpace.Permission;
import se.uu.it.jdooms.workerdispatcher.DSObject;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;

/**
 * Communication class for MPI communication
 */
public class DSObjectCommunication implements Runnable {
    private static final Logger logger = Logger.getLogger(DSObjectCommunication.class);
    public static final int GET_OBJECT_R     = 10;
    public static final int GET_OBJECT_RW    = 15;
    public static final int RET_OBJECT_R     = 20;
    public static final int RET_OBJECT_RW    = 25;
    public static final int LOAD_DSOBJECT    = 30;
    public static final int SYNCHRONIZE      = 40;

    private int nodeID;
    private int clusterSize;
    private int workerCount;
    private boolean receiving;

    private DSObjectSpaceImpl dsObjectSpace;
    private DSObjectSpaceMap<Integer, Object> dsObjectSpaceMap;
    private Queue<DSObjectMessage> queue;
    private DSNodeBarrier dsNodeBarrier;

    public DSObjectCommunication(String[] args, DSObjectSpaceImpl dsObjectSpace, DSObjectSpaceMap dsObjectSpaceMap, Queue<DSObjectMessage> queue) {
        this.dsObjectSpace = dsObjectSpace;
        this.dsObjectSpaceMap = dsObjectSpaceMap;
        this.queue = queue;

        try {
            MPI.Init(args);
            nodeID = MPI.COMM_WORLD.getRank();
            clusterSize = MPI.COMM_WORLD.getSize();
            workerCount = Integer.valueOf(args[1]) * clusterSize;
        } catch (MPIException e) {
            e.printStackTrace();
        }
        dsNodeBarrier = new DSNodeBarrier(nodeID, clusterSize);
        receiving = true;
    }

    /**
     * Returns the local nodeID
     * @return the nodeID
     */
    public int getNodeID() {
        return nodeID;
    }

    /**
     * Returns the total cluster size
     * @return the clusterSize
     */
    public int getClusterSize() {
        return clusterSize;
    }

    /**
     * Returns the total worker count
     * @return the worker count
     */
    public int getWorkerCount() {
        return workerCount;
    }

    /**
     * Return the node barrier
     * @return the node barrier
     */
    public DSNodeBarrier getDsNodeBarrier() {
        return dsNodeBarrier;
    }
    /**
     * Start the communication
     */
    @Override
    public void run() {
        Thread.currentThread().setName("COMM-" + nodeID);
        DSObjectMessage message;
        Status status;
        while (receiving) {
            message = queue.poll();
            if (message != null) {
                try {
                    send(message);
                } catch (MPIException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    status = MPI.COMM_WORLD.iProbe(MPI.ANY_SOURCE, MPI.ANY_TAG);
                    if (status != null) {
                        int tag = status.getTag();
                        if (tag == RET_OBJECT_R || tag == RET_OBJECT_RW) {
                            logger.debug("RET");
                            byte[] byte_buffer = new byte[status.getCount(MPI.BYTE)];
                            Object obj;
                            MPI.COMM_WORLD.recv(byte_buffer, byte_buffer.length, MPI.BYTE, MPI.ANY_SOURCE, tag);
                            obj = SerializationUtils.deserialize(byte_buffer);
                            dsObjectSpaceMap.put(((DSObjectBase)obj).getID(), obj);
                        } else if (tag == GET_OBJECT_R || tag == GET_OBJECT_RW) {
                            logger.debug("GET");
                            int[] int_buffer = new int[1];
                            MPI.COMM_WORLD.recv(int_buffer, 1, MPI.INT, MPI.ANY_SOURCE, tag);
                            int objectId = int_buffer[0];
                            Object obj = dsObjectSpaceMap.get(objectId);
                            if (obj != null) {
                                queue.offer(new DSObjectMessage( ((tag == GET_OBJECT_R) ? RET_OBJECT_R : RET_OBJECT_RW), status.getSource(), obj));
                            }
                        } else if (tag == LOAD_DSOBJECT) {
                            char[] byte_buffer = new char[status.getCount(MPI.CHAR)];
                            String clazz;
                            MPI.COMM_WORLD.recv(byte_buffer, byte_buffer.length, MPI.CHAR, MPI.ANY_SOURCE, tag);
                            clazz = new String(byte_buffer);
                            loadDSClass(clazz);
                        } else if (tag == SYNCHRONIZE) {
                            logger.debug("SYNC");
                            boolean[] bool_buffer = new boolean[1];
                            MPI.COMM_WORLD.recv(bool_buffer, 1,  MPI.BOOLEAN, MPI.ANY_SOURCE, tag);
                            dsNodeBarrier.add(status.getSource());
                        }
                    }
                } catch (MPIException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Puts a synchronize message on the queue and self-invalidates its local cache
     */
    public void synchronize() {
        queue.offer(new DSObjectMessage(SYNCHRONIZE));
        dsObjectSpaceMap.selfInvalidate();
    }

    /**
     * Puts a loadDSClass message on the queue
     * @param clazz the class to load
     */
    public void enqueueloadDSClass(String clazz) {
        queue.offer(new DSObjectMessage(LOAD_DSOBJECT, clazz));
    }

    /**
     * Request object from the nodes
     * @param objectID the ID of the object
     */
    public Object getObject(int objectID, Permission permission) {
        if (permission == Permission.Read) {
            queue.offer(new DSObjectMessage(GET_OBJECT_R, objectID));
        } else {
            queue.offer(new DSObjectMessage(GET_OBJECT_RW, objectID));
        }
        Object obj = null;
        dsObjectSpaceMap.addObserver(this);

        while (obj == null) {
            obj = dsObjectSpaceMap.get(objectID);
            if (obj == null) {
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        dsObjectSpaceMap.removeObserver(this);
        ((DSObjectBase)obj).setPermission(permission);
        ((DSObjectBase)obj).setValid(true);

        return obj;
    }

    /**
     * Sends an DSObjectMessage via MPI
     * @param message the message to send
     * @throws MPIException
     */
    private void send(DSObjectMessage message) throws MPIException {
        if (message.tag == GET_OBJECT_R || message.tag == GET_OBJECT_RW) {
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) MPI.COMM_WORLD.send(message.objectID, 1, MPI.INT, node, message.tag);
            }
        } else if (message.tag == RET_OBJECT_R || message.tag == RET_OBJECT_RW) {
            MPI.COMM_WORLD.send(message.obj, message.obj.length, MPI.BYTE, message.destination, message.tag);
        } else if (message.tag == LOAD_DSOBJECT) {
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) MPI.COMM_WORLD.send(message.clazz, message.clazz.length, MPI.CHAR, node, message.tag);
            }
        } else if (message.tag == SYNCHRONIZE) {
            for (int node = 0; node < clusterSize; node++) {
                if (node != nodeID) MPI.COMM_WORLD.send(message.synchronize, 1, MPI.BOOLEAN, node, message.tag);
            }
        }
    }

    /**
     * Loads a specified DSClass in the class loader
     * @param clazz fully qualified name of the class to load
     */
    private void loadDSClass(String clazz) {
        logger.debug("loadDSClass: " + clazz);
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

    /**
     * Class for the communication queues messages
     */
    class DSObjectMessage {
        public int tag;
        public int destination;
        public int[] objectID;
        public char[] clazz;
        public byte[] obj;
        public boolean[] synchronize;

        public DSObjectMessage(int tag, int objectID) {
            this.tag = tag;
            this.objectID = new int[1];
            this.objectID[0] = objectID;
        }

        public DSObjectMessage(int tag, int destination, Object obj) {
            this.tag = tag;
            this.destination = destination;
            this.obj = SerializationUtils.serialize((Serializable) obj);
        }

        public DSObjectMessage(int tag, String clazz) {
            this.tag = tag;
            this.clazz = new char[clazz.length()];
            this.clazz = clazz.toCharArray();
        }

        public DSObjectMessage(int tag) {
            this.tag = tag;
            this.synchronize = new boolean[1];
            this.synchronize[0] = true;
        }

        public int getTag() {
            return tag;
        }

        public int getObjectID() {
            return objectID[0];
        }

        public int getDestination() {
            return destination;
        }

        public String getClazz() {
            return clazz.toString();
        }
    }
}



