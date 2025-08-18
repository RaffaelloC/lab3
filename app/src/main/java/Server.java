
import java.net.*;
import java.io.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import user_utils.*;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class Server {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private static final String login_greeting = "Welcome to CROSS! would you like to login or create an account?\nselect 1 for login\nselect 2 for create an account\nselect 3 to change password";
    private static final String greeting = "Welcome to CROSS! An application for cryptocurrency trading! What operation are you willing to do today?";

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



    public void register_user(File user_db_file){
        String username = "";
        String password, confirm_password;
        boolean not_in_db = false;
        out.println("Insert Username!");
        out.println("Valid usernames are at most 36 characters");
        out.println("They may only contain alphanumeric characters and _");
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
                username = in.readLine();
                String tmp = username;
                valid_usr = user_regex.matcher(username);
                if(username.length() > 0 && username.length() < 37 && valid_usr.matches()){
                    out.println("Valid Username! I just need to check the database...");
                    boolean in_db = users.stream().anyMatch(w -> w.get_username().equals(tmp)); 
                    if(!in_db){
                        out.println("Username not used before! it's available");
                        not_in_db = true;
                    }
                    else{
                        out.println("Username not available, try a new one!");
                    }
                }
                else{
                    out.println("Invalid Username: try a new one!");
                }
            }
            boolean password_match = false;
            MessageDigest hasher = MessageDigest.getInstance("SHA-256");
            SecureRandom rng = new SecureRandom();
            byte nonce[] = new byte[32];
            rng.nextBytes(nonce);
            byte hashed1[] = new byte[256];
            byte hashed2[] = new byte[256];
            String pswd = new String();
            while(!password_match){
                out.println("Insert Password, it can be anything!");
                password = in.readLine();
                pswd = password;
                out.println("Repeat the password");
                hashed1 = conc_n_hash(password, new String(nonce, "UTF-8"));
                confirm_password = in.readLine();
                hashed2 = conc_n_hash(confirm_password, new String(nonce, "UTF-8"));
                if(!Arrays.equals(hashed1, hashed2)){
                    out.println("Password mismatch, try again!");
                }
                else if(password.length() == 0 || confirm_password.length() == 0){
                    out.println("Password not long enough!");
                }
                else{
                    out.println("Confirmed!");
                    password_match = true;
                }
            }
            Writer writer = new FileWriter(user_db_file);
            System.out.println("password: " + pswd + "\nnonce: " + nonce);
            User user = new User(username, new String(hashed1, "UTF-8"), new String(nonce, "UTF-8"));
            users.add(user);
            gson.toJson(users, writer);
            writer.flush();
            writer.close();
            user.get_status();
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public User login_user(File user_db){
        boolean logged = false;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ArrayList<User> users = new ArrayList<User>();
        Type type = new TypeToken<ArrayList<User>>(){}.getType();
        try{
            Reader read = new FileReader(user_db);
            users = gson.fromJson(read, type);
            out.println("Insert Username");
            String tmp = in.readLine(); 
            String username = tmp;
            Optional<User> user = users.stream().filter(x -> x.get_username().equals(username)).findFirst();
            if(!user.isPresent()){
                out.println("Not in database");
                return null;
            }
            user.get().get_status();
            out.println("Insert Password");
            String password = in.readLine();
            byte[] hashed_password = conc_n_hash(password, user.get().get_nonce());
            if(new String(hashed_password, "UTF-8").equals(user.get().get_password())){
                out.println("Success! logged in");
                logged = true;
            }
            else{
                out.println("Wrong password!");
                return null;
            }
            read.close();
            return user.get();
        }
        catch(Exception e){
            System.out.println(e);
        }
        return null;
    }
    
    public void change_password(File user_db){
        ArrayList<User> users = new ArrayList<User>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Type type = new TypeToken<ArrayList<User>>(){}.getType();
        out.println("Insert your username to change the password");
        try{
            String username = in.readLine();
            Reader read = new FileReader(user_db);
            users = gson.fromJson(read, type);
            Optional<User> user = users.stream().filter(x -> x.get_username().equals(username)).findFirst();
            if(!user.isPresent()){
                out.println("User not in database");
                return;
            }
            out.println("Insert old password");
            String password = in.readLine();
            if(!user.get().get_password().equals(new String(conc_n_hash(password, user.get().get_nonce())))){
                out.println("Password is wrong");
                return;
            }
            out.println("Insert new password");
            String new_password = in.readLine();
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
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    public User logout(){
            
        return null;
    }

    public void start(int port) {
        try{
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            File user_db_file = new File("user_db.json"); 
            user_db_file.createNewFile();
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String server_input = null;
            User user = null;
            boolean logged = false;
            while (user == null){
                out.println(login_greeting);
                server_input = in.readLine();
                if(server_input == null){
                    System.out.println("connection dead, exiting!");
                    break;
                }
                System.out.println(server_input);
                switch(server_input){
                    case "1":
                        out.println("Insert credentials");
                        user = login_user(user_db_file); 
                        if(user != null){
                            logged = true;
                        }
                        break;
                    case "2":
                        out.println("Create credentials");
                        register_user(user_db_file);
                        break;
                    case "3":
                        out.println("Insert credentials");
                        change_password(user_db_file);
                        break;
                    default:
                        out.println("Invalid input!");
                        break;
                }
            }
        while(user != null){
            out.println(greeting);
            out.println("Press 1 to logout");
            String input = in.readLine();
            switch (input) {
                case "1":
                    user = logout();
                    logged = false;
                    break;
                default:
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
    public static void main(String[] args) {
        Server server=new Server();
        server.start(6667);
    }
}
