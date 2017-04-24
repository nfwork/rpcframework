package com.gomo.rpcframework.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode;
import org.apache.curator.framework.recipes.nodes.PersistentEphemeralNode.Mode;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.data.Stat;

import com.gomo.rpcframework.exception.NoDataException;
import com.gomo.rpcframework.util.RPCLog;

public class Server implements Runnable {

	private int port = 8090;
	private int minWorkerNum = 10;
	private int maxWorkerNum = 100;
	private ServerSocketChannel serversocket;

	private Selector selector;
	private ExecutorService executorService;
	private ServiceHandle serviceHandle = new ServiceHandle();
	private int status = 0;// 0初始状态 1已初始化 2 已销毁

	private static final String ZK_BASE_PATH = "/rpcframework";

	private String zkServiceName = "default";
	private String zkHosts;
	private int zkRetryTimes = 10;

	private CuratorFramework client;
	private PersistentEphemeralNode node;

	public void registService(String serviceName, Service service) {
		serviceHandle.regist(serviceName, service);
	}

	/**
	 * 初始化
	 * */
	public void start() {
		try {
			status = 1;
			if (minWorkerNum > maxWorkerNum) {
				maxWorkerNum = minWorkerNum;
			}
			ThreadFactory factory = new RpcServerThreadFactory(port);
			executorService = new ThreadPoolExecutor(minWorkerNum, maxWorkerNum, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), factory);
			this.selector = SelectorProvider.provider().openSelector();
			this.serversocket = ServerSocketChannel.open();
			this.serversocket.configureBlocking(false);
			this.serversocket.socket().bind(new InetSocketAddress(port));
			this.serversocket.register(this.selector, SelectionKey.OP_ACCEPT);
			Thread thread = new Thread(this);
			thread.setName("RPCServer-Main");
			thread.start();
			RPCLog.info("server started service on port:" + port);
			startZK();
		} catch (Exception e) {
			stop();
			throw new RuntimeException("server start error", e);
		}
	}

	private void startZK() throws Exception {
		if (zkHosts != null && zkHosts.trim().equals("") == false) {
			client = CuratorFrameworkFactory.newClient(zkHosts, new RetryNTimes(zkRetryTimes, 5000));
			client.start();
			Stat stat = client.checkExists().forPath(getZkPath());

			if (stat == null) {
				client.create().creatingParentsIfNeeded().forPath(getZkPath());
			}

			InetAddress addr = InetAddress.getLocalHost();
			String ip = addr.getHostAddress().toString();
			String serverAddress = ip + ":" + port;
			String zkPath = getZkPath() + "/" + serverAddress;
			node = new PersistentEphemeralNode(client, Mode.EPHEMERAL, zkPath, serverAddress.getBytes("utf-8"));
			node.start();
			RPCLog.info("service regist success, zkHosts:" + zkHosts + ", zkPath:" + zkPath);
		}
	}

	private String getZkPath() {
		if (zkServiceName != null && zkServiceName.equals("") == false) {
			return ZK_BASE_PATH + "/" + zkServiceName;
		} else {
			return ZK_BASE_PATH;
		}
	}

	public void stop() {
		status = 2;

		try {
			if (node != null) {
				node.close();
			}
		} catch (Exception e) {
		}

		try {
			if (client != null) {
				client.close();
			}
		} catch (Exception e) {
		}

		try {
			if (selector != null) {
				selector.close();
			}
		} catch (Exception e) {
		}

		try {
			if (serversocket != null) {
				serversocket.close();
			}
		} catch (Exception e) {
		}

		if (executorService != null) {
			executorService.shutdown();
		}

	}

	/**
	 * 客户端连接服务器
	 * 
	 * @throws IOException
	 * */
	public void accept(SelectionKey key) throws IOException {
		ServerSocketChannel server = (ServerSocketChannel) key.channel();
		SocketChannel clientchannel = server.accept();
		clientchannel.configureBlocking(false);
		ServerReader serverExecute = new ServerReader(serviceHandle);
		clientchannel.register(this.selector, SelectionKey.OP_READ, serverExecute);
	}

	public void run() {
		while (status == 1) {
			try {
				this.selector.select();
			} catch (IOException e) {
				RPCLog.error("server select error", e);
				continue;
			}

			if (this.selector.isOpen() == false) {
				break;
			}

			// 返回此选择器的已选择键集
			Iterator<SelectionKey> selectorKeys = this.selector.selectedKeys().iterator();

			while (selectorKeys.hasNext()) {
				SelectionKey key = selectorKeys.next();
				selectorKeys.remove();
				try {
					if (!key.isValid()) {
						continue;
					}
					if (key.isAcceptable()) {
						this.accept(key);
					} else if (key.isReadable()) {
						ServerReader serverReader = (ServerReader) key.attachment();
						serverReader.setKey(key);
						key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
						executorService.execute(serverReader);
					} else if (key.isWritable()) {
						ServerReader serverReader = (ServerReader) key.attachment();
						ServerWriter serverWriter = serverReader.getWriter();
						serverWriter.setKey(key);
						key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
						executorService.execute(serverWriter);
					}
				} catch (NoDataException e) {
					closeChannel(key);
				} catch (Exception e) {
					closeChannel(key);
					RPCLog.error("server runtime excetion", e);
				}
			}

		}
	}

	static void closeChannel(SelectionKey key) {
		try {
			key.cancel();
		} catch (Exception e) {
		}
		try {
			key.channel().close();
		} catch (Exception e1) {
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getMinWorkerNum() {
		return minWorkerNum;
	}

	public void setMinWorkerNum(int minWorkerNum) {
		this.minWorkerNum = minWorkerNum;
	}

	public int getMaxWorkerNum() {
		return maxWorkerNum;
	}

	public void setMaxWorkerNum(int maxWorkerNum) {
		this.maxWorkerNum = maxWorkerNum;
	}

	public void setZkHosts(String zkHosts) {
		this.zkHosts = zkHosts;
	}

	public String getZkServiceName() {
		return zkServiceName;
	}

	public void setZkServiceName(String zkServiceName) {
		this.zkServiceName = zkServiceName;
	}

	public int getZkRetryTimes() {
		return zkRetryTimes;
	}

	public void setZkRetryTimes(int zkRetryTimes) {
		this.zkRetryTimes = zkRetryTimes;
	}

}
