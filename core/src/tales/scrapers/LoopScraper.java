package tales.scrapers;




import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Connection;
import tales.services.Download;
import tales.services.TalesDB;
import tales.services.TalesException;
import tales.services.Logger;
import tales.services.Task;
import tales.services.TasksDB;
import tales.services.Document;
import tales.system.AppMonitor;
import tales.system.TalesSystem;
import tales.templates.TemplateInterface;
import tales.workers.AnticipateFailover;
import tales.workers.DefaultFailover;
import tales.workers.FailoverInterface;
import tales.workers.TaskWorker;




public class LoopScraper {




	private static TalesDB talesDB;
	private static TasksDB tasksDB;
	private static long loopReferenceTime;
	private static TaskWorker taskWorker;




	public static void init(ScraperConfig scraperConfig, long loopReferenceTime) throws TalesException{

		try{


			LoopScraper.loopReferenceTime = loopReferenceTime;


			// inits the services
			talesDB = new TalesDB(scraperConfig.getConnection(), scraperConfig.getTemplate().getMetadata());
			tasksDB = new TasksDB(scraperConfig);


			if(LoopScraper.loopReferenceTime == 0){
				LoopScraper.loopReferenceTime = talesDB.getMostRecentCrawledDocuments(1).get(0).getLastUpdate().getTime();
			}


			// starts the task machine with the template
			FailoverInterface failover = new DefaultFailover(Config.getFailover(scraperConfig.getTemplate().getMetadata().getNamespace()), LoopScraper.loopReferenceTime);
			taskWorker = new TaskWorker(scraperConfig, failover);
			taskWorker.init();


			boolean finished = false;
			while(!failover.hasFailover()){

				// adds tasks
				if((tasksDB.count() + taskWorker.getTasksRunning().size()) < Globals.MIN_TASKS){

					ArrayList<Task> tasks = getTasks();

					if(tasks.size() > 0){

						Logger.log(new Throwable(), "adding tasks to \"" + scraperConfig.getTaskName() + "\"");

						tasksDB.add(tasks);

						if(!taskWorker.isWorkerActive() && !failover.isFailingOver() && !failover.hasFailover()){
							taskWorker = new TaskWorker(scraperConfig, failover);
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


			// stops the failover
			if(!failover.hasFailover()){
				failover.stop();
			}


			// deletes the server
			String serverURL = "http://" + TalesSystem.getPublicDNSName() + ":" + Config.getDashbaordPort() + "/delete";

			Download download = new Download();
			while(!download.urlExists(serverURL)){
				Thread.sleep(100);
			}

			download.getURLContent(serverURL + "/delete");


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private static ArrayList<Task> getTasks() throws TalesException{

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
			options.addOption("template", true, "template class path");
			options.addOption("threads", true, "number of templates");
			options.addOption("loopReferenceTime", true, "loopReferenceTime");
			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			String templatePath = cmd.getOptionValue("template");
			int threads = Integer.parseInt(cmd.getOptionValue("threads"));

			long loopReferenceTime = 0;
			if(cmd.hasOption("loopReferenceTime")){
				loopReferenceTime = Long.parseLong(cmd.getOptionValue("loopReferenceTime"));
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


			// reflection / new template
			TemplateInterface template = (TemplateInterface) Class.forName(templatePath).newInstance();


			// connection
			Connection connection = new Connection();
			connection.setConnectionsNumber(threads);


			// scraper config
			ScraperConfig scraperConfig = new ScraperConfig();
			scraperConfig.setScraperName("LoopScraper");
			scraperConfig.setTemplate(template);
			scraperConfig.setConnection(connection);


			// scraper
			LoopScraper.init(scraperConfig, loopReferenceTime);


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
