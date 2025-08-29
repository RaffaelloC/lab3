import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import user_utils.ClientConfig;

import java.lang.Thread;


public class Client{
    private Socket client_socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread io_handler;
    private Object io_mutex = new Object();
    private static boolean server_on = true;

   public void udp_handler(){
                try{
                    DatagramSocket socket = new DatagramSocket(55555);
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    while (server_on){
                        socket.receive(packet);
                        String recv_order = new String(packet.getData(), "UTF-8");
                        synchronized(io_mutex){
                            System.out.println(recv_order);
                        }
                    }
                }
                catch(Exception e){
                    synchronized(io_mutex){
                        System.out.println(e);
                    }
                }
            }  



    public void connect_to_server(String ip, int port, int udp_port){
        try{ 
            Thread udp_receiver = new Thread(() -> udp_handler());            
            udp_receiver.start();
            client_socket = new Socket(ip, port);
            out = new PrintWriter(client_socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
            send_message(Integer.toString(udp_port));
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
                System.out.println("Sorry! Unfortunately, the server went offline! Press any key to exit");
                server_on = false;
                }
                catch(Exception e){
                }
            }
        });
        io_handler.start();
        }
        catch(Exception e){
            System.out.println("server offline!");
            server_on = false;
        }
    }
    public void send_message(String msg){
        out.println(msg);
    }
    
    public static void main(String args[]) throws IOException{
        try{
            Client t = new Client();
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            File client_config_file = new File("client_config.json");
            if(!client_config_file.isFile()){
                client_config_file.createNewFile();
                ClientConfig cfg = new ClientConfig("127.0.0.1", 6667, 55555);
                FileWriter wrt = new FileWriter(client_config_file);
                gson.toJson(cfg, wrt);
                wrt.flush();
                wrt.close();
            }
            FileReader rd_config = new FileReader(client_config_file);
            Type type = new TypeToken<ClientConfig>(){}.getType();
            ClientConfig cfg = gson.fromJson(rd_config, type);

            t.connect_to_server(cfg.server_ip, cfg.server_port, cfg.udp_port);
            while(server_on){
                t.send_message(br.readLine());
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
}
