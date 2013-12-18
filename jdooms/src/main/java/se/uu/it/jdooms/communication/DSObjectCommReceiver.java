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

/**
 * Receiver class
 */
class DSObjectCommReceiver {
    private static final Logger logger = Logger.getLogger(DSObjectCommReceiver.class);
    private DSObjectSpaceMap<Integer, Object> cache;
    private DSObjectSpaceMap<Integer, Object> tmp_cache;
    private DSObjectNodeBarrier dsObjectNodeBarrier;

    public DSObjectCommReceiver(DSObjectSpaceMap<Integer, Object> cache,
                                DSObjectSpaceMap<Integer, Object> tmp_cache,
                                DSObjectNodeBarrier dsObjectNodeBarrier) {
        this.cache = cache;
        this.tmp_cache = tmp_cache;
        this.dsObjectNodeBarrier = dsObjectNodeBarrier;
    }

    /**
     * Receive dispatcher method
     * @param status a status object from iProbe
     */
    public void receive(Status status) {
        Request request;
        try {
            int tag = status.getTag();
            int count = status.getCount(MPI.BYTE);
            int sender = status.getSource();
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(count);
            request = MPI.COMM_WORLD.iRecv(byteBuffer, count, MPI.BYTE, sender, tag);
            request.waitFor();
            switch (tag) {
                case RES_OBJECT_R:
                    gotResponse(Permission.Read, byteBuffer);
                    break;
                case RES_OBJECT_RW:
                    gotResponse(Permission.ReadWrite, byteBuffer);
                    break;
                case REQ_OBJECT_R:
                    gotRequest(Permission.Read, byteBuffer, sender);
                    break;
                case REQ_OBJECT_RW:
                    gotRequest(Permission.ReadWrite, byteBuffer, sender);
                    break;
                case RESERVE_OBJECT:
                    gotReserveObject(byteBuffer);
                    break;
                case LOAD_DSCLASS:
                    gotLoadDsClass(byteBuffer);
                    break;
                case SYNCHRONIZE:
                    gotSynchronize(sender);
                    break;
                default:
            }
        } catch (MPIException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method for reserving an object in the object space
     * @param byteBuffer a ByteBuffer containing the objectID to reserve
     */
    private void gotReserveObject(ByteBuffer byteBuffer) {
        int objectID = byteBuffer.getInt();
        logger.debug("Got RESERVE_OBJECT:" + objectID);
        DSObjectBaseImpl dsObjectBase = new DSObjectBaseImpl();
        dsObjectBase.setPermission(Permission.Read);
        dsObjectBase.setValid(false);
        tmp_cache.put(objectID, dsObjectBase);
    }

    /**
     * Method to load a DSClass
     * @param byteBuffer a ByteBuffer containing the class to load
     */
    private void gotLoadDsClass(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.capacity()];
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
     * @param byteBuffer the ByteBuffer containing the serialized object
     */
    private void gotResponse(Permission permission, ByteBuffer byteBuffer) {
        logger.debug("Got Response");
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        Object obj = SerializationUtils.deserialize(bytes);


        Object cacheObj = tmp_cache.get(((DSObjectBase)obj).getID());
        if (((DSObjectBase)cacheObj).getPermission() != Permission.ReadWrite) {
            if (permission == Permission.Read) {
                ((DSObjectBase)obj).setPermission(Permission.Read);
            } else {
                ((DSObjectBase)obj).setPermission(Permission.ReadWrite);
            }
            ((DSObjectBase)obj).setValid(true);
            tmp_cache.put(((DSObjectBase) obj).getID(), obj);
        }
    }

    /**
     * Method to respond to a getObject request from another node
     * @param permission the specified Permission
     * @param byteBuffer a ByteBuffer containing the requested objectID
     * @param destination the ID of the requester
     */
    private void gotRequest(Permission permission, ByteBuffer byteBuffer, int destination) {
        int objectID = byteBuffer.getInt();
        logger.debug("Got Request, objectid " + objectID + " permission: " + permission);
        Object obj = cache.get(objectID);

        if (obj != null && ((DSObjectBase)obj).getPermission() == Permission.ReadWrite) {
            sendResponse(permission, objectID, obj, destination);
        }
    }

    /**
     * Method to send a response to a requester
     * @param permission the specified Permission
     * @param objectID the objectID
     * @param obj the object to send
     * @param destination the ID of the requester
     */
    private void sendResponse(Permission permission, int objectID, Object obj, int destination) {
        DSObjectComm.enqueuMessage(new DSObjectCommMessage(((permission == Permission.Read) ? RES_OBJECT_R : RES_OBJECT_RW),
                destination,
                obj));
        if (permission == Permission.ReadWrite) {
            tmp_cache.setPermission(objectID, Permission.Read);
        }
    }
}
