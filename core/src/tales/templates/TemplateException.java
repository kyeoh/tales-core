package tales.templates;




import tales.services.Logger;
import tales.services.Task;




public class TemplateException extends Exception{

	

	
	private static final long serialVersionUID = 1L;

	
	
	
	public TemplateException(Throwable origin, Exception error, Task task){

		try {
			Logger.templateError(origin, error, task.getDocumentId(), task.getDocumentName());
		} catch (Exception j) {
			j.printStackTrace();
		}

	}

}
