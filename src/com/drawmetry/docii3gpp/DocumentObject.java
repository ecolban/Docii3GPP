package com.drawmetry.docii3gpp;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * Object representation of an entry in the DB table.
 * 
 * @author Erik
 */
public class DocumentObject {

	public static final Pattern FILE_NAME_PATTERN = Pattern
			.compile("([CGRS][P1-5])-(\\d{2})(\\d{4})");
	public static final int FILE_PREFIX = 1;
	public static final int WG = 2;
	public static final int YR_DCN = 3;
	public static final int YEAR = 4;
	public static final int DCN = 5;
	public static final int REV = 6;
	public static final int GROUP_CODE = 7;
	public static final int REST = 8;
	private int id;
	private String meeting;
	private String agendaItem;
	private String agendaTitle;
	private URL url;
	private String tDoc;
	private String docType;
	private String docTitle;
	private String source;
	private String workItem;
	private String revByTDoc;
	private URL revByUrl;
	private String revOfTDoc;
	private URL revOfUrl;
	private String lsSource;
	private String comment;
	private String decision;
	private String notes;
	private static final DateFormat mentorDateFormat = new SimpleDateFormat(
			"dd-MMM-yyyy HH:mm:ss", Locale.US);

	static {
		mentorDateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
	}

	/**
	 * Constructor
	 * 
	 * @param id
	 * @param meeting
	 * @param agendaItem
	 * @param agendaTitle
	 * @param url
	 * @param tDoc
	 * @param docType
	 * @param docTitle
	 * @param source
	 * @param workItem
	 * @param revByTDoc
	 * @param revOfTDoc
	 * @param lsSource
	 * @param comment
	 * @param decision
	 * @param notes
	 * @throws MalformedURLException
	 */
	public DocumentObject(int id, String meeting, String agendaItem, String agendaTitle,
			String url, String tDoc, String docType, String docTitle,
			String source, String workItem, String revByTDoc, String revOfTDoc,
			String lsSource, String comment, String decision, String notes) throws MalformedURLException {
		this.id = id;
		this.meeting = meeting;
		this.agendaItem = agendaItem;
		this.agendaTitle = agendaTitle;
		if (url != null && !url.isEmpty()) {
			this.url = new URL(url);
		} else {
			this.url = null;
		}
		this.tDoc = tDoc;
		this.docType = docType;
		this.docTitle = docTitle;
		this.source = source;
		this.workItem = workItem;
		this.revByTDoc = revByTDoc;
		this.revOfTDoc = revOfTDoc;
		this.lsSource = lsSource;
		this.comment = comment;
		this.decision = decision;
		this.notes = notes;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final DocumentObject other = (DocumentObject) obj;
		if (this.url != other.url
				&& (this.url == null || !this.url.equals(other.url))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 23 * hash + (this.url != null ? this.url.hashCode() : 0);
		return hash;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("--------------------------\n");
		sb.append("ID = ");
		sb.append(id);
		sb.append("\nMEETING = ");
		sb.append(meeting);
		sb.append("\nAGENDA_ITEM = ");
		sb.append(agendaItem);
		sb.append("\nAGENDA_TITLE = ");
		sb.append(agendaTitle);
		sb.append("\nURL = ");
		if (url != null) {
			sb.append(url.toString());
		}
		sb.append("\nTDOC = ");
		sb.append(tDoc);
		sb.append("\nDOC_TYPE = ");
		sb.append(docType);
		sb.append("\nDOC_TITLE = ");
		sb.append(docTitle);
		sb.append("\nSOURCE = ");
		sb.append(source);
		sb.append("\nWI = ");
		sb.append(workItem);
		sb.append("\nREV_BY = ");
		sb.append(revByTDoc);
		sb.append("\nREV_BY_URL = ");
		if (revByUrl != null) {
			sb.append(revByUrl.toString());
		}
		sb.append("\nREV_OF = ");
		sb.append(revOfTDoc);
		sb.append("\nREV_OF_URL = ");
		if (revOfUrl != null) {
			sb.append(revOfUrl.toString());
		}
		sb.append("\nLS_SRC = ");
		sb.append(lsSource);
		sb.append("\nCOMMENT = ");
		sb.append(comment);
		sb.append("\nDECISION = ");
		sb.append(decision);
		sb.append("\nNOTES = ");
		sb.append(notes);
		sb.append("\n--------------------------");
		return sb.toString();
	}

	int getId() {
		return id;
	}

	void setId(int id) {
		this.id = id;
	}
	
	public String getMeeting() {
		return meeting;
	}
	
	public void setMeeting(String meeting) {
		this.meeting = meeting;
	}
	
	public String getAgendaItem() {
		return agendaItem;
	}

	public void setAgendaItem(String agendaItem) {
		this.agendaItem = agendaItem;
	}

	public String getAgendaTitle() {
		return agendaTitle;
	}

	public void setAgendaTitle(String agendaTitle) {
		this.agendaTitle = agendaTitle;
	}

	public URL getUrl() {
		
		return url;
	}

	public void setUrl(URL url) {
		this.url = url;
	}

	public String getTDoc() {
		return tDoc;
	}

	public void setTDoc(String tDoc) {
		this.tDoc = tDoc;
	}

	public String getDocType() {
		return docType;
	}

	public void setDocType(String docType) {
		this.docType = docType;
	}

	public String getDocTitle() {
		return docTitle;
	}

	public void setDocTitle(String docTitle) {
		this.docTitle = docTitle;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getWorkItem() {
		return workItem;
	}

	public void setWorkItem(String workItem) {
		this.workItem = workItem;
	}

	public String getRevByTDoc() {
		return revByTDoc;
	}

	public void setRevByTDoc(String revByTDoc) {
		this.revByTDoc = revByTDoc;
	}

	public URL getRevByUrl() {
		return revByUrl;
	}

	public void setRevByUrl(URL revByUrl) {
		this.revByUrl = revByUrl;
	}

	public String getRevOfTDoc() {
		return revOfTDoc;
	}

	public void setRevOfTDoc(String revOfTDoc) {
		this.revOfTDoc = revOfTDoc;
	}

	public URL getRevOfUrl() {
		return revOfUrl;
	}

	public void setRevOfUrl(URL revOfUrl) {
		this.revOfUrl = revOfUrl;
	}

	public String getLsSource() {
		return lsSource;
	}

	public void setLsSource(String lsSource) {
		this.lsSource = lsSource;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getDecision() {
		return decision;
	}

	public void setDecision(String decision) {
		this.decision = decision;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

}
