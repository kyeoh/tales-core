package tales.config;




import java.io.File;
import java.util.ArrayList;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FileUtils;

import tales.dirlistener.DirListenerObj;
import tales.server.CloudProviderInterface;
import tales.services.TalesException;
import tales.workers.FailoverAttempt;




public class Config{




	private static JSONObject json;
	private static boolean inited = false;




	public static String getDashbaordURL() throws TalesException{
		load();
		return json.getString("dashboardHost");
	}




	public static int getDashbaordPort() throws TalesException{
		load();
		return json.getInt("dashboardPort");
	}




	public static String getLogDBHost() throws TalesException{
		load();
		return json.getString("logDBHost");
	}




	public static int getLogDBPort() throws TalesException{
		load();
		return json.getInt("logDBPort");
	}




	public static String getLogDBUsername() throws TalesException{
		load();
		return json.getString("logDBUsername");
	}




	public static String getLogDBPassword() throws TalesException{
		load();
		return json.getString("logDBPassword");
	}
	
	
	
	
	public static String getTemplatesJar() throws TalesException{
		load();
		return json.getString("templatesJar");
	}




	public static String getDataDBUsername() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getString("dbUsername");
	}




	public static String getDataDBPassword() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getString("dbPassword");
	}




	public static String getDataDBHost() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getString("dataDBHost");
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
				.getString("tasksDBHost");
	}




	public static int getTasksDBPort() throws TalesException{
		load();
		return json.getJSONObject("templates")
				.getJSONObject("common")
				.getInt("tasksDBPort");
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




	public static ArrayList<CloudProviderInterface> getCloudProviders() throws TalesException{
		load();

		ArrayList<CloudProviderInterface> list = new ArrayList<CloudProviderInterface>();
		
		if(json.has("cloudProviders")){
			for(int i = 0; i < json.getJSONArray("cloudProviders").size(); i++){

				try{

					String path = json.getJSONArray("cloudProviders").getString(i);
					CloudProviderInterface cloudProvider = (CloudProviderInterface ) Class.forName(path).newInstance();

					list.add(cloudProvider);

				}catch(Exception e){
					new TalesException(new Throwable(), e);
				}

			}
		}

		return list;
	}




	public static ArrayList<String> getSyncList() throws TalesException{
		load();

		ArrayList<String> list = new ArrayList<String>();
		
		if(json.has("gitSync")){
			for(int i = 0; i < json.getJSONArray("gitSync").size(); i++){
				list.add(json.getJSONArray("gitSync").getString(i));
			}
		}

		return list;
	}




	public static ArrayList<String> getOnStartCompile() throws TalesException{
		load();

		ArrayList<String> list = new ArrayList<String>();
		
		if(json.has("onStartCompile")){
			for(int i = 0; i < json.getJSONArray("onStartCompile").size(); i++){
				list.add(json.getJSONArray("onStartCompile").getString(i));
			}
		}

		return list;
	}




	public static ArrayList<DirListenerObj> getDirListenerList() throws TalesException{
		load();

		ArrayList<DirListenerObj> list = new ArrayList<DirListenerObj>();
		
		if(json.has("dirListener")){
			for(int i = 0; i < json.getJSONArray("dirListener").size(); i++){

				JSONObject jsonObj = json.getJSONArray("dirListener").getJSONObject(i);

				DirListenerObj obj = new DirListenerObj();
				obj.setDir(jsonObj.getString("dir"));
				obj.setExec(jsonObj.getString("exec"));
				obj.setIgnoreRegex(jsonObj.getString("ignoreRegex"));

				list.add(obj);

			}
		}

		return list;
	}




	public static ArrayList<String> getOnStartList() throws TalesException{
		load();

		ArrayList<String> list = new ArrayList<String>();
		
		if(json.has("onStart")){
			for(int i = 0; i < json.getJSONArray("onStart").size(); i++){
				list.add(json.getJSONArray("onStart").getString(i));
			}
		}

		return list;
	}




	private synchronized static void load(){

		if(!inited){

			inited = true;
			Loader loader = new Config().new Loader();
			loader.run();

		}

	}




	private class Loader implements Runnable{

		public void run() {

			try{

				File file = new File(Globals.CONFIG_FILE);
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
