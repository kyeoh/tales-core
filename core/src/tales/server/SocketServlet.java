package tales.server;




import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import tales.services.Logger;
import tales.utils.GZIP;




public class SocketServlet extends WebSocketHandler {




	private ConcurrentLinkedQueue<SocketController> broadcast = new ConcurrentLinkedQueue<SocketController>();




	public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
		return new SocketController();
	}




	class SocketController implements WebSocket.OnBinaryMessage {

		private Connection connection;

		@Override
		public void onClose(int closeCode, String message) {
			broadcast.remove(this);
		}

		@Override
		public void onMessage(byte[] bytes, int offset, int length) {

			for(SocketController socket : broadcast){
				
				try{
					
					InputStream is = new ByteArrayInputStream(bytes, offset, length); 
					bytes = IOUtils.toByteArray(is);
					
					socket.connection.sendMessage(bytes, 0, bytes.length);	
					
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