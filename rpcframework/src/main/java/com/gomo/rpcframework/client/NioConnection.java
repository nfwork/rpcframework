package com.gomo.rpcframework.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.gomo.rpcframework.RPCConfig;
import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.exception.NoDataException;
import com.gomo.rpcframework.util.ByteUtil;
import com.gomo.rpcframework.util.RPCEncode;
import com.gomo.rpcframework.util.RPCLog;

public class NioConnection implements Connection {

	// 创建缓冲区
	private ByteBuffer lengthBuf = ByteBuffer.allocate(4);
	private String host;
	private int port;
	private int soTimeout = 30;
	private SocketChannel socketChannel = null;

	public NioConnection(String host, int port, int soTimeout) {
		this.host = host;
		this.port = port;
		this.soTimeout = soTimeout;
		init();
	}

	private void init() {
		try {
			InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
			socketChannel = SocketChannel.open();
			socketChannel.socket().setSoTimeout(soTimeout * 1000);
			socketChannel.connect(address);
		} catch (Exception e) {
			RPCLog.error("connection init failed", e);
		}
	}

	public void refresh() {
		close();
		init();
	}

	public Response call(Request request) throws IOException {

		// 发送请求
		byte[] dataByte = RPCEncode.encodeRequest(request);

		byte data[] = ByteUtil.concatAll(new byte[] { RPCConfig.FLAG }, ByteUtil.toByteArray(dataByte.length), dataByte);
		int dataLength = data.length;
		int sendNum = 0;
		do {
			sendNum = sendNum + socketChannel.write(ByteBuffer.wrap(data, sendNum, (dataLength - sendNum)));
		} while (sendNum < dataLength);

		// 读取报文长度
		int index = 0;
		lengthBuf.clear();
		do {
			index = socketChannel.read(lengthBuf);
			if (index == -1) {
				throw new NoDataException();
			}
		} while (lengthBuf.position() < 4);
		int length = ByteUtil.toInt(lengthBuf.array());

		// 报文内容读取
		ByteBuffer dataBuf = ByteBuffer.allocate(length);
		do {
			index = socketChannel.read(dataBuf);
			if (index == -1) {
				throw new NoDataException();
			}
		} while (dataBuf.position() < length);

		return RPCEncode.decodeResponse(dataBuf.array());
	}

	public void close() {
		try {
			if (socketChannel != null) {
				socketChannel.close();
			}
		} catch (Exception e) {
		}
	}
}
