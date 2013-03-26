package tales.workers;




import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import tales.scrapers.ScraperConfig;
import tales.services.Logger;
import tales.services.TalesException;
import tales.services.Task;
import tales.services.TasksDB;
import tales.templates.TemplateInterface;
import tales.utils.Average;




public class TaskWorker{




	private ScraperConfig config;
	private FailoverInterface failover;
	private Worker worker;




	public TaskWorker(ScraperConfig config, FailoverInterface failover) throws TalesException{
		this.config = config;
		this.failover = failover;
	}




	public void init() throws TalesException{

		if(worker == null){
			worker = new Worker(config, failover);
			Thread t = new Thread(worker);
			t.start();
		}

	}




	public boolean isWorkerActive(){
		return worker.isWorkerActive();
	}




	public void stop(){
		worker.stop();
	}




	public ArrayList<Task> getTasksRunning(){
		return worker.getTasksRunning();
	}




	public boolean hasFailover(){
		return failover.hasFailover();
	}




	public boolean isFailingOver() {
		return failover.isFallingOver();
	}




	private class Worker implements Runnable{




		private ScraperConfig config;
		private FailoverInterface failover;
		private boolean stop;
		private CopyOnWriteArrayList<TemplateInterface> threads;
		private Average processAverage;
		private TasksDB taskDB;
		private int processed;
		private int processedOld;
		private int loops;




		public Worker(ScraperConfig config, FailoverInterface failover) throws TalesException{

			this.config                = config;
			this.failover              = failover;
			stop                       = false;
			threads                    = new CopyOnWriteArrayList<TemplateInterface>();
			processAverage             = new Average(20);
			taskDB                     = new TasksDB(config);

		}




		public void run() {

			try {


				if(!stop && !failover.isFallingOver() && !failover.hasFailover()){


					int tasksPending = taskDB.count();


					if(tasksPending > 0){


						// checks the threads
						CopyOnWriteArrayList<TemplateInterface> tempThreads = new CopyOnWriteArrayList<TemplateInterface>();
						for(TemplateInterface thread : threads){

							if(thread.isTemplateActive()){
								tempThreads.add(thread);

							}else if(thread.hasFailed()){
								failover.fail();
							}

						}
						threads = tempThreads;


						// calcs the thread number
						int maxThreads = config.getConnection().getConnectionsNumber() - threads.size();
						if(maxThreads > 0 && !failover.hasFailover()){


							for(Task task : taskDB.getList(maxThreads)){

								// template
								TemplateInterface template = (TemplateInterface) config.getTemplate().getClass().newInstance();

								if(template.isTaskValid(task)){

									template.init(config.getConnection(), taskDB, task);
									threads.add(template);

									Thread t = new Thread((Runnable)template);
									t.start();

									processed++;

								}

								// deletes the task from the queue
								taskDB.deleteTaskWithDocumentId(task.getDocumentId());

							}

						}


						Thread.sleep(50);
						Thread t = new Thread(this);
						t.start();


					}else{

						if(threads.size() == 0){
							stop = true;

						}else{
							Thread.sleep(50);
							Thread t = new Thread(this);
							t.start();
						}

					}


					// process per second
					if(loops == 20){ // num = secs

						processAverage.add((processed - processedOld));
						processedOld = processed;
						loops = 0;

						Logger.log(new Throwable(),
								" -processPerSecond: " + processAverage.getAverage() 
								+ " -processed: " + processed 
								+ " -tasksPending: " + tasksPending);

					}

					loops++;

				}


			} catch (Exception e) {
				new TalesException(new Throwable(), e);
			}
		}




		// returns of the machine is active
		public boolean isWorkerActive(){
			return !stop;
		}




		// stops
		public void stop(){

			stop = true;

			while(getTasksRunning().size() != 0){

				Logger.log(new Throwable(), "waiting for the tasks to finish...");

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}

		}




		// get all active tasks
		public ArrayList<Task> getTasksRunning(){

			ArrayList<Task> tasks = new ArrayList<Task>();

			for(Iterator<TemplateInterface> it = threads.iterator(); it.hasNext();){

				TemplateInterface template = it.next();

				if(template.isTemplateActive()){
					tasks.add(template.getTask());
				}

			}

			return tasks;

		}

	}

}