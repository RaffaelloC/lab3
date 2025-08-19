
import java.net.*;
import java.io.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.Collections;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import user_utils.*;
import java.util.concurrent.*;
import java.util.concurrent.ExecutorService;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Server {

    public static class Client_Handler implements Runnable {
        private ServerSocket serverSocket;
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private static final String login_greeting = "Welcome to CROSS!\nType help to see the commands";
        private static final String greeting = "Welcome to CROSS! An application for cryptocurrency trading! What operation are you willing to do today?";
        private static final String help = "avaliable commands:\nregister\nlogin\nlogout\nupdateCredentials";
        private ArrayList<User> user_list;
        private User logged_user = null;
        private boolean logged = false;
        private Socket client_socket;

        public Client_Handler(ArrayList<User> user_list, Socket client_socket){
            this.client_socket = client_socket;
            this.user_list = user_list;
        };

        public synchronized void print_list(){}

        public synchronized boolean add_user_list(User user_login){
            Optional<User> user = this.user_list.stream().filter(x -> x.get_username().equals(user_login.get_username())).findFirst();
            if(user.isPresent()){
                return false;
            }
            this.user_list.add(user_login);
            return true;
        };

        public synchronized boolean remove_user_list(){
            for(int i = 0; i < this.user_list.size(); i++){
                if(this.user_list.get(i).get_username() == this.logged_user.get_username()){
                    this.user_list.remove(this.user_list.get(i));
                    this.logged = false;
                    return true;
                } 
            }
            return false;
        }

        public byte[] conc_n_hash(String password, String nonce){
            byte hashed[] = new byte[256];
            try{
                MessageDigest hasher = MessageDigest.getInstance("SHA-256");
                ByteArrayOutputStream conc = new ByteArrayOutputStream();
                conc.write(password.getBytes());
                conc.write(nonce.getBytes());
                hasher.update(conc.toByteArray());
                hashed = hasher.digest(conc.toByteArray());
                return hashed;
            }
            catch(Exception e){
                System.out.println(e);
                return hashed;
            }
        }



        public int register_user(File user_db_file, String username, String password){
            boolean not_in_db = false;
            Pattern user_regex = Pattern.compile("[a-zA-Z_0-9]*");
            Matcher valid_usr;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            ArrayList<User> users = new ArrayList<User>();
            Type type = new TypeToken<ArrayList<User>>(){}.getType();
            
            try{
                Reader read = new FileReader(user_db_file);
                users = gson.fromJson(read, type);
                if(users == null){
                    users = new ArrayList<User>();
                };
                System.out.println(users);
                while(!not_in_db){
                    valid_usr = user_regex.matcher(username);
                    if(username.length() > 0 && username.length() < 37 && valid_usr.matches()){
                        out.println("Valid Username! I just need to check the database...");
                        boolean in_db = users.stream().anyMatch(w -> w.get_username().equals(username)); 
                        if(!in_db){
                            out.println("Username available!");
                            not_in_db = true;
                        }
                        else{
                            out.println("Username not available, try a new one!");
                            return 102;
                        }
                    }
                    else{
                        out.println("Invalid Username: try a new one!");
                        return 102;
                    }
                }
                SecureRandom rng = new SecureRandom();
                byte nonce[] = new byte[32];
                rng.nextBytes(nonce);
                byte hashed1[] = new byte[256];
                hashed1 = conc_n_hash(password, new String(nonce, "UTF-8"));
                if(password.length() == 0){
                    out.println("Password not long enough!");
                    return 101;
                }
                else{
                    out.println("Confirmed!");
                    }
                Writer writer = new FileWriter(user_db_file);
                User user = new User(username, new String(hashed1, "UTF-8"), new String(nonce, "UTF-8"));
                users.add(user);
                gson.toJson(users, writer);
                writer.flush();
                writer.close();
                user.get_status();
                return 100;
            }
            catch(Exception e){
                System.out.println(e);
                return 103;
            }
        }

        public int login_user(File user_db, String username, String password){
            if(this.logged == true){
                return 102;
            };
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            ArrayList<User> users = new ArrayList<User>();
            Type type = new TypeToken<ArrayList<User>>(){}.getType();
            try{
                Reader read = new FileReader(user_db);
                users = gson.fromJson(read, type);
                out.println("Insert Username");
                Optional<User> user = users.stream().filter(x -> x.get_username().equals(username)).findFirst();
                if(!user.isPresent()){
                    out.println("Not in database");
                    return 101;
                }
                user.get().get_status();
                byte[] hashed_password = conc_n_hash(password, user.get().get_nonce());
                if(!new String(hashed_password, "UTF-8").equals(user.get().get_password())){
                    out.println("Wrong password!");
                    return 101;
                }
                read.close();
                boolean success = add_user_list(user.get());
                if(!success){
                    out.println("this user is already logged!");
                    return 102;
                }
                this.logged_user = user.get();
                this.logged = true;
                return 100;
            }
            catch(Exception e){
                System.out.println(e);
            }
            return 103;
        }
        
        public int change_password(File user_db, String username, String password, String new_password){
            if(this.logged == true){
                return 104;
            }
            if(new_password == ""){
                return 101;
            }
            ArrayList<User> users = new ArrayList<User>();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Type type = new TypeToken<ArrayList<User>>(){}.getType();
            out.println("Insert your username to change the password");
            try{
                Reader read = new FileReader(user_db);
                users = gson.fromJson(read, type);
                Optional<User> user = users.stream().filter(x -> x.get_username().equals(username)).findFirst();
                if(!user.isPresent()){
                    out.println("User not in database");
                    return 102;
                }
                out.println("Insert old password");
                String hashed_password = new String(conc_n_hash(password, user.get().get_nonce())); 
                if(!user.get().get_password().equals(hashed_password)){
                    out.println("Password is wrong");
                    return 102;
                }
                String new_password_old_nonce = new String(conc_n_hash(new_password, user.get().get_nonce())); 
                if(user.get().get_password().equals(new_password_old_nonce)){
                    out.println("New password equals the other one");
                    return 103;
                }
                SecureRandom rng = new SecureRandom();
                byte nonce[] = new byte[32];
                rng.nextBytes(nonce);
                user.get().get_status();
                user.get().change_password(new String(conc_n_hash(new_password, new String(nonce, "UTF-8"))), new String(nonce, "UTF-8"));
                user.get().get_status();
                FileWriter write = new FileWriter(user_db);
                gson.toJson(users, write);
                write.flush();
                write.close();
                read.close();
                out.println("Password Updated!");
                return 100;
            }
            catch(Exception e){
                System.out.println(e);
                return 105;
            }
        }

        public int logout(User user, boolean logged){
            if(this.logged == false){
                return 101;
            } 
            remove_user_list();
            return 100;
        }

        public void run() {
            try{
                int port = 6667;
                out = new PrintWriter(this.client_socket.getOutputStream(), true);
                out.println(login_greeting);
                File user_db_file = new File("user_db.json"); 
                user_db_file.createNewFile();
                in = new BufferedReader(new InputStreamReader(this.client_socket.getInputStream()));
                User user = null;
                boolean logged = false;
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                int return_message;
                out.println(greeting);
                while (true){
                    out.print("> ");
                    String input = in.readLine();
                    if(input == null){
                        remove_user_list();
                        System.out.println("connection dead, exiting!");
                        break;
                    }
                    String[] commands = input.split("\\s+");
                    switch (commands[0].toLowerCase()) {
                        case "help":
                            out.println(help);
                            break;
                        case "register":
                            if(commands.length != 3){
                                out.println("command malformated");
                                out.println("usage: register username password");
                            }
                            else{
                                return_message = register_user(user_db_file, commands[1], commands[2]);
                                out.println(return_message);
                            }
                            break;
                        case "login":
                            if(commands.length != 3){
                                out.println("command malformated");
                                out.println("usage: login username password");
                            }
                            else{
                                return_message = login_user(user_db_file, commands[1], commands[2]);
                                if(return_message == 100){
                                    logged = true;
                                }
                                out.println(return_message);
                            }
                            break;
                        case "updatecredentials":
                            if(commands.length != 4){
                                out.println("command malformated");
                                out.println("usage: updateCredentials username currentPassword newPassword");
                            }
                            else{
                                return_message = change_password(user_db_file, commands[1], commands[2], commands[3]);
                                out.println(return_message);
                            }
                            break;
                        case "logout":
                            if(commands.length != 1){
                                out.println("command malformated");
                                out.println("usage: logout username");
                            }
                            else{
                                return_message = logout(user, logged);
                                if(return_message == 100){
                                    logged = false; 
                               }
                               out.println(return_message);
                            }
                            break;
                        case "islogged":
                            if(commands.length != 1){
                                out.println("command malformated"); 
                                out.println("usage: islogged"); 
                            }
                            else{
                                if(this.logged == true){
                                    out.println("yes");
                                    out.println(this.logged_user.get_username());
                                }
                                else{
                                    out.println("no");
                                    out.println("logged users:" + this.user_list);
                                }
                            }
                            break;
                        default:
                            out.println("not a known command");
                            out.println(help);
                            break;
                    }
                }
            }
            catch(IOException e){
                System.out.println("hey");}
            }
        public void stop() {
            try{
                in.close();
                out.close();
                clientSocket.close();
                serverSocket.close();
            }
            catch(IOException e){}
            }
        }
    public static void main(String[] args){
        ExecutorService thread_pool = Executors.newFixedThreadPool(32);
        try{
            ServerSocket server_socket = new ServerSocket(6667);
            ArrayList<User> logged_users = new ArrayList<User>();
            while (true){
                Socket client_socket = server_socket.accept();
                thread_pool.execute(new Client_Handler(logged_users, client_socket));
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
}
