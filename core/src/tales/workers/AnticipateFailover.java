package tales.workers;




import java.util.ArrayList;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Download;
import tales.services.Logger;
import tales.services.TalesException;
import tales.system.TalesSystem;




public class AnticipateFailover extends DefaultFailover{




	private ServerCreator serverCreator;




	public AnticipateFailover(ArrayList<FailoverAttempt> attempts, long loopReferenceTime){

		super(attempts, loopReferenceTime);

		// creates the server from the beginning
		serverCreator = new ServerCreator();
		Thread t = new Thread(serverCreator);
		t.start();

	}




	@Override
	public void failover() throws TalesException {
		
		String url = "http://" + TalesSystem.getPublicDNSName() + ":" + Config.getDashbaordPort();
		if(!new Download().urlExists(url)){
			throw new TalesException(new Throwable(), new Exception(), new String[]{"tales server seems to be down"});
		}

		try {


			isFailingOver = true;

			Thread.sleep(attempts.get(index).getSleep());
			Logger.log(new Throwable(), "failing over");

			if(serverCreator.dns == null){
				Logger.log(new Throwable(), "waiting for server to be ready...");
			}

			while(serverCreator.dns == null){
				Thread.sleep(1000);
			}

			String newServerURL = "http://" + serverCreator.dns + ":" + Config.getDashbaordPort();
			Logger.log(new Throwable(), "moving process...");

			String process = TalesSystem.getProcess();
			if(process.contains("loopReferenceTime")){
				process = process.substring(0, process.indexOf("-loopReferenceTime "));

			}

			new Download().getURLContent(newServerURL + "/start " + process + " -loopReferenceTime " + loopReferenceTime);

			failedOver = true;


		}catch (Exception e) {

			isFailingOver = false;
			index = 0;
			fails = 0;

			new TalesException(new Throwable(), e);

		}

	}




	private class ServerCreator implements Runnable{


		private String dns;


		@Override
		public void run(){

			try{


				// creates a new server
				Logger.log(new Throwable(), "creating new server...");
				Globals.DOWNLOADER_MAX_TIMEOUT_INTERVAL = 0;

				String thisServerURL = "http://" + TalesSystem.getPublicDNSName() + ":" + Config.getDashbaordPort();
				Download download = new Download();

				while(!download.urlExists(thisServerURL)){
					Thread.sleep(100);
				}

				String data = download.getURLContent(thisServerURL + "/new");
				JSONObject json = (JSONObject) JSONSerializer.toJSON(data);

				// moving process
				dns = json.getString("dns");


			}catch (Exception e) {
				new TalesException(new Throwable(), e);
			}

		}

	}



	@Override
	public void stop(){

		try{

			
			while(serverCreator.dns == null){
				Thread.sleep(1000);
			}
			
			String serverURL = "http://" + serverCreator.dns + ":" + Config.getDashbaordPort();

			Download download = new Download();
			while(!download.urlExists(serverURL)){
				Thread.sleep(100);
			}

			download.getURLContent(serverURL + "/delete");
			

		}catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

	}

}