package user_utils;

import java.net.InetAddress;

public class Address{
    private String username;
    private InetAddress ip;
    private int port;

    public Address(String username, InetAddress ip, int port){
        this.username = username;
        this.ip = ip;
        this.port = port;
    }

    public String get_username(){
        return this.username;
    }

    public InetAddress get_ip(){
        return this.ip;
    }

    public int get_port(){
        return this.port;
    }
}
