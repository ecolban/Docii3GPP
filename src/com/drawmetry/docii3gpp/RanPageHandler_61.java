package com.drawmetry.docii3gpp;

import com.drawmetry.docii3gpp.database.DataAccessObject;

import java.net.MalformedURLException;
import java.util.List;
//import java.util.Arrays;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser of the HTML file (ftp-TdocsByTdoc_S2-96.htm) containing all document
 * information that 3GPP SA2 MCC generated at SA2 meeting 96. Same format used
 * in later meetings.
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class RanPageHandler_61 {

	// private static final Pattern TDOC_PATTERN = Pattern
	// .compile("(<a .* href=\"(.*\\.zip)\">)?([CGRS][1-5P]-\\d{6})(</a>)?");

	/*
	 * Available,Late,status,Flag,Agenda item,Tdoc,Title,Source,Type,Spec,"CR #
	 * or LS link",rev,cat,Release,SI/WI,further information,related Tdoc
	 */
	private static final int DECISION_COLUMN = 1;
	private static final int AGENDA_ITEM_COLUMN = 3;
	private static final int TDOC_COLUMN = 4;
	private static final int TITLE_COLUMN = 5;
	private static final int SOURCE_COLUMN = 6;
	private static final int TYPE_COLUMN = 7;
	private static final int LS_SOURCE_COLUMN = 9;
	private static final int WI_COLUMN = 13;
	private static final int COMMENT_COLUMN = 14;
	private static final int REV_OF_COLUMN = 15;

	private static final Pattern CSV_FIELD_PATTERN = Pattern
			.compile(",(?:\"((?:[^\"]|\"\")*)\"|([^,]*))");

	private static final Pattern ENTRY_PATTERN = Pattern.compile("^(Yes|No)"
			+ "(?:,(?:\"((?:[^\"]|\"\")*)\"|([^,]*))){16}"); // 17 fields pr
																// line

	StringBuilder lineBuilder = new StringBuilder();
	boolean oddQuotes = false;

	private final DataAccessObject db;
	private final String meeting;
	 private String table = Configuration.getTables()[0];

	private String ftpPrefix = "ftp://ftp.3gpp.org/tsg_ran/TSG_RAN/TSGR_61/Docs/";
//	private String ftpPrefix = Configuration.getFtpPrefix();

	/**
	 * Constructor
	 * 
	 * @param ui
	 *            the {@link UI} instance that has all the contextual
	 *            information needed.
	 * 
	 */
	public RanPageHandler_61(UI ui) {
		this.db = ui.getDb();
		this.meeting = ui.getMeeting();

	}


	/**
	 * Handles one line read from the page.
	 * 
	 * @param line
	 * @throws MalformedURLException
	 */
	public void readLine(String line) throws MalformedURLException {
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
		Matcher lineMatcher = ENTRY_PATTERN.matcher(line);
		Matcher fieldMatcher = CSV_FIELD_PATTERN.matcher(line);
		if (lineMatcher.matches()) {
			String[] fields = new String[16];
			String field;
			for (int i = 0; fieldMatcher.find(); i++) {
				if ((field = fieldMatcher.group(1)) != null) {
					field = field.replace("\"\"", "\"");
				} else {
					field = fieldMatcher.group(2);
				}
				fields[i] = field;
			}

			String decision = fields[DECISION_COLUMN];
			String agendaItem = fields[AGENDA_ITEM_COLUMN];
			String agendaTitle = fields[AGENDA_ITEM_COLUMN];
			String url = ftpPrefix + fields[TDOC_COLUMN] + ".zip";
			String tDoc = fields[TDOC_COLUMN];
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
			// System.out.println(Arrays.toString(fields));
			DocumentObject doc;
			try {
				doc = new DocumentObject(-1, meeting, agendaItem, agendaTitle,
						url, tDoc, docType, docTitle, source, workItem, "", "",
						lsSource, comment, decision, "");
				 List<DocEntry> entries = db.findEntries(table,
				 doc.getTDoc());
				 if (entries.isEmpty()) {
				 db.saveRecord(table, doc);
				 } else {
				 DocEntry entry = entries.get(0);
				
				 db.mergeRecord(table, entry.getId(), doc);
				 }
//				System.out.println(doc);
			} catch (MalformedURLException ex) {
				Synchronizer.LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}

}
