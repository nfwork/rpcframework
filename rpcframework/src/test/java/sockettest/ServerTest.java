package sockettest;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.server.Service;
import com.gomo.rpcframework.server.Server;

public class ServerTest {

	public static void main(String[] args) {
		Server server = new Server();
		server.setPort(8090);
		server.setWorkerNum(10);
		server.registService("helloService", new HelloService());
		server.start();
	}
}

class HelloService implements Service {
	public Response service(Request request) {
		System.err.println(request.getContent());
		Response response = new Response();
		response.setSuccess(true);
		response.setContent("已经收到,消息内容:" + request.getContent());
		return response;
	}
}