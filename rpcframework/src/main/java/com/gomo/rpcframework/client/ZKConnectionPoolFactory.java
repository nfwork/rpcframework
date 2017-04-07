package com.gomo.rpcframework.client;

import java.util.List;
import java.util.Random;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;

import com.gomo.rpcframework.exception.ServiceUnavailableException;

final class ZKConnectionPoolFactory extends BasePooledObjectFactory<Connection> {

	private String zkServers; // ZK服务地址

	private String servers; // 服务地址

	private int soTimeoutMillis; // 链接超时

	private static Random random = new Random();

	private static final String ZK_PATH = "/rpcframework";

	private CuratorFramework client;

	private PathChildrenCache watcher;

	@Override
	public Connection create() throws Exception {
		if (servers==null || servers.equals("")) {
			throw new ServiceUnavailableException("service not found in zookeeper");
		}
		String[] hosts = servers.split(",");
		String server = hosts[random.nextInt(hosts.length)].trim();
		String ce[] = server.split(":");
		Connection connection = new BioConnection(ce[0], Integer.parseInt(ce[1]), soTimeoutMillis);
		return connection;
	}

	public ZKConnectionPoolFactory(String zkServers, int soTimeoutMillis, int ioMode) {
		this.zkServers = zkServers;
		this.soTimeoutMillis = soTimeoutMillis;
	}

	public void startZK() {
		client = CuratorFrameworkFactory.newClient(zkServers, new RetryNTimes(10, 5000));
		client.start();
		System.out.println("zk client start successfully!");

		watcher = new PathChildrenCache(client, ZK_PATH, true);

		watcher.getListenable().addListener(new PathChildrenCacheListener() {
			public void childEvent(CuratorFramework client1, PathChildrenCacheEvent event) throws Exception {
				List<ChildData> childDatas = watcher.getCurrentData();
				servers = getServers(childDatas);
			}
		});

		try {
			watcher.start(StartMode.BUILD_INITIAL_CACHE);
			List<ChildData> childDatas = watcher.getCurrentData();
			servers = getServers(childDatas);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public void closeZK() {
		try {
			watcher.close();
		} catch (Exception e) {
		}
		try {
			client.close();
		} catch (Exception e) {
		}

	}

	private String getServers(List<ChildData> childDatas) throws Exception {
		String serversTmp = "";
		for (ChildData childData : childDatas) {
			if (serversTmp.equals("")) {
				serversTmp = new String(childData.getData(), "utf-8");
			} else {
				serversTmp = serversTmp + "," + new String(childData.getData(), "utf-8");
			}
		}
		return serversTmp;
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
