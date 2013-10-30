package tales.server;




import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import tales.services.Logger;




public class SocketServlet extends WebSocketHandler {




	private ConcurrentLinkedQueue<SocketController> broadcast = new ConcurrentLinkedQueue<SocketController>();




	public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
		return new SocketController();
	}




	class SocketController implements WebSocket.OnTextMessage {

		private Connection connection;

		@Override
		public void onClose(int closeCode, String message) {
			broadcast.remove(this);
		}

		@Override
		public void onMessage(String content) {

			for(SocketController socket : broadcast){
				
				try{
					socket.connection.sendMessage(content);	
				}catch (IOException e){
					broadcast.remove(socket);
					e.printStackTrace();
				}
			}

		}

		@Override
		public void onOpen(Connection connection) {

			this.connection = connection;
			broadcast.add(this);

		}

	}
}