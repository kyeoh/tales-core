package tales.scrapers;




import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import tales.config.Config;
import tales.services.Download;
import tales.services.Logger;
import tales.services.TalesException;
import tales.system.AppMonitor;
import tales.system.ProcessManager;
import tales.system.TalesSystem;




public class ListScraper {




	private static CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<String>();
	private static ArrayList<Process> threads = new ArrayList<Process>();
	private static int processes;




	public static void init(ArrayList<String> notConcurrentArray, int processes){

		ListScraper.processes = processes;

		try{

			// arrayList to concurrentArrayList
			for(String apiCall : notConcurrentArray){
				list.add(apiCall);
			}

			checkProcesses();

		}catch(Exception e){
			new TalesException(new Throwable(), e);	
		}

	}




	public static void checkProcesses(){

		// checks all the threads and see who is alive
		ArrayList<Process> _threads = new ArrayList<Process>();
		for(Process process : threads){
			if(!process.finished){
				_threads.add(process);
			}
		}

		threads = _threads;

		// starts the threads
		Logger.log(new Throwable(), "-threads: " + threads.size() + " -processes: " + processes + " -queued: " + list.size());

		for(int i = threads.size(); i < processes; i++){

			if(list.size() > 0){

				Process process = new ListScraper().new Process(list.remove(0));
				Thread t = new Thread(process);
				t.start();
				
				threads.add(process);

			}

		}

	}




	public static int getQueuedCount(){
		return list.size();
	}




	private class Process implements Runnable{

		public String processCall;
		public boolean finished = false;
		
		public Process(String processCall){
			this.processCall = processCall;
		}

		@Override
		public void run() {

			try{
				
				String dns = "http://" + TalesSystem.getPublicDNSName() + ":" + Config.getDashbaordPort();
				String tpid = ProcessManager.generateId();
				String apiCall = dns + "/start?process=" + processCall + " -tpid " + tpid;

				new Download().getURLContent(apiCall);

				Logger.log(new Throwable(), "-queued: " + list.size() + " -activeThreads: " + threads.size() + " -apiCall: " + apiCall);

				while(true){

					String processData = new Download().getURLContent(dns + "/finished?tpid=" + tpid);
					JSONObject json = (JSONObject) JSONSerializer.toJSON(processData);

					if(json.getBoolean("finished")){
						break;
					}

					Thread.sleep(1000);

				}

			}catch(Exception e){
				new TalesException(new Throwable(), e);

			}

			finished = true;

			// loop
			checkProcesses();

		}

	}




	public static void main(String[] args){

		try{

			Options options = new Options();
			options.addOption("processes", true, "processes");
			options.addOption("listURL", true, "listURL");

			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			int processes = Integer.parseInt(cmd.getOptionValue("processes"));
			String listURL = cmd.getOptionValue("listURL");

			// monitors the app performance
			AppMonitor.init();

			// downloads the listURL
			String data = new Download().getURLContent(listURL);
			JSONArray jArray = (JSONArray) JSONSerializer.toJSON(data);

			ArrayList<String> list = new ArrayList<String>();
			for(int i = 0; i < jArray.size(); i++){
				list.add(jArray.getString(i));
			}

			// starts the scraper
			ListScraper.init(list, processes);

			// waits
			while(ListScraper.getQueuedCount() > 0){
				Thread.sleep(100);
			}

			// stop
			AppMonitor.stop();
			System.exit(0);

		}catch(Exception e){
			AppMonitor.stop();
			new TalesException(new Throwable(), e);	
			System.exit(0);
		}

	}

}
