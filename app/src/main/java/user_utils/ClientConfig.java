package user_utils;

public class ClientConfig{
    public String server_ip;
    public int server_port;
    public int udp_port;

    public ClientConfig(String server_ip, int server_port, int udp_port){
        this.server_ip = server_ip;
        this.server_port = server_port;
        this.udp_port = udp_port;
    }
}
