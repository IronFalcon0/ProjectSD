import java.net.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

public class UDPConnectionListener extends Thread{
    public static ArrayList<String> newFiles = new ArrayList<String>();
    private static final int timeout = 2000;

    public UDPConnectionListener () {
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
        byte buffer[] = new byte[Server.bufsize];

        DatagramSocket dsoc = null;
        try {
            dsoc = new DatagramSocket(Server.UDPFilesPortMain);
            dsoc.setSoTimeout(timeout);

            // receive package, fileName
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
            dsoc.receive(dp);
            String fileName = new String(dp.getData(), 0, dp.getLength(), StandardCharsets.UTF_8);
            System.out.println("fileName received: " + fileName);

            // convert file to byte array
            File fileSend = new File(Server.baseDirServer + fileName);
            byte[] fileContent = Files.readAllBytes(fileSend.toPath());

            byte intBuf[] = ByteBuffer.allocate(4).putInt(fileContent.length).array();


            // send fileSize
            DatagramPacket fileSizePacket = new DatagramPacket(intBuf, intBuf.length, dp.getAddress(), dp.getPort());
            System.out.println(Arrays.toString(fileSizePacket.getData()));
            dsoc.send(fileSizePacket);


            int counter = 0;
            int limit;
            int failedPackets = 0;
            System.out.println(fileContent.length);

            while (counter < fileContent.length) {
                try {

                    if (counter + Server.bufsize > fileContent.length)
                        limit = fileContent.length;
                    else
                        limit = counter + Server.bufsize;
                    DatagramPacket filePacket = new DatagramPacket(fileContent, counter, limit, dp.getAddress(), dp.getPort());
                    dsoc.send(filePacket);

                    // wait for confirmation with timeout, confirmation is 200, we can use the same datagramPacket from the fileSize
                    dsoc.setSoTimeout(timeout);
                    dsoc.receive(fileSizePacket);
                    System.out.println("confirmation received");

                    counter += Server.bufsize;
                    failedPackets = 0;

                } catch (SocketTimeoutException ste) {
                    System.out.println("File sync timeout: " + timeout + "ms");
                    failedPackets++;
                    if (failedPackets == 3) {
                        dsoc.close();
                        return;
                    }

                }
            }
            System.out.println("done sending");

            dsoc.close();

            // remove fileName from the newFiles arrayList
            newFiles.remove(fileName);


        } catch (SocketTimeoutException ste) {
            dsoc.close();
            System.out.println("File sync timeout: " + timeout + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


