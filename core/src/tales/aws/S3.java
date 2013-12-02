package tales.aws;




import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import tales.config.Globals;
import tales.services.Download;
import tales.services.DownloadException;
import tales.services.Logger;
import tales.services.TalesException;
import tales.utils.GZIP;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;




public class S3 {




	private static AmazonS3Client s3;




	public void addTemplateDoc(String filename, org.jsoup.nodes.Document doc) throws TalesException{

		try{

			String bucketName = checkBucket();

			byte[] bytes = new GZIP().compresBytesToGzip(doc.html().getBytes());
			InputStream stream = new ByteArrayInputStream(bytes);

			ObjectMetadata objMetadata = new ObjectMetadata();
			objMetadata.setContentLength(bytes.length);

			s3.putObject(new PutObjectRequest(bucketName, filename, stream, objMetadata));

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public void addBytes(String filename, byte[] bytes) throws TalesException{

		try{

			String bucketName = checkBucket();

			bytes = new GZIP().compresBytesToGzip(bytes);
			InputStream stream = new ByteArrayInputStream(bytes);

			ObjectMetadata objMetadata = new ObjectMetadata();
			objMetadata.setContentLength(bytes.length);

			s3.putObject(new PutObjectRequest(bucketName, filename, stream, objMetadata));

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}
	
	
	
	
	public void downloadAndAddURL(String filename, String url) throws DownloadException{

		try{

			String bucketName = checkBucket();

			byte[] uncompressBytes = new Download().getURLBytes(url).getBytes();
			byte[] compressedBytes = new GZIP().compresBytesToGzip(uncompressBytes);
			InputStream stream = new ByteArrayInputStream(compressedBytes);

			ObjectMetadata objMetadata = new ObjectMetadata();
			objMetadata.setContentLength(compressedBytes.length);

			s3.putObject(new PutObjectRequest(bucketName, filename, stream, objMetadata));

		}catch(Exception e){
			throw new DownloadException(new Throwable(), e, 0);
		}

	}




	public byte[] getFile(String filename) throws Exception{

		String bucketName = checkBucket();
		S3Object obj = s3.getObject(bucketName, filename);
		return IOUtils.toByteArray(obj.getObjectContent());

	}




	public boolean fileExists(String filename) throws TalesException {
		
		String bucketName = checkBucket();
		ObjectListing list = s3.listObjects(bucketName, filename);
	    return list.getObjectSummaries().size() > 0;
		
	}
	
	
	
	
	private synchronized static String checkBucket() throws TalesException{

		try{

			String bucketName = Globals.FILES_S3_BUCKET_NAME;
			bucketName = bucketName.toLowerCase();
			
			if (s3 == null){

				Logger.log(new Throwable(), "checking aws s3 bucket -bucketName: " + bucketName);
				s3 = new AmazonS3Client(new BasicAWSCredentials(AWSConfig.getAWSAccessKeyId(), AWSConfig.getAWSSecretAccessKey()));

				if(!s3.doesBucketExist(bucketName)) {
					Logger.log(new Throwable(), "creating aws s3 bucket -bucketName: " + bucketName);
					s3.createBucket(bucketName);
				}
			}

			return bucketName; 

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}

}
