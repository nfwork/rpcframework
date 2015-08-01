package sockettest;

import com.gomo.rpcframework.client.Client;

public class ClientTest {

	public static String ENCODE = "utf-8";
	
	public static void main(String[] args) throws Exception {

		final Client client  = new Client("127.0.0.1", 808, 10);
		client.init();
		
		for (int i = 0; i < 10; i++) {
			new Thread(){
				public void run() {
					try {
						for (int i = 0; i < 100; i++) {
							long begin = System.currentTimeMillis();
							String response = client.call(("hello world "+ i));
							long end = System.currentTimeMillis();
							System.err.println("cost time:"+(end-begin)+",content:"+response);
							try {
								Thread.sleep(100);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}finally{
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (client!=null) {
							client.destory();
						}
					}
				};
			}.start();
		}
		
	}
}