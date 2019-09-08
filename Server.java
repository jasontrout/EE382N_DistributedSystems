import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.StringBuilder;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.util.Map;
import java.util.TreeMap;
import java.util.Scanner;

public class Server {

  static class ProductData {
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

  private static ProductData productData = new ProductData();

  static class UdpServer {
    private DatagramSocket socket;
    private byte[] buf = new byte[1024];

    public UdpServer(int udpPort) throws IOException {
      socket = new DatagramSocket(udpPort);
    }

    public void run() {
      while (true) {
        try { 
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
	socket.receive(packet);

	InetAddress address = packet.getAddress();
	int port = packet.getPort();
        String cmd = new String(buf);
	String[] tokens = cmd.split(" ");

	if (tokens[0].equals("list")) {
          byte[] retBytes = productData.toString().getBytes();
	  DatagramPacket retPacket = new DatagramPacket(retBytes, retBytes.length, address, port);
	  socket.send(retPacket);
	}
      } catch (Exception ex) {
	    ex.printStackTrace();

      }

    }
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

    // parse the inventory file
    try {
      productData.loadFromFile(fileName);
    } catch (Exception e) { 
      e.printStackTrace();
    }

    // TODO: handle request from clients
    try {
      UdpServer udpServer = new UdpServer(udpPort);
      udpServer.run();
    } catch (Exception e) {  
      e.printStackTrace();
    }
  }
}
