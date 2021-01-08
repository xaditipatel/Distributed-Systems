
// Submitted by Aditi Patel
// ID: 1001704419

//Source/reference from https://docs.oracle.com/javase/tutorial/essential/io/examples/WatchDir.java

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.util.*;

/**
 * Example to watch a directory (or tree) for changes to files.
 */

public class DirectoryWatcher implements Runnable{

	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private final boolean recursive;
	private boolean trace = false;
	private final String LD;
	
	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * Register the given directory with the WatchService
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir);
			} else {
				if (!dir.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir);

				}
			}
		}
		keys.put(key, dir);
	}

	/**
	 * Register the given directory, and all its sub-directories, with the
	 * WatchService.
	 */
	private void registerAll(final Path start) throws IOException {
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				register(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Creates a WatchService and registers the given directory
	 */
	DirectoryWatcher(Path dir, boolean recursive, String LD) throws IOException {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();
		this.recursive = recursive;
		this.LD = LD;

		if (recursive) {
			System.out.format("Scanning %s ...\n", dir);
			registerAll(dir);
			System.out.println("Done.");
		} else {
			register(dir);
		}

		// enable trace after initial registration
		this.trace = true;
	}

	public void clear() {
	    synchronized (keys) {
	      for (WatchKey key : keys.keySet()) {
	        key.cancel();

	      }
	      keys.clear();
	    }
	  }
	
	public void shutdown() throws IOException {
	    clear();
	    watcher.close();
	  }
	
	/**
	 * Process all events for keys queued to the watcher
	 */
	public void run() {
		for (;;) {

			// wait for key to be signalled
			WatchKey key;
			try {
				key = watcher.take();
			} catch (InterruptedException x) {
				return;
			}

			Path dir = keys.get(key);
			if (dir == null) {
				System.err.println("WatchKey not recognized!!");
				continue;
			}

			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();

				// Context for directory entry event is the file name of entry
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);

				// print out event
				System.out.format("%s: %s\n", event.kind().name(), child);

				// if directory is created, and watching recursively, then
				// register it and its sub-directories
				if (recursive && (kind == ENTRY_CREATE)) {
					try {
						String p = child.toString().split("Server")[1];
						DirectoryHelper.createDirectory(System.getProperty("user.dir") + "\\LocalSystem\\" + this.LD + p);

						if (Files.isDirectory(child, NOFOLLOW_LINKS)) {

							registerAll(child);
						}
					} catch (IOException x) {
						// ignore to keep sample readbale
					}
				} else if (recursive && (kind == ENTRY_DELETE)) {
					try {
						String p = child.toString().split("Server")[1];
						DirectoryHelper.deleteDirectory(System.getProperty("user.dir") + "\\LocalSystem\\" + this.LD + p);
						
						if (Files.isDirectory(child, NOFOLLOW_LINKS)) {

							registerAll(child);
						}
					} catch (IOException x) {
						// ignore to keep sample readbale
					}
				} else if (recursive && (kind == ENTRY_MODIFY)) {
					try {
						String sourceServer = child.toString().split("->")[0].split("Server")[1];
						String targetServer = child.toString().split("->")[1].split("Server")[1];

						DirectoryHelper.renameDirectory(System.getProperty("user.dir") + "\\LocalSystem\\" + this.LD+sourceServer, System.getProperty("user.dir") + "\\LocalSystem\\" + this.LD+targetServer);
						
						if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
							DirectoryHelper.copyDirectoryJavaNIO(child,
									Paths.get(System.getProperty("user.dir") + "\\LocalSystem\\" + this.LD));

							registerAll(child);
						}
					} catch (IOException x) {
						// ignore to keep sample readbale
					}
				}
			}

			// reset key and remove from set if directory no longer accessible
			boolean valid = key.reset();
			if (!valid) {
				keys.remove(key);

				// all directories are inaccessible
				if (keys.isEmpty()) {
					break;
				}
			}
		}
	}
}