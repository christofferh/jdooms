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

public class DSObjectCommReceiver {
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

    public void receive(Status status) {
        Request request;
        try {
            int tag = status.getTag();
            int count = status.getCount(MPI.BYTE);
            int sender = status.getSource();
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(count);
            request = MPI.COMM_WORLD.iRecv(byteBuffer, count, MPI.BYTE, MPI.ANY_SOURCE, MPI.ANY_TAG);
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

    private void gotReserveObject(ByteBuffer byteBuffer) {
        int objectID = byteBuffer.getInt();
        logger.debug("Got RESERVE_OBJECT:" + objectID);
        DSObjectBaseImpl dsObjectBase = new DSObjectBaseImpl();
        dsObjectBase.setPermission(Permission.Read);
        dsObjectBase.setValid(false);
        cache.put(objectID, dsObjectBase);
    }

    private void gotLoadDsClass(ByteBuffer byteBuffer) {
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        String clazz = new String(bytes);
        logger.debug("Got LOAD_DSCLASS: " + clazz);
        DSObjectSpaceImpl.loadDSClass(clazz);
    }

    private void gotSynchronize(int nodeID) {
        logger.debug("Got SYNCHRONIZE from nodeID: " + nodeID);
        dsObjectNodeBarrier.add(nodeID);
    }

    private void gotResponse(Permission permission, ByteBuffer byteBuffer) {
        logger.debug("Got Response");
        byte[] bytes = new byte[byteBuffer.capacity()];
        byteBuffer.get(bytes);
        Object obj = SerializationUtils.deserialize(bytes);

        if (permission == Permission.Read) {
            ((DSObjectBase)obj).setPermission(Permission.Read);
        } else {
            ((DSObjectBase)obj).setPermission(Permission.ReadWrite);
        }
        ((DSObjectBase)obj).setValid(true);
        tmp_cache.put(((DSObjectBase) obj).getID(), obj);
    }

    private void gotRequest(Permission permission, ByteBuffer byteBuffer, int destination) {
        int objectID = byteBuffer.getInt();
        logger.debug("Got Request, objectid " + objectID);
        Object obj = cache.get(objectID);

        if (obj != null && ((DSObjectBase)obj).getPermission() == Permission.ReadWrite) { //TODO:kanske kolla om objektet är valid?
            sendResponse(permission, objectID, obj, destination);
        }
    }

    private void sendResponse(Permission permission, int objectID, Object obj, int destination) {
        DSObjectComm.enqueuMessage(new DSObjectCommMessage(((permission == Permission.Read) ? RES_OBJECT_R : RES_OBJECT_RW),
                destination,
                obj));
        if (permission == Permission.ReadWrite) {
            cache.setPermission(objectID, Permission.Read);
        }
    }
}
