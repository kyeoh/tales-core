package tales.s3;




import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Logger;
import tales.services.TalesException;
import tales.services.Task;
import tales.templates.TemplateMetadataInterface;




public class S3 {




	private static AmazonS3Client s3;
	
	
	
	
	public void addTemplateDoc(TemplateMetadataInterface metadata, Task task, org.jsoup.nodes.Document doc) throws TalesException{

		try{
			
			
			String bucketName = Globals.HTML_S3_BUCKET_NAME;
			String fileName = new Date().getTime() + "-" + metadata.getDatabaseName() + task.getDocumentName();

			if (s3 == null){

				s3 = new AmazonS3Client(new BasicAWSCredentials(Config.getAWSAccessKeyId(), Config.getAWSSecretAccessKey()));

				if(!s3.doesBucketExist(bucketName)) {
					Logger.log(new Throwable(), "creating -bucketName: " + bucketName);
					s3.createBucket(bucketName);
				}
			}

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			GZIPOutputStream gzos = new GZIPOutputStream(baos);
			byte[] uncompressedBytes = doc.html().getBytes();

			gzos.write(uncompressedBytes, 0, uncompressedBytes.length);
			gzos.close();

			InputStream stream = new ByteArrayInputStream(baos.toByteArray());
			s3.putObject(new PutObjectRequest(bucketName, fileName, stream, new ObjectMetadata()));

			
		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}

}
