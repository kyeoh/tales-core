package tales.workers;




import tales.services.TalesException;




public interface FailoverInterface {

		
	
	
	public void fail() throws TalesException;
	public void failover() throws TalesException;
	public boolean isFailingOver();
	public boolean hasFailover();
	public void stop();
	
}