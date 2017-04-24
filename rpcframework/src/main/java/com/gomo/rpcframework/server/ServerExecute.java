package com.gomo.rpcframework.server;

import com.gomo.rpcframework.util.ByteUtil;

class ServerExecute {

	private ServiceHandle serviceHandle;

	public ServerExecute(ServiceHandle serviceHandle) {
		this.serviceHandle = serviceHandle;
	}

	public byte[] execute(byte[] requestByte) throws Exception{
		byte[] outputByte = serviceHandle.handle(requestByte);
		byte[] lengthByte = ByteUtil.toByteArray(outputByte.length);
		byte[] data = ByteUtil.concatAll(lengthByte, outputByte);
		return data;
	}

}
