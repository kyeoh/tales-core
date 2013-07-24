package tales.templates;




import tales.scrapers.ScraperConfig;
import tales.services.TalesDB;
import tales.services.TalesException;
import tales.services.Task;
import tales.services.TasksDB;




public abstract class TemplateAbstract implements TemplateInterface, Runnable{
	
	
	
	
	private ScraperConfig scraperConfig;
	private TasksDB tasksDB;
	private Task task;
	protected boolean active = true;
	protected boolean failed = false;
	protected TemplateMetadataInterface metadata;
	
	
	
	
	@Override
	public abstract TemplateMetadataInterface getMetadata();




	@Override
	public TemplateConnectionInterface getConnectionMetadata(){
		return new TemplateConnectionCommon();
	}




	@Override
	public final void init(ScraperConfig scraperConfig, TasksDB tasksDB, Task task) {
		this.scraperConfig = scraperConfig;
		this.tasksDB = tasksDB;
		this.task = task;
	}




	public final ScraperConfig getScraperConfig(){
		return scraperConfig;
	}




	public final TasksDB getTasksDB() {
		return tasksDB;
	}




	@Override
	public final Task getTask() {
		return task;
	}




	public final TalesDB getTalesDB() throws TalesException{
		return new TalesDB(this.getScraperConfig().getThreads(), this.getConnectionMetadata(), this.getMetadata());
	}




	@Override
	public final boolean hasFailed() {
		return failed;
	}




	@Override
	public abstract boolean isTaskInvalid(Task task);




	@Override
	public final boolean isTemplateActive() {
		return active;
	}




	public String getDownloadURL(TemplateMetadataInterface metadata, Task task){

		String baseURL = metadata.getBaseURL();
		if(baseURL == null){
			baseURL = "";
		}

		String url = baseURL + task.getDocumentName();

		return url;

	}




	@Override
	public abstract void run();

}
