package tales.aws;




import java.net.InetAddress;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import tales.server.CloudProviderInterface;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;




public class EC2 implements CloudProviderInterface{
	
	
	
	
	private Instance instance;




	@Override
	public String getId() throws Exception{
		return AWSConfig.getId();
	}
	
	
	
	
	@Override
	public String newServer(HttpServletRequest request) throws Exception{


		// instance type
		String instanceType = request.getParameter("instanceType");
		if(instanceType == null){
			instanceType = AWSConfig.getAWSInstanceType();
		}


		// ec2
		AmazonEC2 ec2 = new AmazonEC2Client(new BasicAWSCredentials(AWSConfig.getAWSAccessKeyId(), AWSConfig.getAWSSecretAccessKey()));
		ec2.setEndpoint(AWSConfig.getAWSEndpoint());

		RunInstancesRequest ec2Request = new RunInstancesRequest();
		ec2Request.withImageId(AWSConfig.getAWSAMI());
		ec2Request.withInstanceType(instanceType);
		ec2Request.withMinCount(1);
		ec2Request.withMaxCount(1);
		ec2Request.withSecurityGroupIds(AWSConfig.getAWSSecurityGroup());


		// creates the server
		RunInstancesResult runInstancesRes = ec2.runInstances(ec2Request);
		String instanceId = runInstancesRes.getReservation().getInstances().get(0).getInstanceId();


		// waits for the instance to be ready
		String publicDNS = "";
		while(true){

			try{

				DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
				describeInstancesRequest.setInstanceIds(Collections.singletonList(instanceId));

				DescribeInstancesResult describeResult = ec2.describeInstances(describeInstancesRequest);
				List <Reservation> list  = describeResult.getReservations();

				// when ready
				publicDNS = list.get(0).getInstances().get(0).getPublicDnsName();					
				if(list.get(0).getInstances().get(0).getState().getCode() != 0 && publicDNS != null){
					break;
				}

			}catch(Exception e){}

			Thread.sleep(100);
		}

		return publicDNS;

	}
	
	
	
	
	private Instance getAWSInstanceMetadata() throws Exception{

		if(instance != null){
			return instance;
		}

		AmazonEC2 ec2 = new AmazonEC2Client(new BasicAWSCredentials(AWSConfig.getAWSAccessKeyId(), AWSConfig.getAWSSecretAccessKey()));

		DescribeInstancesResult describeInstancesRequest = ec2.describeInstances();
		List<Reservation> reservations = describeInstancesRequest.getReservations();

		for(Reservation reservation : reservations) {

			for(Instance instance : reservation.getInstances()){

				if(instance.getPrivateIpAddress() != null && instance.getPrivateIpAddress().equals(InetAddress.getLocalHost().getHostAddress())){
					this.instance = instance;
					return instance;
				}

			}

		}

		return null;

	}
	
	
	
	
	public boolean isApplicationRunningHere() throws Exception {
		if(getAWSInstanceMetadata() != null){
			return true;
		}
		return false;
	}
	
	
	
	
	public String getDNS() throws Exception{
		return instance.getPublicDnsName();
	}
	
	
	
	
	public void delete() throws Exception{
		
		AmazonEC2 ec2 = new AmazonEC2Client(new BasicAWSCredentials(AWSConfig.getAWSAccessKeyId(), AWSConfig.getAWSSecretAccessKey()));
		TerminateInstancesRequest terminate = new TerminateInstancesRequest();
		terminate.getInstanceIds().add(getAWSInstanceMetadata().getInstanceId());
		ec2.terminateInstances(terminate);
		
	}

}
