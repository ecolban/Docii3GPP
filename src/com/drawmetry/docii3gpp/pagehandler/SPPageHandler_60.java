package com.drawmetry.docii3gpp.pagehandler;

import com.drawmetry.docii3gpp.database.DataAccessObject;
import com.drawmetry.docii3gpp.Configuration;
import com.drawmetry.docii3gpp.DocEntry;
import com.drawmetry.docii3gpp.DocumentObject;
import com.drawmetry.docii3gpp.UI;

import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class SPPageHandler_60 implements PageHandler {

	private static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	private static final Pattern TDOC_PATTERN = Pattern
			.compile("(<a .* href=\"(.*\\.zip)\">)?([CGRS][1-5P]-\\d{6})(</a>)?");

	private static final String[] ENTRY_STRINGS = new String[] {
	/*
		 */
	"\\s*<TR VALIGN=TOP>", // ;
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Agenda Item
			// "\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Agenda Item Title
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // TDoc #
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Doc title
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Source
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Summary
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Doc type
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Doc for
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Decision
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Rev_by
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Rev_of
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // LS_source_file
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Comment
			"\\s*<TD .*><FONT [^>]*>(.*)</FONT></TD>", // Rx
			"\\s*</TR>" };
	private static final Pattern[] entryPattern = new Pattern[ENTRY_STRINGS.length];

	private static final String AGENDA_ITEM = "<TD .*><FONT [^>]*>(.*)</FONT></TD>"
			+ "\\s*<TD .*><FONT [^>]*>(.)</FONT></TD>"
			+ "\\s*<TD .*><FONT [^>]*></FONT></TD>";

	static {
		for (int i = 0; i < ENTRY_STRINGS.length; i++) {
			entryPattern[i] = Pattern.compile(ENTRY_STRINGS[i]);
		}
	}
	private int patternIndex = 0;
	private String agendaItem;
	// private String agendaTitle;
	private String tDoc;
	private String url;
	private String docTitle;
	private String source;
	private String docType;
	private String decision;
	// private String workItem;
	private String revByTDoc;
	private String revOfTDoc;
	private String lsSource;
	private String comment;
	private final String table;

	private final DataAccessObject db;

	private final String meeting;

	private final String ftpPrefix;

	/**
	 * Constructor
	 * 
	 * @param ui
	 *            the {@link UI} instance that has all the contextual
	 *            information needed.
	 * 
	 */
	public SPPageHandler_60(String meeting) {
		this.db = DataAccessObject.getInstance();
		this.meeting = meeting;
		this.table = Configuration.getTables()[0];
		this.ftpPrefix = Configuration.getFtpPrefix(meeting);

	}

	/**
	 * Handles one line read from the page.
	 * 
	 * @param line
	 * @throws MalformedURLException
	 */
	public void processLine(String line) throws MalformedURLException {

		Matcher matcher = entryPattern[patternIndex].matcher(line);
		switch (patternIndex) {
		case 0:
			if (matcher.matches()) {
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 1:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Agenda Item
			if (matcher.matches()) {
				agendaItem = matcher.group(1);
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 2:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //TDoc #
			if (matcher.matches()) {
				String tmp = matcher.group(1);
				Matcher tdocMatcher = TDOC_PATTERN.matcher(tmp);
				if (tdocMatcher.matches()) {
					// url = tdocMatcher.group(2);

					tDoc = tdocMatcher.group(3);
					url = ftpPrefix + tDoc + ".zip";
				} else {
					url = "";
					tDoc = "";
				}
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 3:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //TDoc Title
			if (matcher.matches()) {
				docTitle = matcher.group(1);
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 4:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", // Source
			if (matcher.matches()) {
				source = matcher.group(1);
				patternIndex++;
				break;
			} else {
				patternIndex = 0;
			}
			break;
		case 5:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Summary
			if (matcher.matches()) {
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 6:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Doc title
			if (matcher.matches()) {
				docType = matcher.group(1);
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 7:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Doc for
			if (matcher.matches()) {
				patternIndex++;
				break;
			} else {
				patternIndex = 0;
			}
			break;
		case 8:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Decision
			if (matcher.matches()) {
				decision = matcher.group(1);
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 9:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Rev_by
			if (matcher.matches()) {
				Matcher tdocMatcher = TDOC_PATTERN.matcher(matcher.group(1));
				if (tdocMatcher.matches()) {
					revByTDoc = tdocMatcher.group(3);
				} else {
					revByTDoc = "";
				}
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 10:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Rev_of
			if (matcher.matches()) {
				Matcher tdocMatcher = TDOC_PATTERN.matcher(matcher.group(1));
				if (tdocMatcher.matches()) {
					revOfTDoc = tdocMatcher.group(3);
				} else {
					revOfTDoc = "";
				}
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 11:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //LS_source_file
			if (matcher.matches()) {
				lsSource = matcher.group(1);
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 12:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Comment
			if (matcher.matches()) {
				comment = matcher.group(1);
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 13:
			// "<TD .*><FONT .*>(.*)</FONT></TD>", //Rx
			if (matcher.matches()) {
				patternIndex++;
			} else {
				patternIndex = 0;
			}
			break;
		case 14:
			if (matcher.matches()) {
				try {
					DocumentObject doc = new DocumentObject(-1, meeting,
							agendaItem, agendaItem, url, tDoc, docType,
							docTitle, source, "", revByTDoc, revOfTDoc,
							lsSource, comment, decision, "");
					List<DocEntry> entries = db.findEntries(table,
							doc.getTDoc());
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
			patternIndex = 0;
			break;
		default:
		}
	}

}
