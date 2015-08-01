package sockettest;

import com.gomo.rpcframework.server.Service;
import com.gomo.rpcframework.server.Server;

public class ServerTest {

	public static void main(String[] args) {
		Service service = new HelloService();
		Server server = new Server(service, 808, 10);
		server.start();
	}
}

class HelloService implements Service {
	public String service(String param) {
		System.err.println(param);
		return ("已经收到,处理线程：" + Thread.currentThread().getName() + ", content:" + param);
	}

}