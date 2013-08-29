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
 *  @author Erik Colban &copy; 2012 <br> All Rights Reserved Worldwide
 */
public class Initializer {

    private static final Logger LOGGER = Logger.getLogger("com.drawmetry.docii3gpp");

    public static void main(String[] args) {
        LOGGER.setLevel(Level.ALL);
        new Initializer().checkConfigurationSettings();
        Configuration.initialize();
        SwingUtilities.invokeLater(new UI());
    }

    private void checkConfigurationSettings() {
        File file = new File(System.getProperty("user.home"));
        assert file.exists();
        file = new File(file, ".dociimentor");
        boolean configDirExists = file.exists();
        if (!configDirExists) {
            configDirExists = file.mkdir();
        }
        if (!configDirExists) {
            assert false;
        }
        file = new File(file, "dociiconfig.xml");
        if(!file.exists()) {
        	LOGGER.log(Level.SEVERE, "dociiconfig.xml not found");
        }
        try {
            File logFile = new File(System.getProperty("user.home"), ".docii3gpp/logfile.txt");
            Handler logFileHandler = new StreamHandler(
                    new FileOutputStream(logFile), new SimpleFormatter());
            LOGGER.addHandler(logFileHandler);
        } catch (FileNotFoundException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
