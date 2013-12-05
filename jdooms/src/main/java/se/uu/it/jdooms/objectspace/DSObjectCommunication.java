package se.uu.it.jdooms.objectspace;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import mpi.MPI;
import mpi.MPIException;
import mpi.Request;
import mpi.Status;
import org.apache.log4j.Logger;

import se.uu.it.jdooms.objectspace.DSObjectSpace.Permission;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Communication class for MPI communication
 */
public class DSObjectCommunication implements Runnable {
    private static final Logger logger = Logger.getLogger(DSObjectCommunication.class);
    public static final int PUT_LOCAL       = 10;
    public static final int GET_OBJECT      = 20;
    public static final int LOAD_DSOBJECT   = 30;
    public static final int SYNCHRONIZE     = 40;

    private int nodeID;
    private int clusterSize;
    private int workerCount;
    private boolean receiving;

    private DSObjectSpaceImpl dsObjectSpace;
    private DSObjectSpaceMap<Integer, Object> dsObjectSpaceMap;
    private Queue<DSObjectMessage> queue;
    private DSNodeBarrier dsNodeBarrier;

    private DSObjectReceiver receiver;
    private DSObjectSender sender;

    public DSObjectCommunication(String[] args, DSObjectSpaceImpl dsObjectSpace, DSObjectSpaceMap dsObjectSpaceMap, Queue<DSObjectMessage> queue) {
        this.dsObjectSpace = dsObjectSpace;
        this.dsObjectSpaceMap = dsObjectSpaceMap;
        this.queue = queue;
        receiving = true;

        try {
            MPI.Init(args);
            nodeID = MPI.COMM_WORLD.Rank();
            clusterSize = MPI.COMM_WORLD.Size();
            workerCount = Integer.valueOf(args[1]) * clusterSize;
            Thread.currentThread().setName("COMM-" + nodeID);
        } catch (MPIException e) {
            e.printStackTrace();
        }

        dsNodeBarrier = new DSNodeBarrier(nodeID, clusterSize);
        //receiver = new DSObjectReceiver(this, dsObjectSpace);
        sender = new DSObjectSender(this, dsObjectSpace);
    }

    public void putQueue(int tag, Object obj) {
        DSObjectMessage message = new DSObjectMessage(tag, obj);
        queue.offer(message);
        logger.debug("heere!");
    }

    /**
     * Start the communication
     */
    @Override
    public void run() {
        DSObjectMessage message = null;
        Object[] recvBuffer = new Object[1];
        Request request = null;
        while (receiving) {
            try {
                request = MPI.COMM_WORLD.Irecv(recvBuffer, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, MPI.ANY_TAG);
            } catch (MPIException e) {
                e.printStackTrace();
            }
            message = queue.poll();
            if (message != null) {
                int tag = message.tag;
                if (tag == GET_OBJECT) {
                    sender.getObject((Integer) message.obj, Permission.Read);
                } else if (tag == LOAD_DSOBJECT) {
                    sender.loadDSClass((String) message.obj);
                } else if (tag == SYNCHRONIZE) {
                    sender.synchronize();
                }
            } else {

                try {
                    //MPI.COMM_WORLD.Iprobe(MPI.ANY_SOURCE, MPI.ANY_TAG, status);
                    Status status = null;
                    if (request.Test(status)) {
                        int tag = status.getTag();
                        if (tag == PUT_LOCAL) {
                            status = MPI.COMM_WORLD.Recv(recvBuffer, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, PUT_LOCAL);
                            Object obj = recvBuffer[0];
                            dsObjectSpaceMap.put(((DSObjectBase)obj).getID(), Permission.Read);
                        } else if (tag == GET_OBJECT) {
                            status = MPI.COMM_WORLD.Recv(recvBuffer, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, GET_OBJECT);
                            int objectId = (Integer) recvBuffer[0];
                            Object obj = dsObjectSpaceMap.get(objectId);
                            if (obj != null) {
                                queue.offer(new DSObjectMessage(PUT_LOCAL, obj, status.getSource()));
                            }
                        } else if (tag == LOAD_DSOBJECT) {
                            status = MPI.COMM_WORLD.Recv(recvBuffer, 0, 1, MPI.OBJECT, MPI.ANY_SOURCE, LOAD_DSOBJECT);
                            String clazz = (String) recvBuffer[0];
                            loadDSClass(clazz);
                        } else if (tag == SYNCHRONIZE) {
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
     * Request object from the nodes
     * @param ID the ID of the object
     */
    public Object getObject(int ID, Permission permission) {
        return null;
    }

    public DSNodeBarrier getDsNodeBarrier() {
        return dsNodeBarrier;
    }

    public int getNodeID() {
        return nodeID;
    }

    public int getClusterSize() {
        return clusterSize;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public class DSObjectMessage {
        private int tag;
        private Object obj;
        private int destination;

        public DSObjectMessage(int tag, Object obj) {
            this.tag = tag;
            this.obj = obj;
            this.destination = -1;
        }

        public DSObjectMessage(int tag, Object obj, int destination) {
            this.tag = tag;
            this.obj = obj;
            this.destination = destination;
        }

        public int getTag() {
            return tag;
        }

        public Object getObject() {
            return obj;
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
