package com.gomo.rpcframework.util;

import java.io.IOException;

import com.gomo.rpcframework.RPCConfig;
import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.google.gson.Gson;

public class RPCEncode {
	static Gson gson = new Gson();
	static byte spit = '\n';

	public static Response decodeResponse(byte[] data) throws IOException {
		int index = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == spit) {
				index = i;
				break;
			}
		}
		String responseJson = new String(data, 0, index, RPCConfig.ENCODE);
		Response response = gson.fromJson(responseJson, Response.class);

		String content;
		if (response.isCompress()) {
			byte[] contentbyte = new byte[data.length - index - 1];
			System.arraycopy(data, index + 1, contentbyte, 0, contentbyte.length);
			contentbyte = GZIPHelper.unzip(contentbyte);
			content = new String(contentbyte, RPCConfig.ENCODE);
		} else {
			content = new String(data, index + 1, data.length - index - 1, RPCConfig.ENCODE);
		}
		response.setContent(content);
		return response;
	}

	public static byte[] encodeResponse(Response response) throws IOException {
		String content = response.getContent() == null ? "" : response.getContent();
		response.setContent(null);
		String responseJson = gson.toJson(response);
		byte contentbyte[] = content.getBytes(RPCConfig.ENCODE);
		if (response.isCompress()) {
			contentbyte = GZIPHelper.zip(contentbyte);
		}
		return ByteUtil.concatAll(responseJson.getBytes(RPCConfig.ENCODE), new byte[] { spit }, contentbyte);
	}

	public static Request decodeRequest(byte[] data) throws IOException {
		int index = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == spit) {
				index = i;
				break;
			}
		}
		String responseJson = new String(data, 0, index, RPCConfig.ENCODE);
		Request request = gson.fromJson(responseJson, Request.class);
		String content;
		if (request.isCompress()) {
			byte[] contentbyte = new byte[data.length - index - 1];
			System.arraycopy(data, index + 1, contentbyte, 0, contentbyte.length);
			contentbyte = GZIPHelper.unzip(contentbyte);
			content = new String(contentbyte, RPCConfig.ENCODE);
		} else {
			content = new String(data, index + 1, data.length - index - 1, RPCConfig.ENCODE);
		}
		request.setContent(content);
		return request;
	}

	public static byte[] encodeRequest(Request request) throws IOException {
		String content = request.getContent() == null ? "" : request.getContent();
		String requestJson = gson.toJson(request);
		byte contentbyte[] = content.getBytes(RPCConfig.ENCODE);
		if (request.isCompress()) {
			contentbyte = GZIPHelper.zip(contentbyte);
		}
		return ByteUtil.concatAll(requestJson.getBytes(RPCConfig.ENCODE), new byte[] { spit }, contentbyte);
	}
}
