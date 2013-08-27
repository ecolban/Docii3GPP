package com.drawmetry.docii3gpp.pagehandler;

import com.drawmetry.docii3gpp.Configuration;
import com.drawmetry.docii3gpp.DocEntry;
import com.drawmetry.docii3gpp.DocumentObject;
import com.drawmetry.docii3gpp.Synchronizer;
import com.drawmetry.docii3gpp.UI;
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
public class RPPageHandler_60 implements PageHandler {

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

	private static final String CSV_FIELD_REGEX = ",(?:\"((?:[^\"]|\"\")*)\"|([^,]*))";
	private static final Pattern CSV_FIELD_PATTERN = Pattern
			.compile(CSV_FIELD_REGEX);

	private static final Pattern LINE_PATTERN = Pattern.compile("^(Yes|No)"
			+ "(?:" + CSV_FIELD_REGEX + "){16}");

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
	public RPPageHandler_60(String meeting) {
		this.table = Configuration.getTables()[0];
		this.meeting = meeting;
		this.ftpPrefix = Configuration.getFtpPrefix(meeting);
		this.db = DataAccessObject.getInstance();

	}

	private RPPageHandler_60() {
		this.table = null;
		this.ftpPrefix = null;
		this.db = null;
		this.meeting = "RAN-60";

	}

	/**
	 * Handles one line read from the page.
	 * 
	 * @param line
	 * @throws MalformedURLException
	 */
	public void processLine(String line) throws MalformedURLException {
		lineBuilder.append(line);
		if (oddQuotes ^= oddQuotes(line)) { // entry spans multiple lines
			lineBuilder.append(" ");
			return;
		}
		String line1 = lineBuilder.toString();
		Matcher lineMatcher = LINE_PATTERN.matcher(line1);
		if (lineMatcher.lookingAt()) {
			line1 = line1.substring(lineMatcher.start(), lineMatcher.end());
			Matcher fieldMatcher = CSV_FIELD_PATTERN.matcher(line1);
			String[] fields = new String[16];
			String field;
			for (int i = 0; fieldMatcher.find(); i++) {
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
				doc = new DocumentObject(-1, meeting, "", agendaTitle, url,
						tDoc, docType, docTitle, source, workItem, "", "",
						lsSource, comment, decision, "");
				List<DocEntry> entries = db.findEntries(table, doc.getTDoc());
				if (entries.isEmpty()) {
					db.saveRecord(table, doc);
				} else {
					DocEntry entry = entries.get(0);

					db.mergeRecord(table, entry.getId(), doc);
				}
			} catch (MalformedURLException ex) {
				Synchronizer.LOGGER.log(Level.SEVERE, ex.getMessage());
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
		String line = "Yes,03.2: LSin: LTE relevance,noted,,R2-130003,"
				+ "LS on UE capability for the joint operation of downlink CoMP and CA (R1-125392; contact: Huawei),"
				+ "RAN1,LSin,,,,,"
				+ "to: RAN2; received on Fri of RAN2 #80 as R2-126113 and not treated there but taken into account in email discussion [80#14]; no LS answer,REL-11,COMP_LTE_DL-Core,"
				+ "\"R1-125392, R2-126113\",,,,,,,,,,,,,,,,";
		RPPageHandler_60 handler = new RPPageHandler_60();
		Matcher lineMatcher = RPPageHandler_60.LINE_PATTERN.matcher(line);
		if (lineMatcher.lookingAt()) {
			line = line.substring(lineMatcher.start(), lineMatcher.end());
			Matcher fieldMatcher = RPPageHandler_60.CSV_FIELD_PATTERN
					.matcher(line);
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

			String decision = fields[RPPageHandler_60.DECISION_COLUMN];
			String agendaTitle = fields[RPPageHandler_60.AGENDA_ITEM_COLUMN];
			String url = handler.ftpPrefix
					+ fields[RPPageHandler_60.TDOC_COLUMN] + ".zip";
			String tDoc = fields[RPPageHandler_60.TDOC_COLUMN];
			String docTitle = fields[RPPageHandler_60.TITLE_COLUMN];
			String source = fields[RPPageHandler_60.SOURCE_COLUMN];
			String docType = fields[RPPageHandler_60.TYPE_COLUMN];
			String lsSource = fields[RPPageHandler_60.LS_SOURCE_COLUMN];
			String workItem = fields[RPPageHandler_60.WI_COLUMN];
			String comment;
			if (fields[RPPageHandler_60.REV_OF_COLUMN].isEmpty()) {
				comment = fields[RPPageHandler_60.COMMENT_COLUMN];
			} else {
				comment = fields[RPPageHandler_60.COMMENT_COLUMN]
						+ " *** Related docs: "
						+ fields[RPPageHandler_60.REV_OF_COLUMN];
			}
			// System.out.println(Arrays.toString(fields));
			DocumentObject doc;
			try {
				doc = new DocumentObject(-1, handler.meeting, "", agendaTitle,
						url, tDoc, docType, docTitle, source, workItem, "", "",
						lsSource, comment, decision, "");
				List<DocEntry> entries = handler.db.findEntries(handler.table,
						doc.getTDoc());
				if (entries.isEmpty()) {
					handler.db.saveRecord(handler.table, doc);
				} else {
					DocEntry entry = entries.get(0);

					handler.db.mergeRecord(handler.table, entry.getId(), doc);
				}
				// System.out.println(doc);
			} catch (MalformedURLException ex) {
				Synchronizer.LOGGER.log(Level.SEVERE, ex.getMessage());
			}
		}
	}

}
