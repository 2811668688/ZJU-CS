import java.io.BufferedReader; 
import java.io.InputStreamReader;  
import java.io.OutputStreamWriter;  
import java.net.Socket;  
import java.net.ServerSocket;  
import java.net.SocketAddress;
import java.io.PrintWriter;  
import java.util.ArrayList;  
import java.util.List;  
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
  
public class Server {  
    //服务端的socket
    private ServerSocket serverSocket;  
    //服务端用来接收客户端连接的线程池
    private ExecutorService threadPool;
    //所有的和客户端通信的Printwriter  
    private List<PrintWriter> writers;

    //初始化
    public Server(){  
        try{  
            serverSocket = new ServerSocket(2312);  
            threadPool = Executors.newFixedThreadPool(100); 
            writers = new ArrayList<PrintWriter>();
        }catch(Exception e){  
            e.printStackTrace();  
        }  
    }
    
    private synchronized void addWriter(PrintWriter w){
    	writers.add(w);
    }
    private synchronized void removeWriter(PrintWriter w){
    	writers.remove(w);
    }
    private synchronized void sendMessage(String message){
    	for(PrintWriter w:writers){
    		w.println(message);
    	}
    }
    
    class ServerHandler implements Runnable{  
        private Socket socket;  
        //构造
        ServerHandler(Socket s){  
            socket = s;  
        }  
        //执行  
        @Override  
        public void run() {  
            PrintWriter writer=null;  
            try {  
                //初始化进行对客户端写入的writer
                writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);   
                //安全添加到writers中
                addWriter(writer); 
                //初始化进行对客户端读入的reader
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));  
                String message = null;
                while((message = bufferedReader.readLine()) != null){  
                    SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
                	sendMessage(remoteSocketAddress.toString() + ":" + message); 
                }                
            }catch (Exception e) {  
                e.printStackTrace();  
            }finally{  
                //当客户端断开连接时，安全移除
            	removeWriter(writer);
                if(socket != null){  
                    try{  
                        socket.close();  
                    }catch(Exception e){  
                        e.printStackTrace();  
                    }  
                }  
            }             
        }         
    }  
      
      
    public void start(){  
        try{  
            //循环监听客户端的连接  
            while(true){  
                System.out.println("等待客户端连接...");  
                Socket socket = serverSocket.accept();  
                SocketAddress remoteSocketAddress = socket.getRemoteSocketAddress();
                System.out.println("客户端"+remoteSocketAddress+"已连接！");  
                ServerHandler handler = new ServerHandler(socket);  
                //启动一个线程来完成针对该客户端的交互  
                threadPool.execute(handler);  
            }  
        }catch(Exception e){  
            e.printStackTrace();  
        }  
    }  
      
    public static void main(String[] args) {  
        //建立接收连接的server thread Server
        Server server = new Server();  
        server.start();  
    }  
  
}  