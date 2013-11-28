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

        if(dsObjectSpace.getNodeID() == 0) {
            float[][] matrix = generateMatrix();
            int workerCount =  dsObjectSpace.getWorkerCount();
            try {
                Matrix org = ((Matrix)dsObjectSpace.dsNew("Matrix", 100));
                org.Init(matrix, 0, 1);
                System.out.println(org);
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            int matrixLength = matrix.length;
            int[] distribution = new int[workerCount];
            for (int i = 0; i < workerCount; i++) {
                distribution[i] = (matrixLength/workerCount) + matrixLength%workerCount;
                matrixLength -= matrixLength%workerCount;
            }

            int i = 0;
            int start = 0;
            int end = 0;
            for (int columns : distribution) {
                end += columns - 1;
                try {
                    ((Matrix)dsObjectSpace.dsNew("Matrix", i)).Init(copyOfRange(matrix, start, end), i, distribution.length);
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
            try {
                Matrix id = (Matrix)dsObjectSpace.dsNew("Matrix", workerID);
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
            } catch (InstantiationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IllegalAccessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            dsObjectSpace.synchronize();
        }
    }




    private float[][] copyOfRange(float[][] matrix, int start, int end) {
        float[][] result = new float[end - start][matrix[0].length];
        int i = 0;
        for (float[] row : result) {
            row = Arrays.copyOfRange(matrix[i], start, end);
            i++;
        }
        return result;
    }

    private float[][] generateMatrix() {
        Random rnd = new Random();
        float[][] tmp = new float[400][400];
        for(float[] row : tmp) {
            for (float value : row) {
                value = rnd.nextFloat();
            }
        }
        return tmp;
    }
}
