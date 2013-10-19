package tales.templates;




import tales.services.DownloadException;
import tales.services.TalesDB;
import tales.services.Task;




public abstract class TemplateSimple extends TemplateAbstract{




	@Override
	public void run(){

		String url = this.getDownloadURL(this.getMetadata(), this.getTask());
		
		try {	
			process(this.getTalesDB(), this.getTask(), url);	
			
		} catch (Exception e) {
			new TemplateException(new Throwable(), e, this.getTask());

		}

		active = false;

	}




	protected abstract void process(TalesDB talesDB, Task task, String url) throws DownloadException, Exception;

}
