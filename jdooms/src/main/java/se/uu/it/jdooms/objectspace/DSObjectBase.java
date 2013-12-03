package se.uu.it.jdooms.objectspace;

import java.io.Serializable;

import static se.uu.it.jdooms.objectspace.DSObjectSpace.*;

/**
 * Abstract base class to be implemented by all DSObject's
 */
public class DSObjectBase implements Serializable, DSObjectBaseInterface {
    private static final long serialVersionUID = 42L;

    private int ID;
    private Classifier classifier;

    /**
     * Gets the global ID of the distributed object
     * @return Global ID
     */
    public int getID() {
        return ID;
    }

    /**
     * Sets the global ID of the distributed object
     * @param ID Global ID
     */
    public void setID(int ID) {
        this.ID = ID;
    }

    /**
     * Gets the classifier of the distributed object
     * @return classifier
     */
    public Classifier getClassifier() {
        return classifier;
    }

    /**
     * Sets the classifier of the distributed object
     * @param classifier classifier
     */
    public void setClassifier(DSObjectSpace.Classifier classifier) {
        this.classifier = classifier;
    }
}
