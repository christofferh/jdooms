package se.uu.it.jdooms.node;

import se.uu.it.jdooms.objectspace.DSObjectBase;

import java.io.Serializable;

/**
 * A Dummy class for testing
 */
public class Dummy extends DSObjectBase implements Serializable {
    private int id;
    private String name;

    public Dummy(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getID() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
}
