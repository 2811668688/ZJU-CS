import java.util.ArrayList;
public class Model {
	private ArrayList<Shape> shapes; //存放所有图形
    Model(ArrayList<Shape> s) {
        shapes = s;
    }
    
    public ArrayList<Shape> getAll() {
        return shapes;
    }
    
    public Shape Last() {
        return shapes.get(shapes.size()-1);
    }
    
    public void add(Shape shape) {
        shapes.add(shape);
    }

    public void setAll(ArrayList<Shape> s) {//用于打开文件
        shapes = s;
    }

	public void removeAll() {
		shapes.clear();
	}
}