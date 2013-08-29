package tales.templates;




import java.net.URL;
import java.util.ArrayList;

import tales.services.TalesException;




public abstract class TemplateMetadataCommon implements TemplateMetadataInterface{

	
	
		
	@Override
	public abstract String getBaseURL();

	
	
	
	@Override
	public ArrayList<String> getRequiredDocuments() {
		return null;
	}

	
	
	
	@Override
	public String getNamespace() {
		
		try{
			
			return new URL(getBaseURL()).getAuthority().replace(".", "_");
			
		}catch(Exception e){
			
			new TalesException(new Throwable(), e);
			return getBaseURL().replace(".", "_");
			
		}
		
	}

}
