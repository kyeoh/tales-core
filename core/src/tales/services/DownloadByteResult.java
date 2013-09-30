package tales.services;




public class DownloadByteResult {
	
	
	
	
	private String charset = "";
	private byte[] bytes;
	private String cookie;

	
	

	public void setCharset(String charset){
		this.charset = charset;
	}
	
	
	public String getCharset(){
		return charset;
	}
	
	
	
	
	public void setBytes(byte[] bytes){
		this.bytes = bytes;
	}
	
	
	public byte[] getBytes(){
		return bytes;
	}
	
	
	
	
	public void setCookie(String cookie){
		this.cookie = cookie;
	}
	
	
	public String getCookie(){
		return cookie;
	}
	
}
