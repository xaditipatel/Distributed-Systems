
// Submitted by Aditi Patel
// ID: 1001704419

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.awt.List;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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

	static Socket clientSocket; // declare the clientSocket to which the thread belongs

	// input and output streams from and to the client
	DataInputStream din;
	DataOutputStream dout;

	public HashMap<String, String> clientUsernames = new HashMap<>(); // A Map to store client names as key and
																		// usernames as value

	String msg_IN = "";
	String repeat = "Invalid Username. Please Enter username"; // String to store the message for username
	boolean userValidation = false; // flag to validate the username provided by client
	boolean sync = false;

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
		String regex = "^[a-zA-Z]([a-zA-Z0-9]{5,10})$";

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
	public static void closeConnection(DataInputStream din, DataOutputStream dout, Socket socket) {
		try {
			ServerGUI.setTextArea("\n Closing the connection");

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

	// Function to check usernames corresponding to Client names
	public boolean checkUserNameExists(String username) {

		String result = clientUsernames.entrySet().stream().filter(e -> e.getValue().equals(username))
				.map(Map.Entry::getKey).findFirst().orElse(null);

		if (result == null)
			return true;
		else
			return false;
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

				for (int i = 0; i <= 3; i++) {
					if (Server.validateUsername(username)) {
						System.out.println("username validate if" + username);
						userValidation = true;
						break;
					} else {
						dout.writeUTF(repeat);
						username = din.readUTF();
						name = username.split(",")[0];
						username = username.split(",")[1];
						System.out.println("username validate else" + username);
					}
				}

				// If valid username is found creating the home directory
				if (userValidation) {

					ServerGUI.setTextArea("\n Successfully registered Username for client >>" + name);
					if (!ServerGUI.getConnectClientsusername(username))
						ServerGUI.addConnectedClientsUsername(username);
					else {
						dout.writeUTF("Conflicting usernames! Disconnecting");
						break;
					}
					
					// Creates an entry in hashmap only if client is not registered previously
					if (!clientUsernames.containsKey(name) && checkUserNameExists(username)) 
						clientUsernames.put(name, username);
					else {
						dout.writeUTF("Already registered with different Username");
						break;
					}

					System.out.println(clientUsernames);

					// Creates the home directory is not exists
					File userfile = new File(System.getProperty("user.dir") + "\\Server\\" + username);
					userfile.mkdirs();
					ServerGUI.setTextArea("\n Home Directory: " + username + " created for " + name);
					ServerGUI.setTextArea("\n Location:" + System.getProperty("user.dir") + "\\Server");

					dout.writeUTF("HOME DIRECTORY NAME:" + username);
					dout.writeUTF("PLEASE NOTE DOWN YOUR HOME DIRECTORY NAME");
					dout.writeUTF("*******************************************");
					dout.writeUTF("YOU ARE RESTRICTED FOR ANY OPERATION OUTSIDE THIS DIRECTORY");

					dout.writeUTF("Do you want to enable Synchronization? (Enter Y or N)");
					if (din.readUTF().equals("Y")) {
						// getting available identifier for the client
						System.out.println("1");
						Character _LD_ = ServerGUI.getAvailableIdentifier(ServerGUI.identifiersMap);
						System.out.println("2");

						// setting available identifier for the client
						ServerGUI.setIdentifiersMap(_LD_, ServerGUI.identifiersMap, username);

						// create a local home directories
						File localDir = new File(System.getProperty("user.dir") + "\\LocalSystem\\" + _LD_.toString());
						localDir.mkdir();
						System.out.println("3");


						dout.writeUTF("List of home directories available for Synchronization:");
						dout.writeUTF(Arrays.toString(DirectoryHelper.listHomeDirs().toArray()));
						System.out.println("4");

						dout.writeUTF("Enter home directories name separated by a comma to sync");
						String[] syncFiles = din.readUTF().split(",");
						
						
						for (int i = 0; i < syncFiles.length; i++) {
							dout.writeUTF("Enter the Shutdown/Stop Time(in minutes) for Synchronization of: " + syncFiles[i] );
							long timeout = Long.parseLong(din.readUTF());
							try {
								DirectoryHelper.copyDirectoryJavaNIO(
										Paths.get(System.getProperty("user.dir") + "\\Server\\" + syncFiles[i]),
										Paths.get(System.getProperty("user.dir") + "\\LocalSystem\\" + _LD_.toString()
												+ "\\" + syncFiles[i]));

							} catch (IOException e) {
								e.printStackTrace();
							}

							// register directory and process its events
							Path dir = Paths.get(System.getProperty("user.dir") + "\\Server\\" + syncFiles[i]);
							DirectoryWatcher watcher = new DirectoryWatcher(dir,true, _LD_.toString());
							Thread t = new Thread(watcher);
							t.start();
							
						
						}

					} else if (din.readUTF().equals("N")) 
						dout.writeUTF("Synchronization is disabled currently");

					// Client Operations message
					dout.writeUTF("FILE SYSTEM MANAGEMENT");
					dout.writeUTF("1. Create Directories");
					dout.writeUTF("2. Delete Directories");
					dout.writeUTF("3. Move Directories");
					dout.writeUTF("4. Rename Directories");
					dout.writeUTF("5. List contents of directories ");
					dout.writeUTF("6. Undo Logged Operations ");

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
								if (DirectoryHelper.createDirectory(System.getProperty("user.dir") + "\\Server\\" + msg_IN)) {
									ServerGUI.setTextArea("\n Directory:" + msg_IN + "created for " + username);
									dout.writeUTF("Created Successfully");
					                ServerGUI.logger.log(Level.INFO, username+"::"+"CREATE"+"::"+msg_IN);
									ServerGUI.populateLog("logger.log");
								} else
									dout.writeUTF("Directory with this name already present");
							} else
								dout.writeUTF("Restricted");
							break;
						case "2":
							dout.writeUTF("Enter path to delete");
							dout.writeUTF("EXAMPLE: Client1/ OR Client1/dir1 where Client1 is your home directory");
							msg_IN = din.readUTF();
							if (username.equals(msg_IN.split("/")[0]) && msg_IN.split("/").length > 1) {
								java.util.List<Path> deletedDirPath = DirectoryHelper.deleteDirectory(System.getProperty("user.dir") + "\\Server\\" + msg_IN);
								if(deletedDirPath != null) {
									dout.writeUTF("Successfully Deleted");
									for(Path p : deletedDirPath) {
										msg_IN = msg_IN+","+p.toString();
									}
									ServerGUI.logger.log(Level.INFO, username+"::"+"DELETE"+"::"+msg_IN);
									
									ServerGUI.populateLog("logger.log");
								}
								else {
									dout.writeUTF("Failed to Delete");
								}
							}
							else
								dout.writeUTF("Restricted");
							break;
						case "3":
							dout.writeUTF("Enter source and target dir to move separated by comma");
							dout.writeUTF(
									"EXAMPLE: Client1/dir1/subdir1,Client1/dir2/ where Client1 is your home directory");

							msg_IN = din.readUTF();
							source = msg_IN.split(",")[0];
							target = msg_IN.split(",")[1];

							if (username.equals(source.split("/")[0]) && username.equals(target.split("/")[0])) {
								DirectoryHelper.moveDirectory(source, target);
								ServerGUI.logger.log(Level.INFO, username+"::"+"MOVE"+"::"+source+"::"+target);
								ServerGUI.populateLog("logger.log");
								}
							else
								dout.writeUTF("Restricted");
							break;
						case "4":
							dout.writeUTF("Enter source and target dir to rename separated by comma");
							dout.writeUTF("EXAMPLE: Client1/dir1,Client1/dir2 where Client1 is your home directory");

							msg_IN = din.readUTF();
							source = msg_IN.split(",")[0];
							target = msg_IN.split(",")[1];

							if (username.equals(source.split("/")[0]) && username.equals(target.split("/")[0])) {
								dout.writeUTF(DirectoryHelper.renameDirectory(System.getProperty("user.dir") + "\\Server\\" + source, System.getProperty("user.dir") + "\\Server\\" + target));
								ServerGUI.logger.log(Level.INFO, username+"::"+"RENAME"+"::"+source+"::"+target);
								ServerGUI.populateLog("logger.log");
							}
							else
								dout.writeUTF("Restricted");

							break;
						case "5":
							dout.writeUTF("Enter dir name to list the contents");
							dout.writeUTF(
									"EXAMPLE: Client1/ OR Client1/dir1 OR Client1/dir1/subdir1 where Client1 is your home directory");

							msg_IN = din.readUTF();

							if (username.equals(msg_IN.split("/")[0])) {
								if(DirectoryHelper.listFilesDirs(msg_IN) != null)
									dout.writeUTF(Arrays.toString(DirectoryHelper.listFilesDirs(msg_IN).toArray()));
								else
									dout.writeUTF("No sub directories present");
							}
							else
								dout.writeUTF("Restricted");
							break;
						case "6":
							dout.writeUTF("Logged Commands:");
							File logFile = new File(System.getProperty("user.dir")+"\\logger.log");
							String line;
						    FileReader reader;
						    try {
								reader = new FileReader(logFile);
							    BufferedReader br = new BufferedReader(reader);
							    while ((line = br.readLine()) != null) {
									if(line.split("::")[2].contains(username)) {
										dout.writeUTF(line.split("::",4)[3]);
									}
							    }
							} catch (FileNotFoundException e) {
								e.getMessage();
								e.printStackTrace();
							}catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							dout.writeUTF("Enter a Command to Undo:");
						    msg_IN = din.readUTF();
						    String command = msg_IN.split("::")[0];
						    if(command.equals("CREATE"))
							    dout.writeUTF(DirectoryHelper.undoLoggedCreateCommand(msg_IN.split("::")[1], username));
						    else if(command.equals("RENAME")) {
						    	DirectoryHelper.undoLoggedRenameCommand(msg_IN.split("::")[1], msg_IN.split("::")[2], username);
						    }
						    else if(command.equals("DELETE")) {
						    	DirectoryHelper.undoLoggedDeleteCommand(msg_IN.split("::")[1], username);
						    }
						    else if(command.equals("MOVE")) {
						    	DirectoryHelper.undoLoggedMoveCommand(msg_IN.split("::")[1], msg_IN.split("::")[2], username);
						    }
						    else {
						    	dout.writeUTF("Invalid Command");
						    	break;
						    }
						    break;
						default:
							dout.writeUTF("Invalid Input");
							break;
						}
					}
				} else {
					dout.writeUTF("Not able to validate Username. Please reconnect");
					break;
				}
			}
		} catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			ServerGUI.deleteConnectedClientsUsername(username);
			ServerGUI.resetIdentifiersMap(ServerGUI.identifiersMap, username);
			ServerGUI.aliveClientsNum--;
			Server.closeConnection(din, dout, clientSocket);
			if (name != null && username != null)
				ServerGUI.setTextArea("\n Closed connection for " + name + " " + "with username " + username);
			else
				ServerGUI.setTextArea("\n Closed connection for " + clientSocket);

		}
	}

}
