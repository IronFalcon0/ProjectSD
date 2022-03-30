import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.ArrayList;
import java.io.*;

public class Client {
    private static int serverSocket;
    private static String host;
    private static String bars = "/";
    public static String shortClientDir = new String();
    public static String clientDir = new String();
    private static String baseDirConf = "Content_files" + bars + "conf_file";
    private static final int BLOCK_SIZE_FILE = 4096;
    private static String currentCommand = new String();
    private static ArrayList<String> loginInfo= new ArrayList<String>();



    public static void main(String args[]) {

        if (args.length != 4) {
            System.out.println("Wrong Syntax: java Client *mainServerIP* *mainServerPort* *secServerIP* *secServerPort*");
            return;
        } else {
            host = args[0];
            serverSocket = Integer.parseInt(args[1]);
        }

        for(int i = 0; i < 2; i++) {
            try (Socket s = new Socket(host, serverSocket)) {

                DataInputStream in = new DataInputStream(s.getInputStream());
                DataOutputStream out = new DataOutputStream(s.getOutputStream());


                while (!Login(in, out)) ;

                try (Scanner sc = new Scanner(System.in)) {
                    while (true) {
                        if(currentCommand.equals("")) {
                            System.out.printf(shortClientDir + ">");
                            currentCommand = sc.nextLine();
                        } else{
                            System.out.printf(shortClientDir + ">" + currentCommand + "\n");
                        }

                        if (currentCommand.equals("")) {
                            currentCommand = "";
                            continue;
                        }
                        // trim command
                        currentCommand = currentCommand.strip();

                        String[] commandParts = currentCommand.split(" ", 3);

                        switch (commandParts[0]) {
                            case "ls":                                                                  // list files in client|server dir
                                if (commandParts.length != 2) {
                                    System.out.println("wrong syntax: ls client|server");
                                    currentCommand = "";
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
                                    currentCommand = "";
                                    continue;
                                }

                                if (commandParts[1].equals("server")) {
                                    out.writeUTF(commandParts[0] + ":" + commandParts[1] + ":" + commandParts[2]);
                                    System.out.println(in.readUTF());

                                } else if (commandParts[1].equals("client")) {
                                    String resp = changeCurDir(currentCommand);

                                    if (!resp.equals("")) {
                                        System.out.println(resp);
                                    }
                                }
                                break;

                            case "get":                                                                 // get file from server
                                if (commandParts.length == 1) {
                                    System.out.println("wrong syntax: get *file_name*");
                                    currentCommand = "";
                                    continue;
                                }
                                if (commandParts.length == 3) {
                                    commandParts[1] = commandParts[1] + " " + commandParts[2];
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
                                    currentCommand = "";
                                    continue;
                                }
                                if (commandParts.length == 3) {
                                    commandParts[1] = commandParts[1] + " " + commandParts[2];
                                }

                                String fileName = clientDir + bars + commandParts[1];


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
                                    currentCommand = "";
                                    continue;
                                }
                                out.writeUTF(commandParts[0]);
                                out.writeUTF(commandParts[1]);
                                String respond = in.readUTF();
                                System.out.println(respond);
                                while (!Login(in, out)) ;
                                break;

                            default:
                                currentCommand = "";
                                System.out.println("Command not found");
                        }
                        currentCommand = "";
                    }
                }

            } catch (UnknownHostException e) {
                System.out.println("Sock:" + e.getMessage());
            } catch (EOFException e) {
                System.out.println("EOF:" + e.getMessage());
            } catch (IOException e) {
                host = args[2];
                serverSocket = Integer.parseInt(args[3]);
                if(i == 0)
                    System.out.println("Main server offline, trying to connect to secundary server...");
                else
                    System.out.println("Failed to connect to any of the servers");
                continue;
            }
        }
    }

    // changes the client dir
    private static String changeCurDir(String command) {
        try {
            String[] parts = command.split(" ", 3);
            if (!parts[0].equals("cd") || !parts[1].equals("client")) {
                return "wrong syntax: cd client *folder_name*";
            }

            if (parts.length != 3) {
                return "invalid path";
            }

            // go to parent folder
            if (parts[2].contains("..")) {
                Path path = Paths.get(clientDir).getParent();
                clientDir = path.toString();
                getShortDir();
                return "";
            }

            // go to a sub-folder
            Path path = Paths.get(clientDir + bars + parts[2]);
            if (Files.exists(path) && new File(path.toString()).isDirectory()) {
                clientDir += bars + parts[2];
                getShortDir();
                return "";
            }
            return "folder doesn't exist";
        } catch (Exception e) {
            //System.out.println(e.getMessage());
        }
        return "invalid path";
    }

    // list files and folder in current client dir
    private static String listFilesCurDir() {
        File[] files = new File(clientDir).listFiles();
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


    // login client
    private static boolean Login(DataInputStream in, DataOutputStream out) {
        Scanner sc = new Scanner(System.in);
        try {
            String userName = new String();
            String password = new String();

            if(loginInfo.isEmpty()) {
                System.out.printf("Login\nusername: ");
                userName = sc.nextLine();
                System.out.printf("password: ");
                password = sc.nextLine();


                if (userName.isEmpty()) userName = "";
                if (password.isEmpty()) password = "";
            } else{
                userName = loginInfo.get(0);
                password = loginInfo.get(1);
            }

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
                System.out.println("server:" + respond + ">");
                clientDir = System.getProperty("user.dir");
                loginInfo.clear();
                loginInfo.add(userName);
                loginInfo.add(password);

                getShortDir();
                return true;
            }

        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO:" + e.getMessage());
        }
        return false;
    }

    private static void getShortDir() {
        int count = 0;
        File tempFile = new File(clientDir);
        shortClientDir = tempFile.getName();

        while(count < 2) {
            String parentDir = tempFile.getParent();
            if (parentDir == null) {
                break;
            }
            tempFile = new File(parentDir);
            shortClientDir = tempFile.getName() + File.separatorChar + shortClientDir;
            count++;
        }
    }

    // receive file from server
    private static void getFile(String respond) {
        String[] strPort = respond.split(":");

        // connect with server by the port given
        try (Socket fileSocket = new Socket(host, Integer.parseInt(strPort[1]))){

            byte [] buffer  = new byte [BLOCK_SIZE_FILE];

            InputStream is = fileSocket.getInputStream();
            FileOutputStream fos = new FileOutputStream(clientDir + bars + strPort[2]);
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
            File fileSend = new File(clientDir + bars + strPort[2]);
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
