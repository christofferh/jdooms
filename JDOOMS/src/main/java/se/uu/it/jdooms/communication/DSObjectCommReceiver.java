package se.uu.it.jdooms.communication;

import mpi.MPI;
import mpi.MPIException;
import mpi.Request;
import mpi.Status;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.log4j.Logger;
import se.uu.it.jdooms.objectspace.DSObjectBase;
import se.uu.it.jdooms.objectspace.DSObjectBaseImpl;
import se.uu.it.jdooms.objectspace.DSObjectSpace.Permission;
import se.uu.it.jdooms.objectspace.DSObjectSpaceImpl;
import se.uu.it.jdooms.objectspace.DSObjectSpaceMap;

import static se.uu.it.jdooms.communication.DSObjectComm.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Receiver class
 */
class DSObjectCommReceiver {
    private static final Logger logger = Logger.getLogger(DSObjectCommReceiver.class);
    private DSObjectSpaceMap<Integer, Object> cache;
    private DSObjectNodeBarrier dsObjectNodeBarrier;
    private ByteBuffer byteBuffer;

    public DSObjectCommReceiver(DSObjectSpaceMap<Integer, Object> cache,
                                DSObjectNodeBarrier dsObjectNodeBarrier) {
        this.cache = cache;
        this.dsObjectNodeBarrier = dsObjectNodeBarrier;
        byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
    }

    /**
     * Receive dispatcher method
     * @param status a status object from iProbe
     */
    public void receive(Status status) {
        Request request;
        byteBuffer.position(0);
        try {
            int tag = status.getTag();
            int count = status.getCount(MPI.BYTE);
            int sender = status.getSource();

            request = MPI.COMM_WORLD.iRecv(byteBuffer, count, MPI.BYTE, sender, tag);
            request.waitFor();
            switch (tag) {
                case RES_OBJECT_R:
                    gotResponse(Permission.Read, count);
                    break;
                case RES_OBJECT_RW:
                    gotResponse(Permission.ReadWrite, count);
                    break;
                case REQ_OBJECT_R:
                    gotRequest(Permission.Read, sender);
                    break;
                case REQ_OBJECT_RW:
                    gotRequest(Permission.ReadWrite, sender);
                    break;
                case RESERVE_OBJECT:
                    gotReserveObject();
                    break;
                case LOAD_DSCLASS:
                    gotLoadDsClass(count);
                    break;
                case SYNCHRONIZE:
                    gotSynchronize(sender);
                    break;
                default:
                    break;
            }
        } catch (MPIException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method for reserving an object in the object space
     */
    private void gotReserveObject() {
        int objectID = byteBuffer.getInt();
        logger.debug("Got RESERVE_OBJECT:" + objectID);

        DSObjectBaseImpl dsObjectBase = new DSObjectBaseImpl();
        dsObjectBase.setPermission(Permission.Read);
        dsObjectBase.setValid(false);
        cache.put(objectID, dsObjectBase);
    }

    /**
     * Method to load a DSClass
     */
    private void gotLoadDsClass(int count) {
        byte[] bytes = new byte[count];
        byteBuffer.get(bytes);
        String clazz = new String(bytes);
        logger.debug("Got LOAD_DSCLASS: " + clazz);
        DSObjectSpaceImpl.loadDSClass(clazz);
    }

    /**
     * Method to add a nodeID to the barrier
     * @param nodeID the nodeID to add to the barrier
     */
    private void gotSynchronize(int nodeID) {
        logger.debug("Got SYNCHRONIZE from nodeID: " + nodeID);
        dsObjectNodeBarrier.add(nodeID);
    }

    /**
     * Method to store a response object in the object store
     * @param permission the specified Permission
     */
    private void gotResponse(Permission permission, int count) {
        logger.debug("Got Response");
        byte[] bytes = new byte[count];
        byteBuffer.get(bytes);
        Object obj = SerializationUtils.deserialize(bytes);

        if (permission == Permission.ReadWrite) {
            cache.addPermission(((DSObjectBase)obj).getID(), permission);
        }

        ((DSObjectBase)obj).setPermission(Permission.Read);
        ((DSObjectBase)obj).setValid(true);
        cache.put(((DSObjectBase) obj).getID(), obj);
    }

    /**
     * Method to respond to a getObject request from another node
     * @param permission the specified Permission
     * @param destination the ID of the requester
     */
    private void gotRequest(Permission permission, int destination) {
        int objectID = byteBuffer.getInt();
        logger.debug("Got Request, objectid " + objectID + " permission: " + permission);

        Object obj = cache.get(objectID);

        if (obj != null && ((DSObjectBase)obj).getPermission() == Permission.ReadWrite) {
            if (permission == Permission.ReadWrite) {
                cache.addPermission(objectID, Permission.Read);
            }
            sendResponse(permission, obj, destination);
        }
    }

    /**
     * Method to send a response to a requester
     * @param permission the specified Permission
     * @param obj the object to send
     * @param destination the ID of the requester
     */
    private void sendResponse(Permission permission, Object obj, int destination) {
        DSObjectComm.enqueuMessage(new DSObjectCommMessage(((permission == Permission.Read) ? RES_OBJECT_R : RES_OBJECT_RW),
                destination,
                obj));
    }
}
