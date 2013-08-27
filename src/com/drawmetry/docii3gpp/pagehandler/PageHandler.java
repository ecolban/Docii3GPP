package com.drawmetry.docii3gpp.pagehandler;

import java.net.MalformedURLException;

public interface PageHandler {
	
	public void processLine(String line) throws MalformedURLException;

}
