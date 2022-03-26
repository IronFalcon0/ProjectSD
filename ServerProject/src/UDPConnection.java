

public class UDPConnection extends Thread{
    private static int serverUDPPort;

    public UDPConnection (int port) {
        serverUDPPort = port;
        this.start();
    }


    // thread that listen for heartbeats
    public void run() {
        System.out.println("UDP thread");

        while(true) {

        }
    }

}


