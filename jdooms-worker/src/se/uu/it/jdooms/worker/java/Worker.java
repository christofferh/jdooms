package se.uu.it.jdooms.worker.java;
import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;
import se.uu.it.jdooms.workerdispatcher.DSObjectFinalizer;

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
        }
    }

    @Override
    public void Init(DSObjectSpace dsObjectSpace) {
        this.objectSpace = dsObjectSpace;
    }
}
