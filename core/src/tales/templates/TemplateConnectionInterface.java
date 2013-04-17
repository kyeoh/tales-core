package tales.templates;




import java.util.ArrayList;

import tales.workers.FailoverAttempt;




public interface TemplateConnectionInterface {
	
	
	
	
	public String getDBUsername() throws Exception;
	public String getDBPassword() throws Exception;
	
	public String getDataDBHost() throws Exception;
	public int getDataDBPort() throws Exception;
	
	public String getTasksDBHost() throws Exception;
	public int getTasksDBPort() throws Exception;
	
	public String getRedisHost() throws Exception;
	public int getRedisPort() throws Exception;
	
	public String getSolrHost() throws Exception;
	public int getSolrPort() throws Exception;
	
	public ArrayList<FailoverAttempt> getFailoverAttemps() throws Exception;

}
