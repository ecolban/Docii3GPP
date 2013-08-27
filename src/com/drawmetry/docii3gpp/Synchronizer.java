package com.drawmetry.docii3gpp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.drawmetry.docii3gpp.pagehandler.*;

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
	public static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	public Synchronizer(UI ui, URL hostUrl) {
		this.ui = ui;
		this.hostUrl = hostUrl;
	}

	@Override
	public void run() {
		LOGGER.log(Level.INFO, String.format("%s\n", STARTING_SYNC));
		try {
			PageHandler handler = PageHandlerFactory.getInstance(ui.getMeeting());
			BufferedReader input = null;
			if (hostUrl.getAuthority() == null) { // case where the input is a
													// local file
				File file = new File(hostUrl.toURI());
				input = new BufferedReader(new FileReader(file));
			} else { // case where the input is a file on an ftp server (e.g.
						// ftp.3gpp.org).
				URLConnection con = (URLConnection) hostUrl.openConnection();
				input = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
			}
			String line = null;
			while ((line = input.readLine()) != null) {
				handler.processLine(line);
			}

		} catch (SocketException ex) {
			LOGGER.log(Level.INFO, "{0}\n", SYNC_ABORTED);
		} catch (MalformedURLException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		} catch (URISyntaxException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
		} catch (PageHandlerFactoryException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
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

	public static void main(String[] args) {

	}

}
