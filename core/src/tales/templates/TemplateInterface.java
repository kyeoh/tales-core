package tales.templates;




import tales.services.Connection;
import tales.services.TasksDB;
import tales.services.Task;




public interface TemplateInterface {

	
	public void init(Connection connection, TasksDB tasksDB, Task task);
	public Task getTask();
	public TemplateMetadataInterface getMetadata();
	public void run();
	public boolean isTaskValid(Task task);
	public boolean isTemplateActive();
	public boolean hasFailed();
	
	
}
