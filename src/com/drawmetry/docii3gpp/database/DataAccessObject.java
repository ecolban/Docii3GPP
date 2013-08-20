package com.drawmetry.docii3gpp.database;

import com.drawmetry.docii3gpp.Configuration;
import com.drawmetry.docii3gpp.DocEntry;
import com.drawmetry.docii3gpp.DocumentObject;
import com.drawmetry.docii3gpp.UI;
import com.sun.crypto.provider.RSACipher;
import com.sun.rowset.WebRowSetImpl;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.sql.rowset.WebRowSet;

/**
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class DataAccessObject {

	/* the default framework is embedded */
	private Connection dbConnection;
	private Properties dbProperties;
	private boolean isConnected;
	private String dbName;
	private PreparedStatement[] stmtSaveNewRecord;
	private PreparedStatement[] stmtUpdateNotesExistingRecord;
	private PreparedStatement[] stmtUpdateOtherExistingRecord;
	private PreparedStatement[] stmtGetListEntries;
	private PreparedStatement[] stmtFindEntries;
	private PreparedStatement[] stmtGetRecord;
	private PreparedStatement[] stmtDeleteRecord;
	private List<Statement> statements = new ArrayList<Statement>();
	private String[] tables = Configuration.getTables();
	private static final String ABNORMAL_SHUT_DOWN = "Abnormal shutdown";
	private static final String NORMAL_SHUT_DOWN = "Normal shutdown";
	private static final int MEETING_LENGTH = 10;
	private static final int AGENDA_ITEM_LENGTH = 10;
	private static final int AGENDA_TITLE_LENGTH = 300;
	private static final int URL_LENGTH = 80;
	private static final int TDOC_LENGTH = 9;
	private static final int DOC_TYPE_LENGTH = 16;
	private static final int DOC_TITLE_LENGTH = 300;
	private static final int SOURCE_LENGTH = 400;
	private static final int WORK_ITEM_LENGTH = 100;
	private static final int LS_SOURCE_LENGTH = 50;
	private static final int COMMENT_LENGTH = 400;
	private static final int DECISION_LENGTH = 50;
	private static final int NOTES_LENGTH = 2000;
	private static final String strCreateDocumentTable = "create table %s ("
			+ "  ID           INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)" //
			+ ", MEETING      VARCHAR("
			+ MEETING_LENGTH + ")" //
			+ ", AGENDA_ITEM  VARCHAR(" + AGENDA_ITEM_LENGTH + ")" //
			+ ", AGENDA_TITLE VARCHAR(" + AGENDA_TITLE_LENGTH + ")" //
			+ ", URL          VARCHAR(" + URL_LENGTH + ")" //
			+ ", TDOC         CHAR(" + TDOC_LENGTH + ")" //
			+ ", DOC_TYPE     VARCHAR(" + DOC_TYPE_LENGTH + ")" //
			+ ", DOC_TITLE    VARCHAR(" + DOC_TITLE_LENGTH + ")" //
			+ ", SOURCE       VARCHAR(" + SOURCE_LENGTH + ")" //
			+ ", WORK_ITEM    VARCHAR(" + WORK_ITEM_LENGTH + ")" //
			+ ", REV_BY       CHAR(" + TDOC_LENGTH + ")" //
			+ ", REV_OF       CHAR(" + TDOC_LENGTH + ")" //
			+ ", LS_SOURCE    VARCHAR(" + LS_SOURCE_LENGTH + ")" //
			+ ", COMMENT      VARCHAR(" + COMMENT_LENGTH + ")" //
			+ ", DECISION     VARCHAR(" + DECISION_LENGTH + ")" //
			+ ", NOTES        VARCHAR(" + NOTES_LENGTH + ")" //
			+ ")";
	private static final String strGetRecord = "select * from %s "
			+ "where ID = ?";
	private static final String strSaveNewRecord = "insert into %s "
			+ "(MEETING, AGENDA_ITEM, AGENDA_TITLE, URL, TDOC, DOC_TYPE, DOC_TITLE, SOURCE, "
			+ "WORK_ITEM, REV_BY, REV_OF, LS_SOURCE, COMMENT, DECISION, NOTES)"
			+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?)";
	private static final String strGetListEntries = "select ID, TDOC from %s "
			+ "where TDOC like ? order by TDOC desc";
	private static final String strFindEntries = "select ID, TDOC from %s "
			+ "where MEETING like ? and TDOC like ? "
			+ "and LOWER(DOC_TITLE) like ? "
			+ "and LOWER(SOURCE) like ? and LOWER(NOTES) like ? "
			+ "and LOWER(AGENDA_TITLE) like ? and LOWER(WORK_ITEM) like ? "
			+ "and LOWER(DECISION) like ? and LOWER(COMMENT) like ? " + "order by TDOC desc";
	private static final String strUpdateNotesExistingRecord = "update %s "
			+ "set NOTES = ? where ID = ?";
	private static final String strUpdateOtherExistingRecord = "update %s "
			+ "set AGENDA_ITEM = ?,  AGENDA_TITLE = ?, URL = ?, DOC_TYPE = ?, "
			+ "DOC_TITLE = ?, SOURCE = ?, WORK_ITEM = ?, REV_BY = ?, REV_OF = ?, "
			+ "LS_SOURCE = ?, COMMENT = ?, DECISION = ? where ID = ?";
	private static final String strDeleteRecord = "delete from %s "
			+ "where ID = ?";

	/** Creates a new instance of DataAccessObject */
	public DataAccessObject() {
		dbProperties = loadDBProperties();
		setDBSystemDir();
		this.dbName = dbProperties.getProperty("db.name");
		String driverName = dbProperties.getProperty("derby.driver");
		loadDatabaseDriver(driverName);
		createDatabase();
	}

	/**
	 * A method used to test the instantiation of a DataAccessObject.
	 * 
	 * @param args
	 *            the value of this parameter is ignored
	 */
	public static void main(String[] args) {
		// dump("WG80211", new File("D:\\Database\\.docii3gpp\\dump.xml"));
		Configuration.initialize();
		DataAccessObject db = new DataAccessObject();
		db.connect();
		try {
			Statement stmt = db.dbConnection.createStatement();

			// stmt.execute("alter table TDOC_TABLE alter column  WORK_ITEM set data type VARCHAR(100)");
			// db.dbConnection.commit();

			ResultSet rs = stmt
					.executeQuery("select ID from TDOC_TABLE where MEETING like 'R2-83' and TDOC not like 'R2-%'");
			int count = 0;
			while (rs.next()) {
				int id = rs.getInt("ID");
				// System.out.println("" + db.getDocumentOject("WG80221", id));
				db.deleteRecord("TDOC_TABLE", id);
				count++;
				db.dbConnection.commit();
			}
			System.out.println("count = " + count);
		} catch (SQLException ex) {
			UI.LOGGER.log(Level.SEVERE, ex.getMessage());
		}
		db.disconnect();
	}

	@SuppressWarnings("unused")
	private static void dump(String table, File toFile) {
		DataAccessObject db = new DataAccessObject();
		db.connect();
		PrintStream out = null;
		Statement statement = null;
		WebRowSet wrs = null;
		try {
			out = new PrintStream(new FileOutputStream(toFile));
			statement = db.dbConnection.createStatement();
			ResultSet rs = statement.executeQuery("select * from " + table);
			wrs = new WebRowSetImpl();
			wrs.populate(rs);
			wrs.writeXml(out);
		} catch (IOException ex) {
		} catch (SQLException ex) {
			System.out.println("Error code = " + ex.getErrorCode());
		} finally {
			if (out != null) {
				out.close();
			}
			if (wrs != null) {
				try {
					wrs.close();
				} catch (SQLException ex) {
					System.out.println("Error code = " + ex.getErrorCode());
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException ex) {
					System.out.println("Error code = " + ex.getErrorCode());
				}
			}
		}

		db.disconnect();
	}

	private boolean dbExists() {
		boolean bExists = false;
		String dbLocation = getDatabaseLocation();
		File dbFileDir = new File(dbLocation);
		if (dbFileDir.exists()) {
			bExists = true;
		}
		return bExists;
	}

	private void setDBSystemDir() {
		// decide on the db system directory
		String systemDir = Configuration.getSystemHome().getAbsolutePath();
		System.setProperty("derby.system.home", systemDir);
	}

	/**
	 * Loads the embedded JDBC driver:
	 * <code>org.apache.derby.jdbc.EmbeddedDriver</code>.
	 */
	private void loadDatabaseDriver(String driver) {
		/*
		 * The JDBC driver is loaded by loading its class. If you are using JDBC
		 * 4.0 (Java SE 6) or newer, JDBC drivers may be automatically loaded,
		 * making this code optional.
		 * 
		 * In an embedded environment, this will also start up the Derby engine
		 * (though not any databases), since it is not already running. In a
		 * client environment, the Derby engine is being run by the network
		 * server framework.
		 * 
		 * In an embedded environment, any static Derby system properties must
		 * be set before loading the driver to take effect.
		 */
		try {
			Class.forName(driver).newInstance();
			// UI.LOGGER.log(Level.INFO, "Loaded driver:{0}\n", driver);
		} catch (ClassNotFoundException cnfe) {
			UI.LOGGER
					.log(Level.SEVERE,
							"Unable to load the JDBC driver {0}\nPlease check your CLASSPATH.\n",
							driver);
			cnfe.printStackTrace(System.err);
		} catch (InstantiationException ie) {
			UI.LOGGER.log(Level.SEVERE, "Unable to load the JDBC driver {0}\n",
					driver);
			ie.printStackTrace(System.err);
		} catch (IllegalAccessException iae) {
			UI.LOGGER.log(Level.SEVERE,
					"Not allowed to access the JDBC driver {0}\n", driver);
			iae.printStackTrace(System.err);
		}
	}

	private Properties loadDBProperties() {

		return Configuration.getProperties();
	}

	private boolean createTable(Connection conn, String table) {
		boolean bCreateTable = false;
		try {
			Statement stmtCreateTable = conn.createStatement();
			String strStmt = String.format(strCreateDocumentTable, table);
			stmtCreateTable.execute(strStmt);
			conn.commit();
			bCreateTable = true;
		} catch (SQLException ex) {
			printSQLException(ex);
		}

		return bCreateTable;
	}

	private boolean createDatabase() {
		boolean bCreated = false;
		Connection dbCon = null;

		String dbUrl = getDatabaseUrl();
		if (!dbExists()) {
			dbProperties.put("create", "true");
		}
		try {
			dbCon = DriverManager.getConnection(dbUrl, dbProperties);
			DatabaseMetaData dmd = dbCon.getMetaData();
			for (String table : tables) {
				ResultSet rs = dmd.getTables(null, null, table, null);
				if (!rs.next()) {
					createTable(dbCon, table);
				}
			}
			bCreated = true;
		} catch (SQLException ex) {
			printSQLException(ex);
		} finally {
			try {
				if (dbCon != null) {
					dbCon.close();
				}
			} catch (SQLException ex) {
				printSQLException(ex);
			}
		}
		return bCreated;
	}

	public boolean connect() {
		String dbUrl = getDatabaseUrl();
		try {
			dbConnection = DriverManager.getConnection(dbUrl, dbProperties);
			dbConnection.setAutoCommit(false);
			stmtSaveNewRecord = new PreparedStatement[tables.length];
			stmtUpdateNotesExistingRecord = new PreparedStatement[tables.length];
			stmtUpdateOtherExistingRecord = new PreparedStatement[tables.length];
			stmtGetRecord = new PreparedStatement[tables.length];
			stmtDeleteRecord = new PreparedStatement[tables.length];
			stmtGetListEntries = new PreparedStatement[tables.length];
			stmtFindEntries = new PreparedStatement[tables.length];
			String stmt;
			for (int i = 0; i < tables.length; i++) {
				stmt = String.format(strSaveNewRecord, tables[i]);
				stmtSaveNewRecord[i] = dbConnection.prepareStatement(stmt,
						Statement.RETURN_GENERATED_KEYS);
				statements.add(stmtSaveNewRecord[i]);
				stmt = String.format(strUpdateNotesExistingRecord, tables[i]);
				stmtUpdateNotesExistingRecord[i] = dbConnection
						.prepareStatement(stmt);
				statements.add(stmtUpdateNotesExistingRecord[i]);
				stmt = String.format(strUpdateOtherExistingRecord, tables[i]);
				stmtUpdateOtherExistingRecord[i] = dbConnection
						.prepareStatement(stmt);
				statements.add(stmtUpdateOtherExistingRecord[i]);
				stmt = String.format(strGetRecord, tables[i]);
				stmtGetRecord[i] = dbConnection.prepareStatement(stmt);
				statements.add(stmtGetRecord[i]);
				stmt = String.format(strDeleteRecord, tables[i]);
				stmtDeleteRecord[i] = dbConnection.prepareStatement(stmt);
				statements.add(stmtDeleteRecord[i]);
				stmt = String.format(strGetListEntries, tables[i]);
				stmtGetListEntries[i] = dbConnection.prepareStatement(stmt);
				statements.add(stmtGetListEntries[i]);
				stmt = String.format(strFindEntries, tables[i]);
				stmtFindEntries[i] = dbConnection.prepareStatement(stmt);
				statements.add(stmtFindEntries[i]);
			}
			isConnected = dbConnection != null; // auto-commit is on.
		} catch (SQLException ex) {
			isConnected = false;
			printSQLException(ex);
		}
		return isConnected;
	}

	public void disconnect() {

		try {
			// the shutdown=true attribute shuts down Derby
			String dbUrl = getDatabaseUrl();
			dbProperties.put("shutdown", "true");
			DriverManager.getConnection(dbUrl, dbProperties);
			// DriverManager.getConnection("jdbc:derby:" + dbName +
			// ";shutdown=true");

			// To shut down a specific database only, but keep the
			// engine running (for example for connecting to other
			// databases), specify a database in the connection URL:
			// DriverManager.getConnection("jdbc:derby:" + dbName +
			// ";shutdown=true");
		} catch (SQLException se) {
			if (((se.getErrorCode() == 45000) && ("08006".equals(se
					.getSQLState())))) {
				// we got the expected exception
				UI.LOGGER.log(Level.INFO, NORMAL_SHUT_DOWN, dbName);
				// Note that for single database shutdown, the expected
				// SQL state is "08006", and the error code is 45000.
			} else {
				// if the error code or SQLState is different, we have
				// an unexpected exception (shutdown failed)
				UI.LOGGER.log(Level.INFO, ABNORMAL_SHUT_DOWN, dbName);
				printSQLException(se);
			}
		} finally {
			// Statements and PreparedStatements
			for (Iterator<Statement> i = statements.iterator(); i.hasNext();) {
				Statement st = i.next();
				i.remove();
				try {
					if (st != null) {
						st.close();
						st = null;
					}
				} catch (SQLException sqle) {
					printSQLException(sqle);
				}
			}

			// Connection
			try {
				if (dbConnection != null) {
					dbConnection.close();
					dbConnection = null;
				}
			} catch (SQLException sqle) {
				printSQLException(sqle);
			}
		}
	}

	public String getDatabaseLocation() {
		String dbLocation = System.getProperty("derby.system.home") + "/"
				+ dbName;
		return dbLocation;
	}

	public String getDatabaseUrl() {
		return Configuration.getDerbyUrl() + Configuration.getDatabase();
	}

	/**
	 * insert into %s " +
	 * "(MEETING, AGENDA_ITEM, AGENDA_TITLE, URL, TDOC, DOC_TYPE, DOC_TITLE, SOURCE, "
	 * + "WORK_ITEM, REV_BY, REV_OF, LS_SOURCE, COMMENT, DECISION, NOTES)" + "
	 * values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,?, ?, ?)
	 * 
	 * @param table
	 * @param doc
	 * @return
	 */
	public int saveRecord(String table, DocumentObject doc) {
		int id = -1;
		int index = getTableIndex(table);
		try {
			stmtSaveNewRecord[index].clearParameters();
			stmtSaveNewRecord[index].setString(1,
					trim(doc.getMeeting(), MEETING_LENGTH));
			stmtSaveNewRecord[index].setString(2,
					trim(doc.getAgendaItem(), AGENDA_ITEM_LENGTH));
			stmtSaveNewRecord[index].setString(3,
					trim(doc.getAgendaTitle(), AGENDA_TITLE_LENGTH));
			URL url = doc.getUrl();
			stmtSaveNewRecord[index].setString(4,
					trim(url == null ? null : url.toString(), URL_LENGTH));
			stmtSaveNewRecord[index].setString(5,
					trim(doc.getTDoc(), TDOC_LENGTH));
			stmtSaveNewRecord[index].setString(6,
					trim(doc.getDocType(), DOC_TYPE_LENGTH));
			stmtSaveNewRecord[index].setString(7,
					trim(doc.getDocTitle(), DOC_TITLE_LENGTH));
			stmtSaveNewRecord[index].setString(8,
					trim(doc.getSource(), SOURCE_LENGTH));
			stmtSaveNewRecord[index].setString(9,
					trim(doc.getWorkItem(), WORK_ITEM_LENGTH));
			stmtSaveNewRecord[index].setString(10,
					trim(doc.getRevByTDoc(), TDOC_LENGTH));
			stmtSaveNewRecord[index].setString(11,
					trim(doc.getRevOfTDoc(), TDOC_LENGTH));
			stmtSaveNewRecord[index].setString(12,
					trim(doc.getLsSource(), LS_SOURCE_LENGTH));
			stmtSaveNewRecord[index].setString(13,
					trim(doc.getComment(), COMMENT_LENGTH));
			stmtSaveNewRecord[index].setString(14,
					trim(doc.getDecision(), DECISION_LENGTH));
			stmtSaveNewRecord[index].setString(15,
					trim(doc.getNotes(), NOTES_LENGTH));
			dbConnection.commit();
			@SuppressWarnings("unused")
			int rowCount = stmtSaveNewRecord[index].executeUpdate();
			ResultSet results = stmtSaveNewRecord[index].getGeneratedKeys();
			if (results.next()) {
				id = results.getInt(1);
				UI.LOGGER.log(Level.INFO, "{0} added.\n", doc.getTDoc());
			}
		} catch (SQLException sqle) {
			printSQLException(sqle);
		}
		return id;
	}

	private String trim(String string, int length) {
		if (string.length() > length) {
			UI.LOGGER.log(Level.WARNING, "Field {0} truncated to length {1}",
					new Object[] { string, length });
			return string.substring(0, length - 1) + "~";
		} else {
			return string;
		}
	}

	/**
	 * Performs a database query to select all entries that match the given
	 * arguments.
	 * 
	 * @param meeting
	 * @param tDoc
	 * @param source
	 * @param notes
	 * @param decision
	 * @param
	 * @param
	 * @param docTitle
	 * @return
	 */
	public List<DocEntry> findEntries(String table, String meeting,
			String tDoc, String title, String source, String notes,
			String agendaTitle, String workItem, String decision,
			String comments) {
		List<DocEntry> listEntries = new ArrayList<DocEntry>();
		ResultSet results = null;
		int index = getTableIndex(table);
		try {
			stmtFindEntries[index].clearParameters();
			stmtFindEntries[index].setString(1, meeting);
			stmtFindEntries[index].setString(2, tDoc.toLowerCase());
			stmtFindEntries[index].setString(3, title.toLowerCase());
			stmtFindEntries[index].setString(4, source.toLowerCase());
			stmtFindEntries[index].setString(5, notes.toLowerCase());
			stmtFindEntries[index].setString(6, agendaTitle.toLowerCase());
			stmtFindEntries[index].setString(7, workItem.toLowerCase());
			stmtFindEntries[index].setString(8, decision.toLowerCase());
			stmtFindEntries[index].setString(9, comments.toLowerCase());
			results = stmtFindEntries[index].executeQuery();
			while (results.next()) {
				int id = results.getInt(1);
				String fileName = results.getString("TDOC");
				DocEntry entry = new DocEntry(fileName, id);
				listEntries.add(entry);
			}
			dbConnection.commit();

		} catch (SQLException sqle) {
			printSQLException(sqle);
		}

		return listEntries;
	}

	public List<DocEntry> findEntries(String table, String tDoc) {
		List<DocEntry> listEntries = new ArrayList<DocEntry>();
		ResultSet results = null;
		int index = getTableIndex(table);
		try {
			stmtGetListEntries[index].clearParameters();
			stmtGetListEntries[index].setString(1, tDoc);

			results = stmtGetListEntries[index].executeQuery();
			while (results.next()) {
				int id = results.getInt(1);
				DocEntry entry = new DocEntry(results.getString(2), id);
				listEntries.add(entry);
			}
			dbConnection.commit();

		} catch (SQLException sqle) {
			printSQLException(sqle);
		}

		return listEntries;
	}

	// public List<DocEntry> findEntries(String table, String yearStr,
	// String groupCode) {
	// List<DocEntry> listEntries = null;
	// int year = 0;
	// if (yearStr != null) {
	// try {
	// year = Integer.parseInt(yearStr);
	// } catch (NumberFormatException ex) {
	// yearStr = null;
	// }
	// }
	// String stmtStr;
	// if (yearStr == null) {
	// if (groupCode == null) {
	// stmtStr = "select ID, FILENAME from " + table
	// + " order by FILENAME desc";
	// } else {
	// stmtStr = "select ID, FILENAME from " + table
	// + " where GROUPVAR like '" + groupCode + "'"
	// + " order by FILENAME desc";
	// }
	// } else {
	// if (groupCode == null) {
	// stmtStr = "select ID, FILENAME from " + table
	// + " where YEARVAR = " + year
	// + " order by FILENAME desc";
	// } else {
	// stmtStr = "select ID, FILENAME from " + table
	// + " where YEARVAR = " + year + " and GROUPVAR like '"
	// + groupCode + "' order by FILENAME desc";
	// }
	// }
	// try {
	// Statement statement = dbConnection.createStatement();
	// ResultSet rs = statement.executeQuery(stmtStr);
	// listEntries = new ArrayList<DocEntry>();
	// while (rs.next()) {
	// DocEntry entry = new DocEntry(rs.getString(2), rs.getInt(1));
	// listEntries.add(entry);
	// }
	// statement.close();
	// dbConnection.commit();
	// } catch (SQLException sqle) {
	// printSQLException(sqle);
	// }
	// return listEntries;
	// }

	/**
	 * 
	 * "update %s set NOTES = ?  where ID = ?";
	 * 
	 * @param id
	 * @param record
	 * @return
	 */
	public boolean editRecord(String table, int id, DocumentObject record) {

		boolean edited = false;
		int index = getTableIndex(table);
		try {
			stmtUpdateNotesExistingRecord[index].clearParameters();
			stmtUpdateNotesExistingRecord[index].setString(1,
					trim(record.getNotes(), NOTES_LENGTH));
			stmtUpdateNotesExistingRecord[index].setInt(2, id);
			stmtUpdateNotesExistingRecord[index].executeUpdate();
			dbConnection.commit();
			edited = true;
		} catch (SQLException sqle) {
			printSQLException(sqle);
		}

		return edited;
	}

	/**
	 * "update %s " +
	 * "set AGENDA_ITEM = ?,  AGENDA_TITLE = ?, URL = ?, DOC_TYPE = ?, " +
	 * "DOC_TITLE = ?, SOURCE = ?, WORK_ITEM = ?, REV_BY = ?, REV_OF = ?, " +
	 * "LS_SOURCE = ?, COMMENT = ?, DECISION = ? where ID = ?"
	 * 
	 * @param table
	 * @param id
	 * @param doc
	 * @return
	 */
	public boolean mergeRecord(String table, int id, DocumentObject doc) {
		boolean edited = false;
		int index = getTableIndex(table);
		URL url = doc.getUrl();
		try {
			stmtUpdateOtherExistingRecord[index].clearParameters();
			stmtUpdateOtherExistingRecord[index].setString(1,
					trim(doc.getAgendaItem(), AGENDA_ITEM_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(2,
					trim(doc.getAgendaTitle(), AGENDA_TITLE_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(3,
					trim(url == null ? "" : url.toString(), URL_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(4,
					trim(doc.getDocType(), DOC_TYPE_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(5,
					trim(doc.getDocTitle(), DOC_TITLE_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(6,
					trim(doc.getSource(), SOURCE_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(7,
					trim(doc.getWorkItem(), WORK_ITEM_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(8,
					trim(doc.getRevByTDoc(), TDOC_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(9,
					trim(doc.getRevOfTDoc(), TDOC_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(10,
					trim(doc.getLsSource(), LS_SOURCE_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(11,
					trim(doc.getComment(), COMMENT_LENGTH));
			stmtUpdateOtherExistingRecord[index].setString(12,
					trim(doc.getDecision(), DECISION_LENGTH));
			stmtUpdateOtherExistingRecord[index].setInt(13, id);
			stmtUpdateOtherExistingRecord[index].executeUpdate();
			dbConnection.commit();
			edited = true;
		} catch (SQLException sqle) {
			printSQLException(sqle);
		}

		return edited;
	}

	public boolean deleteRecord(String table, int id) {
		boolean deleted = false;
		int index = getTableIndex(table);
		try {
			stmtDeleteRecord[index].clearParameters();
			stmtDeleteRecord[index].setInt(1, id);
			stmtDeleteRecord[index].executeUpdate();
			dbConnection.commit();
			deleted = true;
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

		return deleted;
	}

	/**
	 * ID INTEGER NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY (START WITH
	 * 1, INCREMENT BY 1)" + ", MEETING VARCHAR(10)" // +
	 * ", AGENDA_ITEM  VARCHAR(10)" // + ", AGENDA_TITLE VARCHAR(100)" +
	 * ", URL          VARCHAR(80)" // + ", TDOC         CHAR(9)" // +
	 * ", DOC_TYPE     VARCHAR(16)" // + ", DOC_TITLE        VARCHAR(200)" // +
	 * ", SOURCE       VARCHAR(400)" // + ", WORK_ITEM    VARCHAR(50)" // +
	 * ", REV_BY       CHAR(9)" // + ", REV_OF       CHAR(9)" // +
	 * ", LS_SOURCE    VARCHAR(50)" // + ", COMMENT      VARCHAR(400)" // +
	 * ", DECISION     VARCHAR(50)" // + ", NOTES        VARCHAR(2000)" //
	 * 
	 * @param table
	 * @param id
	 * @return
	 */
	public DocumentObject getDocumentOject(String table, int id) {
		DocumentObject docObj = null;
		int index = getTableIndex(table);
		try {
			stmtGetRecord[index].clearParameters();
			stmtGetRecord[index].setInt(1, id);
			ResultSet result = stmtGetRecord[index].executeQuery();
			if (result.next()) {
				String meeting = result.getString("MEETING");
				String agendaItem = result.getString("AGENDA_ITEM");
				String agendaTitle = result.getString("AGENDA_TITLE");
				String url = result.getString("URL");
				String tDoc = result.getString("TDOC");
				String docType = result.getString("DOC_TYPE");
				String docTitle = result.getString("DOC_TITLE");
				String source = result.getString("SOURCE");
				String workItem = result.getString("WORK_ITEM");
				String revByTDoc = result.getString("REV_BY");
				String revOfTDoc = result.getString("REV_OF");
				String lsSource = result.getString("LS_SOURCE");
				String comment = result.getString("COMMENT");
				String decision = result.getString("DECISION");
				String notes = result.getString("NOTES");

				docObj = new DocumentObject(id, meeting, agendaItem,
						agendaTitle, url, tDoc, docType, docTitle, source,
						workItem, revByTDoc, revOfTDoc, lsSource, comment,
						decision, notes);
			}
			dbConnection.commit();
		} catch (MalformedURLException ex) {
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		return docObj;
	}

	/**
	 * Prints details of an SQLException chain to <code>System.err</code>.
	 * Details included are SQL State, Error code, Exception message.
	 * 
	 * @param e
	 *            the SQLException from which to print details.
	 */
	public static void printSQLException(SQLException e) {
		// Unwraps the entire exception chain to unveil the real cause of the
		// Exception.
		UI.LOGGER.log(Level.SEVERE, e.getMessage());
		while (e != null) {
			System.err.println("\n----- SQLException -----");
			System.err.println("  SQL State:  " + e.getSQLState());
			System.err.println("  Error Code: " + e.getErrorCode());
			System.err.println("  Message:    " + e.getMessage());
			// for stack traces, refer to derby.log or uncomment this:
			// e.printStackTrace(System.err);
			e = e.getNextException();
		}
	}

	private int getTableIndex(String table) {
		for (int i = 0; i < tables.length; i++) {
			if (tables[i].equals(table)) {
				return i;
			}
		}
		return -1;
	}

}
