package com.drawmetry.docii3gpp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Instances of this class are used to download all missing files in the
 * documentList component {@link UI#documentList}. It implements a Runnable,
 * which runs in a separate thread without freezing the user interface.
 * 
 * @author Erik Colban &copy; 2012 <br>
 *         All Rights Reserved Worldwide
 */
public class Downloader implements Runnable {

	private static final String FTP_USERNAME = "anonymous";
	private static final String FTP_PASSWORD = "";
	private static final String DOWNLOAD_ABORTED = "Download aborted.";
	private static final String STARTING_DOWNLOAD = "Starting download";
	private static final String DOWNLOAD_COMPLETE = "Download complete";
	private final UI gui;
	private boolean abortFlag = false;

	public Downloader(UI gui) {
		this.gui = gui;
	}

	@Override
	public void run() {
		UI.LOGGER.log(Level.INFO, String.format("%s\n", STARTING_DOWNLOAD));

		String meeting = gui.getMeeting();
		File localDir = Configuration.getLocalDirectory(meeting);
		String hostname = Configuration.getHost();
		// String inboxDir = Configuration.getRemoteDirectoryAlt(meeting);
		if (!localDir.exists()) {
			localDir.mkdirs();
		}
		FTPClient client = new FTPClient();

		try {
			client.connect(hostname);
			int reply = client.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply)) {
				UI.LOGGER.log(Level.INFO, "FTP server refused connection.");
				return;
			}

			client.login(FTP_USERNAME, FTP_PASSWORD);
			client.enterLocalPassiveMode();
			client.setFileType(FTP.BINARY_FILE_TYPE);
			String remoteDir = Configuration.getRemoteDirectory(meeting);
			UI.LOGGER.log(Level.INFO, "From Docs folder:\n");
			downloadFiles(client, localDir, remoteDir);
			remoteDir = Configuration.getRemoteDirectoryAlt(meeting);
			if (remoteDir != null) {
				UI.LOGGER.log(Level.INFO, "From Inbox folder:\n");
				downloadFiles(client, localDir, remoteDir);
			}
			client.logout();
		} catch (SocketException e1) {
			UI.LOGGER.log(Level.INFO, e1.getMessage());
		} catch (IOException e1) {
			UI.LOGGER.log(Level.INFO, e1.getMessage());
		}

		if (!isAborted()) {
			UI.LOGGER.log(Level.INFO, "{0}\n", DOWNLOAD_COMPLETE);
		} else {
			setAbort(false);
			UI.LOGGER.log(Level.INFO, "{0}\n", DOWNLOAD_ABORTED);
		}
		releaseSynLock();
	}

	private void downloadFiles(FTPClient client, File localDir, String remoteDir)
			throws IOException {
		FTPFile[] files = client.listFiles(remoteDir);
		List<FTPFile> missingFiles = new ArrayList<FTPFile>();
		int count = 0;
		for (FTPFile remoteFile : files) {
			if (remoteFile.isFile()) {
				File localFile = new File(localDir, remoteFile.getName());
				if (!localFile.exists()) {
					count++;
					missingFiles.add(remoteFile);
				}
			}
		}
		for (FTPFile remoteFile : missingFiles) {
			if (isAborted()) {
				break;
			}
			if (remoteFile.isFile()) {
				File localFile = new File(localDir, remoteFile.getName());
				UI.LOGGER.log(Level.INFO, "[{0}]", count);
				downloadFile(client, localFile, remoteFile, remoteDir);
				count--;
				gui.repaint();
			}
		}
	}

	/**
	 * 
	 * @param client
	 *            the FTPClient used to download the file.
	 * @param localFile
	 *            the local file
	 * @param remoteFile
	 *            the FTPFile to download
	 * @param remoteDir
	 *            the remote working directory
	 * @throws IOException
	 */
	private void downloadFile(FTPClient client, File localFile,
			FTPFile remoteFile, String remoteDir) {
		assert client.isConnected();
		assert localFile != null;
		assert !localFile.exists();
		InputStream inputStream = null;
		BufferedOutputStream outputStream = null;
		long contentLength = remoteFile.getSize();
		try {
			inputStream = client.retrieveFileStream(remoteDir + "/"
					+ remoteFile.getName());
			if (inputStream == null) {
				UI.LOGGER.log(Level.WARNING,
						"File not downloaded. Reply = {0}",
						client.getReplyString());
				return;
			}
			UI.LOGGER.log(Level.INFO,
					String.format("Copying %s...", localFile.getName()));
			outputStream = new BufferedOutputStream(new FileOutputStream(
					localFile));
			byte[] buff = new byte[4096];
			int count = 0;
			int len;
			while ((len = inputStream.read(buff)) > 0) {
				outputStream.write(buff, 0, len);
				count += len;
				gui.setDownloadProgress((int) (100 * count / contentLength));
			}
			if (client.completePendingCommand()) {
				UI.LOGGER.log(Level.INFO,
						String.format("%d bytes downloaded.\n", count));
			}
		} catch (MalformedURLException ex) {
			UI.LOGGER.log(Level.SEVERE, "{0}\n", ex.getMessage());
		} catch (IOException ex) {
			UI.LOGGER.log(Level.SEVERE, "{0}\n", ex.getMessage());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException ex) {
			}
			gui.setDownloadProgress(0);
		}

	}

	/**
	 * @return the abortFlag
	 */
	public synchronized boolean isAborted() {
		return abortFlag;
	}

	/**
	 * @param flag
	 *            the abortFlag to set
	 */
	public synchronized void setAbort(boolean flag) {
		this.abortFlag = flag;
	}

	/**
	 * Downloads the file that is specified by the URL into localDirectory
	 * Pre-cond. localFileName != null && source is OK
	 * 
	 * @param url
	 *            URL from where the file is downloaded
	 * @param localDirectory
	 *            directory to which the file is saved.
	 * @param localFileName
	 *            the name of file saved.
	 * @throws IOException
	 */
	private void downloadFile(URL url, File localDirectory, String localFileName)
			throws IOException {

		assert localFileName != null;
		assert localDirectory.isDirectory();
		File file = new File(localDirectory, localFileName);
		assert !file.exists();

		BufferedInputStream inputStream = null;
		BufferedOutputStream outputStream = null;
		try {
			URLConnection con = url.openConnection();
			if (con == null) {
				UI.LOGGER.log(Level.INFO,
						String.format("%s not found.", localFileName));
				return;
			}
			InputStream is = null;
			is = con.getInputStream();
			inputStream = new BufferedInputStream(is);
			UI.LOGGER.log(Level.INFO,
					String.format("Copying %s...", localFileName));
			outputStream = new BufferedOutputStream(new FileOutputStream(file));
			byte[] buff = new byte[4096];
			int count = 0;
			int len;
			while ((len = inputStream.read(buff)) > 0) {
				outputStream.write(buff, 0, len);
				// count += len;
				// gui.setDownloadProgress((int) (100 * count / contentLength));
			}
			UI.LOGGER.log(Level.INFO,
					String.format("%d bytes downloaded.\n", count));
		} catch (MalformedURLException ex) {
			UI.LOGGER.log(Level.SEVERE, "{0}\n", ex.getMessage());
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException ex) {
			}
			// gui.setDownloadProgress(0);
		}

	}

	public void downloadNow(DocumentObject docObj) {
		URL url = docObj.getUrl();
		if (url == null) {
			UI.LOGGER.log(Level.INFO, "No URL for document {0}\n",
					docObj.getTDoc());
			return;
		}
		File localDirectory = Configuration.getLocalDirectory(gui.getMeeting());
		try {
			downloadFile(url, localDirectory, docObj.getTDoc() + ".zip");
		} catch (SocketException ex) {
			UI.LOGGER.log(Level.INFO, "{0} not found\n", docObj.getTDoc()
					+ ".zip");
		} catch (IOException e) {
			UI.LOGGER.log(Level.INFO, "{0}\n", DOWNLOAD_ABORTED);

		}
	}

	private void releaseSynLock() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				gui.setSyncLock(false);
			}
		});
	}
}