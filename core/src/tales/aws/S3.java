package tales.aws;




import java.io.ByteArrayInputStream;
import java.io.InputStream;

import tales.config.Globals;
import tales.services.Download;
import tales.services.DownloadException;
import tales.services.Logger;
import tales.services.TalesException;
import tales.templates.TemplateMetadataInterface;
import tales.utils.Compress;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;




public class S3 {




	private static AmazonS3Client s3;




	public void addTemplateDoc(TemplateMetadataInterface metadata, String filename, org.jsoup.nodes.Document doc) throws TalesException{

		try{

			String bucketName = checkBucket(metadata);
			
			byte[] bytes = new Compress().compresBytesToGzip(doc.html().getBytes());
			InputStream stream = new ByteArrayInputStream(bytes);
			
			s3.putObject(new PutObjectRequest(bucketName, filename, stream, new ObjectMetadata()));

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public void downloadAndAddURL(TemplateMetadataInterface metadata, String filename, String url) throws DownloadException{

		try{

			String bucketName = checkBucket(metadata);

			byte[] uncompressBytes = new Download().getURLBytes(url).getBytes();
			byte[] compressedBytes = new Compress().compresBytesToGzip(uncompressBytes);
			ByteArrayInputStream stream = new ByteArrayInputStream(compressedBytes);
			
			s3.putObject(new PutObjectRequest(bucketName, filename, stream, new ObjectMetadata()));

		}catch(Exception e){
			throw new DownloadException(new Throwable(), e, 0);
		}

	}




	private synchronized static String checkBucket(TemplateMetadataInterface metadata) throws TalesException{

		try{

			String bucketName = Globals.FILES_S3_BUCKET_NAME + metadata.getNamespace().replace("_", "-");

			if (s3 == null){

				s3 = new AmazonS3Client(new BasicAWSCredentials(AWSConfig.getAWSAccessKeyId(), AWSConfig.getAWSSecretAccessKey()));

				if(!s3.doesBucketExist(bucketName)) {
					Logger.log(new Throwable(), "creating -bucketName: " + bucketName);
					s3.createBucket(bucketName);
				}
			}

			return bucketName; 

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}

}
