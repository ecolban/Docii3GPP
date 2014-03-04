package com.drawmetry.docii3gpp.pagehandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.drawmetry.docii3gpp.UI;

//import java.util.Arrays;

/**
 * Utility class for reading CSV files. Its readRow method reads a row and
 * returns it as a String[] containing the fields of that row.
 * 
 * @author Erik Colban &copy; 2013 <br>
 *         All Rights Reserved Worldwide
 */
public class CSVReader {

	private final int numFieldsPrLine;

	private static final String CSV_FIELD_REGEXP = "(?:\"((?:[^\"]|\"\")*)\"|([^,]*))";
	private static final Pattern COMMA_CSV_FIELD_PATTERN = Pattern.compile(","
			+ CSV_FIELD_REGEXP);

	private static final Pattern LINE_PATTERN = Pattern.compile(//
			"^" + CSV_FIELD_REGEXP + "(," + CSV_FIELD_REGEXP + ")*");

	private StringBuilder lineBuilder = new StringBuilder();

	private BufferedReader reader;

	/**
	 * Constructor
	 * 
	 * 
	 */
	public CSVReader(BufferedReader reader, int numFieldsPrLine) {

		this.reader = reader;
		this.numFieldsPrLine = numFieldsPrLine;
	}

	/**
	 * Handles one row read from the csv file.
	 * 
	 * @param line
	 * @throws IOException
	 */
	public String[] readRow() throws IOException {
		// Need to concatenate lines in case a row is broken into more than one
		// line
		boolean oddQuotes = false;
		String line = reader.readLine();
		if(line == null) return null;
		StringBuilder lineBuilder = new StringBuilder(line);
		while ((oddQuotes ^= oddQuotes(line))
				&& (line = reader.readLine()) != null) {
			lineBuilder.append(" ");
			lineBuilder.append(line);
		}
		if (lineBuilder.toString().isEmpty()) {
			return null;
		} else {
			return rowToFieldArray(lineBuilder.toString(), numFieldsPrLine);
		}
	}

	private boolean oddQuotes(String line) {
		if (line == null)
			return false;
		boolean result = false;
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '\"') {
				result = !result;
			}
		}
		return result;
	}

	private static String[] rowToFieldArray(String line, int numFields) {
		Matcher lineMatcher = LINE_PATTERN.matcher(line);
		if (lineMatcher.lookingAt()) {
			line = "," + line.substring(lineMatcher.start(), lineMatcher.end());
			Matcher fieldMatcher = COMMA_CSV_FIELD_PATTERN.matcher(line);
			String[] fields = new String[numFields];
			String field = null;

			for (int i = 0; i < fields.length && fieldMatcher.find(); i++) {
				if ((field = fieldMatcher.group(1)) != null) {
					field = field.replace("\"\"", "\"");
				} else {
					field = fieldMatcher.group(2);
				}
				fields[i] = field;
			}
			return fields;
		} else {
			throw new IllegalArgumentException();
		}

	}

	/**
	 * Used for test purposes only
	 * 
	 * @param args
	 *            ignored
	 */
	public static void main(String[] args) {
		String line = "Yes,,noted,,06.3,RP-131418,\"Letter to ITU in reply to ITU-R "
				+ "WP5D/TEMP/305(Rev.1) = RP-131423 on Text proposal for Revision of report "
				+ "ITU-R M.2039-2 \"\"Characteristics of terrestrial IMT-2000 systems for "
				+ "frequency sharing/interference analyses\"\" (R4-137080; to: RAN; cc: -; "
				+ "contact: Ericsson)\",RAN4,LTIin,,LS01,,,-,-,this LTI is a draft reply to RP-131423"
				+ " from ITU-R WP5D; RAN action requested; LS will be treated under AI 7.2.2; no answer"
				+ " to RAN4,\"R4-137080, RP-131423\",,,,,";
		String[] result = CSVReader.rowToFieldArray(line, 22);
		for (int i = 0; i < result.length; i++) {
			System.out.println(result[i]);
		}

	}
}
