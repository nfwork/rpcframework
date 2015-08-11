package com.gomo.rpcframework.client;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.util.RPCLog;

public class Client {

	private BlockingQueue<Connection> connectionQueue;

	private String servers = "127.0.0.1:8090"; // 服务地址

	private int connectionNum = 10; // 链接数量

	private int soTimeout = 30; // 链接超时 单位秒

	private int status = 0; // 0初始状态 1已初始化 2 已销毁
	
	private int ioMode = BIO;
	
	public static final int NIO=1;
	
	public static final int BIO=0;

	public synchronized void init() {
		if (status != 0) {
			throw new RuntimeException("client has inited");
		} else {
			status = 1;
		}

		String[] hosts = servers.split(",");
		connectionQueue = new LinkedBlockingQueue<Connection>();
		for (int i = 0; i < connectionNum; i++) {
			try {
				int index = i % hosts.length;
				String server = hosts[index].trim();
				String ce[] = server.split(":");
				Connection connection ;
				if (ioMode==BIO) {
					connection = new BioConnection(ce[0], Integer.parseInt(ce[1]), soTimeout);
				}else{
					connection = new NioConnection(ce[0], Integer.parseInt(ce[1]), soTimeout);
				}
				connectionQueue.put(connection);
			} catch (Exception e) {
				RPCLog.error("create client faild", e);
			}
		}
	}

	public synchronized void destory() {
		if (status != 1) {
			throw new RuntimeException("client is not init or aready destory");
		} else {
			status = 2;
		}
		if (connectionQueue != null) {
			for (Connection connection : connectionQueue) {
				connection.close();
			}
			connectionQueue = null;
		}
	}

	private Connection getConnection() {
		try {
			return connectionQueue.take();
		} catch (Exception e) {
			RPCLog.error("take connection faild", e);
		}
		return null;
	}

	private void returnConnection(Connection connection) {
		try {
			if (connectionQueue == null) {
				connection.close();
			} else {
				connectionQueue.put(connection);
			}
		} catch (InterruptedException e) {
			RPCLog.error("return connection faild", e);
		}
	}

	public Response call(Request request) throws IOException {
		if (request == null || request.getServiceName() == null) {
			throw new RuntimeException("request or request servciename cannot be null");
		}
		if (status != 1) {
			throw new RuntimeException("client is not init or aready destory");
		}
		Connection connection = null;
		try {
			connection = getConnection();
			return connection.call(request);
		} catch (SocketTimeoutException e) {
			throw e;
		} catch (Exception e) {
			connection.refresh();
			return connection.call(request);
		} finally {
			if (connection != null) {
				returnConnection(connection);
			}
		}
	}

	public String getServers() {
		return servers;
	}

	public void setServers(String servers) {
		this.servers = servers;
	}

	public int getConnectionNum() {
		return connectionNum;
	}

	public void setConnectionNum(int connectionNum) {
		this.connectionNum = connectionNum;
	}

	public int getSoTimeout() {
		return soTimeout;
	}

	public void setSoTimeout(int soTimeout) {
		this.soTimeout = soTimeout;
	}

	public int getIoMode() {
		return ioMode;
	}

	public void setIoMode(int ioMode) {
		this.ioMode = ioMode;
	}
	
}
