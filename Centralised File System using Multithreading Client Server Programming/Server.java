// Submitted by Aditi Patel
// ID: 1001704419

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author DELL
 */
public class Server extends Thread {

    static Socket clientSocket; //declare the clientSocket to which the thread belongs

    //input and output streams from and to the client
    DataInputStream din;
    DataOutputStream dout;

    public HashMap<String, String> clientUsernames = new HashMap<>(); // A Map to store client names as key and usernames as value
    String msg_IN = "";
    String repeat = "Invalid Username. Please Enter username"; // String to store the message for username
    int count = 0;  // counter to give user maximum 3 chances to enter valid username
    boolean userValidation = true;     // flag to validate the username provided by client

    // Variable declaration for Client operations 
    String name;
    String username;
    String operationCode;
    String source, target;

    // A constructor to creat a client socket
    public Server(Socket clientSocket) {
        Server.clientSocket = clientSocket;
    }

    // Function to validate the username
    public static boolean validateUsername(String username) {
        // Regex to check valid username. 
        String regex = "^[aA-zZ]\\w{5,10}$";

        // Compile the ReGex 
        Pattern p = Pattern.compile(regex);

        // If the username is empty 
        // return false 
        if (username == null) {
            return false;
        }

        // Pattern class contains matcher() method 
        // to find matching between given username 
        // and regular expression. 
        Matcher m = p.matcher(username);

        // Return if the username 
        // matched the ReGex 
        return m.matches();
    }

    // Function to close the input stream, output stream and socket connection
    public static void closeConnection(DataInputStream din, DataOutputStream dout, Socket socket, String username) {
        try {
            ServerGUI.setTextArea("\n Closing this connection :\t" + socket);
            ServerGUI.deleteConnectedClientsUsername(username);

            if (dout != null) {
                dout.close();
            }
            if (din != null) {
                din.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(ServerGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // Function to check conflicting usernames corresponding to Client names
    public boolean checkUserNameExists(String name, String username) {
        // flag to checkc is username is already present
        boolean isUserNamePresent = clientUsernames.containsValue(username);

        // flag to check whether the existing username corresponds to same Client or not
        // if the username is registered for same client, then no conflict else conflicting
        boolean isNamePresent = false;
        if (isUserNamePresent) {
            isNamePresent = clientUsernames.containsKey(name);
        }
        return username.equals(clientUsernames.get(name));
    }

    @Override
    public void run() {
        try {

            ServerGUI.setTextArea("\n Client Connected  " + clientSocket);

            din = new DataInputStream(clientSocket.getInputStream());
            dout = new DataOutputStream(clientSocket.getOutputStream());

            while (true) {
                // Greeting message to client
                dout.writeUTF("Welcome " + clientSocket.getInetAddress().getHostName() + ":" + clientSocket.getPort());

                // Client Registeration on server by First name and username
                dout.writeUTF("Enter FirstName,Username to register");
                dout.writeUTF("**Username length must be of 6-10 characters long ");
                dout.writeUTF("**It should start with lower/upper case Alphabets ");

                // Reading the client input
                msg_IN = din.readUTF();

                // Splitting the input into Name and Username
                name = msg_IN.split(",")[0];
                username = msg_IN.split(",")[1];
                ServerGUI.setTextArea("\n Validating Username for client >>" + name);

                // Validating username maximum 3 times
                while (count <= 3 && !Server.validateUsername(username)) {
                    dout.writeUTF(repeat);
                    username = din.readUTF();
                    count++;
                    userValidation = false;
                }

                // If valid username is found creating the home directory
                if (count <= 3 && userValidation) {

                    // Creates an entry in hashmap only if client is not registered previously
                    if (checkUserNameExists(name, username) && !clientUsernames.containsKey(name)) {
                        clientUsernames.put(name, username);
                    }

                    // Creates the home directory is not exists
                    File userfile = new File(username);
                    if (userfile.mkdir()) {
                        ServerGUI.setTextArea("\n Home Directory:" + msg_IN + "created for " + clientSocket);
                        ServerGUI.setTextArea("\n Location:" + System.getProperty("user.dir"));

                        dout.writeUTF("HOME DIRECTORY NAME:" + msg_IN);
                        dout.writeUTF("PLEASE NOTE DOWN YOUR HOME DIRECTORY NAME");
                        dout.writeUTF("*******************************************");
                        dout.writeUTF("YOU ARE RESTRICTED FOR ANY OPERATION OUTSIDE THIS DIRECTORY");
                    }
                    ServerGUI.addConnectedClientsUsername(username);

                    // Client Operations message
                    dout.writeUTF("FILE SYSTEM MANAGEMENT");
                    dout.writeUTF("1. Create Directories");
                    dout.writeUTF("2. Delete Directories");
                    dout.writeUTF("3. Move Directories");
                    dout.writeUTF("4. Rename Directories");
                    dout.writeUTF("5. List contents of directories ");
                    dout.writeUTF("");

                    // Reading client operations command and performing actions accordingly
                    while (true) {
                        dout.writeUTF("Enter number corresponding to operation");
                        operationCode = din.readUTF();

                        // Switch case to read the command
                        switch (operationCode) {
                            case "1":
                                dout.writeUTF("Enter path to create starting with your home directory");
                                dout.writeUTF("EXAMPLE: Client1/ OR Client1/dir1 where Client1 is your home directory");
                                msg_IN = din.readUTF();

                                if (username.equals(msg_IN.split("/")[0])) {
                                    File op1 = new File(System.getProperty("user.dir") + "/" + msg_IN);
                                    if (op1.mkdir()) {
                                        ServerGUI.setTextArea("\n Directory:" + msg_IN + "created for " + username);
                                        dout.writeUTF("Created Successfully");
                                    }
                                } else {
                                    dout.writeUTF("Restricted");
                                }
                                break;
                            case "2":
                                dout.writeUTF("Enter path to delete");
                                dout.writeUTF("EXAMPLE: Client1/ OR Client1/dir1 where Client1 is your home directory");
                                msg_IN = din.readUTF();
                                if (username.equals(msg_IN.split("/")[0]) && msg_IN.split("/").length > 1) {
                                    Path rootPath = Paths.get(System.getProperty("user.dir") + "/" + msg_IN);
                                    try {
                                        Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS)
                                                .sorted(Comparator.reverseOrder())
                                                .map(Path::toFile)
                                                .peek(System.out::println)
                                                .forEach(File::delete);
                                        ServerGUI.setTextArea("\n Directory:" + msg_IN + "deleted for " + username);
                                        dout.writeUTF("Deleted successfully");
                                    } catch (IOException e) {
                                        dout.writeUTF("Error while deleting");
                                        e.printStackTrace();
                                    }
                                } else {
                                    dout.writeUTF("Restricted");
                                }
                                break;
                            case "3":
                                dout.writeUTF("Enter source and target dir to move separated by comma");
                                dout.writeUTF("EXAMPLE: Client1/dir1/subdir1,Client1/dir2/ where Client1 is your home directory");
                                msg_IN = din.readUTF();
                                source = msg_IN.split(",")[0];
                                target = msg_IN.split(",")[1];
                                if (username.equals(source.split("/")[0]) && username.equals(target.split("/")[0])) {
                                    Files.move(Paths.get(System.getProperty("user.dir") + "/" + source), Paths.get(System.getProperty("user.dir") + "/" + target), REPLACE_EXISTING);
                                } else {
                                    dout.writeUTF("Restricted");
                                }
                                break;
                            case "4":
                                dout.writeUTF("Enter source and target dir to rename separated by comma");
                                dout.writeUTF("EXAMPLE: Client1/dir1,Client1/dir2 where Client1 is your home directory");
                                msg_IN = din.readUTF();

                                source = msg_IN.split(",")[0];
                                target = msg_IN.split(",")[1];

                                if (username.equals(source.split("/")[0]) && username.equals(target.split("/")[0])) {
                                    File sourceFile = new File(System.getProperty("user.dir") + "/" + source);
                                    File destFile = new File(System.getProperty("user.dir") + "/" + target);

                                    if (sourceFile.renameTo(destFile)) {
                                        dout.writeUTF("File renamed successfully");
                                    } else {
                                        dout.writeUTF("Failed to rename file");
                                    }
                                } else {
                                    dout.writeUTF("Restricted");
                                }
                                break;
                            case "5":
                                dout.writeUTF("Enter dir name to list the contents");
                                dout.writeUTF("EXAMPLE: Client1 OR Client1/dir1 OR Client1/dir1/subdir1 where Client1 is your home directory");
                                msg_IN = din.readUTF();
                                if (username.equals(msg_IN.split("/")[0])) {
                                    File dir = new File(System.getProperty("user.dir") + "/" + msg_IN);
                                    String[] files = dir.list();
                                    dout.writeUTF("List::");
                                    for (String file : files) {
                                        dout.writeUTF(file);
                                    }
                                } else {
                                    dout.writeUTF("Restricted");
                                }
                                break;
                            default:
                                dout.writeUTF("Invalid Input");
                                break;
                        }
                    }
                } else {
                    dout.writeUTF("Not able to validate Username. Please reconnect");
                    dout.writeUTF("Closing the connetion");
                    Server.closeConnection(din, dout, clientSocket, username);
                    break;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            Server.closeConnection(din, dout, clientSocket, username);
        }
    }

}
