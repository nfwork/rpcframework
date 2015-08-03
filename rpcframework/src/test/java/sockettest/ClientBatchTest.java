package sockettest;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.client.Client;

public class ClientBatchTest {

	public static String ENCODE = "utf-8";

	public static void main(String[] args) throws Exception {

		final Client client = new Client();
		client.setServers("127.0.0.1:8090");
		client.init();

		for (int i = 0; i < 100; i++) {
			new Thread() {
				public void run() {
					try {
						for (int i = 0; i < 1000; i++) {
							try {
								Thread.sleep(10);
								long begin = System.currentTimeMillis();
								Request request = new Request();
								request.setServiceName("helloService");
								request.setContent("hello world " + i);
								Response response = client.call(request);
								long end = System.currentTimeMillis();
								System.err.println("cost time:" + (end - begin) + ",content:" + response.getContent());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					} finally {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (client != null) {
							client.destory();
						}
					}
				};
			}.start();
		}

	}
}