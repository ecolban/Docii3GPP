package com.drawmetry.docii3gpp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

/**
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class Synchronizer implements Runnable {

	private final UI ui;
	private final URL hostUrl;
	private boolean abortFlag = false;
	private static final String STARTING_SYNC = "Starting sync";
	private static final String SYNC_ABORTED = "Sync aborted";
	private static final String SYNC_COMPLETE = "Syn complete";
	static final Logger LOGGER = Logger.getLogger("com.drawmetry.docii3gpp");
	
	public Synchronizer(UI ui, URL hostUrl) {
		this.ui = ui;
		this.hostUrl = hostUrl;
	}
	

	@Override
	public void run() {
		LOGGER.log(Level.INFO, String.format("%s\n", STARTING_SYNC));
		try {
//			S2PageHandler_1 handler = new S2PageHandler_1(ui);
			S2PageHandler_2 handler = new S2PageHandler_2(ui);
//			SPPageHandler_58 handler = new SPPageHandler_58(ui);
			//			R2PageHandler handler = new R2PageHandler(ui);
			URLConnection con = (URLConnection) hostUrl
					.openConnection();
			BufferedReader input = new BufferedReader(new InputStreamReader(
					con.getInputStream()));

			String line = null;
			while ((line = input.readLine()) != null) {
				handler.readLine(line);
			}

		} catch (SocketException ex) {
			LOGGER.log(Level.INFO, "{0}\n", SYNC_ABORTED);
		} catch (MalformedURLException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		if (!isAborted()) {
			LOGGER.log(Level.INFO, String.format("%s\n", SYNC_COMPLETE));
		} else {
			setAbort(false);
			LOGGER.log(Level.INFO, String.format("%s\n", SYNC_ABORTED));
		}
		releaseSynLock();
	}

	private synchronized boolean isAborted() {
		return abortFlag;
	}

	public synchronized void setAbort(boolean b) {
		abortFlag = b;
	}

	private void releaseSynLock() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				ui.setSyncLock(false);
			}
		});
	}
}
