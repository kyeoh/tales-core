package tales.scrapers;




import tales.templates.TemplateInterface;




public class ScraperConfig {
	
	
	
	
	private String scraperName;
	private TemplateInterface template;
	private int threads;
	
	
	
	
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
	
}
