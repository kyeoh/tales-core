package tales.services;




import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocket.Connection;
import org.eclipse.jetty.websocket.WebSocketClient;
import org.eclipse.jetty.websocket.WebSocketClientFactory;
import org.json.JSONObject;

import tales.config.Config;
import tales.config.Globals;
import tales.utils.GZIP;




public class SocketStream {




	private static Connection connection;
	private static boolean wait = false;




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

		try{

			if(!wait){
				init();
				byte[] bytes = new GZIP().compresBytesToGzip(json.toString().getBytes());
				connection.sendMessage(bytes, 0, bytes.length);
			}

		}catch(Exception e){

			wait = true;
			Logger.cleanPrint(new Throwable(), "cant connect to dashboard, retrying in " + (Globals.SOCKET_STREAM_RECONNECT_INTERVAL / 1000) + " secs...");

			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					wait = false;
				}
			}, Globals.SOCKET_STREAM_RECONNECT_INTERVAL);
			
		}
		
	}

}
