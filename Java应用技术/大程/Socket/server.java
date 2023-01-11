import java.io.*;
import java.net.*;
import java.util.*;

public class server {
    public static LinkedList<PrintWriter> Writers = new LinkedList<>();
    private static int sumnum=50;
    public static int rec[]=new int[sumnum+1];
    public static int port=2312;
    public static int num=0;
    public static void main(String[] args) throws IOException{
        ServerSocket Sersocket=new ServerSocket(port);
        try{
            while(true){
                Socket socket=Sersocket.accept();//接收连接
                rec[num]=1;//记录这个用户已经进来了
                PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
                //要发送给客户端的初始化
                String mes="";
                for (int i=0; i<sumnum; i++){
                    if (rec[i]!=0){
                    mes+=String.valueOf(i);
                    mes+=" ";
                    }
                }
                mes+="###?";
                num++;//连接的客户数量增加1
                writer.println(mes);//完成组装
                writer.flush();// 传id给连接进来的客户端
                Writers.add(writer);//每一个用户需要对应一个给它们写的writer
                ServerHandler reader = new ServerHandler(socket);
                Thread handler = new Thread(reader);
                handler.start();
            }
        } finally{
            Sersocket.close();//关闭
        }
    }
    public static class ServerHandler implements Runnable {//对于单个线程，实现对应客户端的监听
        // 监听客户端并转发
        String message;
        BufferedReader in;
        Socket socket;
        public ServerHandler(Socket s) {
            socket = s;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //服务端的Socket对象上的getInputStream方法得到的输入流其实就是从客户端发送给服务器端的数据流
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        @Override
        public void run() {//单个线程对所有的发送
            try {
                int index=-1;
                while ((message=in.readLine())!=null){
                    System.out.println("Receive "+message);
                    String []otemp=message.split(":");
                    otemp[0]=otemp[0].substring(7);
                    int cc=Integer.parseInt(otemp[0]);//得到是哪个
                    if (rec[cc]==0 && message.indexOf("BACK")<0) continue;//如果这个用户已经暂时离开了，不接受它的消息
                    if (message.indexOf("!!!")>=0)
                    {//如果收到某一个取消请求的处理，这个时候对于取消的客户发送一个特殊的句子
                        //Client 1:CANCEL
                        System.out.println("CANCEL YES");
                        String []o=message.split(":");
                        o[0]=o[0].substring(7);
                        index=Integer.parseInt(o[0]);//得到了是哪个Writer在请求断开连接
                        rec[index]=0;//记录这个已经不在客户列表里了
                        System.out.println("Remove "+index+" ok");//成功移除
                        int cnt=0;
                        for (PrintWriter writer:Writers) {//对于所有的暂时离开用户,不能维护
                            if (rec[cnt]==0) {
                                System.out.println(cnt+" client has out");
                                writer.println("CANCEL");//完成组装
                                writer.flush();
                                //writer.close();
                                cnt++;
                                continue;
                            } 
                            else {
                                cnt++;
                                String mes="";
                                for (int k=0; k<sumnum; k++){
                                    if (rec[k]==1){
                                        mes+=String.valueOf(k);
                                        mes+=" ";
                                    }
                                }
                                mes+="###";
                                mes+="Client "+Integer.toString(index)+" leaves";
                                System.out.println("Send"+message);
                                writer.println(mes);//完成组装
                                writer.flush();
                            }
                        }
                    } 
                    else if (message.indexOf("BACK")>=0){//允许原来离开的人重新回来
                        System.out.println("Handle BACK");
                        System.out.println("REBACK YES");
                        String []o=message.split(":");
                        o[0]=o[0].substring(7);
                        index=Integer.parseInt(o[0]);//得到了是哪个Writer在请求恢复连接
                        rec[index]=1;//记录这个已经人重新回到了这个地方
                        System.out.println("Reback "+index+" ok");//成功
                        int cnt=0;
                        for (PrintWriter writer:Writers) {//对于之后的
                            if (rec[cnt]==0) {
                                System.out.println(cnt+" client has out");
                                writer.println("CANCEL");//完成组装
                                writer.flush();
                                cnt++;
                                continue;
                            } else {
                                cnt++;
                                String mes="";
                                for (int k=0; k<sumnum; k++){
                                    if (rec[k]==1){
                                        mes+=String.valueOf(k);
                                        mes+=" ";
                                    }
                                }
                                mes+="###";
                                mes+="Client "+Integer.toString(index)+" reback";
                                System.out.println("Send"+message);
                                writer.println(mes);//完成组装
                                writer.flush();
                            }                   
                        }
                    }
                    else {
                        int cnt=0;
                        for (PrintWriter writer:Writers) {
                            if (rec[cnt]==0){
                                writer.println("CANCEL");
                                writer.flush();
                                continue;
                            }
                            cnt++;
                            String mes="";
                            for (int i=0; i<sumnum; i++){
                                if (rec[i]!=0){
                                mes+=String.valueOf(i);
                                mes+=" ";
                                }
                            }
                            mes+="###";
                            mes+=message;
                            System.out.println("Send "+message);
                            writer.println(mes);//完成组装
                            writer.flush();
                        }
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }
}
