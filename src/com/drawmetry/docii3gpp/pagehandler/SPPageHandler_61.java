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
public class SPPageHandler_61 implements PageHandler {

	private static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	/*
	 * 0:MEETING, 1:TDOC, 2:TITLE, 3:SOURCE, 4:AGENDA ITEM, 5:Document For,
	 * 6:REPLACED BY, 7:Available, 8:Doc Type, 9:Orig TD #,10: , 11:Result
	 * Summary, 12:Contribution Summary / Further comments
	 */
	private static final int DECISION_COLUMN = 11;
	private static final int AGENDA_ITEM_COLUMN = 4;
	private static final int TDOC_COLUMN = 1;
	private static final int TITLE_COLUMN = 2;
	private static final int SOURCE_COLUMN = 3;
	private static final int TYPE_COLUMN = 8;
	private static final int LS_SOURCE_COLUMN = 9;
	// private static final int WI_COLUMN = 13;
	private static final int COMMENT_COLUMN = 12;
	private static final int REV_BY_COLUMN = 6;
	// private static final int REV_OF_COLUMN = 15;

	private static final String CSV_FIELD_REGEX = "(?:\"((?:[^\"]|\"\")*)\"|([^,]*))";
	private static final Pattern COMMA_CSV_FIELD_PATTERN = Pattern
			.compile("," + CSV_FIELD_REGEX);

	private static final Pattern LINE_PATTERN = Pattern.compile(CSV_FIELD_REGEX
			+ "(?:," + CSV_FIELD_REGEX + "){12}");

	StringBuilder lineBuilder = new StringBuilder();
	boolean oddQuotes = false;

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
	public SPPageHandler_61(String meeting) {
		this.table = Configuration.getTables()[0];
		this.meeting = meeting;
		this.ftpPrefix = Configuration.getFtpPrefix(meeting);
		this.db = DataAccessObject.getInstance();

	}

	private SPPageHandler_61() {
		this.table = null;
		this.ftpPrefix = "ftp://ftp.3gpp.org/tsg_sa/TSG_SA/TSGS_61/Docs/";
		this.db = null;
		this.meeting = "SP-61";

	}

	public void processInput(BufferedReader input) throws MalformedURLException, IOException {
		String line = null;
		while ((line = input.readLine()) != null) {
			processLine(line);
		}
	}

	/**
	 * Handles one line read from the page.
	 * 
	 * @param line
	 * @throws MalformedURLException
	 */
	private void processLine(String line) throws MalformedURLException {
		lineBuilder.append(line);
		if (oddQuotes ^= oddQuotes(line)) { // entry spans multiple lines
			lineBuilder.append(" ");
			return;
		}
		String line1 = lineBuilder.toString();
		Matcher lineMatcher = LINE_PATTERN.matcher(line1);
		if (lineMatcher.lookingAt()) {
			line1 = line1.substring(lineMatcher.start(), lineMatcher.end());
			Matcher fieldMatcher = COMMA_CSV_FIELD_PATTERN.matcher(line1);
			String[] fields = new String[16];
			String field;
			for (int i = 1; fieldMatcher.find(); i++) {
				if ((field = fieldMatcher.group(1)) != null) {
					field = field.replace("\"\"", "\""); // replace "" with "
				} else {
					field = fieldMatcher.group(2);
				}
				fields[i] = field;
			}

			String decision = fields[DECISION_COLUMN];
			String agendaTitle = fields[AGENDA_ITEM_COLUMN];
			String url = ftpPrefix + fields[TDOC_COLUMN] + ".zip";
			String tDoc = fields[TDOC_COLUMN];
			String docTitle = fields[TITLE_COLUMN];
			String source = fields[SOURCE_COLUMN];
			String docType = fields[TYPE_COLUMN];
			String lsSource = fields[LS_SOURCE_COLUMN];
//			String workItem = fields[WI_COLUMN];
			String comment = fields[COMMENT_COLUMN];
			String revBy = fields[REV_BY_COLUMN];
			DocumentObject doc;
			try {
				doc = new DocumentObject(-1, meeting, "", agendaTitle, url,
						tDoc, docType, docTitle, source, "", revBy, "",
						lsSource, comment, decision, "");
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
		lineBuilder.setLength(0);
	}

	private boolean oddQuotes(String line) {
		boolean result = false;
		for (int i = 0; i < line.length(); i++) {
			result ^= line.charAt(i) == '\"';
		}
		return result;
	}

	/**
	 * Used for test purposes only
	 * 
	 * @param args
	 *            ignored
	 */
	public static void main(String[] args) {
		String line = "SA_61,SP-130334,Draft Prioritization Input to TSG SA#61,SA WG2 Chairman,5.2,Discussion,,YES,DISCUSSION,,,,\"At TSG SA#61, TSG SA will review SA WG2 progress and make any decisions necessary to ensure success of approved work and study items. Time Budget vs. Available Time is highlighted as well as updated information regarding the status of work items, if relevant to work prioritization.\",ALLOCATED DOCS SA_61,125,,,";
		SPPageHandler_61 handler = new SPPageHandler_61();
		Matcher lineMatcher = LINE_PATTERN.matcher(line);
		if (lineMatcher.lookingAt()) {
			line = line.substring(lineMatcher.start(), lineMatcher.end());
			Matcher fieldMatcher = COMMA_CSV_FIELD_PATTERN.matcher(line);
			String[] fields = new String[13];
			String field;
			for (int i = 1; fieldMatcher.find(); i++) {
				if ((field = fieldMatcher.group(1)) != null) {
					field = field.replace("\"\"", "\"");
				} else {
					field = fieldMatcher.group(2);
				}
				fields[i] = field;
			}

			String decision = fields[DECISION_COLUMN];
			String agendaTitle = fields[AGENDA_ITEM_COLUMN];
			String url = handler.ftpPrefix + fields[TDOC_COLUMN] + ".zip";
			String tDoc = fields[TDOC_COLUMN];
			String docTitle = fields[TITLE_COLUMN];
			String source = fields[SOURCE_COLUMN];
			String docType = fields[TYPE_COLUMN];
			String lsSource = fields[LS_SOURCE_COLUMN];
//			String workItem = fields[WI_COLUMN];
			String comment = fields[COMMENT_COLUMN];
			String revBy = fields[REV_BY_COLUMN];
			// System.out.println(Arrays.toString(fields));
			DocumentObject doc;
			try {
				doc = new DocumentObject(-1, handler.meeting, agendaTitle,
						agendaTitle, url, tDoc, docType, docTitle, source,
						"", revBy, "", lsSource, comment, decision, "");
				System.out.println(doc);
			} catch (MalformedURLException ex) {
				LOGGER.log(Level.SEVERE, ex.getMessage());
			}
		}
	}

}
