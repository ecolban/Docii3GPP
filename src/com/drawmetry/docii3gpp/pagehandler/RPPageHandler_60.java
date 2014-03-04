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
//import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Parser of the HTML file (ftp-TdocsByTdoc_S2-96.htm) containing all document
 * information that 3GPP SA2 MCC generated at SA2 meeting 96. Same format used
 * in later meetings.
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class RPPageHandler_60 implements PageHandler {

	private static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	private static final int DECISION_COLUMN = 2;
	private static final int AGENDA_ITEM_COLUMN = 4;
	private static final int TDOC_COLUMN = 5;
	private static final int TITLE_COLUMN = 6;
	private static final int SOURCE_COLUMN = 7;
	private static final int TYPE_COLUMN = 8;
	private static final int LS_SOURCE_COLUMN = 10;
	private static final int WI_COLUMN = 14;
	private static final int COMMENT_COLUMN = 15;
	private static final int REV_OF_COLUMN = 16;
	private static final int NUM_COLUMNS = 17;
	private static final Pattern TDOC_PATTERN = Pattern.compile("RP-\\d{6}");

	private final DataAccessObject db;
	private final String meeting;
	private final String table;
	private final String ftpPrefix;

	/**
	 * Constructor
	 * 
	 * @param ui
	 *            the {@link UI} instance that has all the contextual
	 *            information needed.
	 * 
	 */
	public RPPageHandler_60(String meeting) {
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

	/**
	 * Handles one line read from the page.
	 * 
	 * @param line
	 * @throws MalformedURLException
	 */
	public void processRow(String[] fields) throws MalformedURLException {

		String decision = fields[DECISION_COLUMN];
		String agendaTitle = fields[AGENDA_ITEM_COLUMN];
		String url = ftpPrefix + fields[TDOC_COLUMN] + ".zip";
		String tDoc = fields[TDOC_COLUMN];
		if(!TDOC_PATTERN.matcher(tDoc).matches()) {
			return;
		}
		String docTitle = fields[TITLE_COLUMN];
		String source = fields[SOURCE_COLUMN];
		String docType = fields[TYPE_COLUMN];
		String lsSource = fields[LS_SOURCE_COLUMN];
		String workItem = fields[WI_COLUMN];
		String comment;
		if (fields[REV_OF_COLUMN].isEmpty()) {
			comment = fields[COMMENT_COLUMN];
		} else {
			comment = fields[COMMENT_COLUMN] + " *** Related docs: "
					+ fields[REV_OF_COLUMN];
		}
		DocumentObject doc;
		try {
			doc = new DocumentObject(-1, meeting, "", agendaTitle, url, tDoc,
					docType, docTitle, source, workItem, "", "", lsSource,
					comment, decision, "");
			List<DocEntry> entries = db.findEntries(table, doc.getTDoc());
			if (entries.isEmpty()) {
				db.saveRecord(table, doc);
			} else {
				DocEntry entry = entries.get(0);

				db.mergeRecord(table, entry.getId(), doc);
			}
		} catch (MalformedURLException ex) {
			LOGGER.log(Level.SEVERE, ex.getMessage());
		}
	}
}
