package com.gomo.rpcframework.server;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;

public interface Service {

	public Response service(Request request);
}
