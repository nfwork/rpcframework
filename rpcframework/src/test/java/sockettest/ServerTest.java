package sockettest;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.server.Service;
import com.gomo.rpcframework.server.Server;

public class ServerTest {

	public static void main(String[] args) {
		Server server = new Server();
		server.setPort(8090);
		server.setWorkNum(10);
		server.registService("helloService", new HelloService());
		server.start();
	}
}

class HelloService implements Service {
	public Response service(Request request) {
		System.err.println(request.getContent());
		Response response = new Response();
		response.setContent("已经收到,处理线程：" + Thread.currentThread().getName() + ", content:" + request.getContent());
		return response;
	}

}