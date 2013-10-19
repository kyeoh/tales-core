package tales.templates;




import tales.services.DownloadException;
import tales.services.TalesDB;
import tales.services.TalesException;
import tales.services.Task;




public abstract class TemplateSimple extends TemplateAbstract{




	@Override
	public void run(){

		String url = this.getDownloadURL(this.getMetadata(), this.getTask());
		
		try {
			
			process(this.getTalesDB(), this.getTask(), url);	
			
		} catch (DownloadException e) {
			
			if(e.getResponseCode() != 404){
				failed = true;
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




	protected abstract void process(TalesDB talesDB, Task task, String url) throws DownloadException, Exception;

}
