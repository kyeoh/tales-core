package tales.scrapers;




import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.codec.binary.Base64;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Document;
import tales.services.Download;
import tales.services.Logger;
import tales.services.TalesDB;
import tales.services.TalesDBHelper;
import tales.services.TalesException;
import tales.services.Task;
import tales.services.TasksDB;
import tales.system.AppMonitor;
import tales.system.ProcessManager;
import tales.system.TalesSystem;
import tales.templates.TemplateMetadata;
import tales.templates.TemplateAbstract;
import tales.templates.TemplateConfig;
import tales.templates.TemplateMetadataInterface;
import tales.workers.FailoverController;
import tales.workers.TaskWorker;




public class LoopScraper {




	private static TalesDB talesDB;
	private static TasksDB tasksDB;
	private static TaskWorker taskWorker;




	public static void init(TemplateConfig templateConfig, long loopReferenceTime) throws TalesException{

		try{


			// inits the services
			talesDB = new TalesDB(templateConfig.getThreads(), 
					templateConfig.getTemplate().getConnectionMetadata(), 
					templateConfig.getTemplateMetadata());
			tasksDB = new TasksDB(templateConfig);


			if(loopReferenceTime == 0){

				ArrayList<Document> documents = talesDB.getMostRecentCrawledDocuments(1);

				if(documents.size() > 0){
					loopReferenceTime = documents.get(0).getLastUpdate().getTime();
				}

			}


			// starts the task machine with the template
			FailoverController failover = new FailoverController(templateConfig.getTemplate().getConnectionMetadata().getFailoverAttemps());
			taskWorker = new TaskWorker(templateConfig, failover);
			taskWorker.init();


			boolean finished = false;
			while(!failover.hasFailed() && !taskWorker.isBroken()){

				// adds tasks
				if((tasksDB.count() + taskWorker.getTasksRunning().size()) < Globals.MIN_TASKS){
					
					TalesDBHelper.finish(templateConfig);

					ArrayList<Task> tasks = getTasks(loopReferenceTime);

					if(tasks.size() > 0){

						Logger.log(new Throwable(), "adding tasks to \"" + templateConfig.getTaskName() + "\"");

						tasksDB.add(tasks);

						if(!taskWorker.isWorkerActive() && !failover.hasFailed()){
							taskWorker = new TaskWorker(templateConfig, failover);
							taskWorker.init();
						}

					}

				}


				// if no tasks means we are finished
				if((tasksDB.count() + taskWorker.getTasksRunning().size()) == 0){

					if(finished){
						break;
					}
					// forces the loop to happen 1 last time
					finished = true;

				}else{
					finished = false;
				}


				Thread.sleep(1000);
			}


			// failover
			if(failover.hasFailed()){


				String process = TalesSystem.getProcess();

				if(!process.contains("-loopReferenceTime")){
					process += " -loopReferenceTime " + loopReferenceTime;
				}

				if(!process.contains("-tpid")){
					process += " -tpid " + ProcessManager.getId();
				}

				String url = "http://" + Config.getDashbaordURL() + ":" + Config.getDashbaordPort();

				while(!new Download().urlExists(url)){
					Thread.sleep(1000);
				}

				url += "/failover"
						+ "?cloud-provider=aws"
						+ "&process=" + URLEncoder.encode(process, "UTF-8");

				Logger.log(new Throwable(), "failing over: " + url);
				new Download().urlExists(url);


				// logs process
				ProcessManager.failedover();


			}else{

				// when finished
				ProcessManager.finished();
			}


			// deletes the server
			String url =  "http://" + TalesSystem.getPublicDNSName() + ":" + Config.getDashbaordPort()
					+ "/delete";
			new Download().getURLBytes(url);


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private static ArrayList<Task> getTasks(long loopReferenceTime) throws TalesException{

		Logger.log(new Throwable(), "adding more tasks to the queue");

		ArrayList<Task> tasks = new ArrayList<Task>();

		for(Document document : talesDB.getAndUpdateLastCrawledDocuments(Globals.MAX_TASKS)){

			// checks if the most recently crawled user is older than this new user, 
			// this means that the "most recent user" is now old and we have looped			
			if(loopReferenceTime >= document.getLastUpdate().getTime()){

				Task task = new Task();
				task.setDocumentId(document.getId());
				task.setDocumentName(document.getName());

				tasks.add(task);

			}

		}

		return tasks;

	}




	public static void main(String[] args){

		try{

			Options options = new Options();
			options.addOption("template", true, "template");
			options.addOption("threads", true, "threads");
			options.addOption("loopReferenceTime", true, "loopReferenceTime");
			options.addOption("tpid", true, "tales process id");
			options.addOption("namespace", true, "namespace");
			options.addOption("baseURL", true, "baseURL");
			options.addOption("requiredDocuments", true, "requiredDocuments");
			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			String templateClass = cmd.getOptionValue("template");
			int threads = Integer.parseInt(cmd.getOptionValue("threads"));


			// reflection / new template
			TemplateAbstract template = (TemplateAbstract) Class.forName(templateClass).newInstance();


			// loop references
			long loopReferenceTime = 0;
			if(cmd.hasOption("loopReferenceTime")){
				loopReferenceTime = Long.parseLong(cmd.getOptionValue("loopReferenceTime"));
			}


			// tpid
			if(cmd.hasOption("tpid")){

				String tpid = cmd.getOptionValue("tpid");
				ProcessManager.registerId(tpid);

			}else{
				ProcessManager.start();
			}


			// namespace
			String namespace = null;
			if(cmd.hasOption("namespace")){
				namespace = cmd.getOptionValue("namespace");
			}else{
				namespace = template.getMetadata().getNamespace();
			}


			// baseURL
			String baseURL = null;
			if(cmd.hasOption("baseURL")){
				baseURL = cmd.getOptionValue("baseURL");
			}else{
				baseURL = template.getMetadata().getBaseURL();
			}


			// requiredDocuments
			ArrayList<String> requiredDocuments = null;
			if(cmd.hasOption("requiredDocuments")){

				String data = cmd.getOptionValue("requiredDocuments");

				if(Base64.isBase64(data.getBytes())){
					data = 	new String(Base64.decodeBase64(data.getBytes()), "UTF-8");
				}

				requiredDocuments = new ArrayList<String>(Arrays.asList(data.split(",")));

			}else{
				
				if(template.getMetadata() != null){
					requiredDocuments = template.getMetadata().getRequiredDocuments();	
				}else{
					requiredDocuments = new ArrayList<String>();
				}

			}
			
			
			// when app is killed
			Runtime.getRuntime().addShutdownHook(new Thread() {

				public void run() {

					if(taskWorker != null){
						taskWorker.stop();
					}

					Logger.log(new Throwable(), "---> bye...");

				}
			});


			// monitors the app performance
			AppMonitor.init();


			// template metadata
			TemplateMetadataInterface templateMetadata = new TemplateMetadata(namespace, baseURL, requiredDocuments);


			// template config
			TemplateConfig templateConfig = new TemplateConfig();
			templateConfig.setScraperName("LoopScraper");
			templateConfig.setTemplate(template);
			templateConfig.setTemplateMetadata(templateMetadata);
			templateConfig.setThreads(threads);


			// scraper
			LoopScraper.init(templateConfig, loopReferenceTime);


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
