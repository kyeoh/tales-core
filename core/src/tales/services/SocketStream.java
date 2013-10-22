package tales.services;




import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

import net.sf.json.JSONArray;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.json.JSONObject;

import tales.config.Config;
import tales.config.Globals;




public class SocketStream {




	private static Connection connection;
	private static boolean wait = false;
	private static JSONArray logs;
	private static Stream stream;




	private static synchronized void init() throws Exception{

		if(connection == null){


			URI uri = new URI("ws://" + Config.getDashbaordURL() + ":" + Config.getDashbaordPort());

			WebSocketClientFactory webSocketClientFactory = new WebSocketClientFactory();
			webSocketClientFactory.start();

			WebSocketClient client = webSocketClientFactory.newWebSocketClient();

			Future<Connection> futureConnection = client.open(uri, new WebSocket.OnTextMessage(){

				public void onOpen(Connection connection){}
				public void onClose(int closeCode, String message){
					connection.close();
					connection = null;
				}
				public void onMessage(String data){}

			});

			connection = futureConnection.get();
		}

	}




	public synchronized static void stream(JSONObject json) throws Exception{
		
		logs.add(json);
		
		if(stream == null){
			stream = new SocketStream().new Stream();
			stream.run();
		}
		
	}




	private class Stream implements Runnable{

		public void run(){

			try{

				if(!wait){
					init();
					connection.sendMessage(logs.toString());
				}
				
				// loop
				Thread.sleep(100);
				Thread t = new Thread(new Stream());
				t.start();

			}catch(Exception e){

				wait = true;
				Logger.cleanPrint(new Throwable(), "cant connect to dashboard, retrying in " + (Globals.SOCKET_STREAM_RECONNECT_INTERVAL / 1000) + " secs...");

				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						
						wait = false;
						
						// loop
						try{Thread.sleep(100);}catch(Exception e){};
						Thread t = new Thread(new Stream());
						t.start();
						
					}
					
				}, Globals.SOCKET_STREAM_RECONNECT_INTERVAL);

			}

			logs = null;

		}

	}

}
