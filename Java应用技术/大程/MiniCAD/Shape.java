import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;

/*
 * 每个形状需要的参数至少有：颜色、粗细、两个能代表本图形的点（线段的两点，矩形、椭圆的左和右两个对角点）
 * 每个形状还需要能够：被选中，被画出，本类完成实现
 */
public abstract class Shape implements Serializable{
    private static final long serialVersionUID = 1L;
    public ArrayList<Point> points=new ArrayList<Point>();//每种图形都是两个节点
    public Color color;
	public float thick;
    public Shape(Point p1,Point p2,Color CColor)//进行初始化定义
    {
        points.add(new Point(p1.x, p1.y));
        points.add(new Point(p2.x, p2.y));
        color = CColor;
		thick = 4;
    }//无法保证点的左右，因为后续拉拽的时候会导致第二个点的位置不确定
    public void CHANGE(int position){
        int del=4;
        int x1=points.get(0).x;
        int x2=points.get(1).x;
        int y1=points.get(0).y;
        int y2=points.get(1).y;
        switch (position){
            case 0://向上
                y1-=del;
                y2-=del;
                break;
            case 1://向下
                y1+=del;
                y2+=del;
                break;
            case 2://向左
                x1-=del;
                x2-=del;
                break;
            case 3://向右
                x1+=del;
                x2+=del;
                break;
        }
        points.get(0).y=y1;
        points.get(1).y=y2;
        points.get(0).x=x1;
        points.get(1).x=x2;
    }
    // public Point getCenterPoint(){
    //     int a1=(points.get(0).x+points.get(1).x)/2;
    //     int a2=(points.get(0).y+points.get(1).y)/2;
    //     return new Point(a1,a2);
    // }
    public void setColor(Color ccolor){
        color=ccolor;
    }
    public abstract void draw(Graphics g);//画图
	public abstract boolean isSelected(Point p);//确认是否鼠标选中
    public abstract void ChangeSize(int delta);//更改图形大小
}

class Line extends Shape {
    private static final long serialVersionUID = 1L;
    Line(Point p1,Point p2,Color CColor){//构造函数
        super(p1,p2,CColor);
    }
    @Override
    public void draw(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(new BasicStroke(thick));
		g2.setColor(color);//设置粗细和颜色
        //adjustPoint();
        g2.drawLine(points.get(0).x, points.get(0).y, points.get(1).x, points.get(1).y);	
    }
    @Override
    public boolean isSelected(Point p)
    {
        //选中一条线就是看这个点到这条线的距离
        int f=0;
        if (points.get(0).y>points.get(1).y)
          f=1;//f指向y比较小的那个
        if (p.x>points.get(0).x && p.x<points.get(1).x && p.y>points.get(f).y && p.y<points.get(f^1).y){
            //首先要在正确范围内，其次距离要合适
            double k = (points.get(1).y - points.get(0).y) * 1.0 / (points.get(1).x - points.get(0).x);
            double b = points.get(1).y - k * points.get(1).x;
            double d = java.lang.Math.abs((k * p.x - p.y + b) / (java.lang.Math.sqrt(k * k + 1)));
            if (d<10) return true;
        }
        return false;
    }
    @Override
    public void ChangeSize(int delta){
        //首先得到解析式
        double k = (points.get(1).y - points.get(0).y) * 1.0 / (points.get(1).x - points.get(0).x);
        double b = points.get(1).y - k * points.get(1).x;
        int x1=Math.min(points.get(0).x,points.get(1).x);
        int x2=Math.max(points.get(0).x,points.get(1).x);
        x1=x1-delta*5;
        x2=x2+delta*5;
        points.get(0).y+=k*x1+b-points.get(0).y;
        points.get(1).y+=k*x2+b-points.get(1).y;
        points.get(0).x=x1;
        points.get(1).x=x2;
    }
}


class Rectangle extends Shape {
    private static final long serialVersionUID = 1L;
    Rectangle(Point p1,Point p2,Color CColor){//构造函数,分别是左和右的两个对角点
        super(p1,p2,CColor);
    }
    @Override
    public void draw(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(new BasicStroke(thick));
		g2.setColor(color);//设置粗细和颜色
        //前一个是左边的点，后一个是右边的点
        int width=Math.abs(points.get(1).x-points.get(0).x);
        int height=Math.abs(points.get(1).y-points.get(0).y);	
        int x=Math.min(points.get(0).x,points.get(1).x);
        int y=Math.min(points.get(1).y,points.get(0).y);
        g2.drawRect(x, y, width, height);//做一个矩形，使用左上角的点和长宽
    }
    @Override
    public boolean isSelected(Point p)
    {
        int width=Math.abs(points.get(1).x-points.get(0).x);
        int height=Math.abs(points.get(1).y-points.get(0).y);	
        int x=Math.min(points.get(0).x,points.get(1).x);
        int y=Math.min(points.get(1).y,points.get(0).y);
        if (p.x>x && p.x<x+width && p.y>y && p.y<y+height) return true;//就是看选中点的坐标是不是在内部
        else return false;
    }
    public void ChangeSize(int delta){
        int width=Math.abs(points.get(1).x-points.get(0).x);
        int height=Math.abs(points.get(1).y-points.get(0).y);	
        int x=Math.min(points.get(0).x,points.get(1).x);
        int y=Math.min(points.get(1).y,points.get(0).y);//得到几个重要的变量
        double k = (height)*1.0/(width);//得到斜率
        double b = y*1.0-k*x*1.0;//得到b
        int x1=x-delta*5;
        int x2=x+width+delta*5;
        points.get(0).y+=k*x1+b-points.get(0).y;
        points.get(1).y+=k*x2+b-points.get(1).y;
        points.get(0).x=x1;
        points.get(1).x=x2;
    }
}


class Oval extends Shape{
    private static final long serialVersionUID = 1L;
    Oval(Point p1,Point p2,Color CColor){//构造函数,分别是左和右的两个对角点
        super(p1,p2,CColor);
    }
    @Override
    public void draw(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(new BasicStroke(thick));
		g2.setColor(color);//设置粗细和颜色
        int width=Math.abs(points.get(1).x-points.get(0).x);
        int height=Math.abs(points.get(1).y-points.get(0).y);	
        int x=Math.min(points.get(0).x,points.get(1).x);
        int y=Math.min(points.get(1).y,points.get(0).y);
        g2.drawOval(x, y, width, height);//做一个椭圆形，使用左上角的点和长宽
    }
    @Override
    public boolean isSelected(Point p)
    {
        int width=Math.abs(points.get(1).x-points.get(0).x);
        int height=Math.abs(points.get(1).y-points.get(0).y);	
        int x=Math.min(points.get(0).x,points.get(1).x);
        int y=Math.min(points.get(1).y,points.get(0).y);
        if (p.x>x && p.x<x+width && p.y>y && p.y<y+height) return true;
        else return false;
    }
    public void ChangeSize(int delta){
        int width=Math.abs(points.get(1).x-points.get(0).x);
        int height=Math.abs(points.get(1).y-points.get(0).y);	
        int x=Math.min(points.get(0).x,points.get(1).x);
        int y=Math.min(points.get(1).y,points.get(0).y);//得到几个重要的变量
        double k = (height)*1.0/(width);//得到斜率
        double b = y*1.0-k*x*1.0;//得到b
        int x1=x-delta*5;
        int x2=x+width+delta*5;
        points.get(0).y+=k*x1+b-points.get(0).y;
        points.get(1).y+=k*x2+b-points.get(1).y;
        points.get(0).x=x1;
        points.get(1).x=x2;
    }
}


class Text extends Shape {
	private static final long serialVersionUID = 1L;
	private String t;
	Text(Point p1, Point p2, String tt,Color CColor) {
		super(p1,p2,CColor);
		t = tt;
	}
	@Override
	public void draw(Graphics g) {
		g.setColor(color);
		g.setFont(new Font("宋体", Font.PLAIN, Math.abs(points.get(1).y-points.get(0).y)/2));
		g.drawString(t, points.get(0).x, Math.max(points.get(1).y,points.get(0).y));
	}

	@Override
	public boolean isSelected(Point p) {
        int f=0;
        if (points.get(0).y>points.get(1).y)
          f=1;//f指向y比较小的那个
		if(p.x > points.get(0).x && p.x<points.get(1).x && p.y>points.get(f).y && p.y<points.get(f^1).y)
			return true;
		else return false;
	}
	
	public void setText(String tt) {
		t = tt;
	}
    public void ChangeSize(int delta){
        int width=Math.abs(points.get(1).x-points.get(0).x);
        int height=Math.abs(points.get(1).y-points.get(0).y);	
        int x=Math.min(points.get(0).x,points.get(1).x);
        int y=Math.min(points.get(1).y,points.get(0).y);//得到几个重要的变量
        double k = (height)*1.0/(width);//得到斜率
        double b = y*1.0-k*x*1.0;//得到b
        int x1=x-delta*5;
        int x2=x+width+delta*5;
        points.get(0).y+=k*x1+b-points.get(0).y;
        points.get(1).y+=k*x2+b-points.get(1).y;
        points.get(0).x=x1;
        points.get(1).x=x2;
    }
}