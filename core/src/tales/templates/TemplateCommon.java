package tales.templates;




import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import tales.config.Globals;
import tales.scrapers.ScraperConfig;
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




	private ScraperConfig scraperConfig;
	private TasksDB tasksDB;
	private Task task;
	protected boolean active = true;
	protected boolean failed = false;




	@Override
	public abstract TemplateMetadataInterface getMetadata();




	@Override
	public TemplateConnectionInterface getConnectionMetadata(){
		return new TemplateConnectionCommon();
	}




	@Override
	public final void init(ScraperConfig scraperConfig, TasksDB tasksDB, Task task) {
		this.scraperConfig = scraperConfig;
		this.tasksDB = tasksDB;
		this.task = task;
	}




	public final ScraperConfig getScraperConfig(){
		return scraperConfig;
	}




	public final TasksDB getTasksDB() {
		return tasksDB;
	}




	@Override
	public final Task getTask() {
		return task;
	}




	public final TalesDB getTalesDB() throws TalesException{
		return new TalesDB(this.getScraperConfig().getThreads(), this.getConnectionMetadata(), this.getMetadata());
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




	public String getDownloadCookie(){
		return null;
	}




	public String getDownloadPost(){
		return null;
	}




	public String getDownloadURL(TemplateMetadataInterface metadata, Task task){

		String baseURL = metadata.getBaseURL();
		if(baseURL == null){
			baseURL = "";
		}

		String url = baseURL + task.getDocumentName();
		Logger.log(new Throwable(), "id: " + task.getDocumentId() + " - " + url);

		return url;

	}




	@Override
	public void run(){	

		String url = getDownloadURL(this.getMetadata(), this.getTask());

		try {	

			String html;

			// checks the cache
			if(!this.getScraperConfig().getUseCache()){

				// downloads the html
				html = new Download().getURLContentWithCookieAndPost(url, this.getDownloadCookie(), this.getDownloadPost());

			}else{

				// gets the cache html
				html = null;
				//DigestUtils.shaHex("aff")

			}

			Document doc = Jsoup.parse(html);

			// parses, extracts and saves the data
			process(this.getTalesDB(), task, url, doc);

			// extracts links from the doc and stores them
			storeLinks(extractLinks(doc));


		} catch (DownloadException e) {

			if(e.getResponseCode() != 404){
				failed = true;
			}	

			if(e.getResponseCode() == 503){
				
				try {
					tasksDB.add(task);
				} catch (TalesException e1) {
					new TemplateException(new Throwable(), e1, task.getDocumentId(), url);
				}

			}
			
			new TemplateException(new Throwable(), e, task.getDocumentId(), url);

		} catch (Exception e) {

			try {
				tasksDB.add(task);
			} catch (TalesException e1) {
				new TemplateException(new Throwable(), e1, task.getDocumentId(), url);
			}

			new TemplateException(new Throwable(), e, task.getDocumentId(), url);

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
					new TemplateException(new Throwable(), new Exception("Data too long: " + link), task.getDocumentId(), link);
				}

			} catch (Exception e) {
				new TemplateException(new Throwable(), e, task.getDocumentId(), link);
			}

		}

	}

}
