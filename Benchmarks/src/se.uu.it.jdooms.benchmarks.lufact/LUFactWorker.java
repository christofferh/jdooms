package se.uu.it.jdooms.benchmarks.lufact;

import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class LUFactWorker implements DSObject{
    DSObjectSpace dsObjectSpace;
    private int size;
    private int blockSize;
    private int blocksPerSide;

    private int[] diagonalBlocks;
    private int[][] idDistribution;
    private float[][] AMatrix;
    private ArrayList<ArrayList<Integer>> WorkItems;

    @Override
    public void Init(String[] args, DSObjectSpace dsObjectSpace) {
        this.dsObjectSpace = dsObjectSpace;
        this.size = Integer.valueOf(args[2]);
        this.blockSize = Integer.valueOf(args[3]);
        if (blockSize < 2) {
            System.out.println("Blocksize can not be less than 2");
            System.exit(-1);
        }
        blocksPerSide = size/blockSize;
    }

    @Override
    public void run() {
        final long startTimeInit = System.currentTimeMillis();
        generateWorkList();
        generateMatrix();
        blockDistribution();
        generateDiagonalList();
        /* Setting up all the objects needed to do the factorization */
        if(dsObjectSpace.getWorkerID() == 0) {
            for (int i = 0; i < idDistribution.length; i ++) {
                for (int j = 0; j < idDistribution[i].length; j ++) {
                    try {
                        ((MatrixBlock)dsObjectSpace.dsNew("MatrixBlock",
                                idDistribution[i][j])).Init(copyOfRange(i*blockSize, j*blockSize), idDistribution[i][j]);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        dsObjectSpace.synchronize();
        final long endTimeInit = System.currentTimeMillis();

        final long startTimeCalculate = System.currentTimeMillis();
        /* The actual distributed computation loop */
        for (int i  = 0; i < WorkItems.size(); i++) {
            if (dsObjectSpace.getWorkerID() == 0) {
                MatrixBlock diag = (MatrixBlock)dsObjectSpace.getObject(diagonalBlocks[i], DSObjectSpace.Permission.ReadWrite);
                diag.LUDecomposition();
                for (int block : WorkItems.get(i)) {
                    MatrixBlock matrixBlock = (MatrixBlock)dsObjectSpace.getObject(block, DSObjectSpace.Permission.ReadWrite);
                    if (block % 2 == 0) {
                        matrixBlock.setLBlock(diag.getLBlock());
                    } else {
                        matrixBlock.setUBlock(diag.getUBlock());
                    }
                }
            }
            dsObjectSpace.synchronize();

            for (int item : WorkItems.get(i)) {
                if (item%dsObjectSpace.getWorkerCount() == dsObjectSpace.getWorkerID()) {
                    MatrixBlock matrixBlock = (MatrixBlock)dsObjectSpace.getObject(item, DSObjectSpace.Permission.ReadWrite);
                    matrixBlock.compute();
                }
            }
            dsObjectSpace.synchronize();

            for (int j = i + 1; j < idDistribution.length; j++) {
                for (int k = i + 1; k < idDistribution[j].length; k++) {
                    int block = idDistribution[j][k];
                    if (block%dsObjectSpace.getWorkerCount() == dsObjectSpace.getWorkerID()) {
                        int lblock = idDistribution[j][i];
                        int ublock = idDistribution[i][k];
                        MatrixBlock matrixBlock = (MatrixBlock)dsObjectSpace.getObject(block, DSObjectSpace.Permission.ReadWrite);
                        MatrixBlock lBlock = (MatrixBlock)dsObjectSpace.getObject(lblock, DSObjectSpace.Permission.Read);
                        MatrixBlock uBlock = (MatrixBlock)dsObjectSpace.getObject(ublock, DSObjectSpace.Permission.Read);
                        matrixBlock.subtract(matrixBlock.multiplyMatrix(lBlock.getLBlock(), uBlock.getUBlock()));
                    }
                }
            }
            dsObjectSpace.synchronize();
        }

        if (dsObjectSpace.getWorkerID() == 0) {
            MatrixBlock diagonal = (MatrixBlock)dsObjectSpace.getObject(diagonalBlocks[diagonalBlocks.length - 1], DSObjectSpace.Permission.ReadWrite);
            diagonal.LUDecomposition();
        }
        final long endTimeCalculate = System.currentTimeMillis();


        /**
         * Finishing up the work
         */
        if(dsObjectSpace.getWorkerID() == 0) {
            System.out.println(dsObjectSpace.getNodeCount() + "," + dsObjectSpace.getWorkerCount() + "," + size + "," + blockSize
                    + "," + (endTimeInit - startTimeInit) + "," + (endTimeCalculate - startTimeCalculate));
        }
        dsObjectSpace.dsFinalize();
    }

    private void blockDistribution() {
        idDistribution = new int[blocksPerSide][blocksPerSide];
        int rowLength = idDistribution.length;
        idDistribution[0][0] = 0;

        for (int i = 1; i < idDistribution.length; i++) {
            idDistribution[i][i] = ((rowLength * 2) - 1) + idDistribution[i-1][i-1];
            rowLength--;
        }

        int i, j, m = 1, k = 2;
        for (i = 0; i < idDistribution.length; i++) {
            k = idDistribution[i][m-1];
            if (k%2 != 0) {
                k++;
            } else {
                k += 2;
            }
            for (j = m; j < idDistribution[i].length; j++) {
                idDistribution[i][j] = k;
                k += 2;
            }
            m++;
        }

        m = 1;
        for (i = 0; i < idDistribution.length; i++) {
            k = idDistribution[i][m-1];
            if (k%2 == 0) {
                k++;
            } else {
                k += 2;
            }
            for (j = m; j < idDistribution[i].length; j++) {
                idDistribution[j][i] = k;
                k += 2;
            }
            m++;
        }
    }

    private void generateDiagonalList() {
        diagonalBlocks = new int[idDistribution.length];
        for (int i = 0; i < idDistribution.length; i++) {
            diagonalBlocks[i] = idDistribution[i][i];
        }
    }

    private void generateWorkList() {
        WorkItems = new ArrayList<ArrayList<Integer>>();
        int n = 1;
        for (int i = 0; i < blocksPerSide-1; i++) {
            ArrayList<Integer> tmp = new ArrayList<Integer>();
            for (int j = 0; j < (2*(blocksPerSide - (i + 1))); j++) {
                tmp.add(n);
                n++;
            }
            WorkItems.add(i, tmp);
            n++;
        }
    }

    private void generateMatrix() {
        AMatrix = new float[size][size];
        Random rnd = new Random();
        int n = 0;
        for (int i = 0; i < AMatrix.length; i++) {
            for (int j = 0; j < AMatrix[i].length; j++) {
                AMatrix[i][j] = rnd.nextInt(4);
                n++;
            }
            n++;
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

    public String printMatrix(int[][] matrix) {
        StringBuilder sb = new StringBuilder();
        for (int[] row : matrix) {
            sb.append("[");
            for (int value : row) {
                sb.append(value);
                sb.append( ", ");
            }
            sb.append("]");
            sb.append(System.getProperty("line.separator"));
        }
        return sb.toString();
    }

    public String printDistribution(int[] distribution) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int value : distribution) {
            sb.append(value);
            sb.append( ", ");
        }
        sb.append("]");
        sb.append(System.getProperty("line.separator"));
        return sb.toString();
    }

    public String printDistribution(ArrayList<ArrayList<Integer>> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (ArrayList<Integer> list : values) {
            sb.append("[");
            for (int value : list) {
                sb.append(value + ", ");
            }
            sb.append("]");
        }
        sb.append("]");
        sb.append(System.getProperty("line.separator"));
        return sb.toString();
    }

    private float[][] copyOfRange(int row, int column) {
        float[][] result = new float[blockSize][blockSize];
        for (int i = row, j = 0; i < (row + blockSize); i++, j++) {
            result[j] = Arrays.copyOfRange(AMatrix[i], column, column + blockSize);
        }
        return result;
    }

    private float[][] multiplyMatrix(float[][] A, float[][] B) {
        int aRows = A.length;
        int aColumns = A[0].length;
        int bRows = B.length;
        int bColumns = B[0].length;

        if (aColumns != bRows) {
            throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
        }

        float[][] C = new float[aRows][bColumns];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                C[i][j] = 0;
            }
        }

        for (int i = 0; i < aRows; i++) { // aRow
            for (int j = 0; j < bColumns; j++) { // bColumn
                for (int k = 0; k < aColumns; k++) { // aColumn
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }
}
