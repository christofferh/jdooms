package se.uu.it.jdooms.objectspace;

import java.io.Serializable;

import static se.uu.it.jdooms.objectspace.DSObjectSpace.*;

/**
 * Abstract base class to be implemented by all DSObject's
 */
public class DSObjectBaseImpl implements Serializable, DSObjectBase {
    private static final long serialVersionUID = 42L;

    private int ID;
    private Classifier classifier;
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
     * Gets the classifier of the distributed object
     * @return classifier
     */
    @Override
    public Classifier getClassifier() {
        return classifier;
    }

    /**
     * Sets the classifier of the distributed object
     * @param classifier classifier
     */
    @Override
    public void setClassifier(DSObjectSpace.Classifier classifier) {
        this.classifier = classifier;
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
     * @param valid
     */
    @Override
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * get object read/write permission
     * @return
     */
    @Override
    public Permission getPermission() {
        return permission;
    }

    /**
     * set the object read/write permission
     * @param permission
     */
    @Override
    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}
