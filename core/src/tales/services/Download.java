package tales.services;




import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import tales.config.Globals;




public class Download {




	private static String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_6_8) AppleWebKit/535.3 (KHTML, like Gecko) Chrome/15.0.874.121 Safari/535.2";
	private static boolean isSSLDisabled = false;




	public boolean urlExists(String url) {


		HttpURLConnection conn = null;


		try {


			if(url.equals(URLDecoder.decode(url, "UTF-8"))){
				url = URIUtil.encodeQuery(url);
			}


			if(url.contains("https://")){
				disableSSLValidation();
				conn = (HttpsURLConnection) new URL(url).openConnection();
			}else{
				conn = (HttpURLConnection) new URL(url).openConnection();
			}


			conn.setFollowRedirects(true);			
			conn.setRequestProperty("User-Agent", userAgent);			
			conn.setReadTimeout(Globals.DOWNLOADER_MAX_TIMEOUT_INTERVAL);			
			conn.setConnectTimeout(Globals.DOWNLOADER_MAX_TIMEOUT_INTERVAL);			
			conn.setRequestMethod("HEAD");			


			boolean result = (conn.getResponseCode() == HttpURLConnection.HTTP_OK);


			conn.disconnect();


			return result;


		}catch (Exception e){

			if (conn != null) {
				conn.disconnect();
			}

			return false;

		}

	}




	public String getURLContent(String url) throws DownloadException {

		try{

			DownloadByteResult result = getURLBytesWithCookieAndPost(url, null, null);
			return IOUtils.toString(result.getBytes(), result.getCharset());

		}catch(Exception e){
			String[] args = {url};
			throw new DownloadException(new Throwable(), e, 0, args);
		}

	}




	public String getURLContentWithPost(String url, String post) throws DownloadException {

		try{

			DownloadByteResult result = getURLBytesWithCookieAndPost(url, null, post);
			return IOUtils.toString(result.getBytes(), result.getCharset());

		}catch(Exception e){
			String[] args = {url, post};
			throw new DownloadException(new Throwable(), e, 0, args);
		}

	}




	public String getURLContentWithCookie(String url, String cookie) throws DownloadException {

		try{

			DownloadByteResult result = getURLBytesWithCookieAndPost(url, cookie, null);
			return IOUtils.toString(result.getBytes(), result.getCharset());

		}catch(Exception e){
			String[] args = {url, cookie};
			throw new DownloadException(new Throwable(), e, 0, args);
		}

	}




	public DownloadByteResult getURLBytes(String url) throws DownloadException{
		return getURLBytesWithCookieAndPost(url, null, null);
	}




	public String getURLContentWithCookieAndPost(String url, String cookie, String post) throws DownloadException{

		try{

			DownloadByteResult result = getURLBytesWithCookieAndPost(url, cookie, post);
			return IOUtils.toString(result.getBytes(), result.getCharset());

		}catch(Exception e){
			String[] args = {url, cookie, post};
			throw new DownloadException(new Throwable(), e, 0, args);
		}

	}





	public DownloadByteResult getURLBytesWithCookieAndPost(String url, String cookie, String post) throws DownloadException{


		HttpURLConnection conn = null;


		try {


			if(url.equals(URLDecoder.decode(url, "UTF-8"))){
				url = URIUtil.encodeQuery(url);
			}


			if(url.contains("https://")){
				disableSSLValidation();
				conn = (HttpsURLConnection) new URL(url).openConnection();
			}else{
				conn = (HttpURLConnection) new URL(url).openConnection();
			}


			conn.setFollowRedirects(true);
			
			if(cookie != null){
				conn.setRequestProperty("Cookie", cookie);
			}
			
			conn.setRequestProperty("Accept-Encoding", "deflate, gzip");
			conn.setRequestProperty("User-Agent", userAgent);
			conn.setRequestProperty("Accept","*/*");
			conn.setReadTimeout(Globals.DOWNLOADER_MAX_TIMEOUT_INTERVAL);
			conn.setConnectTimeout(Globals.DOWNLOADER_MAX_TIMEOUT_INTERVAL);

			
			// post
			if(post != null){

				conn.setDoOutput(true);
				conn.setRequestMethod("POST");

				DataOutputStream wr = new DataOutputStream (conn.getOutputStream());
				wr.writeBytes(post);
				wr.flush();
				wr.close();

			}

			
			// downloads the content
			InputStream is = conn.getInputStream();


			// checks the types of data
			String encoding = conn.getContentEncoding();
			if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
				is = new GZIPInputStream(is);


			} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
				is = new InflaterInputStream((is), new Inflater(true));

			}


			byte[] bytes = IOUtils.toByteArray(is);


			// charset
			String contentType = conn.getHeaderField("Content-Type");
			String charset = null;

			if(contentType != null){
				for (String param : contentType.replace(" ", "").split(";")) {
					if (param.startsWith("charset=")) {
						charset = param.split("=", 2)[1];
						break;
					}
				}
			}

			if(charset == null){

				try {

					String utf8Content = new String(bytes, "UTF-8");
					int posCharset = utf8Content.indexOf("charset");

					if (posCharset > 0) {

						utf8Content = utf8Content.substring(posCharset);
						int posEnd = utf8Content.indexOf("\"");
						if(posEnd == -1){
							posEnd = utf8Content.indexOf(";");
						}
						if(posEnd == -1){
							posEnd = utf8Content.indexOf("\\");
						}
						utf8Content = utf8Content.substring(0, posEnd);
						utf8Content = StringUtils.remove(utf8Content, " ");
						charset = utf8Content.substring(8);

					}

				} catch (Exception e) {
					String[] args = {url};
					new TalesException(new Throwable(), e, args);
				}

			}


			// close input stream conns
			is.close();
			conn.disconnect();


			// return
			DownloadByteResult result = new DownloadByteResult();
			result.setCharset(charset);
			result.setBytes(bytes);
			return result;


		}catch (Exception e) {

			String[] args = {url};
			int responseCode = 0;

			if (conn != null) {
				try {
					responseCode = conn.getResponseCode();
				} catch (IOException e1) {
					new DownloadException(new Throwable(), e1, responseCode, args);
				}
				conn.disconnect();
			}

			throw new DownloadException(new Throwable(), e, responseCode, args);
		}

	}




	private void disableSSLValidation() throws NoSuchAlgorithmException, KeyManagementException {

		if(!isSSLDisabled){

			/*
			 *  fix for
			 *    Exception in thread "main" javax.net.ssl.SSLHandshakeException:
			 *       sun.security.validator.ValidatorException:
			 *           PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException:
			 *               unable to find valid certification path to requested target
			 */
			TrustManager[] trustAllCerts = new TrustManager[] {
					new X509TrustManager() {
						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return null;
						}

						public void checkClientTrusted(X509Certificate[] certs, String authType) {  }

						public void checkServerTrusted(X509Certificate[] certs, String authType) {  }

					}
			};

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) {
					return true;
				}
			};

			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);


			isSSLDisabled = true;

		}

	}

}