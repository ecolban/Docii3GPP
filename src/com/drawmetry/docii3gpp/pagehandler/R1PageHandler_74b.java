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
 * Parser of XLS file containing all document information that 3GPP SA2 MCC
 * generates at R2 meetings since meeting 81.
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class R1PageHandler_74b implements PageHandler {

	private static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	private static final int AGENDA_ITEM_COLUMN = 1;
	private static final int TYPE_COLUMN = 2;
	private static final int TDOC_COLUMN = 3;
	private static final int TITLE_COLUMN = 4;
	private static final int SOURCE_COLUMN = 5;
	private static final int REVISED_TO_FROM = 6;
	private static final int DECISION_COLUMN = 7;

	private static final String CSV_FIELD_REGEXP = "(?:\"((?:[^\"]|\"\")*)\"|([^,]*))";
	private static final Pattern COMMA_CSV_FIELD_PATTERN = Pattern.compile(","
			+ CSV_FIELD_REGEXP);

	private static final Pattern LINE_PATTERN = Pattern.compile(//
			"^(Yes|No)(," + CSV_FIELD_REGEXP + "){7}");

	private static final String TDOC_REGEX = "[CGRS][P1-6]-\\d{6}";
	private static final String REV_REGXP = String.format(
			//
			"(?:\\((%1$s(?:,\\s*%1$s)*)\\))?\\s*(%1$s(?:,\\s*%1$s)*)?",
			TDOC_REGEX);
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
	public R1PageHandler_74b(String meeting) {
		this.table = Configuration.getTables()[0];
		this.meeting = meeting;
		this.ftpPrefix = Configuration.getFtpPrefix(meeting);
		this.db = DataAccessObject.getInstance();

	}

	/**
	 * Used for test purposes only
	 */
	private R1PageHandler_74b() {
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
			String[] fields = new String[8];
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
			String agendaTitle = fields[AGENDA_ITEM_COLUMN];
			String url = ftpPrefix + fields[TDOC_COLUMN] + ".zip";
			String docTitle = fields[TITLE_COLUMN];
			String source = fields[SOURCE_COLUMN];
			String docType = fields[TYPE_COLUMN];
			String revisedTo = "";
			String revisedFrom = "";
			String comment = "";
			Matcher m = REV_PATTERN.matcher(fields[REVISED_TO_FROM]);
			while (m.find()) {
				String s = m.group(0);
				if (!s.trim().isEmpty()) {
					if (m.group(1) != null) {
						revisedFrom = m.group(1);
						if (revisedFrom.length() > 9) {
							comment += "*** Related docs: " + revisedFrom;
							revisedFrom = revisedFrom.substring(0, 9);
						}
					}
					if (m.group(2) != null) {
						revisedTo = m.group(2);
						if (revisedTo.length() > 9) {
							comment = "*** Related docs: " + revisedTo;
							revisedTo = revisedTo.substring(0, 9);
						}
					}
					break;
				}
			}
			if (revisedFrom.isEmpty() && revisedTo.isEmpty()
					&& !fields[REVISED_TO_FROM].isEmpty()) {
				comment += "*** Related docs: " + fields[REVISED_TO_FROM];
			}

			DocumentObject doc;
			try {
				doc = new DocumentObject(-1, meeting, "", agendaTitle, url,
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
		System.out.println(String.format(
				//
				"(?:\\((%1$s(?:,\\s*%1$s)*)\\))?\\s*(%1$s(?:,\\s*%1$s)*)?",
				TDOC_REGEX));
		String line = "Yes,7.2.6.4,Discussion/Decision,R1-134136,"
				+ "EPDCCH ICIC in Small Cell Environment,Intel Corporation,"
				+ "= R2-133626,Not treated,Jong-kae Fwu,0,1,1";
		R1PageHandler_74b handler = new R1PageHandler_74b();
		handler.processEntry(line);

	}
}
