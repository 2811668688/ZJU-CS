/*
 * 本代码尝试使用了MVC模式
 * 即为Model、View、Control
 * 本java为主函数层，负责接收上三层
 */
import java.awt.*;
import java.util.ArrayList;
import javax.swing.*;
public class MiniCAD extends JFrame{
    //此处要完成Title,Menu bar还有pane种的四个方位
    private static final long serialVersionUID = 1L;
    public MiniCAD(){
        //初始化布局设置
        setSize(1000,1000);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(20, 20));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Title设置
        setTitle("MiniCAD Project");//CAD项目
        //Menu设置
        JMenuBar menuBar=new JMenuBar();
        JMenu menu1=new JMenu("文件");
        JMenuItem menuItem1 = new JMenuItem("打开");
        JMenuItem menuItem2 = new JMenuItem("保存");
        menuItem1.addActionListener(Controller.pl);
        menuItem2.addActionListener(Controller.pl);
        menu1.add(menuItem1);
        menu1.add(menuItem2);      
        menuBar.add(menu1);
        setJMenuBar(menuBar);

        //north栏：颜色面板
        JPanel colorPanel = new JPanel();
        Color[] colors = {Color.red,Color.yellow,Color.blue,Color.green,Color.black};
        String[] ColorName = {"红色","黄色","蓝色","绿色","黑色"};
        colorPanel.setBackground(Color.black);
        colorPanel.setLayout(new GridLayout(1,colors.length));    
        for (int i=0; i<colors.length; i++){
            JButton button=new JButton(ColorName[i]);
            button.addActionListener(Controller.pl);
            //button.setFocusPainted(false);
            button.setPreferredSize(new Dimension(80, 80));
            button.setBackground(colors[i]);
            colorPanel.add(button);
        }//添加相应的按钮
        add(colorPanel,BorderLayout.NORTH);

        //west栏：图形面板
        String[] shape = { "线段", "矩形", "椭圆", "文字块"};
        JPanel shapePanel = new JPanel();
        shapePanel.setBackground(Color.black);
        shapePanel.setLayout(new GridLayout(shape.length,1));        
        for (int i = 0; i < shape.length; i++) {
                JButton button = new JButton(shape[i]);
                button.setBackground(Color.gray);
                button.setBorder(BorderFactory.createRaisedBevelBorder());//让它凸起来
                button.addActionListener(Controller.pl);
                shapePanel.add(button);
            }
        add(shapePanel,BorderLayout.WEST);

        //east栏：功能面板
        String[] functions = { "选定", "放大", "缩小","删除","更改色彩","清屏"};
        JPanel functionPanel = new JPanel();
        functionPanel.setBackground(Color.black);
        functionPanel.setLayout(new GridLayout(functions.length,1));
        for (int i = 0; i < functions.length; i++) {
            JButton button = new JButton(functions[i]);
            button.setBackground(Color.gray);
            button.setBorder(BorderFactory.createRaisedBevelBorder());//让它凸起来
            button.addActionListener(Controller.pl);
            functionPanel.add(button);
        }
        add(functionPanel,BorderLayout.EAST);
    }
    public static void main(String[] args){
        MiniCAD frame=new MiniCAD();//创建新类
        View view=new View();
        ArrayList<Shape> shapes = new ArrayList<Shape>();
        Model model=new Model(shapes);//初始化两个model和view
        frame.add(view,BorderLayout.CENTER);//主界面放在中间
        frame.setFocusable(true);
        frame.setVisible(true);//视为可见
        frame.requestFocus();
        @SuppressWarnings("unused")
        Controller ctr=new Controller(model,view);//使用控制器进行控制
    }
}
