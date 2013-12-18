import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

public class LUFactWorker implements DSObject{
    DSObjectSpace dsObjectSpace;
    private static int blockSize = 4;

    @Override
    public void Init(DSObjectSpace dsObjectSpace) {
        this.dsObjectSpace = dsObjectSpace;
    }

    @Override
    public void run() {
        if(dsObjectSpace.getWorkerID() == 0) {
            final long startTimeInit = System.currentTimeMillis();

            int workerCount =  dsObjectSpace.getWorkerCount();
            System.out.println("Workercount: " + workerCount);

            for (int i = 0; i < workerCount; i++) {
                //((Matrix)dsObjectSpace.dsNew("Matrix", i)).Init(copyOfRange(matrix, start, end-1), i, distribution.length);
            }
            final long endTimeInit = System.currentTimeMillis();
            System.out.println("Startup time: " + (endTimeInit - startTimeInit));
        }
        dsObjectSpace.synchronize();

        final long startTimeCalculate = System.currentTimeMillis();
        int workerID = dsObjectSpace.getWorkerID();

        for (int tolerance = 0; tolerance < 1; tolerance++){ // while the tolerance criteria is not met
            try {
                /*Matrix id = (Matrix) dsObjectSpace.getObject(workerID, DSObjectSpace.Permission.ReadWrite);
                Matrix left = null;
                Matrix right = null;
                //System.out.println(printMatrix(id.matrix));
                if (workerID < 1) { //leftmost
                    right = (Matrix) dsObjectSpace.getObject(workerID + 1, DSObjectSpace.Permission.Read);
                } else if (workerID == dsObjectSpace.getWorkerCount() - 1) { //rightmost
                    left = (Matrix) dsObjectSpace.getObject(workerID - 1, DSObjectSpace.Permission.Read);
                } else { //in the middle
                    left = (Matrix) dsObjectSpace.getObject(workerID - 1, DSObjectSpace.Permission.Read);
                    right = (Matrix) dsObjectSpace.getObject(workerID + 1, DSObjectSpace.Permission.Read);
                }
                id.calculateRed(left, right);
                dsObjectSpace.synchronize();
                if (workerID < 1) { //leftmost
                    right = (Matrix) dsObjectSpace.getObject(workerID + 1, DSObjectSpace.Permission.Read);
                } else if (workerID == dsObjectSpace.getWorkerCount() - 1) { //rightmost
                    left = (Matrix) dsObjectSpace.getObject(workerID - 1, DSObjectSpace.Permission.Read);
                } else { //in the middle
                    left = (Matrix) dsObjectSpace.getObject(workerID - 1, DSObjectSpace.Permission.Read);
                    right = (Matrix) dsObjectSpace.getObject(workerID + 1, DSObjectSpace.Permission.Read);
                }*/
                //id.calculateBlack(left, right);
                dsObjectSpace.synchronize();
            } catch (ClassCastException cce) {
                System.out.println("Thread: " + Thread.currentThread().getName());
                System.out.println("WorkerID: " + workerID);
                System.out.println("Tolerance: " + tolerance);
                cce.printStackTrace();
            }
        }
        final long endTimeCalculate = System.currentTimeMillis();

        if(dsObjectSpace.getWorkerID() == 0) {
            /*for (int i = 0; i < dsObjectSpace.getWorkerCount(); i++) {
                System.out.print(dsObjectSpace.getObject(i, DSObjectSpace.Permission.Read));
            }*/
            System.out.println("Calculation time: " + (endTimeCalculate - startTimeCalculate));
        }
        dsObjectSpace.dsFinalize();
    }
}
