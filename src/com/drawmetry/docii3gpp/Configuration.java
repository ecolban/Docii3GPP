package com.drawmetry.docii3gpp;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class is used to parse the config.xml file. It is also used to retrieve
 * configuration parameter read from the config.xml file.
 * 
 * This class is a singleton class and the parameters are retrieved through
 * static methods
 * 
 * @author Erik Colban &copy; 2012 <br>
 *         All Rights Reserved Worldwide
 */
public class Configuration extends DefaultHandler {

	private static File configFile;
	private String user;
	private String password;
	private String driver;
	private String derbyUrl;
	private String schema;
	private File systemHome;
	private File localFilesRoot;
	private String host;
	private static Configuration instance;
	private String database;
	private String tDocTable;
	private static Map<String, Meeting> meetings = new HashMap<String, Meeting>();
	private static List<String> meetingNames = new ArrayList<String>();
	public static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	/**
	 * Private constructor. Used to instantiate the single instance of this
	 * class.
	 */
	private Configuration() {
		configFile = new File(System.getProperty("user.home"),
				".docii3gpp/dociiconfig.xml");
		parseDocument();
	}

	private class Meeting {
		private final URL tDocUrl;
		private final File localDirectory;
		private final String remoteDir;
		private final String remoteDirAlt;

		private Meeting(URL tDocUrl, String localDir, String remoteDir,
				String remoteDirAlt) throws MalformedURLException {
			File localDirectory = new File(localFilesRoot, localDir);
			this.tDocUrl = tDocUrl;
			this.localDirectory = localDirectory;
			this.remoteDir = remoteDir;
			this.remoteDirAlt = remoteDirAlt;
		}

	}

	public static void main(String[] args) {
	}

	public static void initialize() {
		instance = new Configuration();
	}

	/**
	 * Get the directory of the database
	 * 
	 * @return the directory of the database
	 */
	public static File getSystemHome() {
		return instance.systemHome;
	}

	/**
	 * Gets the user id for the database
	 * 
	 * @return the user
	 */
	public static String getUser() {
		return instance.user;
	}

	/**
	 * Gets the password for the user
	 * 
	 * @return the password
	 */
	public static String getPassword() {
		return instance.password;
	}

	/**
	 * Gets the database driver
	 * 
	 * @return the driver, e.g., "org.apache.derby.jdbc.EmbeddedDriver"
	 */
	public static String getDriver() {
		return instance.driver;
	}

	/**
	 * Gets the Derby URL
	 * 
	 * @return the Derby URL, e.g., "jdbc:derby:"
	 */
	public static String getDerbyUrl() {
		return instance.derbyUrl;
	}

	/**
	 * Gets the database schema, e.g., "APP"
	 * 
	 * @return
	 */
	public static String getSchema() {
		return instance.schema;
	}

	/**
	 * Gets the database name.
	 * 
	 * @return the database name
	 */
	public static String getDatabase() {
		return instance.database;
	}

	/**
	 * Gets the database table name
	 * 
	 * @return the table name
	 */
	public static String[] getTables() {
		return new String[] { instance.tDocTable };
	}

	public static String getHost() {
		return instance.host;
	}

	/**
	 * Gets the database properties
	 * 
	 * @return the database properties
	 */
	public static Properties getProperties() {
		Properties props = new Properties();
		props.put("user", instance.user);
		props.put("password", instance.password);
		props.put("derby.driver", instance.driver);
		props.put("derby.url", instance.derbyUrl);
		props.put("derby.system.home", instance.systemHome);
		props.put("db.name", instance.database);
		props.put("schema", instance.schema);
		return props;
	}

	public static String[] getMeetings() {
		String[] a = new String[0];
//		List<String> list = new ArrayList<String>(
//				Configuration.meetings.keySet());
//		Collections.sort(list);
//		Collections.reverse(list);
		return meetingNames.toArray(a);
	}

	public static File getLocalDirectory(String meetingName) {

		return Configuration.meetings.get(meetingName).localDirectory;
	}

	public static String getRemoteDirectory(String meetingName) {

		return Configuration.meetings.get(meetingName).remoteDir;
	}

	public static String getRemoteDirectoryAlt(String meetingName) {

		return Configuration.meetings.get(meetingName).remoteDirAlt;
	}

	public static URL getTDocList(String meetingName) {
		return Configuration.meetings.get(meetingName).tDocUrl;
	}

	public static File getLocalFile(String meeting, String fileName) {
		return new File(Configuration.meetings.get(meeting).localDirectory,
				fileName + ".zip");
	}

	private void parseDocument() {

		// get a factory
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {

			// get a new instance of parser
			SAXParser parser = factory.newSAXParser();

			// parse the file and also register this class for call backs
			parser.parse(configFile, this);

		} catch (ParserConfigurationException ex) {
			Logger.getLogger("com.drawmetry.docii3gpp").log(Level.SEVERE, null,
					ex);
		} catch (SAXException ex) {
			Logger.getLogger("com.drawmetry.docii3gpp").log(Level.SEVERE, null,
					ex);
		} catch (IOException ex) {
			Logger.getLogger("com.drawmetry.docii3gpp").log(Level.SEVERE, null,
					ex);
		}
	}

	// Event Handlers

	@Override
	public void startElement(String uri, String localName, String qName,
			Attributes atts) throws SAXException {
		// reset
		if (qName.equalsIgnoreCase("docii3gpp")) {
		} else if (qName.equalsIgnoreCase("derby")) {
			startDerby(atts);
		} else if (qName.equalsIgnoreCase("localfiles")) {
			startLocalFiles(atts);
		} else if (qName.equalsIgnoreCase("remote")) {
			startRemote(atts);
		} else if (qName.equalsIgnoreCase("meeting")) {
			startMeeting(atts);
		} else if (qName.equalsIgnoreCase("table")) {
			startTable(atts);
		} else {
		}
	}

	@Override
	public void characters(char[] ch, int start, int length)
			throws SAXException {
	}

	@Override
	public void endElement(String uri, String localName, String qName)
			throws SAXException {

	}

	private void startDerby(Attributes atts) throws SAXException {
		for (int i = 0; i < atts.getLength(); i++) {
			if (atts.getQName(i).equalsIgnoreCase("user")) {
				user = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("password")) {
				password = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("driver")) {
				driver = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("url")) {
				derbyUrl = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("schema")) {
				schema = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("systemhome")) {
				File sysHome = new File(atts.getValue(i));
				boolean success = false;
				if (!sysHome.isAbsolute()) {
					sysHome = new File(System.getProperty("user.home"),
							sysHome.getPath());
				}
				if (!sysHome.exists()) {
					success = sysHome.mkdirs();
				} else if (!sysHome.isDirectory()) {
					success = false;
				} else {
					success = true;
				}
				if (!success) {
					throw new SAXException(sysHome.toString()
							+ " is not a directory");
				} else {
					systemHome = sysHome;
				}
			} else if (atts.getQName(i).equalsIgnoreCase("database")) {
				database = atts.getValue(i);
			}
		}
	}

	private void startLocalFiles(Attributes atts) throws SAXException {
		for (int i = 0; i < atts.getLength(); i++) {
			if (atts.getQName(i).equalsIgnoreCase("root")) {
				localFilesRoot = new File(atts.getValue(i));
				if (!localFilesRoot.isAbsolute()) {
					localFilesRoot = new File(System.getProperty("user.home"),
							atts.getValue(i));
				}
				boolean success;
				if (!localFilesRoot.exists()) {
					success = localFilesRoot.mkdirs();
				} else if (!localFilesRoot.isDirectory()) {
					success = false;
				} else {
					success = true;
				}
				if (!success) {
					localFilesRoot = null;
					throw new SAXException(atts.getValue(i)
							+ " is not a directory.");
				}
			}
		}
	}

	private void startRemote(Attributes atts) throws SAXException {
		for (int i = 0; i < atts.getLength(); i++) {
			if (atts.getQName(i).equalsIgnoreCase("host")) {
				host = atts.getValue(i);
			}
		}
	}

	private void startMeeting(Attributes atts) throws SAXException {
		if (localFilesRoot == null) {
			throw new SAXException("No root");
		}
		String meetingName = null;
		String urlString = null;
		String localDir = null;
		String remoteDir = null;
		String remoteDirAlt = null;
		for (int i = 0; i < atts.getLength(); i++) {
			if (atts.getQName(i).equalsIgnoreCase("name")) {
				meetingName = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("tdoclist")) {
				urlString = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("localdir")) {
				localDir = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("remotedir")) {
				remoteDir = atts.getValue(i);
			} else if (atts.getQName(i).equalsIgnoreCase("remotedir_alt")) {
				remoteDirAlt = atts.getValue(i);
			}
		}
		if (localDir == null) {
			localDir = remoteDir;
		}
		if (meetingName != null && urlString != null && remoteDir != null) {
			try {
				URL url = new URL(urlString);
//				File localDirectory = new File(localFilesRoot, localDir);
				Configuration.meetings.put(meetingName, new Meeting(url,
						localDir, remoteDir, remoteDirAlt));
				Configuration.meetingNames.add(meetingName);
			} catch (MalformedURLException e) {
				LOGGER.log(Level.SEVERE, e.getMessage());
			}
		}
	}

	private void startTable(Attributes atts) throws SAXException {

		for (int i = 0; i < atts.getLength(); i++) {
			if (atts.getQName(i).equalsIgnoreCase("name")) {
				this.tDocTable = atts.getValue(i);
			}
		}

	}

}
