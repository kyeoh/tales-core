package tales.system;




import java.text.DecimalFormat;
import java.util.Date;

import net.sf.json.JSONObject;

import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.Mem;
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
		private final double BASE = 1024, KB = BASE, MB = KB*BASE, GB = MB*BASE;
	    private final DecimalFormat df = new DecimalFormat("#.##");




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
				Mem memory = sigar.getMem();
				json.put("freeMemory", formatSize(memory.getActualFree()));
				json.put("usedMemory", formatSize(memory.getActualUsed()));
				json.put("totalMemory", formatSize(memory.getTotal()));
				json.put("freeMemoryPorcent", df.format(memory.getFreePercent()) + "%");
				
				// cpu
				json.put("cpu", df.format(sigar.getCpuPerc().getUser() * 100) + "%");
				
				// disk
				FileSystemUsage fileSystem = sigar.getFileSystemUsage("/");
				json.put("freeDisk", formatSize(fileSystem.getAvail() * BASE));
				json.put("usedDisk", formatSize(fileSystem.getUsed() * BASE));
				json.put("totalDisk", formatSize(fileSystem.getTotal() * BASE));
				json.put("freeDiskPorcent", (1 - fileSystem.getUsePercent()) * 100 + "%");

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
		
		
		
		
		private String formatSize(double size) {
	        if(size >= GB) {
	            return df.format(size/GB) + " GB";
	        }
	        if(size >= MB) {
	            return df.format(size/MB) + " MB";
	        }
	        if(size >= KB) {
	            return df.format(size/KB) + " KB";
	        }
	        return "" + (int)size + " bytes";
	    }

	}
	
	
	
	
	public static void main(String[] args) {
		ServerMonitor.init();
	}

}
