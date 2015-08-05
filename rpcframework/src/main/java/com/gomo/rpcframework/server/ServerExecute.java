package com.gomo.rpcframework.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.gomo.rpcframework.RPCConfig;
import com.gomo.rpcframework.exception.DatagramFormatException;
import com.gomo.rpcframework.exception.NoDataException;
import com.gomo.rpcframework.util.ByteUtil;
import com.gomo.rpcframework.util.RPCLog;

public class ServerExecute implements Runnable {

	private SelectionKey key;
	private ByteBuffer headerBuf = ByteBuffer.allocate(5);
	private ByteBuffer contentBuf;
	private ServiceHandle serviceHandle;
	private byte[] requestByte;

	public ServerExecute(ServiceHandle serviceHandle) {
		this.serviceHandle = serviceHandle;
	}

	public boolean read() throws Exception {
		SocketChannel channel = (SocketChannel) key.channel();
		int index = 0;

		// 读取头
		if (contentBuf == null) {
			while ((index = channel.read(headerBuf)) > 0) {
				if (headerBuf.position() < 5) {
					return false;
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
					continue;
				} else {
					requestByte = contentBuf.array();
					contentBuf = null;
					return true;
				}
			}
			if (index == -1) {
				throw new NoDataException();
			}
		}
		
		return false;
	}

	public void run() {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			byte[] outputByte = serviceHandle.handle(requestByte);
			byte[] lengthByte = ByteUtil.toByteArray(outputByte.length);
			byte[] data = ByteUtil.concatAll(lengthByte, outputByte);

			int dataLength = data.length;
			int sendTotalNum = 0;
			int sendNum = 0;
			ByteBuffer byteBuffer = null;
			do {
				if (!channel.isOpen()) {
					break;
				}
				if (sendNum > 0 || sendTotalNum == 0) {
					byteBuffer = ByteBuffer.wrap(data, sendTotalNum, (dataLength - sendTotalNum));
				}
				sendNum = channel.write(byteBuffer);
				if (sendNum == 0) {// 未发送数据时，等待1ms 再次发送
					Thread.sleep(1);
					sendNum = channel.write(byteBuffer);
				}
				sendTotalNum += sendNum;
			} while (sendTotalNum < dataLength);
		} catch (IOException e) {
			closeChannel(key);
			RPCLog.info(e.getMessage());
		} catch (Exception e) {
			closeChannel(key);
			RPCLog.error("server execute run error", e);
		}
	}

	public void closeChannel(SelectionKey key) {
		try {
			key.cancel();
		} catch (Exception e) {
		}
		try {
			key.channel().close();
		} catch (Exception e1) {
		}
	}

	public SelectionKey getKey() {
		return key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

}
