package tales.aws;




import tales.services.TalesDB;
import tales.services.Task;
import tales.templates.TemplateAbstract;
import tales.templates.TemplateException;
import tales.utils.GZIP;




public abstract class AWSParserTemplate extends TemplateAbstract{




	@Override
	public void run(){	

		String url = this.getTask().getDocumentName();

		try {	

			S3 s3 = new S3();

			if(s3.fileExists(url)){

				byte[] bytes = s3.getFile(url);
				bytes = new GZIP().decompresGzipToBytes(bytes);

				process(this.getTalesDB(), this.getTask(), url, bytes);

			}

		} catch (Exception e) {
			new TemplateException(new Throwable(), e, this.getTask());
		}

		active = false;

	}




	protected abstract void process(TalesDB talesDB, Task task, String url, byte[] bytes) throws Exception;

}
