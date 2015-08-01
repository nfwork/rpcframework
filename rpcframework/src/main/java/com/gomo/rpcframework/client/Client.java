package com.gomo.rpcframework.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Client {

	private BlockingQueue<Connection> connectionQueue;
	
	private String host = "127.0.0.1";
	
	private int port = 808;
	
	private int connectionSize = 10;
	
	private Log log = LogFactory.getLog(getClass());

	public Client(String host, int port, int connectionSize) {
		this.host = host;
		this.port = port;
		this.connectionSize = connectionSize;
	}
	
	public Client(){
		
	}
	
	public void init(){
		connectionQueue = new LinkedBlockingQueue<Connection>();
		for (int i = 0; i < connectionSize; i++) {
			Connection connection = new Connection(host, port);
			try {
				connectionQueue.put(connection);
			} catch (Exception e) {
				log.error("create client faild", e);
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
			log.error("take connection faild", e);
		}
		return null;
	}

	private void returnConnection(Connection connection) {
		try {
			if (connectionQueue == null) {
				connection.close();
			}else {
				connectionQueue.put(connection);
			}
		} catch (InterruptedException e) {
			log.error("return connection faild", e);
		}
	}

	public String call(String param) {
		Connection connection = null;
		try {
			connection = getConnection();
			return connection.call(param);
		}catch(Exception e){
			connection.refresh();
			return connection.call(param);
		}finally {
			if (connection != null) {
				returnConnection(connection);
			}
		}
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getConnectionSize() {
		return connectionSize;
	}

	public void setConnectionSize(int connectionSize) {
		this.connectionSize = connectionSize;
	}
	
}
