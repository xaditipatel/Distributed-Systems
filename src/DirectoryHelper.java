
// Submitted by Aditi Patel
// ID: 1001704419

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DirectoryHelper {


	/**
	 * Given Path, Creates a Directory and returns boolean 
	 */	
	public static boolean createDirectory(String in) {
		File op1 = new File(in);
		if (op1.mkdir())
			return true;
		else
			return false;
	}

	/**
	 *  Given path, Deletes a Directory and returns status message 
	 */	
	public static List<Path> deleteDirectory(String in) throws IOException {
		Path rootPath = Paths.get(in);
		try {
			//Files.walk(rootPath, FileVisitOption.FOLLOW_LINKS).sorted(Comparator.reverseOrder()).map(Path::toFile)
			//	.peek(System.out::println).forEach(File::delete);
			final List<Path> pathsToDelete = Files.walk(rootPath).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
			for(Path path : pathsToDelete) {
				Files.deleteIfExists(path);
			}
			ServerGUI.setTextArea("\n Directory:" + in + "deleted");
			return pathsToDelete;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 *  Given source and target, moves a Directory
	 */
	public static void moveDirectory(String source, String target) throws IOException {
		Files.move(Paths.get(System.getProperty("user.dir") + "\\Server\\" + source),
				Paths.get(System.getProperty("user.dir") + "\\Server\\" + target), REPLACE_EXISTING);
	}

	/**
	 *  Given path, Renames a Directory and returns status message 
	 */
	public static String renameDirectory(String source, String target) {
		File sourceFile = new File(source);
		File destFile = new File(target);

		if (sourceFile.renameTo(destFile)) {
			return "File renamed successfully";
		} else {
			return "Failed to rename file";
		}
	}

	/**
	 *  Given path, creates a List of a Directories/SubDirectories under that path and returns List
	 */
	public static ArrayList<String> listFilesDirs(String path) throws IOException {
		ArrayList<String> listOfDir = new ArrayList<String>();
		File dir = new File(System.getProperty("user.dir") + "\\Server\\" + path);
		File[] files = dir.listFiles();

		if(files == null) return null;
		for (File file : files) {
			if (file.isFile())
				listOfDir.add(file.getName());
			else if (file.isDirectory()) {
				listOfDir.add(file.getName());
				listFilesDirs(file.getAbsolutePath());
			}
		}
		return listOfDir;
	}

	/**
	 *  Given path, creates a List of a Directories under that path and returns List
	 */
	public static ArrayList<String> listHomeDirs() throws IOException {
		File dir = new File(System.getProperty("user.dir") + "\\Server\\");
		ArrayList<String> listOfHomeDir = new ArrayList<String>();

		String[] files = dir.list();
		for (String file : files)
			listOfHomeDir.add(file);
		return listOfHomeDir;
	}

	/**
	 *  Given source and target, copies a directory recursively
	 */
	public static void copyDirectoryJavaNIO(Path source, Path target) throws IOException {

		// is this a directory?
		if (Files.isDirectory(source)) {

			// if target directory exist?
			if (Files.notExists(target)) {
				// create directory
				Files.createDirectories(target);
			}

			// list all files or folders from the source, returns a stream
			try (Stream<Path> paths = Files.list(source)) {

				// recursive loop for subdirectories
				paths.forEach(p -> copyDirectoryJavaNIOWrapper(p, target.resolve(source.relativize(p))));

			}

		} else {
			// if file exists, replace it
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 *  method to copy directory
	 */
	public static void copyDirectoryJavaNIOWrapper(Path source, Path target) {

		try {
			copyDirectoryJavaNIO(source, target);
		} catch (IOException e) {
			System.err.println("IO errors : " + e.getMessage());
		}

	}

	/**
	 *  method to check directory empty or not
	 */
	public static boolean isEmpty(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			System.out.println("is dir");
			try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
				return !directory.iterator().hasNext();
			}
		}
		return false;
	}

	/**
	 *  method to remove logged commands from the log file
	 */
	public static void removeLoggedCommands(String username, String command, String operation) throws IOException {
		File logFile = new File(System.getProperty("user.dir")+"\\logger.log");
		File tempLogFile = new File(System.getProperty("user.dir")+"\\logger_temp.log");
		
		// PrintWriter object for temp log file 
		PrintWriter pw = new PrintWriter(new FileWriter(tempLogFile)); 
		
		// BufferedReader object for logger.log
		BufferedReader br1 = new BufferedReader(new FileReader(logFile)); 
		String line = br1.readLine();
		String line1 = line.split("::", 3)[2]; 
		command = command.replace("\\", "/");

		 FileOutputStream fileOut = new FileOutputStream(System.getProperty("user.dir")+"\\logger.log");
		// loop for each line of logger.log 
		while(line1 != null) 
		{ 
			boolean flag = false; 
			String line2 = username+"::"+operation+"::"+command; 
			
				if(line1.equals(line2)) 
				{ 
					flag = true; 
					line = line.replace(line1, line1 + "UNDO DONE");
					fileOut.write(line.getBytes());
					break; 
				} 	

			// if flag = false 
			if(!flag) 
				pw.println(line1); 

			line1 = br1.readLine().split("::", 3)[2]; 

		} 

		pw.flush();        
        
        fileOut.close();
        
	      if (!logFile.delete()) {
	        System.out.println("Could not delete file");
	      }

	      //Rename the new file to the filename the original file had.
	      if (!tempLogFile.renameTo(logFile))
	        System.out.println("Could not rename file");		
		// closing resources 
		br1.close(); 
		pw.close(); 
	}

	/**
	 *  method to undo operation for CREATE Command
	 */
	public static String undoLoggedCreateCommand(String in, String username) throws IOException {
		System.out.println(Paths.get(System.getProperty("user.dir") + "\\Server\\" +in));

		if(!new File(System.getProperty("user.dir") + "\\Server\\" +in).exists()) return "File doesn't exist";
		
		java.util.List<Path> deletedDirPath = deleteDirectory(System.getProperty("user.dir") + "\\Server\\" +in);
		if(deletedDirPath != null) {
			for(Path p : deletedDirPath) {
				removeLoggedCommands(username, p.toString().split("Server\\\\")[1], "CREATE");
			}			
		}
		
		ServerGUI.populateLog("logger.log");
		return "Operation undo Successful";
	}

	/**
	 *  method to undo operation for RENAME Command
	 */
	public static void undoLoggedRenameCommand(String source,String target, String username) throws IOException {
		renameDirectory(target, source);
		removeLoggedCommands(username,source+"::"+target,"CREATE");
		ServerGUI.populateLog("logger.log");
	}

	/**
	 *  method to undo operation for DELETE Command
	 */
	public static void undoLoggedDeleteCommand(String in, String username) throws IOException {

		File logFile = new File(System.getProperty("user.dir")+"\\logger.log");
		String line;
	    FileReader reader;
	    String[] filesToBeCreated = null;
	    try {
			reader = new FileReader(logFile);
		    BufferedReader br = new BufferedReader(reader);
		    while ((line = br.readLine()) != null) {
				if(line.contains("DELETE"+"::"+in)) {
					filesToBeCreated = line.split(",",2)[1].split(",");
				}
		    }
		    for(int i = filesToBeCreated.length; i>=0; i--) {
		    	createDirectory(filesToBeCreated[i]);
		    }
		} catch (FileNotFoundException e) {
			e.getMessage();
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    removeLoggedCommands(username,in,"DELETE");
		ServerGUI.populateLog("logger.log");
	}

	/**
	 *  method to undo operation for MOVE Command
	 */
	public static void undoLoggedMoveCommand(String source,String target, String username) throws IOException {
		moveDirectory(target, source);
		removeLoggedCommands(username,source+"::"+target,"CREATE");
		ServerGUI.populateLog("logger.log");
	}

}