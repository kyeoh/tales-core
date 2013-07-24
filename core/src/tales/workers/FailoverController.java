package tales.workers;




import java.util.ArrayList;
import java.util.Date;

import tales.services.Logger;
import tales.services.TalesException;




public class FailoverController{




	private ArrayList<FailoverAttempt> attempts;
	private int index;
	private int fails;
	private Date date;
	private boolean failedOver = false;




	public FailoverController(ArrayList<FailoverAttempt> attempts){
		this.attempts = attempts;
	}




	public void fail() throws TalesException{


		fails++;


		if(fails == 1){

			date = new Date();


		}else if((index + 1) < attempts.size()
				&& fails >= attempts.get(index).getMaxFails() 
				&& (new Date().getTime() - date.getTime()) < attempts.get(index).getDuring()){


			Logger.log(new Throwable(), "failover attempt " + (index + 1) + " of " + attempts.size());

			try{Thread.sleep(attempts.get(index).getSleep());}catch(Exception e){};

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

			Logger.log(new Throwable(), "failing over...");
			failedOver = true;

		}

	}




	public boolean hasFailed(){
		return failedOver;
	}

}