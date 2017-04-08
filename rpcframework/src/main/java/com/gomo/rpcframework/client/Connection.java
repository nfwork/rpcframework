package com.gomo.rpcframework.client;

import java.io.IOException;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;

interface Connection {

	public void refresh();

	public Response call(Request request) throws IOException ;

	public void close();
	
	public boolean validate();
	
	public String getRemoteAddress();
}
