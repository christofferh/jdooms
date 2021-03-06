package se.uu.it.jdooms.objectspace;

/**
 * Interface to the abstract base for all distributed objects
 */
public interface DSObjectBase {
    public int getID();

    public void setID(int ID);

    DSObjectSpace.Permission getPermission();

    void setPermission(DSObjectSpace.Permission permission);

    boolean isValid();

    void setValid(boolean valid);
}
