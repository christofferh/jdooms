package se.uu.it.jdooms.objectspace;

/**
 * User interface for DSObject
 */
public interface DSObjectSpace {
    public enum Permission {
        Read ("Read"), ReadWrite ("ReadWrite");

        private final String permission;

        private Permission(String s) {
            permission = s;
        }

        public String toString() {
            return permission;
        }
    }
    int getNodeID();

    int getWorkerID();

    int getWorkerCount();

    int getNodeCount();

    void putObject(Object obj);

    Object dsNew(String clazz, int ID) throws InstantiationException, IllegalAccessException;

    Object getObject(int ID, Permission permission);

    void synchronize();

    void dsFinalize();
}
