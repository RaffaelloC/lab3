import java.io.*;
import java.net.*;
import java.util.stream.Collectors;
import java.lang.Thread;


public class Client{
    private Socket client_socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread io_handler;
    private Object io_mutex = new Object();

    
    public void connect_to_server(String ip, int port){
        try{ 
            client_socket = new Socket(ip, port);
            out = new PrintWriter(client_socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
            }
        catch(UnknownHostException e){
            System.out.println("Server is out of service :(");
        }
        catch(IOException e){
        }
        io_handler = new Thread(new Runnable(){
            public void run(){
                try{
                String server_msg = in.readLine();
                while(server_msg != null){
                    synchronized(io_mutex){
                        System.out.println(server_msg);
                    }
                    server_msg = in.readLine();
                }
                }
                catch(IOException e){
                    System.out.println("over");
                }
            }
        });
        io_handler.start();
    }
    public void send_message(String msg){
        out.println(msg);
    }
    
    public static void main(String args[]) throws IOException{
        Client t = new Client();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        t.connect_to_server("127.0.2.1", 6667);
        System.out.println();
        String text = br.readLine();
        while(!text.equals("exit")){
            t.send_message(text);
            text = br.readLine();
        }
    }
}
