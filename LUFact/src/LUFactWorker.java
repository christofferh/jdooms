import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

public class LUFactWorker implements DSObject{
    DSObjectSpace dsObjectSpace;
    private static int size = 100;
    private static int blockSize = 10;
    private static int blocksPerSide = size/blockSize;

    private static int matrixOffset = 0;
    private static int LOffset = blocksPerSide*blocksPerSide;
    private static int UOffset = blocksPerSide*blocksPerSide*2;
    private int[][] idDistribution;
    private float[][] AMatrix;

    @Override
    public void Init(DSObjectSpace dsObjectSpace) {
        this.dsObjectSpace = dsObjectSpace;
    }

    @Override
    public void run() {

        nameTest();
        generateMatrix();
        System.out.println(printMatrix(idDistribution));


        if(dsObjectSpace.getWorkerID() == 0) {
            for (int i = 0; i < AMatrix.length; i += blockSize) {
                for (int j = 0; j < AMatrix[i].length; j += blockSize) {
                    try {
                        ((MatrixBlock)dsObjectSpace.dsNew("MatrixBlock", i)).Init(copyOfRange(matrix, start, end-1), i, distribution.length);
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }
            }
        }


        /*if(dsObjectSpace.getWorkerID() == 0) {
            final long startTimeInit = System.currentTimeMillis();

            float[][] AMatrix = generateMatrix();
            float[][] LMatrix = generateZeroMatrix();
            float[][] UMatrix = generateZeroMatrix();

            int workerCount =  dsObjectSpace.getWorkerCount();
            System.out.println("Workercount: " + workerCount);

            for (int i = 0; i < workerCount; i++) {
                //
            }
            final long endTimeInit = System.currentTimeMillis();
            System.out.println("Startup time: " + (endTimeInit - startTimeInit));
        }*/
        /*dsObjectSpace.synchronize();

        final long startTimeCalculate = System.currentTimeMillis();
        int workerID = dsObjectSpace.getWorkerID();

        for (int tolerance = 0; tolerance < 1; tolerance++){ // while the tolerance criteria is not met





        }
        final long endTimeCalculate = System.currentTimeMillis();

        if(dsObjectSpace.getWorkerID() == 0) {
            System.out.println("Calculation time: " + (endTimeCalculate - startTimeCalculate));
        }
        */
        dsObjectSpace.dsFinalize();
    }

    private void nameTest() {
        int n = 20;
        idDistribution = new int[n][n];
        int i, j, m = 0, k = 0;
        for (i = 0; i < idDistribution.length; i++) {
            for (j = m; j < idDistribution[i].length; j++) {
                idDistribution[i][j] = k;
                k++;
            }
            m++;
            k += n - m;
        }

        m = 1;
        k = n;
        for (i = 0; i < idDistribution.length; i++) {
            for (j = m; j < idDistribution[i].length; j++) {
                idDistribution[j][i] = k;
                k++;
            }
            k += n - m;
            m++;
        }
    }

    private void generateMatrix() {
        AMatrix = new float[size][size];
        int n = 0;
        for (int i = 0; i < AMatrix.length; i++) {
            for (int j = 0; j < AMatrix[i].length; j++) {
                AMatrix[i][j] = n%size;
                n++;
            }
            n++;
        }
    }

    /*private float[][] generateZeroMatrix() {
        float[][] tmp = new float[size][size];
        for (int i = 0; i < tmp.length; i++) {
            for (int j = 0; j < tmp[i].length; j++) {
                tmp[i][j] = 0;
            }
        }
        return tmp;
    }*/

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
}
