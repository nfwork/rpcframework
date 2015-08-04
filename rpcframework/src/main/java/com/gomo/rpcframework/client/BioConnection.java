package com.gomo.rpcframework.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.gomo.rpcframework.RPCConfig;
import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.util.ByteUtil;
import com.gomo.rpcframework.util.RPCEncode;
import com.gomo.rpcframework.util.RPCLog;

public class BioConnection implements Connection {

	private Socket socket;
	private OutputStream outputStream;
	private InputStream inputStream;
	private String host;
	private int port;
	private int soTimeout = 30;

	public BioConnection(String host, int port, int soTimeout) {
		this.host = host;
		this.port = port;
		this.soTimeout = soTimeout;
		init();
	}

	private void init() {
		try {
			socket = new Socket(host, port);
			socket.setSoTimeout(soTimeout * 1000);
			outputStream = socket.getOutputStream();
			inputStream = socket.getInputStream();
		} catch (Exception e) {
			RPCLog.error("connection init failed", e);
		}
	}

	public void refresh() {
		close();
		init();
	}

	public Response call(Request request) throws IOException {

		byte[] dataByte = RPCEncode.encodeRequest(request);
		byte data[] = ByteUtil.concatAll(new byte[] { RPCConfig.FLAG }, ByteUtil.toByteArray(dataByte.length), dataByte);
		outputStream.write(data);
		outputStream.flush();

		int index = 0;
		byte blenght[] = new byte[4];

		index = inputStream.read(blenght);
		do {
			index = index + inputStream.read(blenght, index, (4 - index));
		} while (index != 4);

		int responseLength = ByteUtil.toInt(blenght);
		index = 0;
		byte pkg[] = new byte[responseLength];
		do {
			index = index + inputStream.read(pkg, index, (responseLength - index));
		} while (index != responseLength);

		return RPCEncode.decodeResponse(pkg);
	}

	public void close() {
		try {
			if (outputStream != null) {
				outputStream.close();
			}
		} catch (Exception e) {
		}
		try {
			if (inputStream != null) {
				inputStream.close();
			}
		} catch (Exception e) {
		}
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (Exception e) {
		}
	}
}
