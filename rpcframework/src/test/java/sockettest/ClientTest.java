package sockettest;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.client.Client;

public class ClientTest {

	public static void main(String[] args) throws Exception {

		Client client = new Client();
		client.setZkHosts("192.168.2.78:2181");
		//client.setServers("127.0.0.1:8090");
		client.init();

		for (int i = 0; i < 100000; i++) {
			try {
				long begin = System.currentTimeMillis();
				Request request = new Request();
				request.setServiceName("helloService");
				request.setContent("hello world " + i);
				Response response = client.call(request);
				long end = System.currentTimeMillis();
				System.err.println("cost time:" + (end - begin) + ",content:" + response.getContent());
				Thread.sleep(2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		client.destory();
	}
}