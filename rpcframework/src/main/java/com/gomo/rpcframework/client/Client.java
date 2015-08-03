package com.gomo.rpcframework.client;

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

	public Client() {

	}

	public void init() {
		String[] hosts = servers.split(",");
		connectionQueue = new LinkedBlockingQueue<Connection>();
		for (int i = 0; i < connectionNum; i++) {
			try {
				int index = i % hosts.length;
				String server = hosts[index].trim();
				String ce[] = server.split(":");
				Connection connection = new Connection(ce[0], Integer.parseInt(ce[1]), soTimeout);
				connectionQueue.put(connection);
			} catch (Exception e) {
				RPCLog.error("create client faild", e);
			}
		}
	}

	public void destory() {
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

	public Response call(Request request) {
		Connection connection = null;
		try {
			connection = getConnection();
			return connection.call(request);
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

}
