package tales.templates;




import java.util.ArrayList;
import tales.templates.TemplateMetadataInterface;




public class TemplateMetadata implements TemplateMetadataInterface{




	private String namespace;
	private String baseURL;
	private ArrayList<String> requiredDocuments;




	public TemplateMetadata(String namespace, String baseURL, ArrayList<String> requiredDocuments){
		this.namespace = namespace;
		this.baseURL = baseURL;
		this.requiredDocuments = requiredDocuments;
	}
	
	
	
	
	@Override
	public String getNamespace() {
		return namespace;
	}
	
	
	
	
	@Override
	public String getBaseURL() {
		return baseURL;
	}
	
	
	
	
	@Override
	public ArrayList<String> getRequiredDocuments(){
		return requiredDocuments;
	}

}