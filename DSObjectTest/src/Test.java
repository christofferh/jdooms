public class Test {
    private int id;

    public void Init(int id) {
        this.id = id;
    }

    public Test() {

    }

    public int getTestId() {
        return id;
    }

    @Override
    public String toString() {
        return "Test object, id: " + id;
    }
}
