package com.gomo.rpcframework.server;

import java.util.HashMap;
import java.util.Map;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.util.RPCLog;
import com.google.gson.Gson;

public class ServiceHandle {

	Map<String, Service> serviceMap = new HashMap<String, Service>();

	public void regist(String name, Service service) {
		serviceMap.put(name, service);
	}

	public String handle(String inputParam) {
		Gson gson = new Gson();
		Response response;
		try {
			Request request = gson.fromJson(inputParam, Request.class);
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
		return gson.toJson(response);
	}
}
