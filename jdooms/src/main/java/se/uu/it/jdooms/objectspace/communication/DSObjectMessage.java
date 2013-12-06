package se.uu.it.jdooms.objectspace.communication;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

/**
 * Message structure for a DSOObject Message
 */
public class DSObjectMessage  {
    public int tag;
    public int destination;
    public int[] objectID;
    public char[] clazz;
    public byte[] obj;
    public boolean[] synchronize;

    /**
     * Message for REQ_OBJECT_R/W (Get object)
     * @param tag REQ_OBJECT_R/REQ_OBJECT_RW
     * @param objectID the object ID
     */
    public DSObjectMessage(int tag, int objectID) {
        this.tag = tag;
        this.objectID = new int[1];
        this.objectID[0] = objectID;
    }

    /**
     * Message for RES_OBJECT_R/W (Response to get object)
     * @param tag           RES_OBJECT_R or RES_OBJECT_RW
     * @param destination   the destination nodeID
     * @param obj           the object to send
     */
    public DSObjectMessage(int tag, int destination, Object obj) {
        this.tag = tag;
        this.destination = destination;
        this.obj = SerializationUtils.serialize((Serializable) obj);
    }

    /**
     * Message for LOAD_DSCLASS (Preloadsa class on the remote nodes)
     * @param tag           LOAD_DSCLASS
     * @param clazz         the class to load (fully qualified name)
     */
    public DSObjectMessage(int tag, String clazz) {
        this.tag = tag;
        this.clazz = new char[clazz.length()];
        this.clazz = clazz.toCharArray();
    }

    /**
     * Message for SYNCHRONIZE
     * @param tag SYNCHRONIZE
     */
    public DSObjectMessage(int tag) {
        this.tag = tag;
        this.synchronize = new boolean[1];
        this.synchronize[0] = true;
    }
}
