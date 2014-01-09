public class MatrixBlock {
    private float[][] ABlock = null;
    private float[][] UBlock = null;
    private float[][] LBlock = null;
    private int blockID;

    public void Init(float[][] matrixBlock, int blockID) {
        this.setABlock(matrixBlock);
        this.setBlockID(blockID);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Matrix ID: " + getBlockID());
        sb.append(System.getProperty("line.separator"));
        sb.append("AMatrix:");
        sb.append(System.getProperty("line.separator"));
        for (float[] row : getABlock()) {
            sb.append("[");
            for (float value : row) {
                sb.append(value);
                sb.append( ", ");
            }
            sb.append("]");
            sb.append(System.getProperty("line.separator"));
        }
        if (getLBlock() != null) {
            sb.append("LMatrix:");
            sb.append(System.getProperty("line.separator"));
            for (float[] row : getLBlock()) {
                sb.append("[");
                for (float value : row) {
                    sb.append(value);
                    sb.append( ", ");
                }
                sb.append("]");
                sb.append(System.getProperty("line.separator"));
            }
        }
        if (getUBlock() != null) {
            sb.append("UMatrix:");
            sb.append(System.getProperty("line.separator"));
            for (float[] row : getUBlock()) {
                sb.append("[");
                for (float value : row) {
                    sb.append(value);
                    sb.append( ", ");
                }
                sb.append("]");
                sb.append(System.getProperty("line.separator"));
            }
        }
        /*if (getLBlock() != null && getUBlock() != null) {
            sb.append("LxU Matrix:");
            sb.append(System.getProperty("line.separator"));
            for (float[] row : multiplyMatrix(getLBlock(), getUBlock())) {
                sb.append("[");
                for (float value : row) {
                    sb.append(value);
                    sb.append( ", ");
                }
                sb.append("]");
                sb.append(System.getProperty("line.separator"));
            }
        }*/
        return sb.toString();
    }

    private void U() {
        UBlock = multiplyMatrix(ABlock, invert(LBlock));
        LBlock = null;
    }

    private void L() {
        LBlock = multiplyMatrix(invert(UBlock), ABlock);
        UBlock = null;
    }

    public void compute() {
        if (blockID % 2 == 0) {
            System.out.println("even object calculating U");
            U();
        } else {
            System.out.println("odd object calculating L");
            L();
        }
    }

    public void LUDecomposition() {
        UBlock = new float[ABlock.length][ABlock.length];
        LBlock = new float[ABlock.length][ABlock.length];

        try {
            for (int i = 0; i < ABlock.length; i++) {
                UBlock[0][i] = ABlock[0][i];
                LBlock[i][0] = ABlock[i][0]/ABlock[0][0];
            }

            for (int k = 1; k < ABlock.length; k++) {
                for (int i = 0; i < k; i++) {
                    UBlock[k][i] = 0;
                    LBlock[i][k] = 0;
                }
                for (int i = k; i < ABlock.length; i++) {
                    UBlock[k][i] = ABlock[k][i];
                    for (int m = 0; m < k; m++) {
                        UBlock[k][i] -= UBlock[m][i] * LBlock[k][m];
                    }
                }
                LBlock[k][k] = 1;
                for (int i = k+1; i < ABlock.length; i++) {
                    LBlock[i][k] = ABlock[i][k];
                    for (int m = 0; m < k; m++) {
                        LBlock[i][k] -= (UBlock[m][k] * LBlock[i][m]);
                    }
                    LBlock[i][k] /= UBlock[k][k];
                }
            }
        } catch (Exception e) {
            System.out.println("Something went sideways...");
        }
    }

    public float[][] multiplyMatrix(float[][] A, float[][] B) {
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

    public float[][] invert(float[][] A)
    {
        int n = A.length;
        int row[] = new int[n];
        int col[] = new int[n];
        float temp[] = new float[n];
        int hold , I_pivot , J_pivot;
        float pivot, abs_pivot;

        if(A[0].length!=n)
        {
            System.out.println("Error in Matrix.invert, inconsistent array sizes.");
        }
        // set up row and column interchange vectors
        for(int k=0; k<n; k++)
        {
            row[k] = k ;
            col[k] = k ;
        }
        // begin main reduction loop
        for(int k=0; k<n; k++)
        {
            // find largest element for pivot
            pivot = A[row[k]][col[k]] ;
            I_pivot = k;
            J_pivot = k;
            for(int i=k; i<n; i++)
            {
                for(int j=k; j<n; j++)
                {
                    abs_pivot = Math.abs(pivot) ;
                    if(Math.abs(A[row[i]][col[j]]) > abs_pivot)
                    {
                        I_pivot = i ;
                        J_pivot = j ;
                        pivot = A[row[i]][col[j]] ;
                    }
                }
            }
            if(Math.abs(pivot) < 1.0E-10)
            {
                System.out.println("Matrix is singular !");
                return new float[0][0];
            }
            hold = row[k];
            row[k]= row[I_pivot];
            row[I_pivot] = hold ;
            hold = col[k];
            col[k]= col[J_pivot];
            col[J_pivot] = hold ;
            // reduce about pivot
            A[row[k]][col[k]] = 1 / pivot ;
            for(int j=0; j<n; j++)
            {
                if(j != k)
                {
                    A[row[k]][col[j]] = A[row[k]][col[j]] * A[row[k]][col[k]];
                }
            }
            // inner reduction loop
            for(int i=0; i<n; i++)
            {
                if(k != i)
                {
                    for(int j=0; j<n; j++)
                    {
                        if( k != j )
                        {
                            A[row[i]][col[j]] = A[row[i]][col[j]] - A[row[i]][col[k]] *
                                    A[row[k]][col[j]] ;
                        }
                    }
                    A[row[i]][col [k]] = - A[row[i]][col[k]] * A[row[k]][col[k]] ;
                }
            }
        }
        // end main reduction loop

        // unscramble rows
        for(int j=0; j<n; j++)
        {
            for(int i=0; i<n; i++)
            {
                temp[col[i]] = A[row[i]][j];
            }
            for(int i=0; i<n; i++)
            {
                A[i][j] = temp[i] ;
            }
        }
        // unscramble columns
        for(int i=0; i<n; i++)
        {
            for(int j=0; j<n; j++)
            {
                temp[row[j]] = A[i][col[j]] ;
            }
            for(int j=0; j<n; j++)
            {
                A[i][j] = temp[j] ;
            }
        }
        return A;
    }

    /***
     * Subtracts matrix A from matrix ABlock. Both matrices should be of the same dimensions
     * @param A Some matrix
     */
    public void subtract(float[][] A) {
        for (int i = 0; i < ABlock.length; i++) {
            for (int j = 0; j < ABlock[i].length; j++) {
                ABlock[i][j] -= A[i][j];
            }
        }
    }

    public float[][] getABlock() {
        return ABlock;
    }

    public void setABlock(float[][] ABlock) {
        this.ABlock = ABlock;
    }

    public float[][] getUBlock() {
        return UBlock;
    }

    public void setUBlock(float[][] UBlock) {
        this.UBlock = UBlock;
    }

    public float[][] getLBlock() {
        return LBlock;
    }

    public void setLBlock(float[][] LBlock) {
        this.LBlock = LBlock;
    }

    public int getBlockID() {
        return blockID;
    }

    public void setBlockID(int blockID) {
        this.blockID = blockID;
    }
}
