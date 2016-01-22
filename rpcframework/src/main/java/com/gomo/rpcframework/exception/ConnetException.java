package com.gomo.rpcframework.exception;

public class ConnetException extends RuntimeException{

	private static final long serialVersionUID = 6375960024677691077L;

	public ConnetException(String message){
		super(message);
	}
	
	public ConnetException(String message,Throwable throwable){
		super(message, throwable);
	}
}
