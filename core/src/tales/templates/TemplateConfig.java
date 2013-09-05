package tales.templates;




public class TemplateConfig {
	
	
	
	
	private String scraperName;
	private TemplateInterface template;
	private int threads;
	private TemplateMetadataInterface templateMetadata;
	
	
	
	
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
		return scraperName + "_" + getTemplateMetadata().getNamespace();
	}




	public void setTemplateMetadata(TemplateMetadataInterface templateMetadata) {
		this.templateMetadata = templateMetadata;
	}
	
	
	
	
	public TemplateMetadataInterface getTemplateMetadata(){
		return templateMetadata;
	}
	
}
