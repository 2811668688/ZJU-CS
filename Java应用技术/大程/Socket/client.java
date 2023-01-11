import java.awt.*;
import java.net.*;
import javax.swing.*;
import java.awt.event.*;
import java.io.*;

public class client extends JFrame{
    int PORT=2312;//用学号的后4位作为端口号
    int id=-1;//作为客户的编号,-1为初始设置
    int num=0;//记录一共有多少个客户端
    Socket socket;
    JTextField textField;
    BufferedReader in;
    PrintWriter out;
    JButton button;
    JTextArea Display,clientarea;
    public client(){
        //初始化布局设置
        setSize(600,450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //Title设置
        setTitle("The chatting room");//Client项目
        //SOUTH栏设置
        JPanel chatPanel = new JPanel();
        textField = new JTextField(45);
        chatPanel.setBackground(Color.BLUE);
        button = new JButton("Send");
        chatPanel.add(textField);
        chatPanel.add(button);
        add(chatPanel,BorderLayout.SOUTH);
        //EAST栏设置
        JPanel ClinetPanel = new JPanel();//用来显示聊天室成员名单的
        ClinetPanel.setBackground(Color.GRAY);
        clientarea = new JTextArea("This is all the users",20,12);
        ClinetPanel.add(clientarea);
        add(ClinetPanel,BorderLayout.EAST);
        //主页面设置 
        Display = new JTextArea();
        Display.setLineWrap(true);//设置自动换行
        JScrollPane scroller= new JScrollPane(Display);
        Display.setFocusable(false);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroller,BorderLayout.CENTER);
    }

    public void connect() throws IOException{
        try {
            socket = new Socket("localhost", PORT);//开启监听
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //客户端的Socket对象上的getInputStream方法得到输入流其实就是从服务器端发回的数据
            // Enable auto-flush:
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
            //客户端的Socket对象上的getOutputStream方法得到的输出流其实就是发送给服务器端的数据。
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Fail to connect server!");
            //是JavaSwing 内部已实现好的，以静态方法的形式提供调用，能够快速方便的弹出要求用户提供值或向其发出通知的标准对话框
            socket.close();
            //关闭socket连接
        }

        ActionListener listener = new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e){//如果按下了按钮
                String message = textField.getText();//得到了一条要发送的消息
                System.out.println("Send:"+message);
                if (message!=null && !message.equals("") && id!=-1) {
                    try {
                        out.println("Client "+id+":"+message);
                        //这里进行强行设置，如果发送的内容为CANCEL,将会断开连接
                        out.flush();//把缓冲区的内容全部先发出去，message内可能太长导致上一次残留
                    } catch (Exception e1) {//应out要求，设置异常检测
                        e1.printStackTrace();
                    }
                }
                if (message.equals("!!!")==true){
                    System.out.println("Closed");
                } else if (message.equals("BACK")==true){
                    System.out.println("you have back");
                } 
                textField.setText("");//重新设置回原来的空白文件
            }
        };
        textField.addActionListener(listener);
        button.addActionListener(listener);
    }

    public class ClientHandler implements Runnable {//线程处理口
        String message;
        @Override
        public void run() {
            try {
                while ((message=in.readLine()) != null) {
                    System.out.println("Reciveve:"+message);
                    if (message.equals("CANCEL")==true){

                        clientarea.setText("");//清空
                        Display.setText("");
                        Display.append("You have left"+"\n");
                        continue;
                    }
                    /*
                    * 这里约定：客户端传回来的数据包为以下格式:
                    * 1 2 3 4 5 6 7 8 9 ###信息
                    * 也就是，前面是按照一个用户id+一个空格，然后###，最后是message
                    * message格式为:Client id:xxxxx
                    */
                    String []tmp=message.split("###");
                    clientarea.setText("");//清空
                    String []Tmpclient=tmp[0].split(" ");//分出信息
                    clientarea.append("Client now"+"\n");
                    for (int i=0; i<Tmpclient.length; i++)
                    clientarea.append("Client:"+Tmpclient[i]+"\n");//给出当前的用户数
                    if (Tmpclient.length>num){
                        Display.append("Client enter"+"\n");
                    }
                    else if (Tmpclient.length<num){
                        Display.append("Client leave"+"\n");
                    }
                    if (id==-1){
                        id=Integer.parseInt(Tmpclient[Tmpclient.length-1]);
                        //id=0;
                        Display.append("Client "+id+" has connected"+"\n");
                    }
                    num=Tmpclient.length;
                    if (tmp[1].equals("?")==false)
                    //如果有真的消息传过来
                        Display.append(tmp[1] + "\n");//约定格式，用户号+发送信息号
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }
    public static void main(String args[]) throws IOException{
        client frame=new client();//创建新类
        frame.setVisible(true);//视为可见
        frame.connect();//连接socket
        ClientHandler reader=frame.new ClientHandler();//开启处理
        Thread handler=new Thread(reader);
        handler.run();
    }

}
