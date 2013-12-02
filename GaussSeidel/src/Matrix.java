public class Matrix {
    public float[][] matrix;
    private int columnID;
    private int nrOfColumns;

    public Matrix() {

    }

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
}
