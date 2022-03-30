import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Server {
    private static int serverTCPPort;
    private static int serverTCPPort1;
    private static int serverTCPPort2;
    protected static int serverUDPPort = 5000;
    protected static String serverHost;
    protected static int UDPFilesPortMain = 1000;
    protected static final int bufsize = 1024;

    public static String bars = "/";
    private static String usersInfoStr;
    private static String baseDirConf = "Content_files" + bars + "conf_file";
    public static String baseDirServer;
    public static String baseDirServer1 = "Content_files" + bars + "Server" + bars;
    public static String baseDirServer2 = "Content_files" + bars + "Server2" + bars;
    public static volatile ArrayList<ClientInfo> clientsInfo = new ArrayList<ClientInfo>();


    public static void main(String args[]) throws InterruptedException {

        if (args.length != 1) {
            System.out.println("wrong syntax: java Server *folder (0|1)*");
            System.exit(0);
        }

        loadConfigurations();
        loadUserInfo(usersInfoStr);

        try {
            int folder = Integer.parseInt(args[0]);
            if (folder == 0) {
                baseDirServer = baseDirServer1;
                serverTCPPort = serverTCPPort1;

            } else {
                baseDirServer = baseDirServer2;
                serverTCPPort = serverTCPPort2;
            }

            System.out.println(serverHost + serverTCPPort);

        } catch (NumberFormatException e) {
            e.printStackTrace();
        }



        // init UDP threads
        // listen for hearthbeats
        //new UDPConnectionListener(serverUDPPort);
        Thread secondServer = new UDPHeartbeats();


        // wait to be main server to accept connections
        secondServer.join();



        try (ServerSocket listenSocketClient = new ServerSocket(serverTCPPort)) {
            while(true) {
                // creates a thread for each client
                Socket clientSocket = listenSocketClient.accept();
                new Connection(clientSocket);
            }

        } catch (IOException e) {
            System.out.println("Server listen for clients exception: " +e.getMessage());
        }
        System.exit(0);
    }

    // load vars from configuration file
    private static void loadConfigurations() {
        try {
            File configurationFile = new File(baseDirConf);
            Scanner scannerFile = new Scanner(configurationFile);

            String line = scannerFile.nextLine();
            String[] strParts = line.split(" ");
            serverHost = strParts[1];

            line = scannerFile.nextLine();
            String[] ucDriveServer = line.split(" ");
            serverTCPPort1 = Integer.parseInt(ucDriveServer[1]);

            line = scannerFile.nextLine();
            strParts = line.split(" ");
            serverTCPPort2 = Integer.parseInt(strParts[1]);

            line = scannerFile.nextLine();
            String[] usersPath = line.split(" ");
            usersInfoStr = usersPath[1];

            scannerFile.close();

        } catch (FileNotFoundException e) {
            System.out.println("Error loading usersInfo: " +e.getMessage());
        }
    }

    // load user info and last visited dirs from a file
    private static void loadUserInfo(String fileName) {
        try {
            File userInfoFile = new File(fileName);
            Scanner scannerFile = new Scanner(userInfoFile);

            while (scannerFile.hasNextLine()) {
                String line = scannerFile.nextLine();
                String[] user = line.split(" ");

                ClientInfo ci;
                if (user.length == 2) {
                    ci = new ClientInfo(user[0], user[1]);
                } else {
                    ci = new ClientInfo(user[0], user[1], user[2]);
                }
                //System.out.println(line);
                clientsInfo.add(ci);


                // verify if user folder already exists. If not, creates it
                Path path = Paths.get(baseDirServer1 + "Home");
                if (!Files.exists(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        System.out.println("IO: " + e.getMessage());
                    }
                }

                // do the same for the secondary server
                path = Paths.get(baseDirServer2 + "Home");
                if (!Files.exists(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        System.out.println("IO: " + e.getMessage());
                    }
                }
            }

            scannerFile.close();

        } catch (FileNotFoundException e) {
            System.out.println("Error loading usersInfo: " + e.getMessage());
            e.printStackTrace();
        }

    }

    // saves userInfo in a file, runs each time a client closes a connection
    public static void saveUserInfo(String userName) {
        try {
            FileWriter userInfoFile = new FileWriter(usersInfoStr);
            for (ClientInfo c: clientsInfo) {
                userInfoFile.write(c.name + " " + c.password + " " + c.directoryS + "\n");
            }
            userInfoFile.close();
        } catch (IOException e) {
            System.out.println("Error saving usersInfo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
