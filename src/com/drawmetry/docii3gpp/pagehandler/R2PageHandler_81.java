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

	private static final String CSV_FIELD_REGEXP = "(?:\"((?:[^\"]|\"\")*)\"|([^,]*))";
	private static final Pattern COMMA_CSV_FIELD_PATTERN = Pattern.compile(","
			+ CSV_FIELD_REGEXP);

	private static final Pattern LINE_PATTERN = Pattern.compile(//
			"^" + CSV_FIELD_REGEXP + ",(Yes|No)(," + CSV_FIELD_REGEXP + "){15}");

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

	/**
	 * Used for test purposes only
	 */
	private R2PageHandler_81() {
		this.table = null;
		this.ftpPrefix = "ftp://ftp.3gpp.org/tsg_ran/WG2_RL2/TSGR2_81/Docs/";
		db = null;
		meeting = "R2-81";
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
			String[] fields = new String[17];
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

			String decision = fields[DECISION_COLUMN];
			String agendaTitle = fields[AGENDA_ITEM_COLUMN];
			String url = ftpPrefix + fields[TDOC_COLUMN] + ".zip";
			String tDoc = fields[TDOC_COLUMN];
			String docTitle = fields[TITLE_COLUMN];
			String source = fields[SOURCE_COLUMN];
			String docType = fields[TYPE_COLUMN];
			String lsSource = fields[LS_SOURCE_COLUMN];
			String workItem = fields[WI_COLUMN];
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
				// System.out.println(doc);
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
		String line = "\"03.2: LSin: LTE, relevance\",No,noted,,R2-130003,"
				+ "LS on UE capability for the joint operation of downlink CoMP and CA (R1-125392; contact: Huawei),"
				+ "RAN1,LSin,,,,,"
				+ "to: RAN2; received on Fri of RAN2 #80 as R2-126113 and not treated there but taken into account in email discussion [80#14]; no LS answer,REL-11,COMP_LTE_DL-Core,"
				+ "\"R1-125392, R2-126113\",,,,,,,,,,,,,,,,";
		R2PageHandler_81 handler = new R2PageHandler_81();
		handler.processEntry(line);

	}
}
