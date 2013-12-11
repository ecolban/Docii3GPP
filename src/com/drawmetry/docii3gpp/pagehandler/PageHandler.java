package com.drawmetry.docii3gpp.pagehandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;

public interface PageHandler {

	public void processInput(BufferedReader reader)
			throws MalformedURLException, IOException;

}
