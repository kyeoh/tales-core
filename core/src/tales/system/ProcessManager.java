package tales.system;




import java.util.UUID;

import tales.services.ProcessDB;
import tales.services.ProcessState;
import tales.services.TalesException;




public class ProcessManager {




	private static String tpid;




	public static void start() throws TalesException{

		if(ProcessManager.tpid == null){
			setId(generateId());
		}

	}




	public static String generateId(){
		return UUID.randomUUID().toString().replace("-", "_");
	}




	public static void setId(String tpid) throws TalesException{

		if(ProcessManager.tpid == null){

			ProcessManager.tpid = tpid;
			
			ProcessDB.add(ProcessManager.tpid, 
					TalesSystem.getPid(), 
					TalesSystem.getProcess(), 
					TalesSystem.getPublicDNSName(), 
					ProcessState.ACTIVE);

		}
	}




	public static String getId() throws TalesException{
		
		if(ProcessManager.tpid == null){
			start();
		}
		
		return ProcessManager.tpid;
		
	}




	public static void finished() throws TalesException{

		ProcessDB.add(ProcessManager.tpid, 
				TalesSystem.getPid(), 
				TalesSystem.getProcess(), 
				TalesSystem.getPublicDNSName(), 
				ProcessState.FINISHED);

	}




	public static void failedover() throws TalesException{

		ProcessDB.add(ProcessManager.tpid, 
				TalesSystem.getPid(), 
				TalesSystem.getProcess(), 
				TalesSystem.getPublicDNSName(), 
				ProcessState.FAILED_OVER);

	}

}
