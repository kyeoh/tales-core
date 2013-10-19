package tales.services;




import java.util.Formatter;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.json.JSONObject;

import tales.system.TalesSystem;




public class Logger {




	public static final String LOG = "tales_log";
	public static final String ERROR = "tales_error";
	public static final String TEMPLATE_ERROR = "tales_template_error";
	public static final String DOWNLOAD_ERROR = "tales_download_error";




	private static void emit(StackTraceElement[] e, String logType, String data){

		try{

			
			int pid = TalesSystem.getPid();
			String methodPath = e[0].getClassName() + "." + e[0].getMethodName();
			int lineNumber = e[0].getLineNumber();

			
			// websockets stream
			JSONObject obj = new JSONObject()
			.put("pid", pid)
			.put("process", TalesSystem.getProcess())
			.put("publicDNS", TalesSystem.getPublicDNSName())
			.put("methodPath", methodPath)
			.put("data", data);

			
			// saves logs
			LogsDB.log(TalesSystem.getPublicDNSName(), pid, logType, methodPath, lineNumber, data);
			
			
			// socket stream
			SocketStream.stream(obj);


		}catch(Exception k){
			printError(new Throwable(), k, new String[]{});
		}

	}



	public static void log(Throwable origin, int data){
		log(origin, Integer.toString(data));
	}
	
	
	
	
	public static void log(Throwable origin, String data){
		System.out.format("%-50s %-2s %s %n", origin.getStackTrace()[0].getClassName(), "|", data);
		emit(origin.getStackTrace(), LOG, data + "");
	}



	
	public static void error(Throwable origin, Throwable error) {
		error(origin, error, new String[]{});
	}



	
	public static void error(Throwable origin, Throwable error, String[] args) {
		emit(error.getStackTrace(), ERROR, printError(origin, error, args));
	}




	public static void templateError(Throwable origin, Throwable error, int documentId, String documentName) {
		String[] args = {"documentId: " + Integer.toString(documentId), "documentName: " + documentName};
		emit(error.getStackTrace(), TEMPLATE_ERROR, printError(origin, error, args));
	}
	
	
	
	
	public static void downloadError(Throwable origin, Throwable error) {
		downloadError(origin, error, new String[]{});
	}
	
	
	
	
	public static void downloadError(Throwable origin, Throwable error, String[] args) {
		emit(error.getStackTrace(), DOWNLOAD_ERROR, printError(origin, error, args));
	}
	
	
	
	
	public static String printError(Throwable origin, Throwable error, String[] args){
		
		String data = "";
		data += "[ERROR START] >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n";
		data += new Formatter().format("%-15s %-2s %s %n", "  * found", "|", origin.getStackTrace()[0].toString()).toString();
		data += new Formatter().format("%-15s %-2s %s %n", "  * error", "|", error.getStackTrace()[0].toString()).toString();

		// args
		for(int i = 0; i < args.length; i++){
			data += new Formatter().format("%-15s %-2s %s %n", "  * args", "|", args[i]).toString();
		}

		data += "----------------------------------------------------------------------------------------------------------------------------------------------------\n";
		data += "[FULL ERROR]\n";
		data += ExceptionUtils.getFullStackTrace(error);
		data += "[ERROR END] <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<";
		
		System.out.println(data);
		
		return data;
		
	}
	
	
	
	
	protected static void cleanPrint(Throwable origin, String data){	
		System.out.format("%-50s %-2s %s %n", origin.getStackTrace()[0].getClassName(), "|", data);
	}
}
