package se.uu.it.jdooms.workerdispatcher;

/**
 * Interface for distributed objects to dsFinalize themselves.
 */
public interface DSObjectFinalizer {
    /**
     * Finalizes the worker
     * @param obj reference to the worker
     */
    void done(DSObject obj);
}
