package tales.config;




import java.io.File;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FileUtils;

import tales.dirlistener.DirListenerObj;
import tales.services.TalesException;
import tales.system.TalesSystem;
import tales.workers.FailoverAttempt;




public class Config{




	private static JSONObject json;
	private static boolean inited = false;




	public static String getDashbaordURL() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getString("dashboardURL");
	}




	public static int getDashbaordPort() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getInt("dashboardPort");
	}




	public static String getLogDBHost() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getString("logDB");
	}




	public static String getJarPath() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("static")
				.getString("jarPath");
	}




	public static String getDBUsername() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getString("dbUsername");
	}
	
	
	
	
	public static String getDBPassword() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getString("dbPassword");
	}
	
	
	
	
	public static String getDataDBHost() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getString("dataDB");
	}
	
	
	
	
	public static int getDataDBPort() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getInt("dataDBPort");
	}
	
	
	
	
	public static String getTasksDBHost() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getString("tasksDB");
	}
	
	
	
	
	public static int getTasksDBPort() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getInt("tasksDBPort");
	}
	
	
	
	
	public static String getRedisHost() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getString("redisHost");
	}
	
	
	
	
	public static int getRedisPort() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getInt("redisPort");
	}
	
	
	
	
	public static String getSolrHost() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getString("solrHost");
	}
	
	
	
	
	public static int getSolrPort() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getInt("solrPort");
	}
	
	
	
	
	public static ArrayList<FailoverAttempt> getFailoverAttemps() throws TalesException{
		load();

		JSONArray failovers = json.getJSONObject("templates")
				.getJSONObject("common")
				.getJSONArray("failover");
		
		ArrayList<FailoverAttempt> objs = new ArrayList<FailoverAttempt>();

		for(int i = 0; i < failovers.size(); i++){

			FailoverAttempt failover = new FailoverAttempt();
			failover.setMaxFails(failovers.getJSONObject(i).getInt("fails"));
			failover.setDuring(failovers.getJSONObject(i).getInt("during"));
			failover.setSleep(failovers.getJSONObject(i).getInt("sleep"));
			
			objs.add(failover);

		}

		return objs;
	}




	public static boolean AWSConfigExists() throws TalesException{
		load();
		if(json.containsKey("cloud") && json.getJSONObject("cloud").containsKey("aws")){
			return true;
		}
		return false;
	}
	
	
	
	
	public static String getAWSAccessKeyId() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("accessKeyId");
	}




	public static String getAWSSecretAccessKey() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("secretAccessKey");
	}




	public static String getAWSAMI() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("ami");
	}




	public static String getAWSSecurityGroup() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("securityGroup");
	}




	public static String getAWSInstanceType() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("instanceType");
	}




	public static String getAWSEndpoint() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("aws")
				.getString("endpoint");
	}
	
	
	
	
	public static boolean rackspaceConfigExists() throws TalesException{
		load();
		if(json.containsKey("cloud") && json.getJSONObject("cloud").containsKey("rackspace")){
			return true;
		}
		return false;
	}
	
	
	
	
	public static String getRackspaceUsername() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getString("username");
	}




	public static String getRackspaceKey() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getString("key");
	}




	public static int getRackspaceImageId() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getInt("imageId");
	}




	public static int getRackspaceFlavor() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getInt("flavor");
	}




	public static int getRackspaceAccount() throws TalesException{
		load();
		return json.getJSONObject("cloud")
				.getJSONObject("rackspace")
				.getInt("account");
	}

	
	
		
	public static ArrayList<String> getSyncList() throws TalesException{
		load();

		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < json.getJSONArray("gitSync").size(); i++){
			list.add(json.getJSONArray("gitSync").getString(i));
		}

		return list;
	}




	public static ArrayList<String> getOnStartCompile() throws TalesException{
		load();

		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < json.getJSONArray("onStartCompile").size(); i++){
			list.add(json.getJSONArray("onStartCompile").getString(i));
		}

		return list;
	}




	public static ArrayList<DirListenerObj> getDirListenerList() throws TalesException{
		load();

		ArrayList<DirListenerObj> list = new ArrayList<DirListenerObj>();
		for(int i = 0; i < json.getJSONArray("dirListener").size(); i++){

			JSONObject jsonObj = json.getJSONArray("dirListener").getJSONObject(i);

			DirListenerObj obj = new DirListenerObj();
			obj.setDir(jsonObj.getString("dir"));
			obj.setExec(jsonObj.getString("exec"));
			obj.setIgnoreRegex(jsonObj.getString("ignoreRegex"));

			list.add(obj);
			
		}

		return list;
	}




	public static ArrayList<String> getOnStartList() throws TalesException{
		load();

		ArrayList<String> list = new ArrayList<String>();
		for(int i = 0; i < json.getJSONArray("onStart").size(); i++){
			list.add(json.getJSONArray("onStart").getString(i));
		}

		return list;
	}




	private synchronized static void load() throws TalesException{

		if(!inited){

			inited = true;
			Loader loader = new Config().new Loader();
			loader.run();

		}

	}




	private class Loader implements Runnable{

		public void run() {

			try{

				File file = new File(Globals.ENVIRONMENTS_CONFIG_DIR + "/" + TalesSystem.getTemplatesGitBranchName() + ".json");
				String data = FileUtils.readFileToString(file);
				Config.json = (JSONObject) JSONSerializer.toJSON(data);

				Thread.sleep(Globals.RELOAD_CONFIG_INTERNAL);
				Thread t = new Thread(this);
				t.start();

			}catch(Exception e){
				try{
					new TalesException(new Throwable(), e);
				}catch(Exception e1){
					e.printStackTrace();
				}
				
			}

		}

	}

}
