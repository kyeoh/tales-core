package tales.templates;




import java.util.ArrayList;

import tales.config.Config;
import tales.workers.FailoverAttempt;




public class TemplateConnectionCommon implements TemplateConnectionInterface{

	@Override
	public String getDBUsername() throws Exception {
		return Config.getDBUsername();
	}

	@Override
	public String getDBPassword() throws Exception {
		return Config.getDBPassword();
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
	public String getRedisHost() throws Exception {
		return Config.getRedisHost();
	}

	@Override
	public int getRedisPort() throws Exception {
		return Config.getRedisPort();
	}

	@Override
	public String getSolrHost() throws Exception {
		return Config.getSolrHost();
	}

	@Override
	public int getSolrPort() throws Exception {
		return Config.getSolrPort();
	}

	@Override
	public ArrayList<FailoverAttempt> getFailoverAttemps() throws Exception {
		return Config.getFailoverAttemps();
	}
	
}
