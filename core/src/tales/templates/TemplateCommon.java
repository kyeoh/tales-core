package tales.templates;




import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import tales.config.Globals;
import tales.services.Connection;
import tales.services.Download;
import tales.services.DownloadException;
import tales.services.Logger;
import tales.services.TalesDB;
import tales.services.TalesException;
import tales.services.Task;
import tales.services.TasksDB;
import tales.templates.TemplateException;
import tales.utils.Array;




public abstract class TemplateCommon implements Runnable, TemplateInterface{



	
	private Connection connection;
	private TasksDB tasksDB;
	private Task task;
	protected boolean active = true;
	protected boolean failed = false;




	@Override
	public abstract TemplateMetadataInterface getMetadata();




	@Override
	public final void init(Connection connection, TasksDB tasksDB, Task task) {
		this.connection = connection;
		this.tasksDB = tasksDB;
		this.task = task;
	}




	public final Connection getConnection(){
		return connection;
	}
	
	
	
	
	public final TasksDB getTasksDB() {
		return tasksDB;
	}
	
	
	
	
	@Override
	public final Task getTask() {
		return task;
	}




	public final TalesDB getTalesDB() throws TalesException{
		return new TalesDB(this.getConnection(), this.getMetadata());
	}
	
	
	
	
	@Override
	public final boolean hasFailed() {
		return failed;
	}




	@Override
	public boolean isTaskValid(Task task) {
		return true;
	}




	@Override
	public final boolean isTemplateActive() {
		return active;
	}




	@Override
	public void run(){

		try {

			
			String baseURL = this.getMetadata().getBaseURL();
			if(baseURL == null){
				baseURL = "";
			}
			
			String url = baseURL + task.getDocumentName();
			Logger.log(new Throwable(), task.getDocumentId() + " - " + url);
			

			// downloads the html
			Download download = new Download();
			String html = download.getURLContent(url);
			Document doc = Jsoup.parse(html);

			// parses, extracts and saves the data
			process(this.getTalesDB(), task, url, doc);

			// extracts links from the doc and stores them
			storeLinks(extractLinks(doc));

			
		} catch (DownloadException e) {
			
			if(e.getResponseCode() != 404){
				failed = true;
			}
			
			new TemplateException(new Throwable(), e, task.getDocumentId());
			
		} catch (Exception e) {

			try {
				tasksDB.add(task);
			} catch (TalesException e1) {
				new TemplateException(new Throwable(), e1, task.getDocumentId());
			}

			new TemplateException(new Throwable(), e, task.getDocumentId());

		}

		active = false;

	}




	protected abstract void process(TalesDB talesDB, Task task, String url, org.jsoup.nodes.Document document) throws Exception;




	protected ArrayList<String> extractLinks(org.jsoup.nodes.Document doc) throws TalesException{

		ArrayList<String> links = new ArrayList<String>();
		for(Element element : doc.select("a")) {

			if(element.hasAttr("href")){

				String link = element.attr("href");

				if(link.startsWith("/")) {
					links.add(link);
				}
			}
		}

		return Array.removeDuplicates(links);

	}




	protected void storeLinks(ArrayList<String> links){
		
		for(String link : links){

			try{

				if(link.length() < Globals.DOCUMENT_NAME_MAX_LENGTH){
					if(!this.getTalesDB().documentExists(link)){
						this.getTalesDB().addDocument(link);
					}
				}else{
					new TemplateException(new Throwable(), new Exception("Data too long: " + link), task.getDocumentId());
				}

			} catch (Exception e) {
				new TemplateException(new Throwable(), e, task.getDocumentId());
			}

		}
		
	}

}
