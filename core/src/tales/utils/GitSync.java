package tales.utils;




import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;

import tales.config.Config;
import tales.config.Globals;
import tales.services.TalesException;
import tales.system.TalesSystem;




public class GitSync {




	private static Caller caller;




	public static void init(){
		
		if(caller == null){
			caller = new Caller();
			caller.run();
		}
		
	}




	public static String pull() throws TalesException, IOException, InterruptedException{
		
		String output = "";
		
		for(String folderPath : Config.getSyncList()){
			
			String command = "cd " + folderPath + " && git pull origin " + TalesSystem.getFolderGitBranchName(folderPath);

			ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", command);
			Process process = builder.start();
			process.waitFor();
			
			output += IOUtils.toString(process.getInputStream()) + "\n";
			process.destroy();

		}
		
		return output;
		
	}
	
	
	
	
	private static class Caller extends TimerTask{

		public void run(){

			try{
				GitSync.pull();
			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}

			new Timer().schedule(new Caller(), Globals.GIT_SYNC_REFESH_INTERVAL);
		}

	}	




	public static void main(String[] args){
		GitSync.init();
	}

}
