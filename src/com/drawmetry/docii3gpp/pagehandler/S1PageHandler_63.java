package com.drawmetry.docii3gpp.pagehandler;

import com.drawmetry.docii3gpp.database.DataAccessObject;
import com.drawmetry.docii3gpp.Configuration;
import com.drawmetry.docii3gpp.DocEntry;
import com.drawmetry.docii3gpp.DocumentObject;
import com.drawmetry.docii3gpp.UI;
import com.sun.xml.internal.messaging.saaj.packaging.mime.internet.ParseException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser of the HTML file (SA1_63_tdoc_listing.htm) containing all document
 * information that was generated at SA1 meeting 63. Same format used in later
 * meetings.
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class S1PageHandler_63 implements PageHandler {

	private static final Logger LOGGER = Logger
			.getLogger("com.drawmetry.docii3gpp");

	private static final Pattern TDOC_PATTERN = Pattern
			.compile("(?:<a .* href=\"(.*\\.zip)\">)?([CGRS][1-5P]-\\d{6})(</a>)");
	private static final Pattern CHAR_PATTERN = Pattern
			.compile(">(.+?)<");
	private static final Pattern TR_START_PATTERN = Pattern
			.compile("^\\s*<tr.*>\\s*$");
	private static final Pattern TD_START_PATTERN = Pattern
			.compile("^\\s*<td .*$");
	private static final Pattern TD_END_PATTERN = Pattern
			.compile("^\\s*</td>\\s*$");
	private static final Pattern TR_END_PATTERN = Pattern
			.compile("^\\s*</tr>\\s*$");
	private static final Pattern P_PATTERN = Pattern
			.compile("^\\s*<p.*?>.*</p>\\s*$");
	private static final Pattern P_START_PATTERN = Pattern
			.compile("^\\s*<p.*$");
	private static final Pattern P_END_PATTERN = Pattern
			.compile("^.*</p>\\s*$");

	private enum State {
		OUTSIDE_TR, INSIDE_TR, INSIDE_TD, INSIDE_P
	}

	private StringBuilder lineBuilder = new StringBuilder();

	State currentState = State.OUTSIDE_TR;

	private String agendaItem = "";
	private String agendaTitle = "";
	private String url = "";
	private String tDoc = "";
	private String docType = "";
	private String docTitle = "";
	private String source = "";
	private String workItem = "";
	private String revByTDoc = "";
	private String revOfTDoc = "";
	private String lsSource = "";
	private String comment = "";
	private String decision = "";
	private String table = "";

	private final DataAccessObject db;

	private final String meeting;

	private final String ftpPrefix;

	private boolean docEntrymatch;
	private int tdCount = 0;
	private int pCount;

	/**
	 * Constructor
	 * 
	 * @param ui
	 *            the {@link UI} instance that has all the contextual
	 *            information needed.
	 * 
	 */
	public S1PageHandler_63(String meeting) {
		this.db = DataAccessObject.getInstance();
		this.meeting = meeting;
		this.table = Configuration.getTables()[0];
		this.ftpPrefix = Configuration.getFtpPrefix(meeting);

	}

	public S1PageHandler_63() {
		this.db = null;
		this.meeting = "S1-63";
		this.table = null;
		this.ftpPrefix = "ftp://ftp.3gpp.org/tsg_sa/WG1_Serv/TSGS1_63_Zagreb/docs/";
	}

	/**
	 * Handles one line read from the page.
	 * 
	 * @param line
	 * @throws MalformedURLException
	 */
	public void processLine(String line) throws MalformedURLException {
		switch (currentState) {
		case OUTSIDE_TR:
			if (TR_START_PATTERN.matcher(line).matches()) {
				currentState = State.INSIDE_TR;
				tdCount = 0;
				resetEntry();
				docEntrymatch = true;
			}
			break;
		case INSIDE_TR:
			if (TD_START_PATTERN.matcher(line).matches()) {
				pCount = 0;
				currentState = State.INSIDE_TD;
			} else if (TR_END_PATTERN.matcher(line).matches()) {
				docEntrymatch &= tdCount == 6;
				if (docEntrymatch) {
					saveDocData();
				}
				currentState = State.OUTSIDE_TR;
			}
			break;
		case INSIDE_TD:
			if (P_START_PATTERN.matcher(line).matches()) {
				currentState = State.INSIDE_P;
				lineBuilder.setLength(0);
				lineBuilder.append(line);
			}
			if (P_END_PATTERN.matcher(line).matches()) {
				try {
					processP();
				} catch (ParseException e) {
					System.out.println("STOP");
				}
				pCount++;
				currentState = State.INSIDE_TD;
			} else if (TD_END_PATTERN.matcher(line).matches()) {
				tdCount++;
				currentState = State.INSIDE_TR;
			}
			break;
		case INSIDE_P:
			lineBuilder.append(line);
			if (P_END_PATTERN.matcher(line).matches()) {
				try {
					processP();
				} catch (ParseException e) {
					System.out.println("STOP");
				}
				pCount++;
				currentState = State.INSIDE_TD;
			}
			break;
		default:

		}
	}

	private void resetEntry() {
		agendaTitle = "";
		tDoc = "";
		source = "";
		docTitle = "";
		decision = "";
		comment = "";

	}

	private void processP() throws ParseException {
		String pLine = lineBuilder.toString();
		pLine = filter(pLine);
		Matcher m = P_PATTERN.matcher(pLine);
		String data = "";
		if (m.matches()) {
			m = CHAR_PATTERN.matcher(pLine);
			while(m.find()) {
				data += m.group(1);
			}
			data = data.replaceAll("&nbsp;", "");
			data = data.replaceAll("&quot;", "\"");
			data = data.replaceAll("&amp;", "&");
		} else {
			throw new ParseException();
		}
		if (pCount > 0) {
			data = "\n" + data;
		}
		if (tdCount == 0) { // Agenda Item
			agendaTitle += data;
		} else if (tdCount == 1) { // TDoc column
			m = TDOC_PATTERN.matcher(pLine);
			if (m.find()) {
				tDoc = m.group(2);
				url = ftpPrefix + tDoc + ".zip";
				if(tDoc.equals("S1-135014")){
					System.out.println("STOP");
				}
			} else {
				docEntrymatch = false;
			}
		} else if (tdCount == 2) {// source
			source += data;
		} else if (tdCount == 3) { // title
			docTitle += data;
		} else if (tdCount == 4) {// decision
			decision += data;
		} else if (tdCount == 5) {// comments
			comment += data;
		}

	}

	private String filter(String pLine) {
		pLine = pLine.replaceAll("<!--.*?-->", "");
		return pLine.replaceAll("</?span.*?>", "");
		
	}

	private void saveDocData() {
		try {
			DocumentObject doc = new DocumentObject(-1, meeting, agendaItem,
					agendaTitle, url, tDoc, docType, docTitle, source,
					workItem, revByTDoc, revOfTDoc, lsSource, comment,
					decision, "");
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

//	private void saveDocDataTest() {
//		try {
//			DocumentObject doc = new DocumentObject(-1, meeting, agendaItem,
//					agendaTitle, url, tDoc, docType, docTitle, source,
//					workItem, revByTDoc, revOfTDoc, lsSource, comment,
//					decision, "");
//			System.out.println(doc);
//		} catch (MalformedURLException ex) {
//			LOGGER.log(Level.SEVERE, null, ex);
//		}
//	}

	public static void main(String[] args) {
		URL url = S1PageHandler_63.class
				.getResource("../files/SA1_63_tdoc_listing.htm");
		S1PageHandler_63 handler = new S1PageHandler_63();
		BufferedReader reader = null;
		try {
			File input = new File(url.toURI());
			reader = new BufferedReader(new FileReader(input));
			String line = null;
			while ((line = reader.readLine()) != null) {
				handler.processLine(line);
			}
		} catch (URISyntaxException e) {
			return;
		} catch (FileNotFoundException e) {
			return;
		} catch (IOException e) {
			return;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					return;
				}
			}
		}
	}
}
