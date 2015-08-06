package com.gomo.rpcframework.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import com.gomo.rpcframework.util.RPCLog;

class ServerWriter implements Runnable {

	private SelectionKey key;
	private byte[] data;
	private ExecutorService executorService;
	private int sendIndex;

	public ServerWriter(ExecutorService executorService, SelectionKey key, byte[] data) {
		this.key = key;
		this.data = data;
		this.executorService = executorService;
	}

	public void run() {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			if (channel.isOpen()) {
				writer(channel);
				if (sendIndex<data.length) {
					executorService.execute(this);
				}
			}
		} catch (IOException e) {
			Server.closeChannel(key);
			RPCLog.info(e.getMessage());
		} catch (Exception e) {
			Server.closeChannel(key);
			RPCLog.error("server execute run error", e);
		}
	}

	public void writer(SocketChannel channel ) throws Exception {
		ByteBuffer byteBuffer = ByteBuffer.wrap(data, sendIndex, (data.length - sendIndex));
		sendIndex += channel.write(byteBuffer);
	}

}
