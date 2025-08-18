package user_utils;

import java.util.ArrayList;
import java.io.*;

public class User {
    public String username;
    private String password;
    private String nonce;
    private ArrayList<Integer> pending_operations;
    
    public User(String username, String password, String nonce){
        this.username = username;
        this.password = password;
        this.nonce = nonce;
        this.pending_operations = new ArrayList<Integer>();
    }
    
    public void get_status(){
        System.out.println("username: " + this.username);
        System.out.println("hashed password: " + this.password);
        System.out.println("nonce: " + this.nonce);
    }

    public String get_username(){
        return this.username;
    }

    public String get_password(){
        return this.password;
    }

    public String get_nonce(){
        return this.nonce;
    }

    public void change_password(String password, String nonce){
        this.password = password;
        this.nonce = nonce;
    }

}    
