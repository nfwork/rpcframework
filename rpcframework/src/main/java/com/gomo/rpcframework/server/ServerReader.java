package com.gomo.rpcframework.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import com.gomo.rpcframework.RPCConfig;
import com.gomo.rpcframework.exception.DatagramFormatException;
import com.gomo.rpcframework.exception.NoDataException;
import com.gomo.rpcframework.util.ByteUtil;
import com.gomo.rpcframework.util.RPCLog;

class ServerReader implements Runnable {

	private SelectionKey key;
	private ByteBuffer headerBuf = ByteBuffer.allocate(5);
	private ByteBuffer contentBuf;
	private ExecutorService executorService;
	private ServiceHandle serviceHandle;

	public ServerReader(ExecutorService executorService, ServiceHandle serviceHandle) {
		this.serviceHandle = serviceHandle;
		this.executorService = executorService;
	}

	public void run() {
		try {

			SocketChannel channel = (SocketChannel) key.channel();
			int index = 0;

			// 读取头
			if (contentBuf == null) {
				while ((index = channel.read(headerBuf)) > 0) {
					if (headerBuf.position() < 5) {
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
						return;
					} else {
						ServerExecute execute = new ServerExecute(key, executorService, serviceHandle, contentBuf.array());
						executorService.execute(execute);
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
		} finally {
			wakeup(key);
		}
	}

	private void wakeup(SelectionKey key) {
		try {
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
			key.selector().wakeup();
		} catch (Exception e) {
		}
	}

	public SelectionKey getKey() {
		return key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

}
