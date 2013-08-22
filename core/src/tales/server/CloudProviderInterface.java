package tales.server;




import javax.servlet.http.HttpServletRequest;




public interface CloudProviderInterface {

	
	
	
	public String getId() throws Exception;
	public String newServer(HttpServletRequest request) throws Exception;
	public boolean isApplicationRunningHere() throws Exception;
	public String getDNS() throws Exception;
	public void delete() throws Exception;
	
}
