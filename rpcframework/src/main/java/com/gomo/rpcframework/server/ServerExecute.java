package com.gomo.rpcframework.server;

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
	private ByteBuffer dataBuf;
	private ByteBuffer falgBuf = ByteBuffer.allocate(1);
	private ByteBuffer lengthBuf = ByteBuffer.allocate(4);
	private String inputParam;
	private ServiceHandle serviceHandle;

	public ServerExecute(ServiceHandle serviceHandle) {
		this.serviceHandle = serviceHandle;
	}

	public void read() throws Exception {
		SocketChannel channel = (SocketChannel) key.channel();
		int index;

		// 协议标识 校验合法性
		falgBuf.clear();
		do {
			index = channel.read(falgBuf);
			if (index == -1) {
				throw new NoDataException();
			}
		} while (falgBuf.position() < 1);
		if (falgBuf.array()[0] != RPCConfig.FLAG) {
			throw new DatagramFormatException("Illegal request");
		}

		// 读取报文长度
		lengthBuf.clear();
		do {
			index = channel.read(lengthBuf);
			if (index == -1) {
				throw new NoDataException();
			}
		} while (lengthBuf.position() < 4);
		int length = ByteUtil.toInt(lengthBuf.array());

		
		// 报文内容读取
		dataBuf = ByteBuffer.allocate(length);
		do {
			index = channel.read(dataBuf);
			if (index == -1) {
				throw new NoDataException();
			}
		} while (dataBuf.position() < length);
		inputParam = new String(dataBuf.array(), RPCConfig.ENCODE);
	}

	public void run() {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			String output = serviceHandle.handle(inputParam);
			byte[] outputByte = output.getBytes(RPCConfig.ENCODE);
			byte[] lengthByte = ByteUtil.toByteArray(outputByte.length);
			channel.write(ByteBuffer.wrap(lengthByte));
			channel.write(ByteBuffer.wrap(outputByte));
		} catch (Exception e) {
			key.cancel();
			RPCLog.error("server execute run error", e);
		}
	}

	public SelectionKey getKey() {
		return key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

}
