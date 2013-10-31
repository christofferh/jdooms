package se.uu.it.jdooms.objectspace;

/**
 * Interface to the abstract base for all distributed objects
 */
public interface DSObjectBaseInterface {
    public int getID();
    public void setID(int ID);
    public DSObjectSpace.Classifier getClassifier();
    public void setClassifier(DSObjectSpace.Classifier classifier);
}
