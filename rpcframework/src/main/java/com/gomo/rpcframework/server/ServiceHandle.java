package com.gomo.rpcframework.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.util.RPCEncode;
import com.gomo.rpcframework.util.RPCLog;

class ServiceHandle {

	Map<String, Service> serviceMap = new HashMap<String, Service>();

	public void regist(String name, Service service) {
		serviceMap.put(name, service);
	}

	public byte[] handle(byte[] requestData) throws IOException{
		Response response;
		try {
			Request request = RPCEncode.decodeRequest(requestData);
			Service service = serviceMap.get(request.getServiceName());
			if (service == null) {
				response = new Response();
				response.setContent("service " + request.getServiceName() + " not found");
				response.setSuccess(false);
			} else {
				response = service.service(request);
				if (response==null) {
					response = new Response();
					response.setContent("service return null");
					response.setSuccess(false);
				}
			}
		} catch (Exception e) {
			RPCLog.error("service handle run error", e);
			response = new Response();
			response.setSuccess(false);
			response.setContent(e.getMessage());
		}
		return RPCEncode.encodeResponse(response);
	}
}
