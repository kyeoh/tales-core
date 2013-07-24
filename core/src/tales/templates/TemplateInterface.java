package tales.templates;




import tales.scrapers.ScraperConfig;
import tales.services.TasksDB;
import tales.services.Task;




public interface TemplateInterface {

	

	
	public TemplateMetadataInterface getMetadata();
	public TemplateConnectionInterface getConnectionMetadata();
	public boolean isTaskInvalid(Task task);
	public void init(ScraperConfig scraperConfig, TasksDB tasksDB, Task task);
	public Task getTask();
	public void run();
	public boolean isTemplateActive();
	public boolean hasFailed();
	
}
