package tales.scrapers;




import tales.config.Globals;
import tales.templates.TemplateInterface;




public class ScraperConfig {
	
	
	
	
	private String scraperName;
	private TemplateInterface template;
	private int threads = Globals.DEFAULT_NUM_THREADS;
	private boolean useCache = false;
	private boolean saveCache = false;
	
	
	
	
	public void setScraperName(String scraperName){
		this.scraperName = scraperName;	
	}
	
	
	
	
	public void setTemplate(TemplateInterface template){
		this.template = template;
	}
	
	
	
	
	public TemplateInterface getTemplate(){
		return template;
	}
	
	
	
	
	public void setThreads(int threads){
		this.threads = threads;
	}
	
	
	
	
	public int getThreads() {
		return threads;
	}
	
	
	
	
	public String getTaskName(){
		return scraperName + "_" + template.getMetadata().getNamespace();
	}



	
	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}

	
	
	
	public boolean getUseCache() {
		return useCache;
	}




	public void setSaveCache(boolean saveCache) {
		this.saveCache = saveCache;
	}

	
	
	
	public boolean getSaveCache() {
		return saveCache;
	}
	
}
