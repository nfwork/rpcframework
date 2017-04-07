package com.gomo.rpcframework.client;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;

import com.gomo.rpcframework.util.RPCLog;

class ZKConnectionPoolFactory extends ConnectionPoolFactory {

	private CuratorFramework client;

	private PathChildrenCache watcher;

	public ZKConnectionPoolFactory(int soTimeoutMillis, int ioMode) {
		super(soTimeoutMillis, ioMode);
	}

	public void startZK(final String zkHosts, final String zkPath, final int zkRetryTimes) {
		client = CuratorFrameworkFactory.newClient(zkHosts, new RetryNTimes(zkRetryTimes, 5000));
		client.start();

		watcher = new PathChildrenCache(client, zkPath, true);

		watcher.getListenable().addListener(new PathChildrenCacheListener() {
			public void childEvent(CuratorFramework client1, PathChildrenCacheEvent event) throws Exception {
				List<ChildData> childDatas = watcher.getCurrentData();
				servers = getServers(childDatas);
				RPCLog.info("zk service node changed, zkHosts:" + zkHosts + ", zkPath:" + zkPath + ", current node:" + servers);
			}
		});

		try {
			watcher.start(StartMode.BUILD_INITIAL_CACHE);
			List<ChildData> childDatas = watcher.getCurrentData();
			servers = getServers(childDatas);
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

	public String getServers() {
		return servers;
	}
}
