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




	protected TemplateMetadataInterface templateMetadata;
	protected Connection connection;
	protected TasksDB tasksDB;
	protected Task task;
	protected TalesDB talesDB;
	protected boolean active = true;
	protected boolean failed = false;




	public TemplateCommon(TemplateMetadataInterface templateMetadata){
		this.templateMetadata = templateMetadata;
	}




	@Override
	public TemplateMetadataInterface getMetadata() {
		return templateMetadata;
	}




	@Override
	public void init(Connection connection, TasksDB tasksDB, Task task) {
		this.connection = connection;
		this.tasksDB = tasksDB;
		this.task = task;
	}




	@Override
	public Task getTask() {
		return task;
	}




	@Override
	public boolean hasFailed() {
		return failed;
	}




	@Override
	public boolean isTaskValid() {
		return true;
	}




	@Override
	public boolean isTemplateActive() {
		return active;
	}




	@Override
	public void run(){

		try {


			String url = this.getMetadata().getBaseURL() + task.getDocumentName();
			Logger.log(new Throwable(), url);

			// db connect
			talesDB = new TalesDB(connection, templateMetadata);

			// downloads the html
			Download download = new Download();
			String html = download.getURLContent(url);
			Document doc = Jsoup.parse(html);

			// parses, extracts and saves the data
			process(talesDB, url, doc);

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




	protected abstract void process(TalesDB talesDB, String url, org.jsoup.nodes.Document document) throws Exception;




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
					if(!talesDB.documentExists(link)){
						talesDB.addDocument(link);
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
