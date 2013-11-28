/**
 * Created with IntelliJ IDEA.
 * User: Daniel
 * Date: 25/11/13
 * Time: 16:00
 * To change this template use File | Settings | File Templates.
 */
public class Matrix {
    public float[][] matrix;
    private int start;
    private int end;
    private boolean[] doneRows;
    private int columnID;
    private int nrOfColumns;

    public Matrix() {

    }

    public void Init(float[][] matrix, int columnID, int nrOfColumns) {
        this.matrix = matrix;
        this.columnID = columnID;
        this.nrOfColumns = nrOfColumns;
        doneRows = new boolean[matrix.length];
        for (boolean row : doneRows) {
            row = false;
        }
        doneRows[0] = true;
        doneRows[matrix.length - 1] = true;
    }

    public boolean isDone(int row) {
        return doneRows[row];
    }

    public void calculateRed(Matrix neighbour) {

    }

    public void calculateRed(Matrix left, Matrix right) {

    }

    public void calculateBlack() {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Matrix ID: " + columnID);
        sb.append(System.getProperty("Line.separator"));
        for (float[] row : matrix) {
            System.out.print("[");
            sb.append("[");
            for (float value : row) {
                sb.append(value);
                sb.append( ", ");
            }
            sb.append("]");
            sb.append(System.getProperty("Line.separator"));
        }
        return sb.toString();
    }
}
