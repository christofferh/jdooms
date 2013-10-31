package se.uu.it.jdooms.workerdispatcher;

/**
 * Interface for distributed objects to finalize themselves.
 */
public interface DSObjectFinalizer {
    void done(DSObject obj);
}
