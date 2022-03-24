import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.io.*;

public class Client {
    private static int serverSocket;
    private static String host;
    private static String bars = "\\\\";
    public static String currentDir = new String();
    private static String baseDirConf = "Content_files" + bars + "conf_file";
    private static String baseDirClientInit = "Content_files" + bars + "Client" + bars;
    public static String baseDirClient = baseDirClientInit;
    private static final int BLOCK_SIZE_FILE = 8192;


    public static void main(String args[]) {
        //loadConfigurations();

        if (args.length != 2) {
            System.out.println("wrong args given");
            System.out.println("java Client *serverIP* *serverPort*");
            return;
        } else {
            host = args[0];
            serverSocket = Integer.parseInt(args[1]);
        }

        try (Socket s = new Socket(host, serverSocket)) {

            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());


            while(!Login(in, out));


            try(Scanner sc = new Scanner(System.in)) {
                while (true) {
                    System.out.printf(currentDir + ">");
                    String command = sc.nextLine();

                    if (command.equals("")) {
                        continue;
                    }
                    // trim command
                    command = command.strip();

                    String[] commandParts = command.split(" ", 3);

                    switch (commandParts[0]) {
                        case "ls":                                                                  // list files in client|server dir
                            if (commandParts.length != 2) {
                                System.out.println("wrong syntax: ls client|server");
                                continue;
                            }

                            if (commandParts[1].equals("server")) {
                                out.writeUTF(commandParts[0] + ":" + commandParts[1]);
                                System.out.println(in.readUTF());

                            } else if (commandParts[1].equals("client")) {
                                System.out.println(listFilesCurDir());
                            }
                            break;

                        case "cd":                                                              // changes client|server dir
                            if (commandParts.length != 3) {
                                System.out.println("wrong syntax: cd client|server *new_dir*");
                                continue;
                            }

                            if (commandParts[1].equals("server")) {
                                out.writeUTF(commandParts[0] + ":" + commandParts[1] + ":" + commandParts[2]);
                                System.out.println(in.readUTF());

                            } else if (commandParts[1].equals("client")) {
                                String resp = changeCurDir(command);
                                if (resp.contains("Home")) {
                                    resp = resp.substring(resp.indexOf("Home"), resp.length());
                                    out.writeUTF("CLIENT_DIR_UPD:" + resp);
                                } else {
                                    System.out.println(resp);
                                }
                            }
                            break;

                        case "get":                                                                 // get file from server
                            if (commandParts.length == 1) {
                                System.out.println("wrong syntax: get *file_name*");
                                continue;
                            }
                            if (commandParts.length == 3) {
                                commandParts[1] = commandParts[1] + " " +commandParts[2];
                            }

                            out.writeUTF("GET:" + commandParts[1]);
                            String resp = in.readUTF();
                            if (resp.contains("CLIENT_CONNECT_GET")) {
                                getFile(resp);
                            } else {
                                System.out.println(resp);
                            }
                            break;

                        case "send":                                                            // send file to server
                            if (commandParts.length == 1) {
                                System.out.println("wrong syntax: send *file_name*");
                                continue;
                            }
                            if (commandParts.length == 3) {
                                commandParts[1] = commandParts[1] + " " +commandParts[2];
                            }

                            String fileName = baseDirClient + currentDir + bars + commandParts[1];


                            File file = new File(fileName);
                            if (file.exists() && file.isFile()) {
                                out.writeUTF("SEND:" + commandParts[1]);
                                String res = in.readUTF();
                                if (res.contains("CLIENT_CONNECT_SEND")) {
                                    sendFile(res);
                                }

                            } else {
                                System.out.println("File not found");
                            }
                            break;

                        case "exit()":                                                        // client closes connection, server saves client current dir
                            out.writeUTF("CLOSE_CONNECTION");
                            System.exit(0);
                            break;

                        case "cp":
                            if (commandParts.length != 2) {
                                System.out.println("wrong syntax: cp *new_password*");
                                continue;
                            }
                            out.writeUTF(commandParts[0]);
                            out.writeUTF(commandParts[1]);
                            String respond = in.readUTF();
                            System.out.println(respond);
                            while(!Login(in, out));
                            break;
                    }
                }
            }

        } catch (UnknownHostException e) {
            System.out.println("Sock:" + e.getMessage());
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }
    }

    // changes the client dir
    private static String changeCurDir(String command) {
        try {
            String[] parts = command.split(" ", 3);
            if (!parts[0].equals("cd") || !parts[1].equals("client")) {
                return "wrong syntax: cd client *folder_name*";
            }
            String newDir = command.substring(command.indexOf("client") + 7, command.length());

            if (newDir.equals("")) {
                return "invalid path";
            }

            // go to parent folder
            if (newDir.contains("..")) {
                // verify if user already in base dir
                if (currentDir.equals("Home")) {
                    return currentDir;
                } else {
                    Path path = Paths.get(currentDir).getParent();
                    currentDir = path.toString();
                    return path.toString();
                }
            }

            // go to a sub-folder
            Path path = Paths.get(baseDirClient + currentDir + bars + newDir);
            if (Files.exists(path) && new File(path.toString()).isDirectory()) {
                currentDir = currentDir + bars + newDir;
                return path.toString();
            }
            return "folder doesn't exist";
        } catch (Exception e) {
            //System.out.println(e.getMessage());
        }
        return "invalid path";
    }

    // list files and folder in current client dir
    private static String listFilesCurDir() {
        File[] files = new File(baseDirClient + currentDir).listFiles();
        String str = "";
        if (files == null) {
            return str;
        }

        for (File file : files) {
            if (file.getName().contains(" ")) {
                str += "\"" + file.getName() + "\"  ";
            } else {
                str += file.getName() + "  ";
            }

        }
        return str;
    }


    // load vars from configuration file
    private static void loadConfigurations() {
        try {
            File configurationFile = new File(baseDirConf);
            Scanner scannerFile = new Scanner(configurationFile);
            scannerFile.reset();

            String line = scannerFile.nextLine();
            String[] hostInfo = line.split(" ");
            host = hostInfo[1];

            line = scannerFile.nextLine();
            String[] ucDriveServer = line.split(" ");
            serverSocket = Integer.parseInt(ucDriveServer[1]);

            scannerFile.close();

        } catch (FileNotFoundException e) {
            System.out.println("Error loading usersInfo: " +e.getMessage());
        }
    }


    // login client
    private static boolean Login(DataInputStream in, DataOutputStream out) {
        Scanner sc = new Scanner(System.in);
        try {
            System.out.printf("Login\nusername: ");
            String userName = sc.nextLine();
            System.out.printf("password: ");
            String password = sc.nextLine();


            if (userName.isEmpty()) userName = "";
            if (password.isEmpty()) password = "";


            //String loginStr = "LOGIN:" + userName + ":" + password;
            out.writeUTF("LOGIN");
            in.readUTF();

            // send username
            out.writeUTF(userName);
            // receive login respond to username and ignores it
            in.readUTF();

            // send password
            out.writeUTF(password);
            // receive login respond to username and ignores it
            String respond = in.readUTF();


            if (respond.equals("NOT_VALID")) {
                System.out.println("Login failed");
                return false;
            } else {
                System.out.println("Login with success");
                baseDirClient += userName + bars;

                VerifyFolder(userName);
                currentDir = respond;
                return true;
            }

        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }
        return false;
    }

    // verify if client side folder is initialized
    private static void VerifyFolder(String username) {
        Path path = Paths.get(baseDirClientInit + username);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
            }
        }

        //verify if user folder already exists. If not, creates it
        path = Paths.get(baseDirClientInit + username + bars + "Home");
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                System.out.println("IO: " + e.getMessage());
            }
        }
    }

    // receive file from server
    private static void getFile(String respond) {
        String[] strPort = respond.split(":");

        // connect with server by the port given
        try (Socket fileSocket = new Socket(host, Integer.parseInt(strPort[1]))){

            byte [] buffer  = new byte [BLOCK_SIZE_FILE];

            InputStream is = fileSocket.getInputStream();
            FileOutputStream fos = new FileOutputStream(Client.baseDirClient + Client.currentDir + bars + strPort[2]);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            // write file
            int bytesRead = 0;
            while (bytesRead != -1) {
                bos.write(buffer, 0, bytesRead);
                bos.flush();
                bytesRead = is.read(buffer, 0, buffer.length);
            }

            // close streams
            fos.close();
            bos.close();

            System.out.println("File Received");

        } catch(IOException e) {
            System.out.println("Receive File Connection:" + e.getMessage());
        }
    }

    // send file to server
    private static void sendFile(String respond) {
        String[] strPort = respond.split(":");

        // connect with server by the port given
        try (Socket fileSocket = new Socket(host, Integer.parseInt(strPort[1]))){

            // open file and convert it to bin array
            File fileSend = new File(Client.baseDirClient + Client.currentDir + bars + strPort[2]);
            byte[] mybytearray = new byte[(int) fileSend.length()];

            FileInputStream fis = new FileInputStream(fileSend);
            BufferedInputStream bis = new BufferedInputStream(fis);
            bis.read(mybytearray,0,mybytearray.length);

            OutputStream output = fileSocket.getOutputStream();
            DataOutputStream dout = new DataOutputStream(output);

            // send file
            OutputStream os = fileSocket.getOutputStream();
            os.write(mybytearray,0,mybytearray.length);
            os.flush();


            // close stream
            bis.close();
            os.close();

            System.out.println("File Sent");

        } catch(IOException e) {
            System.out.println("Send File Connection:" + e.getMessage());
        }

    }

}
