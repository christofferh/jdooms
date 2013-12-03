public class Matrix {
    public float[][] matrix;
    private int columnID;
    private int nrOfColumns;

    public void Init(float[][] matrix, int columnID, int nrOfColumns) {
        this.matrix = matrix;
        this.columnID = columnID;
        this.nrOfColumns = nrOfColumns;
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

    public void calculate(Matrix left, Matrix right) {
        for (int color = 0; color < 2; color++) {
            if (left == null) { //leftmost
                calculateMiddle(color);
                calculateRight(color, right);
            } else if (right == null) { //rightmost
                calculateLeft(color, left);
                calculateMiddle(color);
            } else { //middle
                calculateLeft(color, left);
                calculateMiddle(color);
                calculateRight(color, right);
            }
        }
    }

    private void calculateLeft(int color, Matrix left) {
        for (int row = 1; row < matrix.length - 2; row += 2) {
            int column = 0;
            if ((row + color) % 2 == column % 2) {
                matrix[row][column] = (matrix[row - 1][column] + matrix[row + 1][column] + left.matrix[row][left.matrix[row].length - 1] + matrix[row][column + 1]);
            }
        }
    }

    private void calculateRight(int color, Matrix right) {
        for (int row = 1; row < matrix.length - 2; row += 2) {
            int column = matrix[row].length - 1;
            if ((row + color) % 2 == column % 2) {
                matrix[row][column] = (matrix[row - 1][column] + matrix[row + 1][column] + matrix[row][column - 1] + right.matrix[row][0]);
            }
        }
    }

    public void calculateMiddle(int color) {
        for (int row = 1; row < matrix.length - 2; row++) {
            for (int column = 1; column < matrix[row].length - 2; column++) {
                if ((row + color) % 2 == column % 2) {
                    matrix[row][column] = (matrix[row - 1][column] + matrix[row + 1][column] + matrix[row][column - 1] + matrix[row][column + 1])/4;
                }
            }
        }
    }
}
