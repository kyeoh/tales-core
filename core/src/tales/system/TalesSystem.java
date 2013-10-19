package tales.system;




import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.io.IOUtils;

import tales.config.Config;
import tales.server.CloudProviderInterface;
import tales.services.TalesException;




public class TalesSystem {




	private static String serverIP;
	private static OperatingSystemMXBean osbean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
	private static RuntimeMXBean runbean = (RuntimeMXBean) ManagementFactory.getRuntimeMXBean();
	private static int nCPUs = osbean.getAvailableProcessors();
	private static long prevUpTime = runbean.getUptime();
	private static float lastResult = 0;
	private static int pid = 0;
	private static long prevProcessCpuTime = ((com.sun.management.OperatingSystemMXBean) osbean).getProcessCpuTime();
	private static String processName;
	private static String branchName;




	public static float getServerCPUUsage() {

		long upTime = runbean.getUptime();
		long processCpuTime = ((com.sun.management.OperatingSystemMXBean) osbean).getProcessCpuTime();

		if (prevUpTime > 0L && upTime > prevUpTime && processCpuTime > prevProcessCpuTime && upTime > 0L && processCpuTime > 0L) {

			long elapsedCpu = processCpuTime - prevProcessCpuTime;
			long elapsedTime = upTime - prevUpTime;
			float cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * nCPUs));
			lastResult = cpuUsage;

		}

		prevUpTime = upTime;
		prevProcessCpuTime = processCpuTime;
		return lastResult;

	}




	public static float getFreeMemory() {
		return (Runtime.getRuntime().freeMemory()) / 1048576;
	}




	public static double getMemoryUsage() {
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
	}




	public static int getPid() {

		if (pid == 0) {
			String name = runbean.getName();
			String[] parts = name.split("@");
			pid = Integer.parseInt(parts[0]);
		}

		return pid;

	}




	public static String getPublicDNSName() throws TalesException{

		try{


			// cloud providers
			try{

				if(serverIP == null){

					for(CloudProviderInterface cloudProvider : Config.getCloudProviders()){

						if(cloudProvider.isApplicationRunningHere()){
							serverIP = cloudProvider.getDNS();
							break;
						}

					}

				}

			}catch(Exception e){
				e.printStackTrace();
			}


			// checks if its has an external ip		
			if(serverIP == null){

				Enumeration<NetworkInterface> n = NetworkInterface.getNetworkInterfaces();
				while(n.hasMoreElements()){

					NetworkInterface e = n.nextElement();
					Enumeration<InetAddress> a = e.getInetAddresses();

					while(a.hasMoreElements()){

						InetAddress addr = a.nextElement();
						String publicIP = addr.getHostAddress();

						if(publicIP.split("\\.").length == 4 && !publicIP.startsWith("10.") && !publicIP.startsWith("127.")){
							serverIP = publicIP;
							return serverIP;
						}

					}

				}

			}


			// last try to get the ip
			try{
				if(serverIP == null){
					serverIP = InetAddress.getLocalHost().getHostAddress();
				}
			}catch(Exception e){
				e.printStackTrace();
			}


			return serverIP;


		}catch( Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public static String getProcess() {

		if(processName == null){

			try{

				ProcessBuilder builder = new ProcessBuilder("/bin/sh", "-c", "/bin/ps aux | grep " + TalesSystem.getPid());
				Process process = builder.start();
				process.waitFor();

				processName = IOUtils.toString(process.getInputStream(), "utf-8");

				String find = "jar";
				int ini = processName.indexOf(find) + find.length() + 1;
				int end = processName.indexOf("\n", ini);

				if(end < ini){
					end = processName.length();
				}

				processName = processName.substring(ini, end);

				process.destroy();

			}catch( Exception e){
				String[] args = new String[]{processName};
				new TalesException(new Throwable(), e, args);
			}

		}

		return processName;
	}
	
	
	
	
	public static String getFolderGitBranchName(String path) throws TalesException{

		try{


			if(branchName == null){

				Process process = null;

				try{

					// linux
					ProcessBuilder builder = new ProcessBuilder("/usr/lib/git-core/git", "--git-dir", path + ".git", "branch");
					process = builder.start();

					String output = IOUtils.toString(process.getInputStream());
					process.destroy();

					int ini = output.indexOf("*");
					int end = output.indexOf("\n", ini);
					branchName = output.substring(ini + 2, end).trim(); // 2 cuz of * + space (example: * master);

				}catch(Exception e){
					branchName = "development";
				}

			}

			return branchName;


		}catch( Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}
}
