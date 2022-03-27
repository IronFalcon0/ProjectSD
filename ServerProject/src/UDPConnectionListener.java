import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;

public class UDPConnectionListener extends Thread{
    private static int serverUDPPort;
    private static final int bufsize = 4096;

    public UDPConnectionListener (int port) {
        serverUDPPort = port;
        System.out.println("PORT: " + port);
        this.start();
    }


    // thread that listen for heartbeats
    public void run() {

        try (DatagramSocket lSocket = new DatagramSocket(serverUDPPort)) {
            System.out.println("UDP listener up");

            while(true) {
                System.out.println("Im main server");
                // receive heartbeat
                byte[] buffer = new byte[bufsize];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                System.out.println("1");
                lSocket.receive(request);
                System.out.println("2");
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, request.getLength()));
                System.out.println("3");
                int heartbeat = dis.readInt();
                System.out.println("heartbeat received: " + heartbeat);
                System.out.println("4");
                // send respond
                byte[] buf = ByteBuffer.allocate(4).putInt(heartbeat).array();
                System.out.println("5");
                DatagramPacket reply = new DatagramPacket(buf, buf.length, request.getAddress(), request.getPort());
                System.out.println("6");
                lSocket.send(reply);
                System.out.println("7");

            }




        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }
}


