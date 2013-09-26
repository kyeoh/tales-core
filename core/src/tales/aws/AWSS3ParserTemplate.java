package tales.aws;




import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import tales.services.Download;
import tales.services.DownloadException;
import tales.services.Logger;
import tales.services.TalesDB;
import tales.services.TalesException;
import tales.services.Task;
import tales.templates.TemplateCommon;
import tales.templates.TemplateException;
import tales.templates.TemplateMetadataInterface;
import tales.utils.Compress;




public class AWSS3ParserTemplate extends TemplateCommon{

	
	
	
	@Override
	public TemplateMetadataInterface getMetadata() {

		if(this.getTemplateConfig() == null){
			return null;
		}

		return this.getTemplateConfig().getTemplateMetadata();

	}




	@Override
	public void run(){	

		String url = getDownloadURL(this.getMetadata(), this.getTask());
		Logger.log(new Throwable(), "id: " + this.getTask().getDocumentId() + " - " + url);
		
		try {	
			
			S3 s3 = new S3();
			byte[] bytes = s3.getFile(metadata, url);
			bytes = new Compress().decompresGzipToBytes(bytes);
			
			System.out.println(new String(bytes, "UTF-8"));

			process(getTalesDB(), getTask(), url, null);

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
	
	
	
	
	@Override
	protected void process(TalesDB talesDB, Task task, String url, org.jsoup.nodes.Document doc) throws Exception{

	}




	@Override
	public boolean isTaskInvalid(Task task) {
		return false;
	}

}
