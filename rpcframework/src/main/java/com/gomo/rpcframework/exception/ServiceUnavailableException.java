package com.gomo.rpcframework.exception;

public class ServiceUnavailableException extends RuntimeException{

	private static final long serialVersionUID = 6375960024677691077L;

	public ServiceUnavailableException(String message){
		super(message);
	}
	
	public ServiceUnavailableException(String message,Throwable throwable){
		super(message, throwable);
	}
}
