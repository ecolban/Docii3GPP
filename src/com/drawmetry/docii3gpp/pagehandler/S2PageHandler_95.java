package com.drawmetry.docii3gpp.pagehandler;

import com.drawmetry.docii3gpp.database.DataAccessObject;
import com.drawmetry.docii3gpp.Configuration;
import com.drawmetry.docii3gpp.DocEntry;
import com.drawmetry.docii3gpp.DocumentObject;
import com.drawmetry.docii3gpp.UI;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser of the HTML file (ftp-TDListByTD_S2-95.htm) containing all document
 * information that 3GPP SA2 MCC generated at SA2 meeting 95.
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class S2PageHandler_95 implements PageHandler {
	
	private static final Logger LOGGER = Logger.getLogger("com.drawmetry.docii3gpp");

	private static final Pattern TDOC_PATTERN = Pattern
			.compile("(<a .* href=\"(.*\\.zip)\">)?([CGRS][1-5P]-\\d{6})(</a>)?");

	private static final String[] ENTRY_STRINGS = new String[] {
			"\\s*<TR VALIGN=TOP>", // ;
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Agenda Item
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Agenda Item Title
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // TDoc #
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Doc type
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Doc title
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Source
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Doc type (duplicate)
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // WI
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Rev_by
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Rev_of
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // LS_source_file
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Comment
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Decision
			"\\s*</TR>" };

	private static final Pattern[] ENTRY_PATTERNS = new Pattern[ENTRY_STRINGS.length];

	static {
		for (int i = 0; i < ENTRY_STRINGS.length; i++) {
			ENTRY_PATTERNS[i] = Pattern.compile(ENTRY_STRINGS[i]);
		}
	}
	private int patternIndex = 0;
	private String agendaItem;
	private String agendaTitle;
	private String url;
	private String tDoc;
	private String docType;
	private String docTitle;
	private String source;
	private String workItem;
	private String revByTDoc;
	private String revOfTDoc;
	private String lsSource;
	private String comment;
	private String decision;
	private String table;

	private final DataAccessObject db;

	private final String meeting;

	/**
	 * Constructor
	 * 
	 * @param ui
	 *            the {@link UI} instance that has all the contextual
	 *            information needed.
	 * 
	 */
	public S2PageHandler_95(String meeting) {
		this.db = DataAccessObject.getInstance();
		this.meeting = meeting;
		this.table = Configuration.getTables()[0];
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

		Matcher matcher = ENTRY_PATTERNS[patternIndex].matcher(line);
		if (!matcher.matches()) {
			patternIndex = 0;
			return;
		}
		switch (patternIndex) {
		case 0:
			// "\\s*<TR VALIGN=TOP>", // ;
			patternIndex++;
			break;
		case 1:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Agenda Item
			agendaItem = matcher.group(1);
			patternIndex++;
			break;
		case 2:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Agenda Item Title
			agendaTitle = matcher.group(1);
			patternIndex++;
			break;

		case 3:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //TDoc #
			String tmp = matcher.group(1);
			Matcher tdocMatcher = TDOC_PATTERN.matcher(tmp);
			if (tdocMatcher.matches()) {
				url = tdocMatcher.group(2);
				tDoc = tdocMatcher.group(3);
			} else {
				url = "";
				tDoc = "";
			}
			patternIndex++;
			break;
		case 4:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Doc type
			docType = matcher.group(1);
			patternIndex++;
			break;
		case 5:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Doc title
			docTitle = matcher.group(1);
			patternIndex++;
			break;
		case 6:
			// 6 "<TD .*><FONT .*>(.*)</FONT></TD>", //Source
			source = matcher.group(1);
			patternIndex++;
			break;
		case 7:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Doc type
			// docType = matcher.group(1);
			patternIndex++;
			break;
		case 8:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //WI
			workItem = matcher.group(1);
			patternIndex++;
			break;
		case 9:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Rev_by
			tdocMatcher = TDOC_PATTERN.matcher(matcher.group(1));
			if (tdocMatcher.matches()) {
				revByTDoc = tdocMatcher.group(3);
			} else {
				revByTDoc = "";
			}
			patternIndex++;
			break;
		case 10:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Rev_of
			tdocMatcher = TDOC_PATTERN.matcher(matcher.group(1));
			if (tdocMatcher.matches()) {
				revOfTDoc = tdocMatcher.group(3);
			} else {
				revOfTDoc = "";
			}
			patternIndex++;
			break;
		case 11:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //LS_source_file
			lsSource = matcher.group(1);
			patternIndex++;
			break;
		case 12:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Comment
			comment = matcher.group(1);
			patternIndex++;
			break;
		case 13:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Decision
			decision = matcher.group(1);
			patternIndex++;
			break;
		case 14:
			try {
				DocumentObject doc = new DocumentObject(-1, meeting,
						agendaItem, agendaTitle, url, tDoc, docType, docTitle,
						source, workItem, revByTDoc, revOfTDoc, lsSource,
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
			patternIndex = 0;
			break;
		default:
		}
	}

}
