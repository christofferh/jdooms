import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

import java.util.Arrays;

public class GaussSeidelWorker implements DSObject{
    DSObjectSpace dsObjectSpace;
    int matrixSize = 32;

    @Override
    public void Init(String[] args, DSObjectSpace dsObjectSpace) {
        matrixSize = Integer.parseInt(args[2]);
        this.dsObjectSpace = dsObjectSpace;
    }

    @Override
    public void run() {
        final long startTimeInit = System.currentTimeMillis();

        if(dsObjectSpace.getWorkerID() == 0) {
            float[][] matrix = generateMatrix();

            int workerCount =  dsObjectSpace.getWorkerCount();
            if ((matrix.length/workerCount) < 2) {
                System.out.println("Array to small for number of workers");
                System.exit(-1);
            }

            //System.out.println("Nodes: " + dsObjectSpace.getNodeCount() + " Threads: " + workerCount + " Matrix: " + matrixSize);

            int matrixLength = matrix.length;
            int[] distribution = new int[workerCount];
            for (int i = 0; i < workerCount; i++) {
                distribution[i] = (matrixLength/workerCount) + matrixLength%workerCount;
                matrixLength -= matrixLength%workerCount;
            }

            int i = 0, start = 0, end = 0;
            for (int columns : distribution) {
                end += columns;
                try {
                    ((Matrix)dsObjectSpace.dsNew("Matrix", i)).Init(copyOfRange(matrix, start, end-1), i, distribution.length);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                start += columns;
                i++;
            }
        }
        dsObjectSpace.synchronize();
        final long endTimeInit = System.currentTimeMillis();

        final long startTimeCalculate = System.currentTimeMillis();
        int workerID = dsObjectSpace.getWorkerID();

        for (int tolerance = 0; tolerance < 5; tolerance++){ // while the tolerance criteria is not met
            try {
                Matrix id = (Matrix) dsObjectSpace.getObject(workerID, DSObjectSpace.Permission.ReadWrite);
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
                dsObjectSpace.synchronize();
                id.calculateRed(left, right);
                dsObjectSpace.synchronize();
                if (workerID < 1) { //leftmost
                    right = (Matrix) dsObjectSpace.getObject(workerID + 1, DSObjectSpace.Permission.Read);
                } else if (workerID == dsObjectSpace.getWorkerCount() - 1) { //rightmost
                    left = (Matrix) dsObjectSpace.getObject(workerID - 1, DSObjectSpace.Permission.Read);
                } else { //in the middle
                    left = (Matrix) dsObjectSpace.getObject(workerID - 1, DSObjectSpace.Permission.Read);
                    right = (Matrix) dsObjectSpace.getObject(workerID + 1, DSObjectSpace.Permission.Read);
                }
                dsObjectSpace.synchronize();
                id.calculateBlack(left, right);
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
            //System.out.println("Startup time: " + (endTimeInit - startTimeInit));
            //System.out.println("Calculation time: " + (endTimeCalculate - startTimeCalculate));
            System.out.println(dsObjectSpace.getNodeCount() + "," + dsObjectSpace.getWorkerCount() + "," + matrixSize
                               + "," + (endTimeInit - startTimeInit) + "," + (endTimeCalculate - startTimeCalculate));
        }
        dsObjectSpace.dsFinalize();
    }




    private float[][] copyOfRange(float[][] matrix, int start, int end) {
        float[][] result = new float[matrix[0].length][end - start];
        for (int i = 0; i < result.length; i++) {
            result[i] = Arrays.copyOfRange(matrix[i], start, end + 1);
        }
        return result;
    }

    private float[][] generateMatrix() {
        //int l = 64;
        float[][] tmp = new float[matrixSize][matrixSize];
        int n = 0;
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {
                tmp[i][j] = n%matrixSize;
                n++;
            }
            n++;
        }
        return tmp;
    }

    public String printMatrix(float[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (float[] row : matrix) {
            sb.append("[");
            for (float value : row) {
                sb.append(value);
                sb.append( ", ");
            }
            sb.append("]");
            sb.append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }

    public String printDistribution(int[] values) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int value : values) {
            sb.append(value + ", ");
        }
        sb.append("]");
        sb.append(System.getProperty("line.separator"));
        return sb.toString();
    }
}
