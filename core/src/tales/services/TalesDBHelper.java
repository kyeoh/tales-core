package tales.services;




import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import tales.config.Config;
import tales.templates.TemplateConfig;




public class TalesDBHelper {




	private static HashMap<String, CopyOnWriteArrayList<String>> pending = new HashMap<String, CopyOnWriteArrayList<String>>();
	private static HashMap<String, ArrayList<String>> all = new HashMap<String, ArrayList<String>>();
	private static boolean paused = false;




	public static synchronized void queueAddDocumentName(TemplateConfig config, String documentName) throws Exception{
		
		while(paused){
			Thread.sleep(1000);
		}

		String key = config.getTaskName();

		// inits
		if(!all.containsKey(key)){

			pending.put(key, new CopyOnWriteArrayList<String>());
			all.put(key, new ArrayList<String>(Config.getCacheSize()));

			TalesDB talesDB = new TalesDB(config.getThreads(), config.getTemplate().getConnectionMetadata(), config.getTemplateMetadata());
			
			new Thread(new TalesDBHelper.Inserter(key, talesDB)).start();
			new Thread(new TalesDBHelper.Monitor(key)).start();

		}

		if(!all.get(key).contains(documentName)){
			
			// checks size
			if(all.get(key).size() == Config.getCacheSize()){
				all.get(key).remove(all.get(key).size() - 1);
			}
			
			if(!pending.get(key).contains(documentName)){
				pending.get(key).add(documentName);
			}
			
			all.get(key).add(0, documentName);
			
		}else{
			
			all.get(key).remove(documentName);
			all.get(key).add(0, documentName);
			
		}
		
		// pause
		if(pending.get(key).size() > Config.getCacheSize()){
			paused = true;	
		}

	}




	public static class Inserter extends TimerTask implements Runnable{




		private String key;
		private TalesDB talesDB;




		public Inserter(String key, TalesDB talesDB){
			this.key = key;
			this.talesDB = talesDB;
		}




		@Override
		public void run() {

			try{
								
				if(pending.get(key).size() > 0){

					ArrayList<String> names = new ArrayList<String>();
					
					for(Iterator<String> it = pending.get(key).iterator(); it.hasNext();){

						String name = it.next().toString();
						names.add(name);
						pending.get(key).remove(name);

					}
					
					talesDB.bulkAddDocumentsIfDontExist(names);
					paused = false;

				}

				Timer timer = new Timer();
				timer.schedule(new TalesDBHelper.Inserter(key, talesDB), 10000);

			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}

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
				
				Logger.log(new Throwable(), "-pending: " + pending.get(key).size() + " -cached: " + all.get(key).size());
				Timer timer = new Timer();
				timer.schedule(new TalesDBHelper.Monitor(key), 10000);

			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}

		}

	}

}
