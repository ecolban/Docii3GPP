package com.drawmetry.docii3gpp;

import com.drawmetry.docii3gpp.database.DataAccessObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * This class is used to export the entries shown in the UI to an Excel file.
 * 
 * @author Erik Colban &copy; 2012 <br>
 *         All Rights Reserved Worldwide
 */
public class Exporter {
	
	

	private static final int HEADING_ROW = 0;
	private static final int TDOC_COL = 0;
	private static final int AGENDA_TITLE_COL = 1;
	private static final int TITLE_COL = 2;
	private static final int SOURCE_COL = 3;
	private static final int DECISION_COL = 4;
	private static final int NOTES_COL = 5;
//	private static final ResourceBundle messageBundle = ResourceBundle
//			.getBundle("com/drawmetry/docii3gpp/resources/MessageBundle");
	private Workbook wb;
	private DocEntry[] entries;
	private String table;
	// private final UI ui;
	private final DataAccessObject db;

	/**
	 * Constructor
	 * 
	 * @param db
	 *            the database where the entries can be looked up.
	 */
	public Exporter(UI ui) {
		// this.ui = ui;
		this.db = DataAccessObject.getInstance();
		this.table = Configuration.getTables()[0];
		this.entries = ui.getEntries();
	}

	/**
	 * Writes the entries to an Excel file
	 * 
	 * @param fileOut
	 *            the output stream
	 * 
	 * @throws IOException
	 *             if cannot write to fileOut
	 */
	public void write(FileOutputStream fileOut) throws IOException {
		this.wb = new HSSFWorkbook();
		insertDocsSheet();
		if (fileOut != null) {
			wb.write(fileOut);
		} else {
			throw new IOException("fileOut is null");
		}
	}

	private void insertDocsSheet() {
		Sheet sheet1 = wb.createSheet("docs");
		// create the heading row
		insertHeadingRow(sheet1);
		insertDataRows(sheet1, HEADING_ROW + 1);

		// Set the column widths
		sheet1.setColumnWidth(TDOC_COL, 27 * 256);
		sheet1.setColumnWidth(AGENDA_TITLE_COL, 16 * 256);
		sheet1.setColumnWidth(TITLE_COL, 40 * 256);
		sheet1.setColumnWidth(SOURCE_COL, 20 * 256);
		sheet1.setColumnWidth(DECISION_COL, 30 * 256);
		sheet1.setColumnWidth(NOTES_COL, 70 * 256);
		sheet1.createFreezePane(TDOC_COL, HEADING_ROW + 1);
	}

	private void insertHeadingRow(Sheet sheet1) {
		CellStyle headingStyle = createHeadingStyle();
		Row row = sheet1.createRow(HEADING_ROW);
		Cell documentHeading = row.createCell(TDOC_COL);
		documentHeading.setCellValue("TDoc");
		documentHeading.setCellStyle(headingStyle);
		Cell groupHeading = row.createCell(AGENDA_TITLE_COL);
		groupHeading.setCellValue("Agenda Title");
		groupHeading.setCellStyle(headingStyle);
		Cell titelHeading = row.createCell(TITLE_COL);
		titelHeading.setCellValue("Title");
		titelHeading.setCellStyle(headingStyle);
		Cell timeHeading = row.createCell(SOURCE_COL);
		timeHeading.setCellValue("Source");
		timeHeading.setCellStyle(headingStyle);
		Cell authorsHeading = row.createCell(DECISION_COL);
		authorsHeading.setCellValue("Decision");
		authorsHeading.setCellStyle(headingStyle);
		Cell notesHeading = row.createCell(NOTES_COL);
		notesHeading.setCellValue("Notes");
		notesHeading.setCellStyle(headingStyle);
	}

	private void insertDataRows(Sheet sheet1, int startRow) {
		// Create the autowrap style
		CellStyle autowrapStyle = createAutowrapStyle();
		// create the hlink style
		CellStyle hlinkStyle = createHlinkStyle();
		// Data rows
		CreationHelper createHelper = wb.getCreationHelper();
		for (int i = 0, length = entries.length; i < length; i++) {
			Row row = sheet1.createRow(i + startRow);
			DocumentObject docObj = db.getDocumentOject(table,
					entries[i].getId());
			Cell tDocCell = row.createCell(TDOC_COL);
			tDocCell.setCellValue(docObj.getTDoc());
			Hyperlink link = createHelper.createHyperlink(Hyperlink.LINK_URL);
			URL url = docObj.getUrl();
			if (url != null) {
				link.setAddress(url.toString());
				tDocCell.setHyperlink(link);
				tDocCell.setCellStyle(hlinkStyle);
			}
			Cell groupCell = row.createCell(AGENDA_TITLE_COL);
			groupCell.setCellValue(docObj.getAgendaTitle());
			groupCell.setCellStyle(autowrapStyle);
			Cell titleCell = row.createCell(TITLE_COL);
			titleCell.setCellValue(docObj.getDocTitle());
			titleCell.setCellStyle(autowrapStyle);
			Cell timeCell = row.createCell(SOURCE_COL);
			timeCell.setCellValue(docObj.getSource());
			timeCell.setCellStyle(autowrapStyle);
			Cell authorsCell = row.createCell(DECISION_COL);
			authorsCell.setCellValue(docObj.getDecision());
			authorsCell.setCellStyle(autowrapStyle);
			Cell notesCell = row.createCell(NOTES_COL);
			notesCell.setCellValue(docObj.getNotes());
			notesCell.setCellStyle(autowrapStyle);
		}
	}

	private CellStyle createHeadingStyle() {
		Font font = wb.createFont();
		font.setFontHeightInPoints((short) 12);
		font.setFontName("Arial");
		font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		CellStyle style = wb.createCellStyle();
		style.setFont(font);
		style.setAlignment(CellStyle.ALIGN_CENTER);
		style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		return style;
	}

	private CellStyle createAutowrapStyle() {
		Font font = wb.createFont();
		font.setFontHeightInPoints((short) 10);
		font.setFontName("Arial");
		CellStyle style = wb.createCellStyle();
		style.setFont(font);
		style.setAlignment(CellStyle.ALIGN_LEFT);
		style.setWrapText(true);
		return style;
	}

	private CellStyle createHlinkStyle() {
		CellStyle style = wb.createCellStyle();
		Font font = wb.createFont();
		font.setUnderline(Font.U_SINGLE);
		font.setColor(IndexedColors.BLUE.getIndex());
		style.setFont(font);
		return style;
	}

}
