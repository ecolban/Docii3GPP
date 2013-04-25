package com.drawmetry.docii3gpp;

import com.drawmetry.docii3gpp.database.DataAccessObject;
import com.drawmetry.docii3gpp.DocEntry;

import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class R2PageHandler {

	private static final Pattern LINE_PATTERN = Pattern
			.compile("([^~]*)~([^~]*)~([^~]*)~([^~]*)~([^~]*)~([^~]*)~([^~]*)~([^~]*)~([^~]*)~([^~]*)~"
					+ "([^~]*)~([^~]*)~([^~]*)~([^~]*)~([^~]*)~([^~]*)~([^~]*).*");
	private static final Pattern AGENDA_PATTERN = Pattern.compile("(.*?):(.*)");
	private static final Pattern TDOC_PATTERN = Pattern.compile("R2-(\\d{6})");
	private static final Pattern QUOTE_PATTERN = Pattern.compile("\"(.*)\"");

	private String agendaItem;
	private String agendaTitle;
	private String url;
	private String tDoc;
	private int lastTDocNum = 0;
	private int lastTDocNum0 = 0;
	private String docType;
	private String docTitle;
	private String source;
	private String workItem;
	private String revByTDoc = "";
	private String revOfTDoc = "";
	private String lsSource;
	private String comment;
	private String decision;
	private String table = Configuration.getTables()[0];

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
	public R2PageHandler(UI ui) {
		this.db = ui.getDb();
		this.meeting = ui.getMeeting();
	}

	public void readLine(String line) throws MalformedURLException {
		Matcher lineMatcher = LINE_PATTERN.matcher(line);
		if (lineMatcher.matches()) {
			// Agenda item~Uploaded~status~Late~Tdoc~Title~Source~Type:~Spec~CR
			// #~rev~cat~further information~Release~SI/WI~related
			// Tdoc~comment~~~~~~~~~~~~~~~
			String agenda = lineMatcher.group(1);
			Matcher m = AGENDA_PATTERN.matcher(agenda);
			if (m.matches()) {
				agendaItem = m.group(1);
				agendaTitle = m.group(2);
			} else {
				agendaItem = "";
				agendaTitle = agenda;
			}
			decision = lineMatcher.group(3);
			tDoc = lineMatcher.group(5);
			m = TDOC_PATTERN.matcher(tDoc);
			if (m.matches()) {
				lastTDocNum = Integer.parseInt(m.group(1));
			} else {
				return;
			}
			docTitle = lineMatcher.group(6);
			m = QUOTE_PATTERN.matcher(docTitle);
			if(m.matches()) {
				docTitle = m.group(1);
			}
			source = lineMatcher.group(7);
			m = QUOTE_PATTERN.matcher(source);
			if(m.matches()) {
				source = m.group(1);
			}
			docType = lineMatcher.group(8);
			workItem = lineMatcher.group(15);
			m = QUOTE_PATTERN.matcher(workItem);
			if(m.matches()) {
				workItem = m.group(1);
			}
			comment = lineMatcher.group(17);
			m = QUOTE_PATTERN.matcher(comment);
			if(m.matches()) {
				comment = m.group(1);
			}

			try {
				DocumentObject doc = new DocumentObject(-1, meeting,
						agendaItem, agendaTitle, url, tDoc, docType, docTitle,
						source, workItem, revOfTDoc, revByTDoc, lsSource, comment, decision,
						"");
				List<DocEntry> entries = db.findEntries(table, doc.getTDoc());
				// System.out.println(doc);
				if (lastTDocNum0 + 1 != lastTDocNum) {
					System.out.println(lastTDocNum0 + " - " + lastTDocNum);
				}
				lastTDocNum0 = lastTDocNum;
				if (entries.isEmpty()) {
					db.saveRecord(table, doc);
				} else {
					DocEntry entry = entries.get(0);
					db.mergeRecord(table, entry.getId(), doc);
				}
			} catch (MalformedURLException ex) {
				Synchronizer.LOGGER.log(Level.SEVERE, null, ex);
			}
		}
	}
}
