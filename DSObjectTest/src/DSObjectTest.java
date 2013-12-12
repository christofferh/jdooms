import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

import java.util.Arrays;
import java.util.Random;

public class DSObjectTest implements DSObject{
    DSObjectSpace dsObjectSpace;
    private final int size = 50;

    @Override
    public void Init(DSObjectSpace dsObjectSpace) {
        //To change body of implemented methods use File | Settings | File Templates.
        this.dsObjectSpace = dsObjectSpace;
    }

    @Override
    public void run() {
        final long startTimeInit = System.currentTimeMillis();

        if(dsObjectSpace.getWorkerID() == 0) {
            int workerCount =  dsObjectSpace.getWorkerCount();
            System.out.println("Workercount: " + workerCount);

            for (int i = 0; i < size ; i++) {
                try {
                    ((Test)dsObjectSpace.dsNew("Test", i)).Init(i);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        dsObjectSpace.synchronize();
        final long endTimeInit = System.currentTimeMillis();

        final long startTimeCalculate = System.currentTimeMillis();
        int workerID = dsObjectSpace.getWorkerID();

        try {
            for (int i = 0; i < size; i++) {
                Test id;
                if (dsObjectSpace.getWorkerID() == 1) {
                    id = (Test) dsObjectSpace.getObject(i, DSObjectSpace.Permission.ReadWrite);
                } else {
                    id = (Test) dsObjectSpace.getObject(i, DSObjectSpace.Permission.Read);
                }
                if (id.getTestId() != i) {
                    System.exit(-1);
                }
            }
        } catch (ClassCastException cce) {
            System.out.println("Thread: " + Thread.currentThread().getName());
            System.out.println("WorkerID: " + workerID);
            throw cce;
        }
        dsObjectSpace.synchronize();
        final long endTimeCalculate = System.currentTimeMillis();

        if(dsObjectSpace.getWorkerID() == 0) {
            System.out.println("Performance");
            System.out.println("Startup time: " + (endTimeInit - startTimeInit));
            System.out.println("Calculation time: " + (endTimeCalculate - startTimeCalculate));
        }
    }
}
