import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

import java.util.Arrays;
import java.util.Random;

public class GaussSeidelWorker implements DSObject{
    DSObjectSpace dsObjectSpace;

    @Override
    public void Init(DSObjectSpace dsObjectSpace) {
        //To change body of implemented methods use File | Settings | File Templates.
        this.dsObjectSpace = dsObjectSpace;
    }

    @Override
    public void run() {
        final long startTimeInit = System.currentTimeMillis();

        if(dsObjectSpace.getWorkerID() == 0) {
            float[][] matrix = generateMatrix();
            //System.out.println(printMatrix(matrix));


            int workerCount =  dsObjectSpace.getWorkerCount();
            System.out.println("Workercount: " + workerCount);


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

        for (int tolerance = 0; tolerance < 10; tolerance++){ // while the tolerance criteria is not met
            Matrix id = (Matrix) dsObjectSpace.getObject(workerID, DSObjectSpace.Permission.ReadWrite);
            Matrix left = null;
            Matrix right = null;
            printMatrix(id.matrix);
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
            }
            id.calculateBlack(left, right);
            dsObjectSpace.synchronize();
        }
        final long endTimeCalculate = System.currentTimeMillis();

        if(dsObjectSpace.getWorkerID() == 0) {
            for (int i = 0; i < dsObjectSpace.getWorkerCount(); i++) {
                System.out.print(dsObjectSpace.getObject(i, DSObjectSpace.Permission.Read));
            }
            System.out.println("Performance");
            System.out.println("Startup time: " + (endTimeInit - startTimeInit));
            System.out.println("Calculation time: " + (endTimeCalculate - startTimeCalculate));
        }
    }




    private float[][] copyOfRange(float[][] matrix, int start, int end) {
        float[][] result = new float[matrix[0].length][end - start];
        for (int i = 0; i < result.length; i++) {
            result[i] = Arrays.copyOfRange(matrix[i], start, end + 1);
        }
        return result;
    }

    private float[][] generateMatrix() {
        Random rnd = new Random();
        float[][] tmp = new float[20][20];
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {
                tmp[i][j] = rnd.nextInt(4);
            }
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
