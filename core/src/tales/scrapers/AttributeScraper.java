package tales.scrapers;




import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Document;
import tales.services.Download;
import tales.services.Logger;
import tales.services.TalesDB;
import tales.services.TalesException;
import tales.services.Task;
import tales.services.TasksDB;
import tales.system.AppMonitor;
import tales.system.TalesSystem;
import tales.templates.TemplateInterface;
import tales.workers.DefaultFailover;
import tales.workers.FailoverInterface;
import tales.workers.TaskWorker;




public class AttributeScraper{




	private static TalesDB talesDB;
	private static TasksDB tasksDB;
	private static long loopReferenceTime;
	private static TaskWorker taskWorker;




	public static void init(ScraperConfig scraperConfig, String attributeName, String query, long loopReferenceTime) throws TalesException{

		try{


			AttributeScraper.loopReferenceTime = loopReferenceTime;


			// inits the services
			talesDB = new TalesDB(scraperConfig.getThreads(), scraperConfig.getTemplate().getConnectionMetadata(), scraperConfig.getTemplate().getMetadata());
			tasksDB = new TasksDB(scraperConfig);


			if(AttributeScraper.loopReferenceTime == 0){
				AttributeScraper.loopReferenceTime = talesDB.getMostRecentCrawledDocumentsWithAttribute(attributeName, 1).get(0).getLastUpdate().getTime();
			}


			// starts the task machine with the template
			FailoverInterface failover = new DefaultFailover(scraperConfig.getTemplate().getConnectionMetadata().getFailoverAttemps(), AttributeScraper.loopReferenceTime);
			taskWorker = new TaskWorker(scraperConfig, failover);
			taskWorker.init();


			boolean finished = false;
			while(!failover.hasFailover()){

				// adds tasks
				if((tasksDB.count() + taskWorker.getTasksRunning().size()) < Globals.MIN_TASKS){

					ArrayList<Task> tasks = getTasks(attributeName, query);

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
			

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private static ArrayList<Task> getTasks(String attributeName, String query) throws TalesException{

		ArrayList<Task> tasks = new ArrayList<Task>();

		for(Document document : talesDB.getAndUpdateLastCrawledDocumentsWithAttributeAndQuery(attributeName, query, Globals.MAX_TASKS)){

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
			options.addOption("attribute", true, "user attribute name");
			options.addOption("threads", true, "number of templates");
			options.addOption("query", true, "query");
			options.addOption("loopReferenceTime", true, "loopReferenceTime");
			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			String templatePath = cmd.getOptionValue("template");
			String attributeName = cmd.getOptionValue("attribute");
			int threads = Integer.parseInt(cmd.getOptionValue("threads"));

			long loopReferenceTime = 0;
			if(cmd.hasOption("loopReferenceTime")){
				loopReferenceTime = Long.parseLong(cmd.getOptionValue("loopReferenceTime"));
			}
			
			String query = null;
			if(cmd.hasOption("bucket")){
				query = cmd.getOptionValue("bucket");
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


			// scraper config
			ScraperConfig scraperConfig = new ScraperConfig();
			scraperConfig.setScraperName("AttributeScraper");
			scraperConfig.setTemplate(template);
			scraperConfig.setThreads(threads);


			// scraper
			AttributeScraper.init(scraperConfig, attributeName, query, loopReferenceTime);


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
