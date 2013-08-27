package com.drawmetry.docii3gpp.pagehandler;

/**
 * An exception thrown by a PageHandlerFactory when anything goes wrong
 * 
 * @author ecolban
 *
 */
@SuppressWarnings("serial")
public class PageHandlerFactoryException extends Exception {
	public PageHandlerFactoryException(Exception cause){
		super(cause.getMessage(), cause);
	}
}