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
			client.setServers("127.0.0.1:8090");
			client.init();
			
			int i=0;
			while (i++<30) {
				long begin = System.currentTimeMillis();
				Map<String, Object> paramMap = new HashMap<String, Object>();
				
				Map<String, Object> pheadMap = new HashMap<String, Object>();
				pheadMap.put("advposids", "298,181,195,194,220,200,387,388,389,302,173,232,189,201");
				pheadMap.put("country", "us");
				pheadMap.put("aid", "38400000-8cf0-11bd-b23e-10b96e40000d");
				pheadMap.put("adid", "hellosdfdfssdfsdfsdfsdf");
				pheadMap.put("cip", "10.0.0.1");
				pheadMap.put("sys", "4.4.2");
				pheadMap.put("count", "1000");
				pheadMap.put("fields", "base.rawPrice,multiLangs,appLang:en");
				
				paramMap.put("phead", pheadMap);
				
				Request request = new Request();
				request.setContent(new Gson().toJson(paramMap));
				request.setServiceName("AdvRpcService");
				Response response = client.call(request);
				
				long end = System.currentTimeMillis();
				String string = response.getContent();
				int length = string.length();
				System.err.println("cost time:"+(end-begin)+", content:"+string.substring(length-1000));
			}
		}finally{
			if (client!=null) {
				client.destory();
			}
		}
		
	}
}