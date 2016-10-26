package com.gomo.rpcframework.server;

import java.io.IOException;
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
		} catch (Exception e) {
			RPCLog.error("server start error", e);
		}
	}

	public void stop() {
		status = 2;
		try {
			serversocket.close();
		} catch (IOException e) {
			RPCLog.error("server stop error", e);
		}
		executorService.shutdown();
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
				int size = this.selector.select();
				if (size == 0) {
					Thread.sleep(1);
				}
			} catch (Exception e) {
				RPCLog.error("server select error", e);
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
						key.interestOps(key.interestOps()&(~SelectionKey.OP_READ));
						executorService.execute(serverReader);
					} else if(key.isWritable()){
						ServerReader serverReader = (ServerReader) key.attachment();
						ServerWriter serverWriter = serverReader.getWriter();
						serverWriter.setKey(key);
						key.interestOps(key.interestOps()&(~SelectionKey.OP_WRITE));
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

}
