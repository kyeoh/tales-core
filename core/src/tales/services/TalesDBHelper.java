package tales.services;




import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

import tales.templates.TemplateConfig;




public class TalesDBHelper {




	private static HashMap<String, CopyOnWriteArrayList<String>> pending = new HashMap<String, CopyOnWriteArrayList<String>>();
	private static HashMap<String, CopyOnWriteArrayList<String>> all = new HashMap<String, CopyOnWriteArrayList<String>>();




	public static synchronized void queueAddDocumentName(TemplateConfig config, String documentName) throws TalesException{

		String key = config.getTaskName();

		if(!pending.containsKey(key)){

			pending.put(key, new CopyOnWriteArrayList<String>());
			all.put(key, new CopyOnWriteArrayList<String>());

			TalesDB talesDB = new TalesDB(config.getThreads(), config.getTemplate().getConnectionMetadata(), config.getTemplateMetadata());
			Thread t = new Thread(new TalesDBHelper.Inserter(key, talesDB));
			t.start();

		}

		if(!all.get(key).contains(documentName)){
			pending.get(key).add(documentName);
			all.get(key).add(documentName);
		}

	}




	public static void finish(){

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
					
					Logger.log(new Throwable(), "-adding " + pending.get(key).size() + " names to the documents table...");

					for(Iterator<String> it = pending.get(key).iterator(); it.hasNext();){

						String name = it.next();

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

}
