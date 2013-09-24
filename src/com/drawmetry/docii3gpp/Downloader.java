package com.drawmetry.docii3gpp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	
	private static final Logger LOGGER = Logger.getLogger("com.drawmetry.docii3gpp");

	private static final String FTP_USERNAME = "anonymous";
	private static final String FTP_PASSWORD = "";
	private static final String DOWNLOAD_ABORTED = "Download aborted.";
	private static final String STARTING_DOWNLOAD = "Starting download";
	private static final String DOWNLOAD_COMPLETE = "Download complete";
	private final UI ui;
	private boolean abortFlag = false;

	public Downloader(UI ui) {
		this.ui = ui;
	}

	@Override
	public void run() {
		LOGGER.log(Level.INFO, String.format("%s\n", STARTING_DOWNLOAD));

		String meeting = ui.getMeeting();
		File localDir = Configuration.getLocalDirectory(meeting);
		String hostname = Configuration.getHost();
		// String inboxDir = Configuration.getRemoteDirectoryAlt(meeting);
		if (!localDir.exists()) {
			localDir.mkdirs();
		}
		FTPClient client = null;
		try {
			client = openFtpConnection(hostname);
			String remoteDir = Configuration.getRemoteDirectory(meeting);
			LOGGER.log(Level.INFO, "From {0}:\n", remoteDir);
			downloadFiles(client, localDir, remoteDir);
			remoteDir = Configuration.getRemoteDirectoryAlt(meeting);
			if (remoteDir != null) {
				LOGGER.log(Level.INFO, "From {0}:\n", remoteDir);
				downloadFiles(client, localDir, remoteDir);
			}
		} catch (SocketException e1) {
			LOGGER.log(Level.INFO, e1.getMessage() + "\n");
			setAbort(true);
		} catch (IOException e1) {
			LOGGER.log(Level.INFO, e1.getMessage() + "\n");
			setAbort(true);
		} finally {
			try {
				if (client != null && client.isConnected()) {
					client.disconnect();
				}
			} catch (IOException e) {
			}
			releaseSyncLock();
		}

		if (!isAborted()) {
			LOGGER.log(Level.INFO, "{0}\n", DOWNLOAD_COMPLETE);
		} else {
			setAbort(false);
			LOGGER.log(Level.INFO, "{0}\n", DOWNLOAD_ABORTED);
		}
	}

	public void downloadAndOpen(final DocumentObject docObj) {
		URL url = docObj.getUrl();
		if (url == null) {
			LOGGER.log(Level.INFO, "No URL for document {0}\n",
					docObj.getTDoc());
			return;
		}
		String hostname = url.getHost();
		FTPClient client = null;
		try {
			final File localFile = new File(Configuration.getLocalFilesRoot(), docObj.getUrl().getPath());
			if (!localFile.exists()) {
				client = openFtpConnection(hostname);
				String remotePath = url.getPath();
				FTPFile[] listing = client.listFiles(remotePath);
				if (listing.length == 1) {
					downloadFile(client, localFile, listing[0], remotePath);
				} else {
					LOGGER.log(Level.INFO, "{0} not found\n", docObj.getUrl());
				}
			}
			if (localFile.exists()) {
				SwingUtilities.invokeLater(new Runnable() {
	
					@Override
					public void run() {
						ui.open(localFile);
	
					}
				});
			}
		} catch (SocketException ex) {
			LOGGER.log(Level.INFO, "{0} not found\n", docObj.getUrl());
		} catch (IOException e) {
			LOGGER.log(Level.INFO, "{0} not found\n", docObj.getUrl());
	
		} finally {
			try {
				if (client.isConnected()) {
					client.disconnect();
				}
			} catch (IOException e) {
			}
		}
		releaseSyncLock();
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

	private FTPClient openFtpConnection(String hostname)
			throws SocketException, IOException {
		FTPClient client = new FTPClient();
		client.connect(hostname);
		int reply = client.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			client.disconnect();
			throw new IOException("FTP server refused connection.");
		}

		client.login(FTP_USERNAME, FTP_PASSWORD);
		client.enterLocalPassiveMode();
		client.setFileType(FTP.BINARY_FILE_TYPE);
		return client;
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
				LOGGER.log(Level.INFO, "[{0}]", count);
				downloadFile(client, localFile, remoteFile, remoteDir + "/" + remoteFile.getName());
				count--;
				ui.repaint();
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
			FTPFile remoteFile, String remotePath) throws IOException {
		assert client.isConnected();
		assert localFile != null;
		assert !localFile.exists();
		InputStream inputStream = null;
		BufferedOutputStream outputStream = null;
		long contentLength = remoteFile.getSize();
		try {
			inputStream = client.retrieveFileStream(remotePath);
			if (inputStream == null) {
				LOGGER.log(Level.WARNING,
						"File not downloaded. Reply = {0}",
						client.getReplyString());
				return;
			}
			LOGGER.log(Level.INFO,
					String.format("Copying %s...", localFile.getName()));
			outputStream = new BufferedOutputStream(new FileOutputStream(
					localFile));
			byte[] buff = new byte[1024];
			int count = 0;
			int len;
			while ((len = inputStream.read(buff)) > 0) {
				outputStream.write(buff, 0, len);
				count += len;
				ui.setDownloadProgress((int) (100 * count / contentLength));
			}
			if (client.completePendingCommand()) {
				LOGGER.log(Level.INFO,
						String.format("%d bytes downloaded.\n", count));
			}
		} catch (MalformedURLException ex) {
			LOGGER.log(Level.SEVERE, "{0}\n", ex.getMessage());
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
			ui.setDownloadProgress(0);
		}

	}

	private void releaseSyncLock() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				ui.setSyncLock(false);
			}
		});
	}
	
}