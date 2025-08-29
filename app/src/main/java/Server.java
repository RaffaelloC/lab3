
import java.net.*;
import java.io.*;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.Calendar;
import javax.net.ssl.SSLSocket;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.Comparator;
import java.util.Date;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList; 
import java.util.Optional; 
import user_utils.*;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.ExecutorService;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

//Order history do

public class Server {

    public static class OrderBook{
        private TreeSet<Order> bid_orders = new TreeSet<Order>(new Comparator<Order>(){
                public int compare(Order o1, Order o2){
                    if(o1.get_price() != o2.get_price()){
                        return Integer.compare(o2.get_price(), o1.get_price());
                    }
                    else{
                        return Long.compare(o1.get_time(), o2.get_time());
                    }
                }
            });

        private TreeSet<Order> ask_orders = new TreeSet<Order>(new Comparator<Order>(){
                public int compare(Order o1, Order o2){
                    if(o1.get_price() != o2.get_price()){
                        return Integer.compare(o1.get_price(), o2.get_price());
                    }
                    else{
                        return Long.compare(o1.get_time(), o2.get_time());
                    }
                }
            });

        public Integer last_order_id;
        private Object order_mutex = new Object();
        private TreeSet<Order> stop_bid_orders = new TreeSet<Order>(new Comparator<Order>(){
                public int compare(Order o1, Order o2){
                    if(o1.get_price() != o2.get_price()){
                        return Integer.compare(o2.get_price(), o1.get_price());
                    }
                    else{
                        return Long.compare(o1.get_time(), o2.get_time());
                    }
                }
            });

        private TreeSet<Order> stop_ask_orders = new TreeSet<Order>(new Comparator<Order>(){
                public int compare(Order o1, Order o2){
                    if(o1.get_price() != o2.get_price()){
                        return Integer.compare(o1.get_price(), o2.get_price());
                    }
                    else{
                        return Long.compare(o1.get_time(), o2.get_time());
                    }
                }
            });
        private ArrayList<Order> order_history = new ArrayList<Order>();

        
        public int[][] get_price_history(int month_year){
            int[][] ret = new int[4][31];
            int year = month_year % 10000;
            int month = (month_year - year) / 10000;
            System.out.println("month: " + month + "\nyear: " + year);
            ArrayList<Order> orders_of_month = new ArrayList<Order>();
            for(int i = 0; i < order_history.size(); i++){
                Instant instant = Instant.ofEpochMilli(order_history.get(i).get_time());
                LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
                System.out.println(ldt.getMonthValue());
                if(ldt.getYear() == year && ldt.getMonthValue() == month){
                    System.out.println("a!");
                    orders_of_month.add(order_history.get(i));
                }
            }
            orders_of_month.sort(new Comparator<Order>(){
                public int compare(Order o1, Order o2){
                    return Long.compare(o1.get_time(), o2.get_time());
                }
            });
            for(int i = 0; i < orders_of_month.size(); i++){
                Instant instant = Instant.ofEpochMilli(orders_of_month.get(i).get_time());
                LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
                int day = ldt.getDayOfMonth() - 1;
                if(ret[0][day] == 0){
                    ret[0][day] = orders_of_month.get(i).get_price();
                    ret[1][day] = orders_of_month.get(i).get_price();
                    ret[2][day] = orders_of_month.get(i).get_price();
                    ret[3][day] = orders_of_month.get(i).get_price();
                    if(i != 0){
                        Instant inst = Instant.ofEpochMilli(orders_of_month.get(i-1).get_time());
                        LocalDateTime ldt2 = LocalDateTime.ofInstant(inst, ZoneId.of("UTC"));
                        ret[1][ldt2.getDayOfMonth() - 1] = orders_of_month.get(i-1).get_price();
                    }
                }
                else if(i+1 == orders_of_month.size()){
                    Instant inst = Instant.ofEpochMilli(orders_of_month.get(i).get_time());
                    LocalDateTime ldt2 = LocalDateTime.ofInstant(inst, ZoneId.of("UTC"));
                    ret[1][ldt2.getDayOfMonth() - 1] = orders_of_month.get(i).get_price();
                }
                ret[2][day] = orders_of_month.get(i).get_price() > ret[2][day] ? orders_of_month.get(i).get_price() : ret[2][day];
                ret[3][day] = orders_of_month.get(i).get_price() < ret[3][day] ? orders_of_month.get(i).get_price() : ret[3][day];
            }
            
            return ret;
        }

        private void load_order_history(File file){
            try{
                FileReader rd = new FileReader(file);
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<Order>>(){}.getType();
                order_history = gson.fromJson(rd, type);
            } 
            catch(Exception e){
                order_history = new ArrayList<Order>();
            }

        }

        public void update_database(){
            try{
                FileWriter write = new FileWriter(new File("ask_orders.json"));
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(ask_orders, write);
                write.flush();
                write.close();
                write = new FileWriter(new File("bid_orders.json"));
                gson.toJson(bid_orders, write);
                write.flush();
                write.close();
                write = new FileWriter(new File("stop_bid_orders.json"));
                gson.toJson(stop_bid_orders, write);
                write.flush();
                write.close();
                write = new FileWriter(new File("stop_ask_orders.json"));
                gson.toJson(stop_ask_orders, write);
                write.flush();
                write.close();
                write = new FileWriter(new File("order_history.json"));
                gson.toJson(order_history, write);
                write.flush();
                write.close();
                }
            catch(Exception e){
                System.out.println(e);
            }
        }

        public void update_periodically(){
            synchronized(order_mutex){
                update_database();
            }
        }

        public synchronized int get_id(){
            return ++last_order_id;
        }

        public OrderBook(int last_order_id){
            Gson gson = new GsonBuilder().setPrettyPrinting().create(); 
            try {
                Reader read = new FileReader("ask_orders.json");
                Type type = new TypeToken<ArrayList<Order>>(){}.getType();
                ArrayList<Order> ask_orders_db = gson.fromJson(read, type);
                for(int i = 0; i < ask_orders_db.size(); i++){
                    System.out.println(ask_orders_db.get(i));
                    this.ask_orders.add(ask_orders_db.get(i)); 
                }
                read.close();
            } 
            catch (Exception e){
                System.out.println("ask_orders database not found, starting a new one"); 
            }
            try {
                Reader read = new FileReader("bid_orders.json");
                Type type = new TypeToken<ArrayList<Order>>(){}.getType();
                ArrayList<Order> bid_orders_db = gson.fromJson(read, type);
                for(int i = 0; i < bid_orders_db.size(); i++){
                    this.bid_orders.add(bid_orders_db.get(i)); 
                }
                read.close();
            } 
            catch (Exception e){
                System.out.println("bid_orders database not found, starting a new one or couldnt close"); 
            }
            try {
                Reader read = new FileReader("stop_ask_orders.json");
                Type type = new TypeToken<ArrayList<Order>>(){}.getType();
                ArrayList<Order> stop_ask_orders_db = gson.fromJson(read, type);
                for(int i = 0; i < stop_ask_orders_db.size(); i++){
                    this.stop_ask_orders.add(stop_ask_orders_db.get(i)); 
                }
                read.close();
            } 
            catch (Exception e){
                System.out.println("stop_ask_orders database not found, starting a new one"); 
            }
            try {
                Reader read = new FileReader("stop_bid_orders.json");
                Type type = new TypeToken<ArrayList<Order>>(){}.getType();
                ArrayList<Order> stop_bid_orders_db = gson.fromJson(read, type);
                for(int i = 0; i < stop_bid_orders_db.size(); i++){
                    this.stop_bid_orders.add(stop_bid_orders_db.get(i)); 
                }
                read.close();
            } 
            catch (Exception e){
                System.out.println("stop_bid_orders database not found, starting a new one"); 
            }
            this.last_order_id = last_order_id;
            try {
                Reader read = new FileReader("order_history.json");
                Type type = new TypeToken<ArrayList<Order>>(){}.getType();
                ArrayList<Order> order_history_db = gson.fromJson(read, type);
                for(int i = 0; i < order_history_db.size(); i++){
                    this.order_history.add(order_history_db.get(i)); 
                }
                read.close();
            } 
            catch (Exception e){
                System.out.println("order_history database not found, starting a new one"); 
            }
            this.last_order_id = last_order_id;
        }
        public int cancel_order(int id, String username){
            Iterator<Order> it = ask_orders.iterator();
            while(it.hasNext()){
                Order tmp = it.next();
                System.out.println(tmp);
                if(tmp.get_id() == id && tmp.get_username().equals(username)){
                    it.remove();
                    return 100;
                }
                else if(tmp.get_id() == id && !tmp.get_username().equals(username)){
                    return 101;
                }
            }
            it = bid_orders.iterator();
            while(it.hasNext()){
                Order tmp = it.next();
                if(tmp.get_id() == id && tmp.get_username().equals(username)){
                    it.remove();
                    return 100;
                }
                else if(tmp.get_id() == id && !tmp.get_username().equals(username)){
                    return 101;
                }
            }
            it = stop_ask_orders.iterator();
            while(it.hasNext()){
                Order tmp = it.next();
                if(tmp.get_id() == id && tmp.get_username().equals(username)){
                    it.remove();
                    return 100;
                }
                else if(tmp.get_id() == id && !tmp.get_username().equals(username)){
                    return 101;
                }
            }
            it = stop_bid_orders.iterator();
            while(it.hasNext()){
                Order tmp = it.next();
                if(tmp.get_id() == id && tmp.get_username().equals(username)){
                    it.remove();
                    return 100;
                }
                else if(tmp.get_id() == id && !tmp.get_username().equals(username)){
                    return 101;
                }
            }
            return 101;
        }
        private int add_order(Order order, int size, TreeSet<Order> order_tree, ArrayList<Order> notify){
            Order new_order = new Order(size, order.get_type(), order.get_order_type(),
                    order_tree.getFirst().get_price(), order.get_time());
            new_order.add_username(order.get_username());
            new_order.add_id(order.get_id());
            if(size > order_tree.first().get_size()){
                size = size - order_tree.first().get_size();
                Order tmp = order_tree.pollFirst();
                notify.add(tmp); 
                notify.add(order);
                order_history.add(order);
                order_history.add(tmp);
                return size;
            }
            else if(size < order_tree.first().get_size()){
                Order removing_order = order_tree.pollFirst();
                Order updated_order = new Order(removing_order.get_size() - size, removing_order.get_type(), removing_order.get_order_type(), removing_order.get_price(), removing_order.get_time());
                updated_order.add_username(removing_order.get_username());
                updated_order.add_id(removing_order.get_id());
                notify.add(new_order);
                notify.add(removing_order);
                order_history.add(new_order);
                order_history.add(removing_order);
                order_tree.add(updated_order);
                size = size - removing_order.get_size();
                return size;
            }
            else{
                Order tmp = order_tree.pollFirst();
                size = size - tmp.get_size();
                notify.add(new_order);
                notify.add(tmp);
                System.out.println("does it arrive here?");
                order_history.add(new_order);
                order_history.add(tmp);
                System.out.println("what about here?");
                return size;
            }
        }
        
        public int add_market_order(Order order, ArrayList<Order> notify){
            int size = order.get_size();
            order.add_id(get_id());
            System.out.println(size);
            System.out.println("yo!");
            synchronized(order_mutex){
                System.out.println("yo2!");
                if(order.get_type().equals("ask")){
                    while(size > 0 && !bid_orders.isEmpty()){
                        size = add_order(order, size, bid_orders, notify);
                    }
                }
                else{
                    while(size > 0 && !ask_orders.isEmpty()){
                        System.out.println("yo3!");
                        size = add_order(order, size, ask_orders, notify);
                        System.out.println("yo4!");
                    }
                }
                if(size > 0){
                    return -1;
                }
            }
            return order.get_id();
        }

        public int add_limit_order(Order order, ArrayList<Order> notify){
            int size = order.get_size();
            order.add_id(get_id());
            synchronized(order_mutex){
                if(order.get_type().equals("ask")){
                    while(!bid_orders.isEmpty() && (size > 0 && order.get_price() <= bid_orders.getFirst().get_price())){
                        size = add_order(order, size, bid_orders, notify);
                    }
                    if(size > 0){
                        Order adding_order = new Order(size, order.get_type(),
                            order.get_order_type(), order.get_price(), order.get_time());
                        adding_order.add_username(order.get_username());
                        adding_order.add_id(order.get_id());
                        ask_orders.add(adding_order);
                    }
                    return order.get_id();
                }
                else{
                    while(!ask_orders.isEmpty() && (size > 0 && order.get_price() >= ask_orders.getFirst().get_price())){
                        size = add_order(order, size, ask_orders, notify);
                        System.out.println(size); 
                    }
                    if(size > 0){
                        
                        Order adding_order = new Order(size, order.get_type(),
                            order.get_order_type(), order.get_price(), order.get_time());
                        adding_order.add_username(order.get_username());
                        adding_order.add_id(order.get_id());
                        bid_orders.add(adding_order);
                    }
                    return order.get_id();
                }
            }
        }

        public void refresh_stop_orders(ArrayList<Order> notify){
            synchronized(order_mutex){
                while(!stop_ask_orders.isEmpty() && !bid_orders.isEmpty() &&
                    stop_ask_orders.getFirst().get_price() >= bid_orders.getFirst().get_price()){
                    int size = stop_ask_orders.getFirst().get_size();
                    add_order(stop_ask_orders.pollFirst(), size, bid_orders, notify);
                }
                while(!stop_bid_orders.isEmpty() && !ask_orders.isEmpty() && 
                stop_bid_orders.getFirst().get_price() <= ask_orders.getFirst().get_price()){
                    int size = stop_bid_orders.getFirst().get_price();
                    add_order(stop_bid_orders.pollFirst(), size, ask_orders, notify);
                }
            }   
        }

        public int add_stop_order(Order order, ArrayList<Order> notify){
            synchronized(order_mutex){
                if(order.get_type().equals("ask")){
                    stop_ask_orders.add(order);
                } 
                else{
                    stop_bid_orders.add(order);
                }
                order.add_id(get_id());
            }
            return order.get_id();
        }

        public void print_orders(){
            System.out.println("**********************************");
            System.out.println("bid orders:");
            Iterator<Order> it = bid_orders.iterator();
            while(it.hasNext()){
                System.out.println(it.next());
            }
            System.out.println("ask orders size: " + ask_orders.size());
            System.out.println("ask orders:");
            Iterator<Order> it2 = ask_orders.iterator();
            while(it2.hasNext()){
                System.out.println(it2.next());
            }
            System.out.println("**********************************");
        }
        
    }

    public static class Client_Handler implements Runnable {
        private ServerSocket serverSocket;
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private static final String greeting = "Welcome to CROSS! An application for cryptocurrency trading! What operation are you willing to do today?\nType help to see the commands";
        private static final String help = "avaliable commands:\nregister\nlogin\nlogout\nupdateCredentials\ninsertlimiorder\ninsertmarketorder\ninsertstoporder\ncancelorder";
        private ArrayList<User> user_list;
        private TreeMap<String, Address> address_list;
        private User logged_user = null;
        private boolean logged = false;
        private Socket client_socket;
        private Object user_db_mutex;
        private OrderBook pending_orders;
        private ServerConfig cfg;
        private Integer port;

        public Client_Handler(ArrayList<User> user_list, Socket client_socket, Object user_mutex, 
            TreeMap<String, Address> ip_list, OrderBook order_list, ServerConfig cfg){          
            this.address_list = ip_list;
            this.client_socket = client_socket;
            this.user_list = user_list;
            this.user_db_mutex = user_mutex;
            this.pending_orders = order_list;
            this.cfg = cfg;
        }

        public void notify_users(ArrayList<Order> notify, int udp_port){
            String username;
            Address ip;
            try {
                DatagramSocket udp_socket = new DatagramSocket(udp_port);
                System.out.println("woa!");
                for(int i = notify.size() - 1; i >= 0; i--){
                    username = notify.get(i).get_username();
                    System.out.println("notifying " + username);
                    ip = address_list.get(username);
                    if(ip == null){
                        System.out.println(username + " not online!");
                        notify.remove(i);
                        continue;
                    }
                    else{
                        DatagramPacket notification = new DatagramPacket(notify.get(i).toString().getBytes("UTF-8"),
                            notify.get(i).toString().getBytes("UTF-8").length, ip.get_ip(), ip.get_port());
                        udp_socket.send(notification);
                        notify.remove(i);
                    }
                }
                udp_socket.close();
            }
            catch(Exception e){
                System.out.println(e);
            }
        }

        public synchronized boolean add_user_list(User user_login){
            Optional<User> user = this.user_list.stream().filter(x -> x.get_username().equals(user_login.get_username())).findFirst();
            if(user.isPresent()){
                return false;
            }
            Address user_ip = new Address(user_login.get_username(), this.client_socket.getInetAddress(), port);
            System.out.println(user_ip.get_ip());
            System.out.println(user_ip.get_port());
            this.address_list.put(user_login.get_username(), user_ip);
            this.user_list.add(user_login);
            return true;
        };
        
        public void market_order(boolean ask, int price, int size){
            if(!this.logged){
                out.println("101 - Not logged in");
                return;
            }
            
            
        }  

        public synchronized boolean remove_user_list(){
            for(int i = 0; i < user_list.size(); i++){
                if(user_list.get(i).get_username() == logged_user.get_username()){
                    user_list.remove(user_list.get(i));
                    logged = false;
                    address_list.remove(logged_user.get_username());
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
            Pattern user_regex = Pattern.compile("\\w*");
            Matcher valid_usr;
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            ArrayList<User> users = new ArrayList<User>();
            Type type = new TypeToken<ArrayList<User>>(){}.getType();
            try{
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
                valid_usr = user_regex.matcher(username);
                if(username.length() > 0 && username.length() < 37 && valid_usr.matches()){
                    out.println("Valid Username! I just need to check the database...");
                }
                else{
                    out.println("Invalid Username: try a new one!");
                    return 102;
                }
                synchronized(this.user_db_mutex){
                    Reader read = new FileReader(user_db_file);
                    users = gson.fromJson(read, type);
                    if(users == null){
                        users = new ArrayList<User>();
                    }
                    System.out.println(users);
                    boolean in_db = users.stream().anyMatch(w -> w.get_username().equals(username)); 
                    if(!in_db){
                        out.println("Username available!");
                    }
                    else{
                        out.println("Username not available, try a new one!");
                        return 102;
                    }
                    Writer writer = new FileWriter(user_db_file);
                    User user = new User(username, new String(hashed1, "UTF-8"), new String(nonce, "UTF-8"));
                    users.add(user);
                    gson.toJson(users, writer);
                    writer.flush();
                    writer.close();
                    user.get_status();
                }
                return 100;
            }
            catch(Exception e){
                System.out.println(e);
                return 103;
            }
        }

        public int login_user(File user_db, String username, String password){
            if(logged){
                return 102;
            };
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            ArrayList<User> users = new ArrayList<User>();
            Type type = new TypeToken<ArrayList<User>>(){}.getType();
            try{
                synchronized(user_db_mutex){
                    Reader read = new FileReader(user_db);
                    users = gson.fromJson(read, type);
                    read.close();
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
                    boolean success = add_user_list(user.get());
                    if(!success){
                        out.println("user is already logged!");
                        return 102;
                    }
                    logged_user = user.get();
                    logged = true;
                }
                return 100;
            }
            catch(Exception e){
                System.out.println(e);
                System.out.println(e.getStackTrace());
                return 103;
            }
        }
        
        public int change_password(File user_db, String username, String password, String new_password){
            if(logged){
                return 104;
            }
            if(new_password == ""){
                return 101;
            }
            ArrayList<User> users = new ArrayList<User>();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Type type = new TypeToken<ArrayList<User>>(){}.getType();
            try{
                synchronized(user_db_mutex){
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
                }
                return 100;
            }
            catch(Exception e){
                System.out.println(e);
                return 105;
            }
        }

        public int logout(User user, boolean logged){
            if(!logged){
                return 101;
            } 
            remove_user_list();
            return 100;
        }

        public void run() {
            try{
                out = new PrintWriter(client_socket.getOutputStream(), true);
                System.out.println(client_socket.getInetAddress());
                System.out.println(client_socket.getPort());
                ArrayList<Order> notify = new ArrayList<Order>();
                File user_db_file = new File("user_db.json"); 
                user_db_file.createNewFile();
                in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
                port = Integer.parseInt(in.readLine());
                System.out.println("port number: " + port);
                boolean logged = false;
                int return_message;
                out.println(greeting);
                while (true){
                    try{
                        String input = in.readLine();
                        client_socket.setSoTimeout(cfg.time_out_timer * 1000);
                        if(input == null){
                            remove_user_list();
                            logged = false;
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
                            case "insertlimitorder":
                                if(commands.length != 4 || !(commands[1].equals("ask") || commands[1].equals("bid"))){
                                    out.println("command malformated");
                                    out.println("usage: insertLimitOrder ask/bid size price");
                                }
                                else if(!logged){
                                    out.println("must be logged to perform this operation");
                                }
                                else{
                                    Order new_order = new Order(Integer.parseInt(commands[2]), commands[1],
                                    "limit_order", Integer.parseInt(commands[3]), System.currentTimeMillis());
                                    new_order.add_username(logged_user.get_username());
                                    out.println("order id: " + pending_orders.add_limit_order(new_order, notify));
                                    pending_orders.refresh_stop_orders(notify);
                                    notify_users(notify, cfg.udp_port);
                                    pending_orders.print_orders();
                                }
                                break;
                            case "insertstoporder":
                                if(commands.length != 4 || !(commands[1].equals("ask") || commands[1].equals("bid"))){
                                    out.println("command malformated");
                                    out.println("usage: insertStopOrder ask/bid size price");
                                }
                                else if(!logged){
                                    out.println("must be logged to perform this operation");
                                }
                                else{
                                    Order new_order = new Order(Integer.parseInt(commands[2]), commands[1],
                                    "stop_order", Integer.parseInt(commands[3]), System.currentTimeMillis());
                                    new_order.add_username(logged_user.get_username());
                                    out.println("order id: " + pending_orders.add_stop_order(new_order, notify));
                                    pending_orders.refresh_stop_orders(notify);
                                    notify_users(notify, cfg.udp_port);
                                    pending_orders.print_orders();
                                }
                                break;
                            case "insertmarketorder":
                                if(commands.length != 3 || !(commands[1].equals("ask") || commands[1].equals("bid"))){
                                    out.println("command malformated");
                                    out.println("usage: insertMarketOrder ask/bid size");
                                }
                                else if(!logged){
                                    out.println("must be logged to perform this operation");
                                }
                                else{
                                    Order new_order = new Order(Integer.parseInt(commands[2]), commands[1],
                                    "market_order", 0, System.currentTimeMillis());
                                    new_order.add_username(logged_user.get_username());
                                    out.println("order id: " + pending_orders.add_market_order(new_order, notify));
                                    pending_orders.refresh_stop_orders(notify);
                                    notify_users(notify, cfg.udp_port);
                                    pending_orders.print_orders();
                                }
                                break;
                            case "cancelorder":
                                if(commands.length != 2){
                                    out.println("command malformated");
                                    out.println("usage: cancelorder order_id");
                                }
                                else if(!logged){
                                    out.println("must be logged to perform this operation");
                                }
                                else{
                                    int ret = pending_orders.cancel_order(Integer.parseInt(commands[1]), logged_user.get_username());
                                    if(ret == 100){
                                        out.println(ret + " - Order canceled!"); 
                                    }
                                    else{
                                        out.println(ret + "- Order not found or it wasn't made by this user");
                                    }
                                }
                                break;
                            case "logout":
                                if(commands.length != 1){
                                    out.println("command malformated");
                                    out.println("usage: logout username");
                                }
                                else{
                                    return_message = logout(logged_user, logged);
                                    if(return_message == 100){
                                        logged = false; 
                                   }
                                   out.println(return_message);
                                }
                                break;
                            case "pricehistory":
                                if(commands.length != 2){
                                    out.println("command malformated"); 
                                    out.println("usage: pricehistory MMYYYY"); 
                                }
                                else{
                                    int ret[][] = pending_orders.get_price_history(Integer.parseInt(commands[1])); 
                                    out.println("price history for each day of month");
                                    out.println("open price/close price/maximum price/minimum price");
                                    for(int i = 0; i < 31; i++){
                                        out.println("day " + (i+1) +":\t" + ret[0][i] + "/" + ret[1][i]
                                        + "/" + ret[2][i] +"/" + ret[3][i]);
                                    }
                                break;
                                }
                            case "islogged":
                                if(commands.length != 1){
                                    out.println("command malformated"); 
                                    out.println("usage: islogged"); 
                                }
                                else{
                                    if(logged){
                                        out.println("yes");
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
                    catch(SocketTimeoutException e){
                        if(logout(logged_user, logged) == 100){
                            logged = false;
                            out.println("Idle for too long, logging out!");
                        }
                    }
                }
            }
            catch(Exception e){}
        }
    }
    public static void main(String[] args){
        ExecutorService thread_pool = Executors.newFixedThreadPool(32);
        try{
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            File server_config_file = new File("server_config.json");
            if(!server_config_file.isFile()){
                server_config_file.createNewFile();
                ServerConfig cfg = new ServerConfig(6667, 6668, 60, 120, 0);
                FileWriter wrt = new FileWriter(server_config_file);
                gson.toJson(cfg, wrt);
                wrt.flush();
                wrt.close();
            }
            FileReader rd_config = new FileReader(server_config_file);
            Type type = new TypeToken<ServerConfig>(){}.getType();
            ServerConfig cfg = gson.fromJson(rd_config, type);
            ServerSocket server_socket = new ServerSocket(cfg.tcp_port);
            ArrayList<User> logged_users = new ArrayList<User>();
            Object user_mutex = new Object();
            TreeMap<String, Address> ip_list = new TreeMap<String, Address>();
            OrderBook order_list = new OrderBook(cfg.last_id);
            
                       
            Thread update_periodically = new Thread(){
               public void run(){
                    while(true){
                        try{
                            sleep(cfg.update_db_timer * 1000);
                            order_list.update_periodically();
                            FileWriter wrt = new FileWriter(server_config_file);
                            cfg.last_id = order_list.last_order_id;
                            gson.toJson(cfg, wrt);
                            wrt.flush();
                            wrt.close();
                        }
                        catch(Exception e){}
                    } 
                } 
            };
            update_periodically.start();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run(){
                    thread_pool.shutdownNow();
                    order_list.update_database();
                    try{
                        FileWriter wrt = new FileWriter(server_config_file);
                        cfg.last_id = order_list.last_order_id;
                        gson.toJson(cfg, wrt);
                        wrt.flush();
                        wrt.close();
                    }
                    catch(Exception e){
                        System.out.println(e);
                    }
            }
        });
            while(true){
                Socket client_socket = server_socket.accept();
                thread_pool.execute(new Client_Handler(logged_users, client_socket, user_mutex, ip_list, order_list, cfg));
            }
        }
        catch(Exception e){
            System.out.println(e);
        }
    }
}
