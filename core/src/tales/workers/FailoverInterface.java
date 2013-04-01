package tales.workers;




public interface FailoverInterface {

		
	
	
	public void fail();
	public void failover();
	public boolean isFailingOver();
	public boolean hasFailover();
	public void stop();
	
}