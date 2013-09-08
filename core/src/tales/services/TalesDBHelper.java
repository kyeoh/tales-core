package tales.services;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import tales.config.Config;
import tales.templates.TemplateConfig;




public class TalesDBHelper {




	private static HashMap<String, ArrayList<String>> all = new HashMap<String, ArrayList<String>>();




	public static synchronized boolean isCached(TemplateConfig config, String documentName) throws Exception{
		
		String key = config.getTaskName();

		// inits
		if(!all.containsKey(key)){
			
			all.put(key, new ArrayList<String>(Config.getCacheSize()));
			new Thread(new TalesDBHelper.Monitor(key)).start();
			
		}

		if(!all.get(key).contains(documentName)){
			
			// checks size
			if(all.get(key).size() == Config.getCacheSize()){
				all.get(key).remove(all.get(key).size() - 1);
			}
			
			all.get(key).add(0, documentName);
			
			return false;
			
		}else{
			
			all.get(key).remove(documentName);
			all.get(key).add(0, documentName);
			
			return true;
			
		}

	}
	
	
	
	
	public static class Monitor extends TimerTask implements Runnable{




		private String key;



		
		public Monitor(String key){
			this.key = key;		
		}




		@Override
		public void run() {

			try{
				
				Logger.log(new Throwable(), "-cached: " + all.get(key).size());
				Timer timer = new Timer();
				timer.schedule(new TalesDBHelper.Monitor(key), 10000);

			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}

		}

	}

}
