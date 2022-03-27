import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class UDPConnectionListener extends Thread{
    public static ArrayList<String> newFiles = new ArrayList<String>();

    public UDPConnectionListener () {
        newFiles.add("test123");
        System.out.println("array newFiles: " + newFiles);
        this.start();
    }


    // thread that listen for heartbeats
    public void run() {

        try (DatagramSocket aSocket = new DatagramSocket(Server.serverUDPPort)) {
            System.out.println("UDP listener up");

            while(true) {
                // receive heartbeat
                byte[] buffer = new byte[Server.bufsize];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(request);
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, request.getLength()));
                int heartbeat = dis.readInt();

                // secondary server is waiting for a file
                if (heartbeat == 101) {
                    sendFileUDP();
                    continue;
                }
                System.out.println("heartbeat received: " + heartbeat);

                // send respond, send array with new files
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(baos);

                for (String element : newFiles) {
                    out.writeUTF(element);
                }
                byte[] bytes = baos.toByteArray();

                //byte[] buf = ByteBuffer.allocate(4).putInt(heartbeat).array();
                DatagramPacket reply = new DatagramPacket(bytes, bytes.length, request.getAddress(), request.getPort());
                aSocket.send(reply);

            }




        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        }
    }


    private void sendFileUDP () {
        // ask first for filename to send
        System.out.println("file requested");
        File file;
        byte buffer[] = new byte[Server.bufsize];

        try {
            DatagramSocket dsoc = new DatagramSocket(Server.UDPFilesPortMain);

            // receive package, fileName
            System.out.println("datagram create");
            DatagramPacket dp = new DatagramPacket(buffer,buffer.length);
            dsoc.receive(dp);
            //DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buffer, 0, dp.getLength()));
            String fileName = new String(dp.getData(), 0, dp.getLength(), StandardCharsets.UTF_8);
            System.out.println("fileName: " + fileName + ":");
            //System.out.println("fileNameBIN: " + Arrays.toString(dp.getData()) + ":");


            //buffer = Files.readAllBytes(file.toPath());;



        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


