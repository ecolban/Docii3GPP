package com.drawmetry.docii3gpp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik Colban &copy; 2012 <br>
 *         All Rights Reserved Worldwide
 */
public class Initializer {

	private static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");
	
	private File configFile;

	public static void main(String[] args) {
		LOGGER.setLevel(Level.ALL);
		Initializer initializer = new Initializer();
		if (initializer.checkConfigurationSettings()) {
			Configuration.read(initializer.configFile);
			SwingUtilities.invokeLater(new UI());
		}
	}

	private boolean checkConfigurationSettings() {
		File homeDir = new File(System.getProperty("user.home"));
		assert homeDir.exists();
		// File docii3gppHome = new File(homeDir, ".docii3gpp");
		// boolean configDirExists = docii3gppHome.exists();
		// if (!configDirExists) {
		// configDirExists = docii3gppHome.mkdir();
		// }
		// if (!configDirExists) {
		// assert false;
		// }
		configFile = new File(homeDir, ".docii3gpp/dociiconfig.xml");
		if (!configFile.exists()) {
			LOGGER.log(Level.SEVERE, homeDir + "/.docii3gpp/dociiconfig.xml not found");
			return false;
		}
		try {
			File logFile = new File(homeDir, ".docii3gpp/logfile.txt");
			Handler logFileHandler = new StreamHandler(new FileOutputStream(
					logFile), new SimpleFormatter());
			LOGGER.addHandler(logFileHandler);
		} catch (FileNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			return false;
		}
		return true;
	}
}
