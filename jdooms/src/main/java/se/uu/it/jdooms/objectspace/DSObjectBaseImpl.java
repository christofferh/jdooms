package se.uu.it.jdooms.objectspace;

import java.io.Serializable;

import static se.uu.it.jdooms.objectspace.DSObjectSpace.*;

/**
 * Base class to be implemented by all DSObject's
 */
public class DSObjectBaseImpl implements Serializable, DSObjectBase {
    private int ID;
    private boolean valid;
    private Permission permission;

    /**
     * Gets the global ID of the distributed object
     * @return Global ID
     */
    @Override
    public int getID() {
        return ID;
    }

    /**
     * Sets the global ID of the distributed object
     * @param ID Global ID
     */
    @Override
    public void setID(int ID) {
        this.ID = ID;
    }

    /**
     * isValid
     * @return if the object is valid
     */
    @Override
    public boolean isValid() {
        return valid;
    }

    /**
     * Set object valid/invalid
     * @param valid boolean if the object should be valid or not
     */
    @Override
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * get object read/write permission
     * @return the permission of the object
     */
    @Override
    public Permission getPermission() {
        return permission;
    }

    /**
     * set the object read/write permission
     * @param permission the permission of the object
     */
    @Override
    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}
