import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Daniel
 * Date: 25/11/13
 * Time: 15:48
 * To change this template use File | Settings | File Templates.
 */
public class GaussSeidelWorker implements DSObject{
    DSObjectSpace objectSpace;

    @Override
    public void Init(DSObjectSpace dsObjectSpace) {
        //To change body of implemented methods use File | Settings | File Templates.
        objectSpace = dsObjectSpace;
    }

    @Override
    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
        if(objectSpace.getRank() == 0) {
            float[][] matrix = generateMatrix();
            int workerCount =  32;

            int matrixLength = matrix.length;
            int[] distribution = new int[workerCount];
            for (int i = 0; i < workerCount; i++) {
                distribution[i] = (matrixLength/workerCount) + matrixLength%workerCount;
                matrixLength -= matrixLength%workerCount;
            }
            for (int dist : distribution) {
                System.out.println("value: " + dist);
            }
        }
        else {

        }
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
