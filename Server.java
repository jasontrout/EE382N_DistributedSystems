import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.InterruptedException;
import java.lang.StringBuilder;
import java.lang.Thread;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.TreeMap;
import java.util.Scanner;


// Specifies the commands the server will support.
interface ICommandProcessor {
  String purchase(String username, String productName, int quantity);
  String cancel(int orderId);
  String search(String username);
  String list();
}

public class Server implements ICommandProcessor {


  // Product data.
  class ProductData {
    
    private Map<String, Integer> productMap = new TreeMap<>();

    public ProductData() {}

    public synchronized void loadFromFile(String fileName) throws FileNotFoundException {
      File file = new File(fileName);
      Scanner sc = new Scanner(file);
      while (sc.hasNextLine()) { 
        String name = sc.next(); 
    	  int quantity = sc.nextInt();
	      productMap.put(name, quantity);
      }
      sc.close();
    }

    public synchronized void addProduct(String name, int quantity) {
      productMap.put(name, quantity);
    }

    public synchronized String toString() { 
      StringBuilder sb = new StringBuilder();
      for (String name : productMap.keySet()) {
	      int quantity = productMap.get(name);
        sb.append(name).append(" ").append(quantity).append("\n");
      }
      return sb.toString();
    }

  }

  // Order data.
  class OrderData {


    class Order {

      private int orderId;
      private String productName;
      private int productQuantity;

      public Order(int orderId, String productName, int productQuantity) {
        this.orderId = orderId;
        this.productName = productName;
        this.productQuantity = productQuantity;
      }

      public int getOrderId() {
        return orderId;
      }

      public String getProductName() {
        return productName;
      }

      public int getProductQuantity() {
        return productQuantity;
      }

      @Override
      public boolean equals(Object o) {
        if (!(o instanceof Order)) return false;
        Order rhsOrder = (Order)o;
        if (orderId == rhsOrder.getOrderId() && 
            productName.equals(rhsOrder.getProductName()) &&
            productQuantity == rhsOrder.getProductQuantity()) {
          return true;
        }
        return false;
      }

      @Override
      public int hashCode() {
        return Objects.hash(orderId, productName, productQuantity);
      }


    } 

    private Map<String, Map<Integer, Order>> userNameToOrdersMap = new TreeMap<>();
    private Map<Integer, String> orderIdToUserNameMap = new TreeMap<>();
    private int orderIdCounter = 1;

    // Get orders.
    public synchronized List<Order> getOrders(String username) {
      List<Order> orders = new ArrayList<>();
      if (userNameToOrdersMap.containsKey(username)) {
        Map<Integer, Order> orderIdToOrderMap = userNameToOrdersMap.get(username);
        for (int orderId : orderIdToOrderMap.keySet()) {
          orders.add(orderIdToOrderMap.get(orderId));
        }
      }
      return orders;
    }

    // Add an order.
    // Returns order id on success or -1 on failure.
    public synchronized int addOrder(String username, String productName, int productQuantity) {
      int orderId = orderIdCounter++;
      Order order = new Order(orderId, productName, productQuantity);
      if (!userNameToOrdersMap.containsKey(username)) {
        userNameToOrdersMap.put(username, new TreeMap<>());
      }
      userNameToOrdersMap.get(username).put(orderId, order);
      orderIdToUserNameMap.put(orderId, username);
      return orderId;
    }

    // Cancel an order.
    // Returns order id on success or -1 on failure.
    public synchronized int cancelOrder(int orderId) {
      return 0;
      
    }

  }

  private ProductData productData = new ProductData();
  private OrderData orderData = new OrderData();

  class UdpServerThread extends Thread {
    private ICommandProcessor processor;
    private DatagramSocket socket;
    private byte[] buf = new byte[1024];
    private int port;
    private boolean running = true;

    public UdpServerThread(int port, ICommandProcessor processor) {
      this.port = port;
      this.processor = processor;
    }

    public void run() {
      try {
        System.out.println("UDP server started. Listening on port " + port);
        socket = new DatagramSocket(port);
        while (running) {
          try { 
            DatagramPacket srcPacket = new DatagramPacket(buf, buf.length);
	          socket.receive(srcPacket);

	          InetAddress srcAddress = srcPacket.getAddress();
	          int srcPort = srcPacket.getPort();
            String cmd = new String(srcPacket.getData(), 0, srcPacket.getLength());
	          String[] tokens = cmd.split(" ");

	          if (tokens[0].equals("list")) {
              byte[] dstBytes = productData.toString().getBytes();
	            DatagramPacket dstPacket = new DatagramPacket(dstBytes, dstBytes.length, srcAddress, srcPort);
	            socket.send(dstPacket);
	          }
          } catch (Exception ex) {
	          ex.printStackTrace();
          }
        }
      } catch (Exception e) {
        System.out.println("Error in UdpServerThread.");
        e.printStackTrace();
      }
    }

    @Override
    public void interrupt() {
      super.interrupt();
      running = false;
      socket.close();
    }
  }

  public Server() { }


  // Handle purchase command.
  public String purchase(String username, String productName, int quantity) {
    return "";
  }
  
  // Handle cancel command.
  public String cancel(int orderId) {
    return "";
  }
  
  // Handle search command.
  public String search(String username) {
    return "";
  }

  // Handle list command.
  public String list() {
    return productData.toString();
  }

  public void start(int tcpPort, int udpPort, String fileName) {

    try {

      // Load inventory file.
      productData.loadFromFile(fileName);

      // Start the UDP server.
      UdpServerThread udpServerThread = new UdpServerThread(udpPort, this);
      udpServerThread.start();

      // Wait for the threads to finish.
      udpServerThread.join();
    } catch (InterruptedException e) {
      System.out.println("Main thread interrupted.");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main (String[] args) {
    int tcpPort;
    int udpPort;
    if (args.length != 3) {
      System.out.println("ERROR: Provide 3 arguments");
      System.out.println("\t(1) <tcpPort>: the port number for TCP connection");
      System.out.println("\t(2) <udpPort>: the port number for UDP connection");
      System.out.println("\t(3) <file>: the file of inventory");

      System.exit(-1);
    }

    tcpPort = Integer.parseInt(args[0]);
    udpPort = Integer.parseInt(args[1]);
    String fileName = args[2];

    Server server = new Server();
    server.start(tcpPort, udpPort, fileName);

  }
}
