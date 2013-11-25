import se.uu.it.jdooms.objectspace.DSObjectSpace;
import se.uu.it.jdooms.workerdispatcher.DSObject;

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
            int workers
        }
        else {

        }
    }


    private float[][] generateMatrix() {
        return new float[][] {{1,2,3,4,5,6,7,8},{1,2,3,4,5,6,7,8},{1,2,3,4,5,6,7,8},{1,2,3,4,5,6,7,8},{1,2,3,4,5,6,7,8},{1,2,3,4,5,6,7,8},{1,2,3,4,5,6,7,8}};
    }
}
