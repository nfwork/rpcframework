package com.gomo.rpcframework.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import com.gomo.rpcframework.util.RPCLog;

class ServerWriter implements Runnable {

	private SelectionKey key;
	private ExecutorService executorService;
	private ByteBuffer byteBuffer;

	public ServerWriter(ExecutorService executorService, SelectionKey key, byte[] data) {
		this.key = key;
		this.byteBuffer = ByteBuffer.wrap(data);
		this.executorService = executorService;
	}

	public void run() {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			if (channel.isOpen()) {
				channel.write(byteBuffer);
				if (byteBuffer.position() < byteBuffer.limit()) {
					executorService.execute(this);
				}
			}
		} catch (IOException e) {
			Server.closeChannel(key);
			RPCLog.info(e.getMessage());
		} catch (Exception e) {
			Server.closeChannel(key);
			RPCLog.error("server writer run error", e);
		}
	}

}
