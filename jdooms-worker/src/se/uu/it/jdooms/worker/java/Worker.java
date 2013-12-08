package se.uu.it.jdooms.worker.java;
import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.objectspace.DSObjectSpace.Permission;
import se.uu.it.jdooms.workerdispatcher.DSObject;

/**
 * Test worker class
 */
public class Worker implements DSObject {
    private DSObjectSpace objectSpace;

    public Worker(){

    }

    @Override
    public void run() {
        System.out.println("Worker alive!");

        /*for (int i = 0; i < 2; i++) {
            try {
                ((DistributedTest)objectSpace.dsNew("se.uu.it.jdooms.worker.java.DistributedTest", i+1)).Test();
            } catch (InstantiationException e) {
                System.out.println("Instantiation exception");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.out.println("Illegal access exception");
                e.printStackTrace();
            }
        }
        for (int i = 0; i < 2; i++) {
            try {
                ((DistributedTest)objectSpace.dsNew("se.uu.it.jdooms.worker.java.DistributedTest", i+1)).Test();
            } catch (InstantiationException e) {
                System.out.println("Instantiation exception");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.out.println("Illegal access exception");
                e.printStackTrace();
            }
        }*/

        if (objectSpace.getNodeID() == 0) {
            try {
                objectSpace.dsNew("se.uu.it.jdooms.worker.java.DistributedTest", 20);
            } catch (InstantiationException e) {
                System.out.println("Instantiation exception");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.out.println("Illegal access exception");
                e.printStackTrace();
            }
            objectSpace.synchronize();/*
            if (objectSpace.getWorkerID() == 0) {
                System.out.println("Worker " + objectSpace.getWorkerID() + " reached barrier");
                objectSpace.synchronize();
            } else {
                try {
                    Thread.sleep(500);
                    System.out.println("Worker " + objectSpace.getWorkerID() + " reached barrier");
                    objectSpace.synchronize();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/

        } else {
            try {
                Thread.sleep(1000);
                DistributedTest test = (DistributedTest)objectSpace.getObject(20, Permission.Read);
                test.Test();

            } catch (InterruptedException e) {
                e.printStackTrace();
            }/*
            if (objectSpace.getWorkerID() == 2) {
                System.out.println("Worker " + objectSpace.getWorkerID() + " reached barrier");
                objectSpace.synchronize();
            } else {
                try {
                    Thread.sleep(2000);
                    System.out.println("Worker " + objectSpace.getWorkerID() + " reached barrier");
                    objectSpace.synchronize();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/
            objectSpace.synchronize();
        }

        //System.out.println(objectSpace.getNodeID() + " passed all barriers");
    }

    @Override
    public void Init(DSObjectSpace dsObjectSpace) {
        this.objectSpace = dsObjectSpace;
    }
}
