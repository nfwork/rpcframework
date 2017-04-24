package com.gomo.rpcframework.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.gomo.rpcframework.RPCConfig;
import com.gomo.rpcframework.exception.DatagramFormatException;
import com.gomo.rpcframework.exception.NoDataException;
import com.gomo.rpcframework.util.ByteUtil;
import com.gomo.rpcframework.util.RPCLog;

class ServerReader implements Runnable {

	private SelectionKey key;
	private ByteBuffer headerBuf = ByteBuffer.allocate(5);
	private ByteBuffer contentBuf;
	private ServerManager serverManager;
	private ServerWriter serverWriter = new ServerWriter(); 
	
	public ServerReader(ServerManager serverManager){
		this.serverManager = serverManager;
	}
	
	public void run() {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			int index = 0;

			// 读取头
			if (contentBuf == null) {
				while ((index = channel.read(headerBuf)) > 0) {
					if (headerBuf.position() < 5) {
						key.interestOps(key.interestOps() | SelectionKey.OP_READ);
						key.selector().wakeup();
						return;
					} else {
						byte[] data = headerBuf.array();
						if (data[0] != RPCConfig.FLAG) {
							throw new DatagramFormatException("Illegal request");
						} else {
							// 读取报文长度
							byte[] lengthByte = new byte[4];
							System.arraycopy(data, 1, lengthByte, 0, 4);
							int DatagramLength = ByteUtil.toInt(lengthByte);
							contentBuf = ByteBuffer.allocate(DatagramLength);
							headerBuf.clear();
							break;
						}
					}
				}
				if (index == -1) {
					throw new NoDataException();
				}
			}

			// 读取内容
			if (contentBuf != null) {
				while ((index = channel.read(contentBuf)) > 0) {
					if (contentBuf.position() < contentBuf.limit()) {
						key.interestOps(key.interestOps() | SelectionKey.OP_READ);
						key.selector().wakeup();
						return;
					} else {
						byte[] resByte= contentBuf.array();
						serverManager.execute(key, this, resByte);
						contentBuf = null;
						return;
					}
				}
				if (index == -1) {
					throw new NoDataException();
				}
			}
		} catch (NoDataException e) {
			Server.closeChannel(key);
		} catch (Exception e) {
			Server.closeChannel(key);
			RPCLog.error("server reader run error", e);
		}
	}

	public SelectionKey getKey() {
		return key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

	public ServerWriter getServerWriter() {
		return serverWriter;
	}
	
}
