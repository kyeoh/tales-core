package tales.workers;




import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

import tales.services.Logger;
import tales.services.TalesException;
import tales.services.Task;
import tales.services.TasksDB;
import tales.templates.TemplateConfig;
import tales.templates.TemplateInterface;
import tales.utils.Average;




public class TaskWorker{




	private TemplateConfig config;
	private FailoverController failover;
	private Worker worker;
	private Monitor monitor;
	private int tasksPending;
	private int processed;




	public TaskWorker(TemplateConfig config, FailoverController failover) throws TalesException{
		this.config = config;
		this.failover = failover;
	}




	public void init() throws TalesException{

		if(worker == null){

			worker = new Worker();
			Thread t = new Thread(worker);
			t.start();

			monitor = new Monitor();
			t = new Thread(monitor);
			t.start();

		}

	}




	public boolean isWorkerActive(){
		return worker.isWorkerActive();
	}




	public void stop(){
		monitor.stop();
		worker.stop();
	}




	public ArrayList<Task> getTasksRunning(){
		return worker.getTasksRunning();
	}




	public boolean isBroken(){
		return worker.isBroken();
	}
	
	
	
	
	private class Worker implements Runnable{




		private boolean stop;
		private CopyOnWriteArrayList<TemplateInterface> threads;
		private TasksDB taskDB;
		private boolean broke;
		



		public Worker() throws TalesException{

			stop = false;
			threads = new CopyOnWriteArrayList<TemplateInterface>();
			taskDB = new TasksDB(config);

		}




		public void run() {

			try {


				if(!stop && !failover.hasFailed()){


					tasksPending = taskDB.count();


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
						int maxThreads = config.getThreads() - threads.size();
						if(maxThreads > 0 && !failover.hasFailed()){


							for(Task task : taskDB.getList(maxThreads)){
								
								// deletes the task from the queue
								taskDB.deleteTaskWithDocumentId(task.getDocumentId());

								// template
								TemplateInterface template = (TemplateInterface) config.getTemplate().getClass().newInstance();

								if(!template.isTaskInvalid(task)){

									template.init(config, taskDB, task);
									threads.add(template);

									Thread t = new Thread((Runnable)template);
									t.start();

									processed++;

								}

							}

						}


						Thread.sleep(50);
						Thread t = new Thread(this);
						t.start();


					}else{

						if(threads.size() == 0){							
							if(monitor != null){
								monitor.stop();
							}
							stop = true;

						}else{
							Thread.sleep(50);
							Thread t = new Thread(this);
							t.start();
						}

					}

				}


			} catch (Exception e) {
				stop();
				broke = true;
				new TalesException(new Throwable(), e);
			}

		}




		public boolean isWorkerActive(){
			return !stop;
		}




		public void stop(){

			stop = true;

			while(getTasksRunning().size() != 0){

				Logger.log(new Throwable(), "waiting for the tasks to finish...");

				try {Thread.sleep(1000);}catch (Exception e){}

			}

		}




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
		
		
		
		
		public boolean isBroken(){
			return broke;
		}

	}




	private class Monitor implements Runnable{




		private boolean stop;
		private Average processAverage;
		private int processedOld;
		private int loops;




		public Monitor(){

			stop = false;
			processAverage = new Average(20);
			
		}




		public void run() {

			try{

				
				if(!stop){

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

					Thread.sleep(50);
					Thread t = new Thread(this);
					t.start();

				}
				

			} catch (Exception e) {
				new TalesException(new Throwable(), e);
			}

		}




		public void stop(){
			stop = true;
		}

	}

}