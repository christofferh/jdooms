package se.uu.it.jdooms.objectspace;

/**
 * User interface for DSObject
 */
public interface DSObjectSpace {
    public enum Classifier {
        Shared, Private;
    }
    public enum Permission {
        Read, ReadWrite;
    }
    int getNodeID();
    int getWorkerID();
    int getWorkerCount();
    void putObject(Object obj, Classifier classifier);
    Object getObject(int ID, Permission permission);
    Object dsNew(String clazz, int ID) throws InstantiationException, IllegalAccessException;
    void synchronize();
}
