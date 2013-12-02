import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

import java.util.Arrays;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Daniel
 * Date: 25/11/13
 * Time: 15:48
 * To change this template use File | Settings | File Templates.
 */
public class GaussSeidelWorker implements DSObject{
    DSObjectSpace dsObjectSpace;

    @Override
    public void Init(DSObjectSpace dsObjectSpace) {
        //To change body of implemented methods use File | Settings | File Templates.
        this.dsObjectSpace = dsObjectSpace;
    }



    @Override
    public void run() {

        System.out.println("WorkerID: " + dsObjectSpace.getWorkerID());


        if(dsObjectSpace.getWorkerID() == 0) {
            float[][] matrix = generateMatrix();
            System.out.println(printMatrix(matrix));


            int workerCount =  dsObjectSpace.getWorkerCount();
            System.out.println("Workercount: " + workerCount);


            int matrixLength = matrix.length;
            int[] distribution = new int[workerCount];
            for (int i = 0; i < workerCount; i++) {
                distribution[i] = (matrixLength/workerCount) + matrixLength%workerCount;
                matrixLength -= matrixLength%workerCount;
            }

            System.out.print(printDistribution(distribution));

            int i = 0;
            int start = 0;
            int end = 0;
            for (int columns : distribution) {
                end += columns;
                System.out.println("strart: " + start);
                System.out.println("end: " + end);
                try {
                    ((Matrix)dsObjectSpace.dsNew("Matrix", i)).Init(copyOfRange(matrix, start, end-1), i, distribution.length);
                } catch (InstantiationException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IllegalAccessException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                start += columns;
                i++;
            }
        }
        dsObjectSpace.synchronize();

        int workerID = dsObjectSpace.getWorkerID();


        for (int n = 0; n < 1; n++){ // while the tolerance criteria is not met
            int row = 1;
            Matrix id = (Matrix)dsObjectSpace.getObject(workerID, DSObjectSpace.Permission.ReadWrite);
            System.out.println(id);
            /*if (workerID < 1) { //leftmost
                Matrix left = (Matrix)dsObjectSpace.dsNew("Matrix", workerID);
                Matrix right = (Matrix)dsObjectSpace.dsNew("Matrix", workerID + 1);

                for (row = 1; row < left.matrix.length; row+=2) {
                    int column;
                    for (column = 1; column < right.matrix[row].length - 1; column+=2) {
                        left.matrix[row][column] = (left.matrix[row - 1][column] + left.matrix[row + 1][column] + left.matrix[row][column - 1] + left.matrix[row][column + 1])/4;
                    }
                    left.matrix[row][column] = (left.matrix[row - 1][column] + left.matrix[row + 1][column] + left.matrix[row][column - 1] + right.matrix[row][0])/4;
                }
                for (row = 2; row < left.matrix.length; row+=2) {
                    int column;
                    for (column = 1; column < right.matrix[row].length - 1; column+=2) {
                        left.matrix[row][column] = (left.matrix[row - 1][column] + left.matrix[row + 1][column] + left.matrix[row][column - 1] + left.matrix[row][column + 1])/4;
                    }
                    left.matrix[row][column] = (left.matrix[row - 1][column] + left.matrix[row + 1][column] + left.matrix[row][column - 1] + right.matrix[row][0])/4;
                }

            } else if (workerID == dsObjectSpace.getWorkerCount() - 1) { //rightmost
                Matrix left = (Matrix)dsObjectSpace.dsNew("Matrix", workerID - 1);
                Matrix right = (Matrix)dsObjectSpace.dsNew("Matrix", workerID);
                int column = 0;
                right.matrix[row][column] = (right.matrix[row - 1][column] + right.matrix[row + 1][column] + left.matrix[row][left.matrix[row].length] + right.matrix[row][column + 1])/4;
                for (column = 1; column < right.matrix[row].length; column++) {
                    right.matrix[row][column] = (right.matrix[row - 1][column] + right.matrix[row + 1][column] + right.matrix[row][column - 1] + right.matrix[row][column + 1])/4;
                }
                row++;
            } else { //in the middle
                Matrix matN0 = (Matrix)dsObjectSpace.dsNew("Matrix", workerID - 1);
                Matrix matN1 = (Matrix)dsObjectSpace.dsNew("Matrix", workerID);
                Matrix matN2 = (Matrix)dsObjectSpace.dsNew("Matrix", workerID + 1);
                int column = 1;
                // do work here
                row++;
            }*/

            dsObjectSpace.synchronize();
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
