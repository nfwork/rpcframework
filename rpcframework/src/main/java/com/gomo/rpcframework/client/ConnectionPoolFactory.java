package com.gomo.rpcframework.client;

import java.util.Random;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.gomo.rpcframework.exception.ServiceUnavailableException;

class ConnectionPoolFactory extends BasePooledObjectFactory<Connection> {

	String servers; // 服务地址

	int soTimeoutMillis; // 链接超时

	private static Random random = new Random();

	@Override
	public Connection create() throws Exception {
		if (isUnavaliable()) {
			throw new ServiceUnavailableException("service is unavailable, servers is null");
		}
		String[] hosts = servers.split(",");
		String server = hosts[random.nextInt(hosts.length)].trim();
		String ce[] = server.split(":");
		Connection connection = new BioConnection(ce[0], Integer.parseInt(ce[1]), soTimeoutMillis);
		return connection;
	}

	public ConnectionPoolFactory(int soTimeoutMillis, int ioMode) {
		this.soTimeoutMillis = soTimeoutMillis;
	}

	public ConnectionPoolFactory(String servers, int soTimeoutMillis, int ioMode) {
		this.servers = servers;
		this.soTimeoutMillis = soTimeoutMillis;
	}

	@Override
	public void destroyObject(PooledObject<Connection> p) throws Exception {
		if (p != null && p.getObject() != null) {
			p.getObject().close();
		}
	}

	@Override
	public PooledObject<Connection> wrap(Connection obj) {
		return new DefaultPooledObject<Connection>(obj);
	}

	@Override
	public boolean validateObject(PooledObject<Connection> p) {
		if (p != null && p.getObject() != null) {
			return p.getObject().validate();
		} else {
			return false;
		}
	}

	public boolean isUnavaliable() {
		if (servers == null || "".equals(servers)) {
			return true;
		} else {
			return false;
		}
	}

}
