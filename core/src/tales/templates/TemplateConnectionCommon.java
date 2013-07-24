package tales.templates;




import java.util.ArrayList;

import tales.config.Config;
import tales.workers.FailoverAttempt;




public class TemplateConnectionCommon implements TemplateConnectionInterface{

	

	
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
		return Config.getDataDBHost();
	}

	@Override
	public int getDataDBPort() throws Exception {
		return Config.getDataDBPort();
	}

	@Override
	public String getTasksDBHost() throws Exception {
		return Config.getTasksDBHost();
	}

	@Override
	public int getTasksDBPort() throws Exception {
		return Config.getTasksDBPort();
	}

	@Override
	public ArrayList<FailoverAttempt> getFailoverAttemps() throws Exception {
		return Config.getFailoverAttemps();
	}
	
}
