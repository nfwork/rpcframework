package com.gomo.rpcframework.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.PooledObject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;

import com.gomo.rpcframework.exception.ServiceUnavailableException;
import com.gomo.rpcframework.util.RPCLog;

class ZKConnectionPoolFactory extends ConnectionPoolFactory {

	private CuratorFramework client;

	private PathChildrenCache watcher;

	private String zkHosts;

	private String zkPath;

	private Set<String> serviceNodeSet = new HashSet<String>();

	public ZKConnectionPoolFactory(int soTimeoutMillis, int ioMode) {
		super(soTimeoutMillis, ioMode);
	}

	public void startZK(final String zkHosts, final String zkPath, final int zkRetryTimes) {
		this.zkHosts = zkHosts;
		this.zkPath = zkPath;

		client = CuratorFrameworkFactory.newClient(zkHosts, new RetryNTimes(zkRetryTimes, 5000));
		client.start();

		watcher = new PathChildrenCache(client, zkPath, true);

		watcher.getListenable().addListener(new PathChildrenCacheListener() {
			public void childEvent(CuratorFramework client1, PathChildrenCacheEvent event) throws Exception {
				List<ChildData> childDatas = watcher.getCurrentData();
				refreshServers(childDatas);
				RPCLog.info("zk service node changed, zkHosts:" + zkHosts + ", zkPath:" + zkPath + ", current node:" + servers);
			}
		});

		try {
			watcher.start(StartMode.BUILD_INITIAL_CACHE);
			List<ChildData> childDatas = watcher.getCurrentData();
			refreshServers(childDatas);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		RPCLog.info("connect zookeeper success, zkHosts:" + zkHosts + ", zkPath:" + zkPath + ", current node:" + servers);

	}

	public void stopZK() {
		try {
			watcher.close();
		} catch (Exception e) {
		}
		try {
			client.close();
		} catch (Exception e) {
		}

	}

	private void refreshServers(List<ChildData> childDatas) throws Exception {
		String serversTmp = "";
		for (ChildData childData : childDatas) {
			String value = new String(childData.getData(), "utf-8").trim();
			if (serversTmp.equals("")) {
				serversTmp = value;
			} else {
				serversTmp = serversTmp + "," + value;
			}
		}

		Set<String> nodeSetTmp = new HashSet<String>();
		if (serversTmp.equals("") == false) {
			Collections.addAll(nodeSetTmp, serversTmp.split(","));
		}

		this.servers = serversTmp;
		this.serviceNodeSet = nodeSetTmp;
	}

	@Override
	public boolean validateObject(PooledObject<Connection> p) {
		if (serviceNodeSet.contains(p.getObject().getRemoteAddress())) {
			return super.validateObject(p);
		} else {
			return false;
		}
	}

	@Override
	public void checkFactory() {
		if (serviceNodeSet.isEmpty()) {
			throw new ServiceUnavailableException("service is unavailable, zkHosts:" + zkHosts + ", zkPath:" + zkPath);
		}
	}
}
