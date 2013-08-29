package tales.services;




public class DownloadException extends Exception {



	
	private int responseCode;
	private static final long serialVersionUID = 1L;



	
	public DownloadException(Throwable origin, Exception error, int responseCode){
		this.responseCode = responseCode;
		try {
        	Logger.downloadError(origin, error);
        } catch (Exception j) {}
    }
    
    
    
    
    public DownloadException(Throwable origin, Exception error, int responseCode, String args[]){
    	this.responseCode = responseCode;
    	try {
        	Logger.downloadError(origin, error, args);
		} catch (Exception j) {}
    }
    
    
    
    
    public int getResponseCode(){
    	return responseCode;
    }
    
}