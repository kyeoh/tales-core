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




	public static synchronized void queueAddDocumentName(TemplateConfig config, String documentName) throws TalesException{

		String key = config.getTaskName();

		if(!pending.containsKey(key)){

			pending.put(key, new CopyOnWriteArrayList<String>());
			all.put(key, new ArrayList<String>(Config.getCacheSize()));

			TalesDB talesDB = new TalesDB(config.getThreads(), config.getTemplate().getConnectionMetadata(), config.getTemplateMetadata());
			
			new Thread(new TalesDBHelper.Inserter(key, talesDB)).start();
			new Thread(new TalesDBHelper.Monitor(key)).start();

		}

		if(!all.get(key).contains(documentName)){
			
			pending.get(key).add(documentName);
			all.get(key).add(documentName);
			
		}else{
			
			all.get(key).remove(documentName);
			all.get(key).add(0, documentName);
			
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
				
				Logger.log(new Throwable(), "-cached: " + all.get(key).size());
				
				if(pending.get(key).size() > 0){

					Logger.log(new Throwable(),"-adding: " + pending.get(key).size() + " names to the documents table.");

					for(Iterator<String> it = pending.get(key).iterator(); it.hasNext();){

						String name = it.next().toString();

						if(!talesDB.documentExists(name)){
							talesDB.addDocument(name);
						}

					}

					pending.get(key).clear();

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
				
				Logger.log(new Throwable(), "-cached: " + all.get(key).size());
				Timer timer = new Timer();
				timer.schedule(new TalesDBHelper.Monitor(key), 10000);

			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}

		}

	}

}
