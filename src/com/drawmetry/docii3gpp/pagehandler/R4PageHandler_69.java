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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser of CSV file containing all document information that 3GPP SA2 MCC
 * generates at R4 meetings since meeting 69.
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class R4PageHandler_69 implements PageHandler {

	private static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	/*
	 * 0:Available 1:Agenda 2:Tdoc 3:Type 4:Release 5:Work Item 6:Title 7:Source
	 * 8:Decision 9:Comment 10:Spec 11:CR 12:R 13:Category 14:Revision_of
	 */

	private static final int AGENDA_ITEM_COLUMN = 1;
	private static final int TDOC_COLUMN = 2;
	private static final int TYPE_COLUMN = 3;
//	private static final int RELEASE_COLUMN = 4;
	private static final int WORK_ITEM_COLUMN = 5;
	private static final int TITLE_COLUMN = 6;
	private static final int SOURCE_COLUMN = 7;
	private static final int DECISION_COLUMN = 8;
	private static final int COMMENT_COLUMN = 9;
	private static final int REVISED_FROM = 14;
	private static final int NUM_COLUMNS = 15;

	private static final String TDOC_REGEX = "[CGRS][P1-6]-\\d{6}";
	private static final String REV_REGXP = "\\d{4}";
	private static final Pattern REV_PATTERN = Pattern.compile(REV_REGXP);

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
	public R4PageHandler_69(String meeting) {
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
		String tDoc = fields[TDOC_COLUMN];
		if (tDoc == null || !tDoc.matches(TDOC_REGEX)) {
			return;
		}
		String decision = fields[DECISION_COLUMN];
		String agendaItem = fields[AGENDA_ITEM_COLUMN];
		String url = ftpPrefix + fields[TDOC_COLUMN] + ".zip";
		String docTitle = fields[TITLE_COLUMN];
		String source = fields[SOURCE_COLUMN];
		String workItem = fields[WORK_ITEM_COLUMN];
		String docType = fields[TYPE_COLUMN];
		String revisedTo = "";
		String revisedFrom = "";
		String comment = fields[COMMENT_COLUMN];
		Matcher m = REV_PATTERN.matcher(fields[REVISED_FROM]);
		if (m.matches()) {
			revisedFrom = "R4-13" + fields[REVISED_FROM];
		}

		DocumentObject doc;
		try {
			doc = new DocumentObject(-1, meeting, agendaItem, "", url, tDoc,
					docType, docTitle, source, workItem, revisedTo, revisedFrom, "",
					comment, decision, "");
			List<DocEntry> entries = db.findEntries(table, doc.getTDoc());
			if (entries.isEmpty()) {
				db.saveRecord(table, doc);
			} else {
				DocEntry entry = entries.get(0);
				db.mergeRecord(table, entry.getId(), doc);
			}
		} catch (MalformedURLException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

}
