import java.awt.Point;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class Controller {
    public static Model model;
    public static View view;
    public static paintListener pl= new paintListener();
    public static String state = "idle";
    Controller(Model m, View v) {
        model = m;
    	view = v;
    }
    public static void updateView() {
        view.paintAll(model.getAll());//每次出现更改就要调用一次次全屏幕的清空和重画
    }
}

/*
 * 这里面的思路过程如下：
 * 修改过程：
 * （1）首先，需要点击选中，切换功能
 * （2）其次，在所有的shape里遍历，找到最后一个被选中范围里的点
 * （3）然后，保存这个点的坐标（一定要新创两个点存储坐标）
 * 新作图功能：
 * （1）首先，需要点击作图的几个按钮，得知需要画什么
 * （2）步骤是：首先建立一个类->通过拖拽改变另一个点->每次拖拽，都要调用repaint进行重画 
 */
class paintListener implements ActionListener, MouseListener, MouseMotionListener,KeyListener {
    String state="idle";
    Color colornow=Color.black;//默认颜色为
    String text;
    Shape Shapenow;
    int colorchange=0;
    String[] ColorName = {"红色","黄色","蓝色","绿色","黑色"};
    Point tmp[]=new Point[2];
    Point p1,p2;//每个图形都是由两个点所标注而生的

    @Override
    public void actionPerformed(ActionEvent e) {//按钮被按下，文本框输入回车会触发
        String btnName=e.getActionCommand();//得到现在按的按钮是什么
        //以下是四个作图的按钮
        if (btnName.equals("线段")==true){
            Controller.state="line";
            Shapenow = null;
        } else if (btnName.equals("矩形")==true){
            Controller.state="rec";
            Shapenow = null;
        } else if (btnName.equals("椭圆")==true){
            Controller.state="oval";
            Shapenow = null;
        } else if (btnName.equals("文字块")==true){
            Controller.state="string";
            Shapenow = null;
            text = JOptionPane.showInputDialog("请输入文本: ");
        } else if (btnName.equals("红色")==true || btnName.equals("黄色")==true || btnName.equals("蓝色")==true || btnName.equals("绿色")==true || btnName.equals("黑色")==true){   //以下是挑选颜色的按钮{
            Color[] colors = {Color.red,Color.yellow,Color.blue,Color.green,Color.black};
            for (int i=0; i<colors.length; i++){
                if (btnName.equals(ColorName[i])==true){
                    colornow=colors[i];
                    break;
                }
            }
            if (Shapenow!=null && colorchange==1){
                Shapenow.setColor(colornow);
                colorchange=0;
                Controller.updateView();
            }
        } else if (btnName.equals("选定")){
            Controller.state="select";
        } else if (btnName.equals("放大")){
            if(Shapenow != null) {
                Shapenow.ChangeSize(1);
            }
            Controller.updateView();
        } else if (btnName.equals("缩小")){
            if(Shapenow != null) {
                Shapenow.ChangeSize(-1);
            }
            Controller.updateView();
        } else if (btnName.equals("删除")){
            if (Shapenow!=null){
                Shapenow.setColor(Color.white);
            }
            Controller.updateView();
        } else if (btnName.equals("更改色彩")){
            colorchange=1;
        } else if (btnName.equals("打开")){
            openFile();
        } else if (btnName.equals("保存")){
            saveFile();
        } else if (btnName.equals("清屏")){
            Shapenow=null;//将几个状态都还原到初始值
            state="idle";
            colorchange=0;
            Controller.model.removeAll();
            Controller.updateView();
        } else{
            state="idle";
        }   
        Controller.view.requestFocus();
    }
    
    @Override
    public void mousePressed(MouseEvent e){
        p1 = new Point(e.getX(), e.getY());
        p2 = new Point(e.getX(), e.getY());
        switch (Controller.state) {
            case "line":
                Controller.model.add(new Line(p1, p2,colornow));
                Shapenow=null;
                break;
            case "rec":
                Controller.model.add(new Rectangle(p1, p2,colornow));
                Shapenow=null;
                break;
            case "oval":
                Controller.model.add(new Oval(p1, p2,colornow));
                Shapenow=null;
                break;
            case "string":
                Controller.model.add(new Text(p1, p2, text,colornow));
                Shapenow=null;
                break;
            case "select":
                Shapenow=findSelected(p1);//得到这个是具体的哪个
                if (Shapenow!=null){
                    for (int i=0; i<2; i++)
                        tmp[i]=null;
                    for (int i=0; i<2; i++)
                        tmp[i]=new Point(Shapenow.points.get(i).x,Shapenow.points.get(i).y);
                }
                break;
            default:
                Shapenow = null;
                break;
        }
        Controller.view.requestFocus();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (Controller.state.equals("idle")) return;
        if (Controller.state.equals("select")==true){
            if (Shapenow!=null){
                for (int i=0; i<Shapenow.points.size(); i++){
                    Shapenow.points.get(i).x=tmp[i].x+e.getX() - p1.x;
                    Shapenow.points.get(i).y=tmp[i].y+e.getY() - p1.y;//相对位置的变化是拖拽位置和点击位置之间的差距
                    Controller.updateView();  
                }
            }
                Controller.updateView();     	
        } else{
            Shape s=Controller.model.Last();
            s.points.get(1).x=e.getX();
            s.points.get(1).y=e.getY();
            Controller.updateView();
        }
        Controller.view.requestFocus();
    }

    @Override
    public void keyPressed(KeyEvent e){
        if (Controller.state.equals("select") && Shapenow!=null){
            int keyName=e.getKeyCode();
            switch (keyName){
                case KeyEvent.VK_UP:
                    Shapenow.CHANGE(0);
                    Controller.updateView();
                    break;
                case KeyEvent.VK_DOWN:
                    Shapenow.CHANGE(1);
                    Controller.updateView();
                    break;
                case KeyEvent.VK_LEFT:
                    Shapenow.CHANGE(2);
                    Controller.updateView();
                    break;   
                case KeyEvent.VK_RIGHT:
                    Shapenow.CHANGE(3);
                    Controller.updateView();
                    break;
                default:
                    break;
            }
        }
    }

    private Shape findSelected(Point p) {//这里是倒叙寻找，从最后一个加进来的开始找选中的是哪个
        ArrayList<Shape> tmp=Controller.model.getAll();
        for (int i=tmp.size()-1; i>=0; i--) {
            if(tmp.get(i).isSelected(p)) {
                return tmp.get(i);  
            }
        }
        return null;
    }

    private void saveFile() {
        JFileChooser chooser=new JFileChooser();
        chooser.showSaveDialog(null);
        chooser.setDialogTitle("保存文件");
        File file = chooser.getSelectedFile();
        
        if(file == null) {
        	JOptionPane.showMessageDialog(null,"未选择文件!请重新选择");
        }
        else {
            try { 
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
                out.writeObject(Controller.model.getAll());
                JOptionPane.showMessageDialog(null,"保存成功!请查看");
                out.close();
            }
            catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,"保存失败!请检查");
            }
        } 
    }

    @SuppressWarnings("unchecked")
	private void openFile() {		
		try {
			JFileChooser chooser = new JFileChooser();
			chooser.setDialogTitle("请打开cad文件");
			chooser.showOpenDialog(null);
			File file = chooser.getSelectedFile();
			
			if(file==null){
				JOptionPane.showMessageDialog(null, "未选择文件,请重新选择");
			}
			else {
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
				Controller.model.setAll((ArrayList<Shape>)in.readObject());
				Controller.updateView();
				in.close();
			}
		} 
		catch (Exception e1) {
			e1.printStackTrace();
			JOptionPane.showMessageDialog(null,"打开失败!请进行检查");
		}
    }



    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void keyTyped(KeyEvent e){}

    @Override
    public void keyReleased(KeyEvent e){}

    
}