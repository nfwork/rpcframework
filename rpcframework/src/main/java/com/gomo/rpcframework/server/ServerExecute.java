package com.gomo.rpcframework.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.gomo.rpcframework.core.RPCConfig;
import com.gomo.rpcframework.exception.DatagramFormatException;
import com.gomo.rpcframework.exception.NoDataException;
import com.gomo.rpcframework.util.ByteUtil;

public class ServerExecute implements Runnable {

	private SelectionKey key;
	private Service service;
	private ByteBuffer dataBuf;
	private ByteBuffer falgBuf = ByteBuffer.allocate(1);
	private ByteBuffer lengthBuf = ByteBuffer.allocate(4);
	private String inputParam;

	public ServerExecute(Service service) {
		this.service = service;
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
		if (index != 4) {
			throw new DatagramFormatException("not found length flag  with 4 byte");
		}
		int length = ByteUtil.toInt(lengthBuf.array());

		
		// 报文内容读取
		dataBuf = ByteBuffer.allocate(length);
		do {
			index = channel.read(dataBuf);
			if (index == -1) {
				throw new NoDataException();
			}
		} while (dataBuf.position() < length);

		if (length != dataBuf.position()) {
			throw new DatagramFormatException(String.format("Datagram length it doesnot match,length:%s position:%s", length, dataBuf.limit()));
		}
		inputParam = new String(dataBuf.array(), RPCConfig.ENCODE);
	}

	public void run() {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			String output = service.service(inputParam);
			byte[] outputByte = output.getBytes(RPCConfig.ENCODE);
			byte[] lengthByte = ByteUtil.toByteArray(outputByte.length);
			channel.write(ByteBuffer.wrap(lengthByte));
			channel.write(ByteBuffer.wrap(outputByte));
		} catch (Exception e) {
			key.cancel();
			e.printStackTrace();
		}
	}

	public SelectionKey getKey() {
		return key;
	}

	public void setKey(SelectionKey key) {
		this.key = key;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

}
