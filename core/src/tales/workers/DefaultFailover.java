package tales.workers;




import java.util.ArrayList;
import java.util.Date;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Download;
import tales.services.Logger;
import tales.services.TalesException;
import tales.system.TalesSystem;




public class DefaultFailover implements FailoverInterface{




	private ArrayList<FailoverAttempt> attempts;
	private long loopReferenceTime;
	private int index;
	private int fails;
	private Date date;
	private boolean isFailingOver = false;
	private boolean failedOver = false;




	public DefaultFailover(ArrayList<FailoverAttempt> attempts, long loopReferenceTime){
		this.attempts = attempts;
		this.loopReferenceTime = loopReferenceTime;
	}




	public void fail(){

		try{

			if(!isFailingOver){


				fails++;


				if(fails == 1){

					date = new Date();


				}else if((index + 1) < attempts.size()
						&& fails >= attempts.get(index).getMaxFails() 
						&& (new Date().getTime() - date.getTime()) < attempts.get(index).getDuring()){


					Logger.log(new Throwable(), "failover attempt " + (index + 1) + " of " + attempts.size());
					Thread.sleep(attempts.get(index).getSleep());

					index++;
					fails = 0;


				}else if((index + 1) <= attempts.size()
						&& fails <= attempts.get(index).getMaxFails() 
						&& (new Date().getTime() - date.getTime()) > attempts.get(index).getDuring()){


					Logger.log(new Throwable(), "reseting failover");
					index = 0;
					fails = 0;


				}else if((index + 1) == attempts.size()
						&& fails >= attempts.get(index).getMaxFails() 
						&& (new Date().getTime() - date.getTime()) < attempts.get(index).getDuring()){


					isFailingOver = true;


					Thread.sleep(attempts.get(index).getSleep());
					Logger.log(new Throwable(), "failing over");


					// creates a new server
					Logger.log(new Throwable(), "creating new server...");
					Globals.DOWNLOADER_MAX_TIMEOUT_INTERVAL = 0;

					String thisServerURL = "http://" + TalesSystem.getPublicDNSName() + ":" + Config.getDashbaordPort();
					Download download = new Download();
					String data = download.getURLContent(thisServerURL + "/new");
					JSONObject json = (JSONObject) JSONSerializer.toJSON(data);


					// moving process
					String newServerURL = "http://" + json.get("dns") + ":" + Config.getDashbaordPort();
					Logger.log(new Throwable(), "moving process...");
					
					String process = TalesSystem.getProcess();
					if(process.contains("loopReferenceTime")){
						process = process.substring(0, process.indexOf("-loopReferenceTime "));
								
					}
					
					download.getURLContent(newServerURL + "/start/" + process + " -loopReferenceTime " + loopReferenceTime);

					failedOver = true;

				}

			}


		}catch (Exception e) {

			isFailingOver = false;
			index = 0;
			fails = 0;

			new TalesException(new Throwable(), e);
		}

	}




	@Override
	public boolean hasFailover(){
		return failedOver;
	}




	@Override
	public boolean isFallingOver() {
		return isFailingOver;
	}

}