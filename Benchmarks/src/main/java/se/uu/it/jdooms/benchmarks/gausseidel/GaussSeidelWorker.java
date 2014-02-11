package se.uu.it.jdooms.benchmarks.gausseidel;

import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

import java.util.Arrays;

public class GaussSeidelWorker implements DSObject {
    DSObjectSpace dsObjectSpace;
    private int size;
    private int blockSize;
    private int blocksPerSide;
    private int[] workList;

    @Override
    public void Init(String[] args, DSObjectSpace dsObjectSpace) {
        this.dsObjectSpace = dsObjectSpace;
        size = Integer.parseInt(args[2]);
        blockSize = Integer.parseInt(args[3]);

        if (blockSize < 2) {
            System.out.println("Block size can not be less than 2");
            System.exit(-1);
        }
        blocksPerSide = size/blockSize;
    }

    @Override
    public void run() {
        final long startTimeInit = System.currentTimeMillis();
        generateWorkList();

        if (dsObjectSpace.getWorkerID() == 0) {
            float[][] matrix = generateMatrix();

            int i = 0, start = 0, end = 0;
            for (int id : workList) {
                end += blockSize;
                try {
                    ((Matrix)dsObjectSpace.dsNew("se.uu.it.jdooms.benchmarks.gausseidel.Matrix", id))
                            .Init(copyOfRange(matrix, start, end-1), i, blockSize);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                start += blockSize;
                i++;
            }
        }
        dsObjectSpace.synchronize();
        final long endTimeInit = System.currentTimeMillis();

        final long startTimeCalculate = System.currentTimeMillis();
        for (int tolerance = 0; tolerance < 5; tolerance++){ // while the tolerance criteria is not met
            for (int item : workList) {
                if (item%dsObjectSpace.getWorkerCount() == dsObjectSpace.getWorkerID()) {
                    Matrix id = (Matrix) dsObjectSpace.getObject(item, DSObjectSpace.Permission.ReadWrite);
                    Matrix left = null;
                    Matrix right = null;
                    if (item < 1) { //leftmost
                        right = (Matrix) dsObjectSpace.getObject(item + 1, DSObjectSpace.Permission.Read);
                    } else if (item == workList.length - 1) { //rightmost
                        left = (Matrix) dsObjectSpace.getObject(item - 1, DSObjectSpace.Permission.Read);
                    } else { //in the middle
                        left = (Matrix) dsObjectSpace.getObject(item - 1, DSObjectSpace.Permission.Read);
                        right = (Matrix) dsObjectSpace.getObject(item + 1, DSObjectSpace.Permission.Read);
                    }
                    id.calculateRed(left, right);
                }
            }
            dsObjectSpace.synchronize();
            for (int item : workList) {
                if (item%dsObjectSpace.getWorkerCount() == dsObjectSpace.getWorkerID()) {
                    Matrix id = (Matrix) dsObjectSpace.getObject(item, DSObjectSpace.Permission.ReadWrite);
                    Matrix left = null;
                    Matrix right = null;
                    if (item < 1) { //leftmost
                        right = (Matrix) dsObjectSpace.getObject(item + 1, DSObjectSpace.Permission.Read);
                    } else if (item == workList.length - 1) { //rightmost
                        left = (Matrix) dsObjectSpace.getObject(item - 1, DSObjectSpace.Permission.Read);
                    } else { //in the middle
                        left = (Matrix) dsObjectSpace.getObject(item - 1, DSObjectSpace.Permission.Read);
                        right = (Matrix) dsObjectSpace.getObject(item + 1, DSObjectSpace.Permission.Read);
                    }
                    id.calculateBlack(left, right);
                }
            }
            dsObjectSpace.synchronize();
        }
        final long endTimeCalculate = System.currentTimeMillis();

        if (dsObjectSpace.getWorkerID() == 0) {
            System.out.println(dsObjectSpace.getNodeCount() + "," + dsObjectSpace.getWorkerCount() + "," + size
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
        float[][] tmp = new float[size][size];
        int n = 0;
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {
                tmp[i][j] = n%size;
                n++;
            }
            n++;
        }
        return tmp;
    }

    private void generateWorkList() {
        workList = new int[blocksPerSide];
        int n = 1;
        for (int i = 0; i < blocksPerSide; i++) {
            workList[i] = i;
        }
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
