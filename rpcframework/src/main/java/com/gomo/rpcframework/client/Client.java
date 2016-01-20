package com.gomo.rpcframework.client;

import java.net.SocketException;

import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;

public class Client {

	private String servers = "127.0.0.1:8090"; // 服务地址

	private int maxTotal = 100; // 链接数量

	private int maxIdle = 10;

	private int minIdle = 5;

	private int soTimeout = 30; // 链接超时 单位秒

	private int status = 0; // 0初始状态 1已初始化 2 已销毁

	private int ioMode = BIO;

	public static final int NIO = 1;

	public static final int BIO = 0;

	GenericObjectPool<Connection> pool;

	public synchronized void init() {
		if (status != 0) {
			throw new RuntimeException("client has inited");
		} else {
			status = 1;
		}

		// 配置基本属性
		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setTestOnBorrow(true);
		poolConfig.setJmxEnabled(true);
		poolConfig.setMaxTotal(maxTotal);
		poolConfig.setMaxIdle(maxIdle);
		poolConfig.setMinIdle(minIdle);
		poolConfig.setTimeBetweenEvictionRunsMillis(1000 * 60 * 5);
		poolConfig.setNumTestsPerEvictionRun(10);
		AbandonedConfig abandonedConfig = new AbandonedConfig();

		ConnectionPoolFactory factory = new ConnectionPoolFactory(servers, soTimeout, ioMode);
		pool = new GenericObjectPool<Connection>(factory, poolConfig, abandonedConfig);
	}

	public synchronized void destory() {
		if (status != 1) {
			throw new RuntimeException("client is not init or aready destory");
		} else {
			status = 2;
		}
		pool.close();
	}


	public Response call(Request request) throws Exception {
		if (request == null || request.getServiceName() == null) {
			throw new RuntimeException("request or request servciename cannot be null");
		}
		if (status != 1) {
			throw new RuntimeException("client is not init or aready destory");
		}
		Connection connection = null;
		try {
			connection = pool.borrowObject();
			return connection.call(request);
		}catch (SocketException e) {
			connection.close();
			throw e;
		} finally {
			if (connection != null) {
				pool.returnObject(connection);
			}
		}
	}

	public String getServers() {
		return servers;
	}

	public void setServers(String servers) {
		this.servers = servers;
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

	public int getMaxTotal() {
		return maxTotal;
	}

	public void setMaxTotal(int maxTotal) {
		this.maxTotal = maxTotal;
	}

	public int getMaxIdle() {
		return maxIdle;
	}

	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}

	public int getMinIdle() {
		return minIdle;
	}

	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}
	
}
