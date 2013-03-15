package tales.system;




import java.util.Date;

import net.sf.json.JSONObject;
import org.hyperic.sigar.Sigar;

import tales.services.Logger;
import tales.services.TalesException;




public class ServerMonitor{




	private static Monitor monitor;




	public static void init(){
		
		if(monitor == null){
			monitor = new ServerMonitor().new Monitor();
			monitor.run();
		}
		
	}




	private class Monitor implements Runnable{




		private long start;




		public Monitor(){
			start = System.currentTimeMillis();
		}




		public void run(){


			try{
				
				
				Sigar sigar = new Sigar();
				JSONObject json = new JSONObject();
				
				// uptime
				json.put("uptime", ((new Date().getTime() - start) / 1000));
				
				// mem
				json.put("freeMemory", sigar.getMem().getFree());
				json.put("usedMemory", sigar.getMem().getUsed());
				json.put("totalMemory", sigar.getMem().getTotal());
				json.put("freeMemoryPorcent", sigar.getMem().getFreePercent());
				
				// cpu
				json.put("cpu", sigar.getCpuPerc().getCombined());
				
				// disk
				json.put("freeDisk", sigar.getFileSystemUsage("/").getFree());
				json.put("usedDisk", sigar.getFileSystemUsage("/").getUsed());
				json.put("totalDisk", sigar.getFileSystemUsage("/").getTotal());
				json.put("freeDiskPorcent", 1 - sigar.getFileSystemUsage("/").getUsePercent());

				// print
				Logger.log(new Throwable(), json.toString());

				
				Thread.sleep(1000);
				
				
			}catch(Exception e){
				new TalesException(new Throwable(), e);
			}
		
			
			// loop
			Thread t = new Thread(this);
			t.start();

		}

	}
	
	
	
	
	public static void main(String[] args) {
		ServerMonitor.init();
	}

}
