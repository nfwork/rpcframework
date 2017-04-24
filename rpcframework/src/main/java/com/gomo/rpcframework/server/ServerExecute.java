package com.gomo.rpcframework.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.gomo.rpcframework.util.ByteUtil;

class ServerExecute {

	private ServiceHandle serviceHandle = new ServiceHandle();

	private ExecutorService execService;

	private ExecutorService ioService;

	public void init(Server server) {
		ThreadFactory factory = new RpcExecThreadFactory(server.getPort());
		ThreadFactory iofFactory = new RpcIOThreadFactory(server.getPort());
		execService = new ThreadPoolExecutor(server.getMinWorkerNum(), server.getMaxWorkerNum(), 60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), factory);
		ioService = new ThreadPoolExecutor(server.getIoWorkerNum(), server.getIoWorkerNum(), 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
				iofFactory);
	}

	public void destory() {
		if (ioService != null) {
			ioService.shutdown();
		}
		if (execService != null) {
			execService.shutdown();
		}
	}

	public void accept(Selector selector, SelectionKey key) throws IOException {
		ServerSocketChannel server = (ServerSocketChannel) key.channel();
		SocketChannel clientchannel = server.accept();
		clientchannel.configureBlocking(false);
		ServerReader serverReader = new ServerReader(this);
		clientchannel.register(selector, SelectionKey.OP_READ, serverReader);
	}

	public void reader(SelectionKey key, ServerReader reader) {
		reader.setKey(key);
		key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
		ioService.execute(reader);
	}

	public void execute(final SelectionKey key, final ServerReader serverReader, final byte[] requestByte) {
		execService.execute(new Runnable() {
			@Override
			public void run() {
				try {
					byte[] outputByte = serviceHandle.handle(requestByte);
					byte[] lengthByte = ByteUtil.toByteArray(outputByte.length);
					byte[] data = ByteUtil.concatAll(lengthByte, outputByte);

					ServerWriter serverWriter = serverReader.getServerWriter();
					serverWriter.setResponseByte(data);

					key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
					key.selector().wakeup();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	public void writer(SelectionKey key, ServerWriter writer) {
		writer.setKey(key);
		key.interestOps(key.interestOps() & (~SelectionKey.OP_WRITE));
		ioService.execute(writer);
	}

	public void registService(String serviceName, Service service) {
		serviceHandle.regist(serviceName, service);
	}

}
