package com.drawmetry.docii3gpp.pagehandler;

import java.lang.reflect.InvocationTargetException;

import com.drawmetry.docii3gpp.Configuration;


public class PageHandlerFactory {

	
	
	private static RegexpMap<String, Class<? extends PageHandler>> handlerMap = new RegexpMap<String, Class<? extends PageHandler>>();

	static {
		handlerMap.put("R1-74bis", R1PageHandler_74b.class);
		handlerMap.put("R1-7[5-9](bis)?", R1PageHandler_74b.class);
		handlerMap.put("R2-[8-9]\\d(bis)?", R2PageHandler_81.class);
		handlerMap.put("R4-[6-7]\\d(bis)?", R4PageHandler_69.class);
		handlerMap.put("RAN-[5-9]\\d", RPPageHandler_60.class);
		handlerMap.put("S1-6[3-9]", S1PageHandler_63.class);
		handlerMap.put("S2-95", S2PageHandler_95.class);
		handlerMap.put("S2-9[6-9]E?(bis)?", S2PageHandler_96.class);
		handlerMap.put("S2-1\\d\\d(bis)?", S2PageHandler_96.class);
		handlerMap.put("SP-5[8-9]", SPPageHandler_58.class);
		handlerMap.put("SP-60", SPPageHandler_60.class);
		handlerMap.put("SP-6[1-9]", SPPageHandler_61.class);
	}
	
	/**
	 * Gets a PageHandler instance for the given meeting
	 * @param meeting the name of the meeting
	 * @return a PageHandler instance
	 * @throws PageHandlerFactoryException
	 */
	public static PageHandler getInstance(String meeting) throws PageHandlerFactoryException {
		try {
			return handlerMap.get(meeting).getConstructor(String.class)
					.newInstance(meeting);
		} catch (InstantiationException e) {
			throw new PageHandlerFactoryException(e);
		} catch (IllegalAccessException e) {
			throw new PageHandlerFactoryException(e);
		} catch (IllegalArgumentException e) {
			throw new PageHandlerFactoryException(e);
		} catch (InvocationTargetException e) {
			throw new PageHandlerFactoryException(e);
		} catch (NoSuchMethodException e) {
			throw new PageHandlerFactoryException(e);
		} catch (SecurityException e) {
			throw new PageHandlerFactoryException(e);
		}
	}
	
	public static void main(String[] args) {
		try {
			Configuration.initialize();
			PageHandler handler = PageHandlerFactory.getInstance("R2-81bis");
			System.out.println(handler.getClass());
		} catch (PageHandlerFactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
