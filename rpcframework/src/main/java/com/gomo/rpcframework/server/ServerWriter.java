package com.gomo.rpcframework.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.gomo.rpcframework.util.RPCLog;

class ServerWriter implements Runnable {

	private SelectionKey key;
	private ByteBuffer byteBuffer;

	public void run() {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			if (channel.isOpen()) {
				channel.write(byteBuffer);
				if (byteBuffer.position() < byteBuffer.limit()) {
					key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
				} else {
					key.interestOps(key.interestOps() | SelectionKey.OP_READ);
				}
				key.selector().wakeup();
			}
		} catch (Exception e) {
			Server.closeChannel(key);
			RPCLog.error("server writer run error", e);
		}
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

	public void setResponseByte(byte[] data) {
		this.byteBuffer = ByteBuffer.wrap(data);
	}

}
