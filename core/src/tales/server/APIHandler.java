package tales.server;




import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import tales.config.Config;
import tales.config.Globals;
import tales.scrapers.ListScraper;
import tales.services.Download;
import tales.services.Log;
import tales.services.Logger;
import tales.services.LogsDB;
import tales.services.ProcessDB;
import tales.services.TalesException;
import tales.templates.TemplateLocalhostConnection;
import tales.utils.DBUtils;




public class APIHandler extends AbstractHandler{




	private ListScraper listScraper = new ListScraper();
	
	
	
	
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {


		baseRequest.setHandled(true);


		// cross domain
		response.setHeader("Access-Control-Allow-Origin", "*"); 


		if(target.startsWith("/reboot")){
			reboot();


		}else if(target.startsWith("/start")){
			start(request, response);


		}else if(target.startsWith("/queue")){
			queue(request, response);


		}else if(target.startsWith("/new")){
			newServer(request, response);


		}else if(target.startsWith("/delete")){
			delete();


		}else if(target.startsWith("/force-delete")){
			forceDelete();


		}else if(target.startsWith("/kill")){
			kill(target.replace("/kill ", ""));


		}else if(target.startsWith("/errors")){
			errors(response);


		}else if(target.startsWith("/databases")){
			databases(response);


		}else if(target.startsWith("/failover")){
			failover(request, response);


		}else if(target.startsWith("/finished")){
			finished(request, response);


		}else if(target.startsWith("/logs")){
			getLog(request, response);

		}

	}




	private void reboot(){

		try{


			Logger.log(new Throwable(), "REBOOT: rebooting...");
			ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "reboot");
			builder.start();


		}catch(Exception e){
			new TalesException(new Throwable(), e);
		}

	}




	private void start(HttpServletRequest request, HttpServletResponse response){

		try{


			String process = URLDecoder.decode(request.getParameter("process"), "UTF-8");

			process = "java -cp " + Config.getTemplatesJar() + " " + process + " >/dev/null 2>&1";
			Logger.log(new Throwable(), "START: launching \"" + process + "/");

			ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", process);
			builder.start();


		}catch(Exception e){
			new TalesException(new Throwable(), e);
		}

	}




	private void queue(HttpServletRequest request, HttpServletResponse response) {

		try{

			
			String process = URLDecoder.decode(request.getParameter("process"), "UTF-8");
			int processes = Integer.parseInt(request.getParameter("processes"));
			
			ArrayList<String> apiCalls = new ArrayList<String>();
			apiCalls.add(process);
			
			listScraper.init(apiCalls, processes);
			

		}catch(Exception e){
			new TalesException(new Throwable(), e);
		}

	}




	private String newServer(HttpServletRequest request, HttpServletResponse response){

		try {


			String requestProvider = request.getParameter("cloud-provider").toLowerCase();

			for(CloudProviderInterface cloudProvider : Config.getCloudProviders()){

				if(requestProvider.equals(cloudProvider.getId().toLowerCase())){


					// new server
					Logger.log(new Throwable(), "NEW: creating new server in: " + cloudProvider.getId());
					String publicDNS = cloudProvider.newServer(request);


					// waits for tales dashboard to be up
					Logger.log(new Throwable(), "NEW: waiting for server (" + publicDNS + ") to be up...");
					while(true){

						if(new Download().urlExists("http://" + publicDNS + ":" + Config.getDashbaordPort())){
							break;
						}

						Thread.sleep(1000);

					}


					Logger.log(new Throwable(), "NEW: finished");


					// http response
					JSONObject json = new JSONObject();
					json.put("dns", publicDNS);

					response.setContentType("application/json");
					response.setStatus(HttpServletResponse.SC_OK);
					response.getWriter().println(json);


					return publicDNS;

				}

			}


		} catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

		return null;

	}




	private void delete(){

		try {


			// makes sure that we dont delete a server with dbs -- we ignore tales logs
			if(DBUtils.getLocalTalesDBNames().size() == 0 || 
					(DBUtils.getLocalTalesDBNames().size() == 1 && DBUtils.getLocalTalesDBNames().get(0).contains(LogsDB.getDBName()))){

				forceDelete();

			}else{
				Logger.log(new Throwable(), "DELETE: cant delete server, it contains tales databases. Delete all the tales databases before trying to delete the server.");
			}


		} catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

	}




	private void forceDelete(){

		try {


			for(CloudProviderInterface cloudProvider : Config.getCloudProviders()){
				if(cloudProvider.isApplicationRunningHere()){
					cloudProvider.delete();
					break;
				}
			}

			Logger.log(new Throwable(), "DELETE: server deleted");


		} catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

	}




	private void kill(String pid){

		try {


			Logger.log(new Throwable(), "KILL: killing pid: " + pid);
			ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "kill " + pid);
			builder.start();


		} catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

	}




	private void errors(HttpServletResponse response) {

		try{

			// gets the errors
			JSONArray array = new JSONArray();

			for(Log log : LogsDB.getErrors(Globals.DASHBOARD_MAX_ERRORS)){

				JSONObject obj = new JSONObject();
				obj.put("added", log.getAdded().toString());
				obj.put("id", log.getId());
				obj.put("publicDNS", log.getPublicDNS());
				obj.put("pid", log.getPid());
				obj.put("logType", log.getLogType());
				obj.put("methodPath", log.getMethodPath());
				obj.put("lineNumber", log.getLineNumber());

				array.add(obj);
			}


			// response
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(array);


		} catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

	}




	private void databases(HttpServletResponse response) {

		try{


			JSONArray json = new JSONArray();

			for(String dbName : DBUtils.getLocalDBNames()){

				JSONArray tables = new JSONArray();
				for(String tableName : DBUtils.getTableNames(new TemplateLocalhostConnection(), dbName)){

					JSONObject table = new JSONObject();
					table.put("table", tableName);
					table.put("size", DBUtils.getTableCount(new TemplateLocalhostConnection(), dbName, tableName));
					tables.add(table);

				}

				JSONObject database = new JSONObject();
				database.put("name", dbName);
				database.put("tables", tables);
				json.add(database);

			}


			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(json);


		} catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

	}




	private void failover(HttpServletRequest request, HttpServletResponse response){

		try {


			Logger.log(new Throwable(), "FAILOVER: failover...");

			// new server
			String publicDNS = newServer(request, response);

			// starts process
			String process = URLDecoder.decode(request.getParameter("process"), "UTF-8");

			String url = "http://" + publicDNS + ":" + Config.getDashbaordPort();

			while(!new Download().urlExists(url)){
				Thread.sleep(1000);
			}

			url += "/start?process=" + process;
			new Download().urlExists(url);

			Logger.log(new Throwable(), "FAILOVER: starting process: " + url);


		} catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

	}




	private void finished(HttpServletRequest request, HttpServletResponse response) {

		try {


			String tpid = request.getParameter("tpid");

			// http response
			JSONObject json = new JSONObject();
			json.put("finished", ProcessDB.isFinished(tpid));

			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(json);


		} catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

	}




	private void getLog(HttpServletRequest request, HttpServletResponse response) {

		try{


			int id = Integer.parseInt(request.getParameter("id"));
			Log log = LogsDB.getLog(id);

			JSONObject obj = new JSONObject();
			obj.put("id", log.getId());
			obj.put("publicDNS", log.getPublicDNS());
			obj.put("pid", log.getPid());
			obj.put("logType", log.getLogType());
			obj.put("methodPath", log.getMethodPath());
			obj.put("lineNumber", log.getLineNumber());
			obj.put("added", log.getAdded());
			obj.put("data", log.getData());

			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(obj);


		} catch (Exception e) {
			new TalesException(new Throwable(), e);
		}

	}

}