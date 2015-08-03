package com.gomo.rpcframework.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RPCLog {

	static Log log = LogFactory.getLog("rpclog");
	
	public static void info(String message){
		log.info(message);
	}
	
	public static void error(String message ,Throwable e){
		log.error(message, e);
	}
	
}
