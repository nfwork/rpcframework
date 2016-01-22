package com.gomo.rpcframework.server;

import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutorService;

import com.gomo.rpcframework.util.ByteUtil;
import com.gomo.rpcframework.util.RPCLog;

class ServerExecute implements Runnable {

	private byte[] requestByte;
	private ExecutorService executorService;
	private ServiceHandle serviceHandle;
	private SelectionKey key;
	private ServerWriter writer;

	public ServerExecute(ExecutorService executorService, ServiceHandle serviceHandle) {
		this.serviceHandle = serviceHandle;
		this.executorService = executorService;
		this.writer = new ServerWriter(executorService);
	}

	public void run() {
		try {
			byte[] outputByte = serviceHandle.handle(requestByte);
			byte[] lengthByte = ByteUtil.toByteArray(outputByte.length);
			byte[] data = ByteUtil.concatAll(lengthByte, outputByte);
			writer.setKey(key);
			writer.setResponseByte(data);
			executorService.execute(writer);
		} catch (Exception e) {
			Server.closeChannel(key);
			RPCLog.error("server execute run error", e);
		}
	}

	public void setRequestByte(byte[] requestByte) {
		this.requestByte = requestByte;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

}
