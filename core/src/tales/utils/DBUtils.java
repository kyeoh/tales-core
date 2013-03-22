package tales.utils;




import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import tales.config.Config;
import tales.config.Globals;
import tales.services.Logger;
import tales.services.TalesException;

import com.mysql.jdbc.Statement;




public class DBUtils {




	public static void checkDatabase(String dbName) throws TalesException{

		try {


			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection("jdbc:mysql://"+
					Config.getDataDBHost(dbName)+":"+Config.getDBPort(dbName)+"/"+
					"mysql" +
					"?user="+Config.getDBUsername() +
					"&password="+Config.getDBPassword() +
					"&useUnicode=true&characterEncoding=UTF-8" +
					"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
					);

			if(!databaseExists(conn, dbName)){
				createDatabase(conn, dbName);
			}

			conn.close();


		}catch(final Exception e){
			String[] args = {Config.getDataDBHost(dbName), Config.getDBPort(dbName) + ""};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	private static boolean databaseExists(Connection conn, String dbName) throws TalesException{


		boolean exists          = false;
		Statement statement     = null;
		ResultSet rs            = null;


		try {


			statement           = (Statement) conn.createStatement();
			rs                  = statement.executeQuery("SHOW DATABASES LIKE '" + Globals.DATABASE_NAMESPACE + dbName + "'");
			if (rs.next()) exists = true;


		}catch(final Exception e){
			final String[] args = {dbName};
			throw new TalesException(new Throwable(), e, args);
		}


		try{rs.close();}catch(final Exception e){}
		try{statement.close();}catch(final Exception e){}


		return exists;

	}




	private static void createDatabase(Connection conn, String dbName) throws TalesException{

		try {


			final String sql = "CREATE DATABASE " + Globals.DATABASE_NAMESPACE + dbName;
			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate(sql);
			statement.close();


		}catch(final Exception e){
			final String[] args = {dbName};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public static void waitUntilLocalMysqlIsReady() throws TalesException{
		waitUntilMysqlIsReady("localhost", Config.getDBPort());
	}
	
	
	
	
	public static void waitUntilMysqlIsReady(String host, int port){

		while(true){

			try {


				Class.forName("com.mysql.jdbc.Driver");
				Connection conn = DriverManager.getConnection("jdbc:mysql://"+
						host + ":" +port+"/"+
						"mysql" +
						"?user="+Config.getDBUsername() +
						"&password="+Config.getDBPassword() +
						"&useUnicode=true&characterEncoding=UTF-8" +
						"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
						);

				conn.close();
				break;


			}catch(final Exception e){	
				try {
					Logger.log(new Throwable(), "waiting for mysql to be ready...");
					Thread.sleep(1000);
				} catch (Exception e1) {}
			}

		}

	}




	public static ArrayList<String> getLocalTalesDBs() throws TalesException{

		try{


			ArrayList<String> dbNames = new ArrayList<String>();


			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection("jdbc:mysql://"+
					"localhost:"+Config.getDBPort()+"/"+
					"mysql" +
					"?user="+Config.getDBUsername() +
					"&password="+Config.getDBPassword() +
					"&useUnicode=true&characterEncoding=UTF-8" +
					"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
					);

			ResultSet rs = conn.getMetaData().getCatalogs();


			// lists the dbs
			while (rs.next()) {

				String dbName = rs.getString("TABLE_CAT");

				if(dbName.contains(Globals.DATABASE_NAMESPACE)){
					dbNames.add(dbName.replace(Globals.DATABASE_NAMESPACE, ""));
				}

			}

			rs.close();
			conn.close();


			return dbNames;


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public static final ArrayList<String> getTableNames(String dbName) throws TalesException{

		try{


			Class.forName("com.mysql.jdbc.Driver");
			final Connection conn = DriverManager.getConnection("jdbc:mysql://"+
					Config.getDataDBHost(dbName)+":"+Config.getDBPort(dbName)+"/"+
					Globals.DATABASE_NAMESPACE + dbName +
					"?user="+Config.getDBUsername(dbName) +
					"&password="+Config.getDBPassword(dbName) +
					"&useUnicode=true&characterEncoding=UTF-8" +
					"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
					);


			final PreparedStatement statement = conn.prepareStatement("SHOW TABLES");
			final ResultSet rs                = statement.executeQuery();
			final ArrayList<String> tables    = new ArrayList<String>();


			while(rs.next()){
				tables.add(rs.getString(1));
			}

			rs.close();
			statement.close();
			conn.close();


			return tables;


		}catch(final Exception e){
			String[] args = {dbName};
			throw new TalesException(new Throwable(), e, args);
		}

	}





	public static int getTableCount(String dbName, String tableName) throws TalesException{

		try{


			Class.forName("com.mysql.jdbc.Driver");
			final Connection conn = DriverManager.getConnection("jdbc:mysql://"+
					Config.getDataDBHost(dbName)+":"+Config.getDBPort(dbName)+"/"+
					Globals.DATABASE_NAMESPACE + dbName +
					"?user="+Config.getDBUsername() +
					"&password="+Config.getDBPassword() +
					"&useUnicode=true&characterEncoding=UTF-8" +
					"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
					);

			final PreparedStatement statement = conn.prepareStatement("SELECT count(*) FROM " + tableName);
			final ResultSet rs = statement.executeQuery();
			rs.next();
			
			final int count = rs.getInt(1);
			
			rs.close();
			statement.close();   
			conn.close();

			
			return count;


		}catch(final Exception e){
			String[] args = {dbName, tableName};
			throw new TalesException(new Throwable(), e, args);
		}

	}

}
