package tales.utils;




import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;




public class Deflate {




	public byte[] deflate(byte[] bytes) throws IOException{

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DeflaterOutputStream gzos = new DeflaterOutputStream(baos);

		gzos.write(bytes, 0, bytes.length);
		gzos.close();

		return baos.toByteArray();

	}

}
