import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Enumeration;

public class UDPHeartbeats extends Thread{
    private static int serverUDPPort;
    private static String serverHost;
    private static final int maxfailedrounds = 3;
    private static final int timeout = 3000;
    private static final int bufsize = 4096;
    private static final int period = 2000;

    public UDPHeartbeats(int port){
        serverUDPPort = port;
        serverHost = "localhost";
        this.start();
    }


    public void run() {
        //int countHeartBeats = 0;

        try (DatagramSocket aSocket = new DatagramSocket()) {
            //aSocket.setSoTimeout(timeout);
            System.out.println("sending heartbeats");
            int failedheartbeats = 0;

            while(failedheartbeats < maxfailedrounds) {
                try {
                    int heartbeat = 200;
                    byte[] buf = ByteBuffer.allocate(4).putInt(heartbeat).array();

                    InetAddress aHost = InetAddress.getByName(serverHost);
                    DatagramPacket request = new DatagramPacket(buf, buf.length, aHost, serverUDPPort);
                    aSocket.send(request);

                    aSocket.setSoTimeout(timeout);

                    byte[] buffer = new byte[bufsize];
                    DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                    aSocket.receive(reply);

                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, reply.getLength()));
                    int respond = dis.readInt();


                    System.out.println("Recebeu: " + respond);
                    if (respond == 200)
                        failedheartbeats = 0;
                    else
                        failedheartbeats++;

                    Thread.sleep(period);


                } catch (SocketTimeoutException ste) {
                        failedheartbeats++;
                        System.out.println("Failed heartbeats: " + failedheartbeats);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            aSocket.close();
            System.out.println("no connection to main server");
            // turn server to main, init thread to listen to secondary servers
            new UDPConnectionListener(serverUDPPort);
            // ends current thread to allow the main process to accept connections from clients




        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        }
    }

}
