package user_utils;


public class Order{
    protected int order_id;
    protected String type;
    protected String order_type;
    protected int price;
    protected int size;
    protected long time;
    protected String username;
    
    
    public Order(int size, String ask_bid, String order_type, int price, long time){
        this.size = size;
        this.type = ask_bid;
        this.order_type = order_type;
        this.time = time;
        this.price = price;

    }

    public int get_size(){
        return this.size;
    }
    
    public String get_type(){
        return this.type;
    }

    public String get_order_type(){
        return this.order_type;
    }

    public int get_price(){
        return this.price;
    }

    public long get_time(){
        return this.time;
    }

    public void add_id(int id){
        this.order_id = id;
    }

    public int get_id(){
        return this.order_id;
    }

   
        public void add_username(String username){
            this.username = username;
        }

        public String get_username(){
            return this.username;
        }
    
    public void update_time(long time){
        this.time = time;
    }

        @Override
        public String toString() {
            return new String("order id: " + order_id + "\nprice: " + price + "\norder type: " + order_type + "\ntype: " + type + "\nsize: " + size + "\ntime: " + time);
        }

    }


