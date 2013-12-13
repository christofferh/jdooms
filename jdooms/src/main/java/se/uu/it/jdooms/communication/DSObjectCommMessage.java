package se.uu.it.jdooms.communication;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.nio.ByteBuffer;
/**
 * Message structure for a DSOObject Message
 */
public class DSObjectCommMessage {
    private static final Logger logger = Logger.getLogger(DSObjectCommMessage.class);
    public int tag;
    public int destination;
    public ByteBuffer data;

    /**
     * Message for REQ_OBJECT_R/W (Get object) / RESERVE_OBJECT
     * @param tag REQ_OBJECT_R/REQ_OBJECT_RW/RESERVE_OBJECT
     * @param objectID the object ID
     */
    public DSObjectCommMessage(int tag, int objectID) {
        this.tag = tag;
        this.data = ByteBuffer.allocateDirect(4).putInt(objectID);
    }

    /**
     * Message for RES_OBJECT_R/W (Response to get object)
     * @param tag           RES_OBJECT_R or RES_OBJECT_RW
     * @param destination   the destination nodeID
     * @param obj           the object to send
     */
    public DSObjectCommMessage(int tag, int destination, Object obj) {
        this.tag = tag;
        this.destination = destination;
        byte[] serialized = SerializationUtils.serialize((Serializable) obj); //om man gör detta parrallel måste org objektets permission sättas när objektet är serialiserat.
        this.data = ByteBuffer.allocateDirect(serialized.length);
        this.data.put(serialized);
    }

    /**
     * Message for LOAD_DSCLASS (Preloadsa class on the remote nodes)
     * @param tag           LOAD_DSCLASS
     * @param clazz         the class to load (fully qualified name)
     */
    public DSObjectCommMessage(int tag, String clazz) {
        this.tag = tag;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(clazz.toCharArray().length);
        byteBuffer.put(clazz.getBytes());
        this.data = byteBuffer;
    }

    /**
     * Message for SYNCHRONIZE/FINALIZE
     * @param tag SYNCHRONIZE/FINALIZE
     */
    public DSObjectCommMessage(int tag) {
        this.tag = tag;
        this.data = ByteBuffer.allocateDirect(0);
    }
}
