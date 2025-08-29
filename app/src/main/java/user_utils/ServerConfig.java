package user_utils;

public class ServerConfig{

    public int tcp_port;
    public int udp_port;
    public int time_out_timer;
    public int update_db_timer;
    public int last_id;

    public ServerConfig(int tcp_port, int udp_port, int time_out_timer, int update_db_timer, int last_id){
        this.tcp_port = tcp_port;
        this.udp_port = udp_port;
        this.time_out_timer = time_out_timer;
        this.update_db_timer = update_db_timer;
        this.last_id = last_id;
    }
}
