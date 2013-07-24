package tales.templates;




import java.util.ArrayList;

import tales.config.Config;
import tales.workers.FailoverAttempt;




public class TemplateLocalhostConnection implements TemplateConnectionInterface{

	
	
	
	@Override
	public String getDataDBUsername() throws Exception {
		return Config.getDataDBUsername();
	}

	@Override
	public String getDataDBPassword() throws Exception {
		return Config.getDataDBPassword();
	}

	@Override
	public String getDataDBHost() throws Exception {
		return "localhost";
	}

	@Override
	public int getDataDBPort() throws Exception {
		return Config.getDataDBPort();
	}

	@Override
	public String getTasksDBHost() throws Exception {
		return "localhost";
	}

	@Override
	public int getTasksDBPort() throws Exception {
		return Config.getTasksDBPort();
	}

	@Override
	public ArrayList<FailoverAttempt> getFailoverAttemps() throws Exception {
		return null;
	}
	
}
