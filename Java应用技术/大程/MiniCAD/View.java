import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;

public class View extends JPanel {
	private static final long serialVersionUID = 1L;
	ArrayList<Shape> shapes = new ArrayList<Shape>();
	View() {//初始化定义,对view添加鼠标的事件监听
		setBackground(Color.white);
        addMouseListener(Controller.pl);
        addMouseMotionListener(Controller.pl);
		addKeyListener(Controller.pl);
	}
	public void paintAll(ArrayList<Shape> s) {
		shapes = s;
		repaint();//无法重写的内置函数，进行重新绘制
		//也就是将图面全白，然后调用paint函数
	}
	public void paint(Graphics g) {
		super.paint(g);		
		if(!shapes.isEmpty()) {		
			for(Shape shape: shapes) 
				shape.draw(g);
		}
	}
}