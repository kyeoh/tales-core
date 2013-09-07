package tales.templates;




import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import tales.services.Download;
import tales.services.DownloadException;
import tales.services.Logger;
import tales.services.TalesDB;
import tales.services.TalesDBHelper;
import tales.services.TalesException;
import tales.services.Task;
import tales.utils.Array;




public abstract class TemplateCommon extends TemplateAbstract{




	public String getDownloadCookie(){
		return null;
	}




	public String getDownloadPost(){
		return null;
	}




	@Override
	public void run(){	

		String url = getDownloadURL(this.getMetadata(), this.getTask());
		Logger.log(new Throwable(), "id: " + this.getTask().getDocumentId() + " - " + url);
		
		try {	

			String html = new Download().getURLContentWithCookieAndPost(url, this.getDownloadCookie(), this.getDownloadPost());

			Document doc = Jsoup.parse(html);

			// parses, extracts and saves the data
			process(this.getTalesDB(), this.getTask(), url, doc);

			// extracts links from the doc and stores them
			storeLinks(extractLinks(doc));


		} catch (DownloadException e) {

			if(e.getResponseCode() != 404){
				failed = true;
			}	

			if(e.getResponseCode() == 503){

				try {
					this.getTasksDB().add(this.getTask());
				} catch (TalesException e1) {
					new TemplateException(new Throwable(), e1, this.getTask());
				}

			}

			new TemplateException(new Throwable(), e, this.getTask());

		} catch (Exception e) {

			try {
				this.getTasksDB().add(this.getTask());
			} catch (TalesException e1) {
				new TemplateException(new Throwable(), e1, this.getTask());
			}

			new TemplateException(new Throwable(), e, this.getTask());

		}

		active = false;

	}




	protected abstract void process(TalesDB talesDB, Task task, String url, org.jsoup.nodes.Document document) throws Exception;




	protected ArrayList<String> extractLinks(org.jsoup.nodes.Document doc) throws TalesException{

		ArrayList<String> links = new ArrayList<String>();
		for(Element element : doc.select("a")) {

			if(element.hasAttr("href")){

				String link = element.attr("href");
				link = link.replace(this.getMetadata().getBaseURL(), "");

				if(link.startsWith("/")) {
					links.add(link);
				}
			}
		}

		return Array.removeDuplicates(links);

	}




	protected void storeLinks(ArrayList<String> links){

		try{

			for(String link : links){
				//TalesDBHelper.queueAddDocumentName(this.getTemplateConfig(), link);
			}

		} catch (Exception e) {
			new TemplateException(new Throwable(), e, this.getTask());
		}

	}

}
