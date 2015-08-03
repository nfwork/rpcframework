package com.gomo.rpcframework.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.gomo.rpcframework.RPCConfig;
import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.exception.DatagramFormatException;
import com.gomo.rpcframework.util.ByteUtil;
import com.google.gson.Gson;

public class Connection {

	private Socket socket;
	private OutputStream outputStream;
	private InputStream inputStream;
	private String host;
	private int port;
	private int soTimeout = 30;
	private Log log = LogFactory.getLog(getClass());

	public Connection(String host, int port, int soTimeout) {
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
			log.error("connection init failed", e);
		}
	}

	public void refresh() {
		close();
		init();
	}

	public Response call(Request request) {
		try {
			Gson gson = new Gson();
			String data = new Gson().toJson(request);
			byte[] dataByte = data.getBytes(RPCConfig.ENCODE);
			outputStream.write(RPCConfig.FLAG);
			outputStream.write(ByteUtil.toByteArray(dataByte.length));
			outputStream.write(dataByte);
			outputStream.flush();

			int index;
			byte blenght[] = new byte[4];

			index = inputStream.read(blenght);

			if (index != 4) {
				throw new DatagramFormatException("not found length flag  with 4 byte");
			}

			int responseLength = ByteUtil.toInt(blenght);
			byte pkg[] = new byte[responseLength];
			index = inputStream.read(pkg);
			if (index != responseLength) {
				throw new DatagramFormatException(String.format("Datagram length it doesnot match,length:%s position:%s", responseLength, index));
			}
			String resData= new String(pkg, 0, index, RPCConfig.ENCODE);
			return gson.fromJson(resData, Response.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
