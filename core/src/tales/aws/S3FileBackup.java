package tales.aws;




import java.io.File;
import java.util.Date;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import tales.config.Globals;
import tales.services.Logger;
import tales.services.TalesException;
import tales.system.AppMonitor;
import tales.system.TalesSystem;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;




public class S3FileBackup {




	private static AmazonS3 s3 = null;




	public static void backupAllExcept(String s3BucketName, String fileName) throws TalesException{

		try{

			
			File file = new File(fileName);
			long id = new Date().getTime();
			String s3Name = id  + "-" + TalesSystem.getPublicDNSName() + "-" + file.getName();
			Logger.log(new Throwable(), "-s3Name: " + s3Name + " -S3bucket: " + s3BucketName + " -file: " + file.getAbsolutePath() + " -filesize(mb): " + (double)(file.length() / (double)(1024L * 1024L)));


			// s3 is not really statefull or multithread -- not sure whats happening in the background
			// but this blows up if you make multiple instances
			if(s3 == null){
				s3 = new AmazonS3Client(new BasicAWSCredentials(AWSConfig.getAWSAccessKeyId(), AWSConfig.getAWSSecretAccessKey()));
			}


			// creates the s3 bucket if it doesnt exists
			if(!s3.doesBucketExist(s3BucketName)){
				Logger.log(new Throwable(), "creating bucket \"" + s3BucketName + "\"");
				s3.createBucket(s3BucketName);
			}


			// stores the file into s3
			s3.putObject(new PutObjectRequest(s3BucketName, s3Name, file));


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public static void main(String[] args){

		try{


			Options options = new Options();
			options.addOption("bucket", true, "s3 bucket name");
			options.addOption("file", true, "file name");
			CommandLineParser parser = new PosixParser();
			CommandLine cmd = parser.parse(options, args);

			String s3Bucket = null;
			if(cmd.hasOption("bucket")){
				s3Bucket = cmd.getOptionValue("bucket");
			}else{
				s3Bucket = Globals.BACKUP_S3_BUCKET_NAME;
			}

			String fileName = cmd.getOptionValue("file");


			// monitors the app performance
			AppMonitor.init();


			// backups the dbs
			S3FileBackup.backupAllExcept(s3Bucket, fileName);


			// stop
			AppMonitor.stop();
			System.exit(0);


		}catch(Exception e){
			AppMonitor.stop();
			new TalesException(new Throwable(), e);
			System.exit(0);
		}

	}
}