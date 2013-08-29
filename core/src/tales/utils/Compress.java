package tales.utils;




import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;




public class Compress {
	
	
	
	
	public byte[] compresBytesToGzip(byte[] bytes) throws IOException{
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);

		gzos.write(bytes, 0, bytes.length);
		gzos.close();

		return baos.toByteArray();
		
	}
	
}
