package tales.services;




import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import tales.config.Config;
import tales.config.Globals;
import tales.templates.TemplateConnectionCommon;
import tales.utils.DBUtils;

import com.mysql.jdbc.Statement;




public class ProcessDB {




	private static Connection conn;
	private static String dbName = "processes";




	private static synchronized void init() throws TalesException{

		try{


			if(conn == null || conn.isClosed()){

				// checks if mysql is up
				DBUtils.waitUntilMysqlIsReady(new TemplateConnectionCommon().getDataDBHost(), 
						new TemplateConnectionCommon().getDataDBPort(),
						new TemplateConnectionCommon().getDataDBUsername(),
						new TemplateConnectionCommon().getDataDBPassword());

				// checks if the database exists, if not create it 
				DBUtils.checkDatabase(new TemplateConnectionCommon().getDataDBHost(), 
						new TemplateConnectionCommon().getDataDBPort(),
						new TemplateConnectionCommon().getDataDBUsername(),
						new TemplateConnectionCommon().getDataDBPassword(),
						dbName);

				// connects
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://"+
						Config.getLogDBHost() +":"+ Config.getDataDBPort() +"/"+
						Globals.DATABASE_NAMESPACE + dbName +
						"?user="+Config.getDataDBUsername() +
						"&password="+Config.getDataDBPassword() +
						"&useUnicode=true&characterEncoding=UTF-8" +
						"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
						);

				if(!tableExists()){
					createTable();
				}

			}


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private static synchronized void createTable() throws TalesException{

		try {


			String sql = "CREATE TABLE " + dbName + " ("
					+ "id int(11) NOT NULL AUTO_INCREMENT,"
					+ "tpid varchar(1000) COLLATE utf8_unicode_ci NOT NULL,"
					+ "pid varchar(1000) COLLATE utf8_unicode_ci NOT NULL,"
					+ "process varchar(1000) COLLATE utf8_unicode_ci NOT NULL,"
					+ "dns varchar(1000) COLLATE utf8_unicode_ci NOT NULL,"
					+ "state varchar(100) COLLATE utf8_unicode_ci NOT NULL,"
					+ "added timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,"
					+ "PRIMARY KEY (id),"
					+ "KEY tpid (tpid),"
					+ "KEY state (state)"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1;";

			Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate(sql);
			statement.close();


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private static synchronized boolean tableExists() throws TalesException{

		boolean exists          = false;
		Statement statement     = null;
		ResultSet rs            = null;

		try {

			statement           = (Statement) conn.createStatement();
			rs                  = statement.executeQuery("SHOW TABLES LIKE '" + dbName + "'");
			if (rs.next()) exists = true;

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

		try{rs.close();}catch(Exception e){}
		try{statement.close();}catch(Exception e){}

		return exists;

	}




	public static void add(String tpid, int pid, String process, String dns, String state) throws TalesException{

		init();

		try{

			PreparedStatement statement = conn.prepareStatement("INSERT INTO " + dbName + " (tpid, pid, process, dns, state) values (?,?,?,?,?)");
			statement.setString(1, tpid);
			statement.setInt(2, pid);
			statement.setString(3, process);
			statement.setString(4, dns);
			statement.setString(5, state);
			statement.executeUpdate(); 
			statement.close();

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}
	
	
	
	
	public static boolean isFinished(String tpid) throws TalesException{

		init();

		try {

			
			boolean exists = false;

			PreparedStatement statement = conn.prepareStatement("SELECT count(*) FROM " + dbName + " WHERE tpid=? AND state=\"" + ProcessState.FINISHED + "\"");
			statement.setString(1, tpid);

			ResultSet rs = statement.executeQuery();
			rs.next();

			if(rs.getInt(1) > 0){
				exists = true;
			}

			rs.close();
			statement.close();

			return exists;
			

		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}			

	}

}
