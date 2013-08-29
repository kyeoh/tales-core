package tales.services;




import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.mysql.jdbc.Statement;

import tales.config.Config;
import tales.config.Globals;
import tales.utils.DBUtils;




public class LogsDB {




	private static Connection conn;
	private static String dbName = "logs";




	private static synchronized void init() throws TalesException{

		try{


			if(conn == null || conn.isClosed()){

				// checks if mysql is up
				DBUtils.waitUntilMysqlIsReady(Config.getLogDBHost(), 
						Config.getLogDBPort(), 
						Config.getLogDBUsername(), 
						Config.getLogDBPassword());

				// checks if the database exists, if not create it 
				DBUtils.checkDatabase(Config.getLogDBHost(), 
						Config.getLogDBPort(), 
						Config.getLogDBUsername(), 
						Config.getLogDBPassword(), 
						dbName);

				// connects
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://"+
						Config.getLogDBHost() +":"+ Config.getLogDBPort() +"/"+
						Globals.DATABASE_NAMESPACE + dbName +
						"?user="+Config.getLogDBUsername() +
						"&password="+Config.getLogDBPassword() +
						"&useUnicode=true&characterEncoding=UTF-8" +
						"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
						);

				if(!tableExists()){
					createTable();
				}

			}


		}catch(Exception e){
			e.printStackTrace(); // dont throw tales exception, this will allow us to reroute the connection.
		}

	}




	public static String getDBName(){
		return dbName;
	}




	private static synchronized void createTable() throws TalesException{

		try {


			String sql = "CREATE TABLE `logs` ("
					+ "`id` int(11) NOT NULL AUTO_INCREMENT,"
					+ "`publicDNS` varchar(100) COLLATE utf8_unicode_ci NOT NULL,"
					+ "`pid` int(7) COLLATE utf8_unicode_ci NOT NULL,"
					+ "`logType` varchar(100) COLLATE utf8_unicode_ci NOT NULL,"
					+ "`methodPath` varchar(500) COLLATE utf8_unicode_ci NOT NULL,"
					+ "`lineNumber` int(11) NOT NULL,"
					+ "`data` TEXT COLLATE utf8_unicode_ci NOT NULL,"
					+ "`added` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,"
					+ "PRIMARY KEY (`id`),"
					+ "KEY `publicDNS` (`publicDNS`),"
					+ "KEY `pid` (`pid`),"
					+ "KEY `logType` (`logType`),"
					+ "KEY `methodPath` (`methodPath`(333))"
					+ ") ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;";

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
			rs                  = statement.executeQuery("SHOW TABLES LIKE 'logs'");
			if (rs.next()) exists = true;

		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

		try{rs.close();}catch(Exception e){}
		try{statement.close();}catch(Exception e){}

		return exists;

	}




	public static void log(String publicDNS, int pid, String logType, String methodPath, int lineNumber, String data) throws TalesException{

		init();

		try{


			if(data != null){

				PreparedStatement statement = conn.prepareStatement("INSERT INTO logs (publicDNS, pid, logType, methodPath, lineNumber, data) values (?,?,?,?,?,?)");
				statement.setString(1, publicDNS);
				statement.setInt(2, pid);
				statement.setString(3, logType);
				statement.setString(4, methodPath);
				statement.setInt(5, lineNumber);
				statement.setString(6, data);
				statement.executeUpdate(); 
				statement.close();

			}


		}catch(Exception e){
			e.printStackTrace();
			//throw new TalesException(e); no logs or stackoverflow
		}

	}




	public static ArrayList<Log> getErrors(int amount) throws TalesException{

		init();

		try {


			ArrayList<Log> list = new ArrayList<Log>();
			PreparedStatement statement  = conn.prepareStatement("SELECT * FROM logs WHERE logType = \"" + Logger.ERROR + "\" ORDER BY id DESC LIMIT 0,?");
			statement.setInt(1, amount);

			statement.executeQuery();
			ResultSet rs                 = statement.executeQuery();

			while(rs.next()){

				Log log = new Log();
				log.setId(rs.getInt("id"));
				log.setPublicDNS(rs.getString("publicDNS"));
				log.setPid(rs.getInt("pid"));
				log.setLogType(rs.getString("logType"));
				log.setMethodPath(rs.getString("methodPath"));
				log.setLineNumber(rs.getInt("lineNumber"));
				log.setData(rs.getString("data"));
				log.setAdded(rs.getTimestamp("added"));

				list.add(log);
			}

			rs.close();
			statement.close();

			return list;


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public static Log getLog(int id) throws TalesException{

		init();

		try {


			PreparedStatement statement  = conn.prepareStatement("SELECT * FROM logs WHERE id = ?");
			statement.setInt(1, id);

			statement.executeQuery();
			ResultSet rs                 = statement.executeQuery();

			rs.next();

			Log log = new Log();
			log.setId(rs.getInt("id"));
			log.setPublicDNS(rs.getString("publicDNS"));
			log.setPid(rs.getInt("pid"));
			log.setLogType(rs.getString("logType"));
			log.setMethodPath(rs.getString("methodPath"));
			log.setLineNumber(rs.getInt("lineNumber"));
			log.setData(rs.getString("data"));
			log.setAdded(rs.getTimestamp("added"));

			rs.close();
			statement.close();

			return log;


		}catch(Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}
}
