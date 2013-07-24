package tales.templates;




import java.util.ArrayList;

import tales.workers.FailoverAttempt;




public interface TemplateConnectionInterface {
	
	
	
	
	public String getDataDBUsername() throws Exception;
	public String getDataDBPassword() throws Exception;
	
	public String getDataDBHost() throws Exception;
	public int getDataDBPort() throws Exception;
	
	public String getTasksDBHost() throws Exception;
	public int getTasksDBPort() throws Exception;
	
	public ArrayList<FailoverAttempt> getFailoverAttemps() throws Exception;

}
