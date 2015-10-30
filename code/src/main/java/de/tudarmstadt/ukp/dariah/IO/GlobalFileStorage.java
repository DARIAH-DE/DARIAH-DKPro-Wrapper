package de.tudarmstadt.ukp.dariah.IO;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;

import org.apache.tools.ant.DirectoryScanner;

/**
 * A global file storage for files that should be 
 * process in UIMA
 * @author Nils Reimers
 *
 */
public class GlobalFileStorage {
	private File lastPolledFile = null;
	
	private static GlobalFileStorage instance;
	
	private GlobalFileStorage () {}

	public static GlobalFileStorage getInstance () {
		if (GlobalFileStorage.instance == null) {
			GlobalFileStorage.instance = new GlobalFileStorage ();
		}
		return GlobalFileStorage.instance;
	}
	
	/**
	 * Reads a path and stores internally all file path
	 * @throws FileNotFoundException 
	 **/
	public void readFilePaths(String sourceLocation, String fileExtentsion) throws FileNotFoundException {
	
		if(sourceLocation.contains("*")) {
			int asterisk = sourceLocation.indexOf('*');
	       
            int separator = Math.max(
                    sourceLocation.lastIndexOf(File.separatorChar, asterisk),
                    sourceLocation.lastIndexOf('/', asterisk));
            
            String sourcePath;
            if(separator >= 0) {
            	sourcePath = sourceLocation.substring(0, separator+1);
            } else {
            	sourcePath = ".";
            }
            
            String pattern = sourceLocation.substring(separator+1);
	         
			
			DirectoryScanner scanner = new DirectoryScanner();
			scanner.setIncludes(new String[]{pattern});
			scanner.setBasedir(sourcePath);
			scanner.setCaseSensitive(false);
			scanner.scan();
			
			for(String file : scanner.getIncludedFiles()) {
				this.push(new File(sourcePath+file));
			}			
		} else {
		
			File inputPath = new File(sourceLocation);
			
			if(inputPath.isFile()) {
				this.push(inputPath);
			} else if(inputPath.isDirectory()) {
				
				File[] files = inputPath.listFiles();
				for (File file : files) {
					if (file.isFile() && (file.toString().endsWith(fileExtentsion))) {
						this.push(file);
					}
				}
			} else {
				throw new FileNotFoundException("Path "+sourceLocation+" does not point to a valid file or directory");
			}
		}
		
	}
	
	private LinkedList<File> files = new LinkedList<>();	
	public boolean isEmpty() {
		return files.isEmpty();		
	}
	
	/**
	 * Retrieves and removes the head (first element) of this list
	 */
	public File poll() {
		this.lastPolledFile = files.poll();
		return this.lastPolledFile;
	}
	
	public void push(File e)  {
		files.push(e);
		
	}
	
	public int size() {
		return files.size();
	}

	
	public File getLastPolledFile() {
		return this.lastPolledFile;
	}
	
	
}
