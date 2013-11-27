/**
 * Created with IntelliJ IDEA.
 * User: Daniel
 * Date: 25/11/13
 * Time: 16:00
 * To change this template use File | Settings | File Templates.
 */
public class Matrix {
    private float[][] matrix;
    private int start;
    private int end;
    private boolean[] doneRows;

    public Matrix() {

    }

    public void Init(float[][] matrix, int start, int end) {
        this.matrix = matrix;
        this.start = start;
        this.end = end;
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

    @Override
    public String toString() {
        return "[123344,123123234,123123]";
    }
}
