package com.drawmetry.docii3gpp.pagehandler;

import com.drawmetry.docii3gpp.Configuration;
import com.drawmetry.docii3gpp.DocEntry;
import com.drawmetry.docii3gpp.DocumentObject;
import com.drawmetry.docii3gpp.UI;
import com.drawmetry.docii3gpp.database.DataAccessObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parser of XLS file containing all document information that 3GPP SA2 MCC
 * generates at R2 meetings since meeting 81.
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class R2PageHandler_81 implements PageHandler {

	private static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	private static final int AGENDA_ITEM_COLUMN = 0;
	private static final int DECISION_COLUMN = 2;
	private static final int TDOC_COLUMN = 4;
	private static final int TITLE_COLUMN = 5;
	private static final int SOURCE_COLUMN = 6;
	private static final int TYPE_COLUMN = 7;
	private static final int LS_SOURCE_COLUMN = 8;
	private static final int FURTHER_INFO_COLUMN = 12;
	private static final int WI_COLUMN = 14;
	private static final int RELATED_TDOC_COLUMN = 15;
	private static final int COMMENT_COLUMN = 16;
	private static final int NUM_COLUMNS = 17;

	StringBuilder lineBuilder = new StringBuilder();
	boolean oddQuotes = false;

	private final DataAccessObject db;
	private final String meeting;
	private final String table;
	private String ftpPrefix;

	/**
	 * Constructor
	 * 
	 * @param ui
	 *            the {@link UI} instance that has all the contextual
	 *            information needed.
	 * 
	 */
	public R2PageHandler_81(String meeting) {
		this.table = Configuration.getTables()[0];
		this.meeting = meeting;
		this.ftpPrefix = Configuration.getFtpPrefix(meeting);
		this.db = DataAccessObject.getInstance();

	}

	public void processInput(BufferedReader input)
			throws MalformedURLException, IOException {
		CSVReader csvReader = new CSVReader(input, NUM_COLUMNS);
		String[] row;
		while ((row = csvReader.readRow()) != null) {
			processRow(row);
		}
	}

	private void processRow(String[] fields) {

		String comment = fields[COMMENT_COLUMN];
		if (!fields[FURTHER_INFO_COLUMN].isEmpty()) {
			comment = comment + " " + fields[FURTHER_INFO_COLUMN];
		}
		if (!fields[RELATED_TDOC_COLUMN].isEmpty()) {
			comment = comment + " *** Related TDocs: "
					+ fields[RELATED_TDOC_COLUMN];
		}
		DocumentObject doc;
		try {
			doc = new DocumentObject(-1, meeting,
					"",
					fields[AGENDA_ITEM_COLUMN], //
					ftpPrefix + fields[TDOC_COLUMN] + ".zip",
					fields[TDOC_COLUMN], //
					fields[TYPE_COLUMN], //
					fields[TITLE_COLUMN], //
					fields[SOURCE_COLUMN], //
					fields[WI_COLUMN], "", "",//
					fields[LS_SOURCE_COLUMN], //
					comment, //
					fields[DECISION_COLUMN], "");
			List<DocEntry> entries = db.findEntries(table, doc.getTDoc());
			if (entries.isEmpty()) {
				db.saveRecord(table, doc);
			} else {
				DocEntry entry = entries.get(0);
				db.mergeRecord(table, entry.getId(), doc);
			}
			// System.out.println(doc);
		} catch (MalformedURLException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

}
