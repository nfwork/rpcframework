package sockettest;

import java.util.HashMap;
import java.util.Map;

import com.gomo.rpcframework.Request;
import com.gomo.rpcframework.Response;
import com.gomo.rpcframework.client.Client;
import com.google.gson.Gson;

public class AdvClientTest {

	public static void main(String[] args) throws Exception {

		Client client = null;
		try {
			client = new Client();
			client.setServers("192.168.2.184:9099");
			client.init();
			
			int i=0;
			while (i++<10000) {
				long begin = System.currentTimeMillis();
				Map<String, Object> pheadMap = new HashMap<String, Object>();
				pheadMap.put("advposids", 298);
				pheadMap.put("country", "us");
				pheadMap.put("aid", "38400000-8cf0-11bd-b23e-10b96e40000d");
				pheadMap.put("adid", "hellosdfdfssdfsdfsdfsdf");
				pheadMap.put("cip", "10.0.0.1");
				pheadMap.put("sys", "4.4.2");
				pheadMap.put("count", "1000");
				pheadMap.put("fields", "base.rawPrice,multiLangs,appLang:en");
				
				Request request = new Request();
				request.setContent(new Gson().toJson(pheadMap));
				request.setServiceName("advService");
				Response response = client.call(request);
				
				long end = System.currentTimeMillis();
				System.err.println("cost time:"+(end-begin)+", content:"+response.getContent().substring(0,300));
				Thread.sleep(10);
			}
		}finally{
			if (client!=null) {
				client.destory();
			}
		}
		
	}
}