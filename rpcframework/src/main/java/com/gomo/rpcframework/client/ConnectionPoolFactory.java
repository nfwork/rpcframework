package com.gomo.rpcframework.client;

import java.util.Random;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

final class ConnectionPoolFactory extends BasePooledObjectFactory<Connection> {

	private String servers; // 服务地址

	private int soTimeout; // 链接超时 单位秒

	private int ioMode;

	private static Random random = new Random();

	@Override
	public Connection create() throws Exception {
		String[] hosts = servers.split(",");

		String server = hosts[random.nextInt(hosts.length)].trim();
		String ce[] = server.split(":");
		Connection connection;
		if (ioMode == Client.BIO) {
			connection = new BioConnection(ce[0], Integer.parseInt(ce[1]), soTimeout);
		} else {
			connection = new NioConnection(ce[0], Integer.parseInt(ce[1]), soTimeout);
		}
		return connection;
	}

	public ConnectionPoolFactory(String servers, int soTimeout, int ioMode) {
		this.servers = servers;
		this.soTimeout = soTimeout;
		this.ioMode = ioMode;
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

}
