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
import java.util.Map;
import java.util.TreeMap;
import java.util.Scanner;

public class Server {

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

  private int tcpPort;
  private int udpPort;
  private String fileName;

  private ProductData productData = new ProductData();

  class UdpServerThread extends Thread {
    private DatagramSocket socket;
    private byte[] buf = new byte[1024];
    private int port;
    private boolean running = true;

    public UdpServerThread(int port) {
      this.port = port;
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

  public Server(int tcpPort, int udpPort, String fileName) {
    this.tcpPort = tcpPort;
    this.udpPort = udpPort;
    this.fileName = fileName;
  }

  public void start() {

    try {

      // Load inventory file.
      productData.loadFromFile(fileName);

      // Start the UDP server.
      UdpServerThread udpServerThread = new UdpServerThread(udpPort);
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

    Server server = new Server(tcpPort, udpPort, fileName);
    server.start();

  }
}
