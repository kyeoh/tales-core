package tales.utils;




import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;




public class GZIP {




	public byte[] compresBytesToGzip(byte[] bytes) throws IOException{

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream gzos = new GZIPOutputStream(baos);

		gzos.write(bytes, 0, bytes.length);
		gzos.close();

		return baos.toByteArray();

	}




	public byte[] decompresGzipToBytes(byte[] bytes) throws IOException{

		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		GZIPInputStream gzis = new GZIPInputStream(bais);
		InputStreamReader is = new InputStreamReader(gzis);
		return IOUtils.toByteArray(is);

	}

}
