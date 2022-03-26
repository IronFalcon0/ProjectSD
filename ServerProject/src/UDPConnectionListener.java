import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;

public class UDPConnectionListener extends Thread{
    private static int serverUDPPort;
    private static final int bufsize = 4096;

    public UDPConnectionListener (int port) {
        serverUDPPort = port;
        this.start();
    }


    // thread that listen for heartbeats
    public void run() {

        try (DatagramSocket aSocket = new DatagramSocket(serverUDPPort)) {
            System.out.println("UDP listener up");

            while(true) {
                // receive heartbeat
                byte[] buffer = new byte[bufsize];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, request.getLength()));
                int heartbeat = dis.readInt();
                System.out.println("heartbeat received: " + heartbeat);

                // send respond
                byte[] buf = ByteBuffer.allocate(4).putInt(heartbeat).array();
                DatagramPacket reply = new DatagramPacket(buf, buf.length, request.getAddress(), request.getPort());
                aSocket.send(reply);

            }




        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }
}


