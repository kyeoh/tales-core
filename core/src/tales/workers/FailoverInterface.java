package tales.workers;




public interface FailoverInterface {

		
	
	public void fail();
	public void failover();
	public boolean isFallingOver();
	public boolean hasFailover();
	public void stop();
	
}