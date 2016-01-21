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

	public ServerExecute(SelectionKey key, ExecutorService executorService, ServiceHandle serviceHandle, byte[] requestByte) {
		this.serviceHandle = serviceHandle;
		this.executorService = executorService;
		this.key = key;
		this.requestByte = requestByte;
	}

	public void run() {
		try {
			byte[] outputByte = serviceHandle.handle(requestByte);
			byte[] lengthByte = ByteUtil.toByteArray(outputByte.length);
			byte[] data = ByteUtil.concatAll(lengthByte, outputByte);

			ServerWriter writer = new ServerWriter(executorService, key, data);
			executorService.execute(writer);
		} catch (Exception e) {
			Server.closeChannel(key);
			RPCLog.error("server execute run error", e);
		}
	}

}
