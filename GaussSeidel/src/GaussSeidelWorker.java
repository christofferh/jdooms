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

        if(dsObjectSpace.getRank() == 0) {
            float[][] matrix = generateMatrix();
            int workerCount =  dsObjectSpace.getWorkerCount();

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
                    ((Matrix)dsObjectSpace.dsNew("Matrix", i)).Init(copyOfRange(matrix, start, end), start, end);
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

        int rank = dsObjectSpace.getRank();


        int row = 1;
        while (true) {

            try {
                if (rank < 1) {
                    Matrix matN1 = (Matrix)dsObjectSpace.dsNew("Matrix", rank);
                    Matrix matN2 = (Matrix)dsObjectSpace.dsNew("Matrix", rank + 1);
                    // do work here
                    row++;
                } else if (rank == dsObjectSpace.getWorkerCount() - 1) {
                    Matrix matN0 = (Matrix)dsObjectSpace.dsNew("Matrix", rank - 1);
                    Matrix matN1 = (Matrix)dsObjectSpace.dsNew("Matrix", rank);
                    if (matN0.isDone(row)) {
                        // do work here
                        row++;
                    }
                } else {
                    Matrix matN0 = (Matrix)dsObjectSpace.dsNew("Matrix", rank - 1);
                    Matrix matN1 = (Matrix)dsObjectSpace.dsNew("Matrix", rank);
                    Matrix matN2 = (Matrix)dsObjectSpace.dsNew("Matrix", rank + 1);
                    if (matN0.isDone(row)) {
                        // do work here
                        row++;
                    }
                }
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
        float[][] tmp = new float[2000][2000];
        for(float[] row : tmp) {
            for (float value : row) {
                value = rnd.nextFloat();
            }
        }
        return tmp;
    }
}
