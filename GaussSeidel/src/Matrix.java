public class Matrix {
    public float[][] matrix;
    private int columnID;
    private int nrOfColumns;

    public void Init(float[][] matrix, int columnID, int nrOfColumns) {
        this.matrix = matrix;
        this.columnID = columnID;
        this.nrOfColumns = nrOfColumns;
    }

    public void calculateRed(Matrix left, Matrix right){
        calculate(left, right, 0);
    }

    public void calculateBlack(Matrix left, Matrix right){
        calculate(left, right, 1);
    }

    public void calculate(Matrix left, Matrix right, int color) {
        if (left != null) { //rightmost
            calculateLeft(color, left);
        }
        calculateMiddle(color);
        if (right != null) { //leftmost
            calculateRight(color, right);
        }
    }

    private void calculateLeft(int color, Matrix left) {
        for (int row = 1; row < matrix.length - 1; row++) {
            int column = 0;
            if ((row + color) % 2 == column % 2) {
                matrix[row][column] = (matrix[row - 1][column] + matrix[row + 1][column] + left.matrix[row][left.matrix[row].length - 1] + matrix[row][column + 1])/4;
            }
        }
    }

    private void calculateRight(int color, Matrix right) {
        for (int row = 1; row < matrix.length - 1; row++) {
            int column = matrix[row].length - 1;
            if ((row + color) % 2 == column % 2) {
                matrix[row][column] = (matrix[row - 1][column] + matrix[row + 1][column] + matrix[row][column - 1] + right.matrix[row][0])/4;
            }
        }
    }

    public void calculateMiddle(int color) {
        for (int row = 1; row < matrix.length - 1; row++) {
            for (int column = 1; column < matrix[row].length - 1; column++) {
                if ((row + color) % 2 == column % 2) {
                    matrix[row][column] = (matrix[row - 1][column] + matrix[row + 1][column] + matrix[row][column - 1] + matrix[row][column + 1])/4;
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Matrix ID: " + columnID);
        sb.append(System.getProperty("line.separator"));
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
}
