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
import java.util.concurrent.Executors;

import com.gomo.rpcframework.exception.NoDataException;
import com.gomo.rpcframework.util.RPCLog;

public class Server implements Runnable {

	private int port=8090;
	private int workNum=10;
	private ServerSocketChannel serversocket;
	private Selector selector;
	private ExecutorService executorService;
	private ServiceHandle serviceHandle = new ServiceHandle();

	public Server() {
	}
	
	public void registService(String serviceName,Service service){
		serviceHandle.regist(serviceName, service);
	}

	/**
	 * 初始化
	 * */
	public void start() {
		try {
			executorService = Executors.newFixedThreadPool(workNum);
			this.selector = SelectorProvider.provider().openSelector();
			this.serversocket = ServerSocketChannel.open();
			this.serversocket.configureBlocking(false);
			this.serversocket.socket().bind(new InetSocketAddress(port));
			this.serversocket.register(this.selector, SelectionKey.OP_ACCEPT);
			Thread thread = new Thread(this);
			thread.setName("RPCServer");
			thread.start();
			RPCLog.info("server started service on port:" + port);
		} catch (Exception e) {
			RPCLog.error("server start error", e);
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
		ServerExecute serverExecute = new ServerExecute(serviceHandle);
		clientchannel.register(this.selector, SelectionKey.OP_READ, serverExecute);
	}

	public void run() {
		while (true) {
			try {
				this.selector.select();
			} catch (IOException e1) {
				e1.printStackTrace();
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
						ServerExecute serverExecute = (ServerExecute) key.attachment();
						serverExecute.setKey(key);
						serverExecute.read();
						executorService.execute(serverExecute);
					}
				} catch (NoDataException e) {
					key.cancel();
					try {
						key.channel().close();
					} catch (IOException e1) {
					}
				} catch (Exception e) {
					key.cancel();
					try {
						key.channel().close();
					} catch (IOException e1) {
					}
					RPCLog.error("server runtime excetion", e);
				}
			}
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getWorkNum() {
		return workNum;
	}

	public void setWorkNum(int workNum) {
		this.workNum = workNum;
	}
	
}
