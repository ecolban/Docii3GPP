package com.drawmetry.docii3gpp.pagehandler;

import com.drawmetry.docii3gpp.Configuration;
import com.drawmetry.docii3gpp.DocEntry;
import com.drawmetry.docii3gpp.DocumentObject;
import com.drawmetry.docii3gpp.UI;
import com.drawmetry.docii3gpp.database.DataAccessObject;

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
//	private static final int WORK_ITEM_COLUMN = 5;
	private static final int TITLE_COLUMN = 6;
	private static final int SOURCE_COLUMN = 7;
	private static final int DECISION_COLUMN = 8;
	private static final int COMMENT_COLUMN = 9;
	private static final int REVISED_FROM = 14;

	private static final String CSV_FIELD_REGEXP = "(?:\"((?:[^\"]|\"\")*)\"|([^,]*))";
	private static final Pattern COMMA_CSV_FIELD_PATTERN = Pattern.compile(","
			+ CSV_FIELD_REGEXP);

	private static final Pattern LINE_PATTERN = Pattern.compile(//
			"^(TRUE|FALSE)(," + CSV_FIELD_REGEXP + "){14}");

	private static final String TDOC_REGEX = "[CGRS][P1-6]-\\d{6}";
	private static final String REV_REGXP = "\\d{4}";
	private static final Pattern REV_PATTERN = Pattern.compile(REV_REGXP);

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
	public R4PageHandler_69(String meeting) {
		this.table = Configuration.getTables()[0];
		this.meeting = meeting;
		this.ftpPrefix = Configuration.getFtpPrefix(meeting);
		this.db = DataAccessObject.getInstance();

	}

	/**
	 * Used for test purposes only
	 */
	private R4PageHandler_69() {
		this.table = null;
		this.ftpPrefix = "ftp://ftp.3gpp.org/tsg_ran/WG1_RL1/TSGR1_74b/Docs/";
		db = null;
		meeting = "R1-74bis";
	}

	/**
	 * Handles one line read from the page.
	 * 
	 * @param line
	 * @throws MalformedURLException
	 */
	public void processLine(String line) throws MalformedURLException {
		if (oddQuotes) {
			lineBuilder.append(" ");
		}
		lineBuilder.append(line);
		if (!(oddQuotes ^= oddQuotes(line))) {
			processEntry(lineBuilder.toString());
			lineBuilder.setLength(0);
		}
	}

	private boolean oddQuotes(String line) {
		boolean result = false;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '\"') {
				result = !result;
			}
		}
		return result;
	}

	private void processEntry(String line) {
		Matcher lineMatcher = LINE_PATTERN.matcher(line);
		if (lineMatcher.lookingAt()) {
			line = "," + line.substring(lineMatcher.start(), lineMatcher.end());
			String[] fields = new String[15];
			Matcher fieldMatcher = COMMA_CSV_FIELD_PATTERN.matcher(line);
			String field = null;

			for (int i = 0; fieldMatcher.find(); i++) {
				if ((field = fieldMatcher.group(1)) != null) {
					field = field.replace("\"\"", "\"");
				} else {
					field = fieldMatcher.group(2);
				}
				fields[i] = field;
			}

			String tDoc = fields[TDOC_COLUMN];
			if (!tDoc.matches(TDOC_REGEX)) {
				return;
			}
			String decision = fields[DECISION_COLUMN];
			String agendaItem = fields[AGENDA_ITEM_COLUMN];
			String url = ftpPrefix + fields[TDOC_COLUMN] + ".zip";
			String docTitle = fields[TITLE_COLUMN];
			String source = fields[SOURCE_COLUMN];
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
				doc = new DocumentObject(-1, meeting, agendaItem, "", url,
						tDoc, docType, docTitle, source, "", revisedTo,
						revisedFrom, "", comment, decision, "");
				List<DocEntry> entries = db.findEntries(table, doc.getTDoc());
				if (entries.isEmpty()) {
					db.saveRecord(table, doc);
				} else {
					DocEntry entry = entries.get(0);
					db.mergeRecord(table, entry.getId(), doc);
				}
//				 System.out.println(doc);
			} catch (MalformedURLException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Used for test purposes only
	 * 
	 * @param args
	 *            ignored
	 */
	public static void main(String[] args) {
		String line = "FALSE,8.12.1,R4-137022,CR,Rel-12,LTE_CA_C_B23-Core," +
				"Introduction of CA_23B UE RF requirements into 36.101," +
				"DISH Network,,,36.101,1955r1,1,B,5803,,,,,,,,,,,,,,,,,";
		R4PageHandler_69 handler = new R4PageHandler_69();
		handler.processEntry(line);

	}
}
