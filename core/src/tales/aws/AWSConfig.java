package tales.aws;




import java.io.File;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.io.FileUtils;

import tales.config.Globals;




public class AWSConfig{
	
	
	
	
	public static String getId() throws Exception{
		return load().getString("id");
	}
	
	
	
	
	public static String getAWSAccessKeyId() throws Exception{
		return load().getString("accessKeyId");
	}
	
	
	
	
	public static String getAWSSecretAccessKey() throws Exception{
		return load().getString("secretAccessKey");
	}




	public static String getAWSAMI() throws Exception{
		return load().getString("ami");
	}




	public static String getAWSSecurityGroup() throws Exception{
		return load().getString("securityGroup");
	}




	public static String getAWSInstanceType() throws Exception{
		return load().getString("instanceType");
	}




	public static String getAWSEndpoint() throws Exception{
		return load().getString("endpoint");
	}




	private static JSONObject load() throws Exception{
		
		File file = new File(Globals.AWS_CONFIG_FILE);
		String data = FileUtils.readFileToString(file);
		return (JSONObject) JSONSerializer.toJSON(data);
		
	}

}
