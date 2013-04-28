package tales.config;




public class Globals {

	
	
	
	public static int MIN_TASKS                          = 500;
	public static int MAX_TASKS                          = 5000;
	
	public static String ATTRIBUTE_TABLE_NAMESPACE       = "_"; // Attribute
	public static String DATABASE_NAMESPACE              = "tales_";
	public static String ENVIRONMENTS_CONFIG_DIR         = System.getProperty("user.home") + "/tales-templates/environments";
	
	public static String SOLR_INSTANCE_DIR               = System.getProperty("user.home") + "/tales-core/solr/tales/solr";
	public static String SOLR_INDEXES_DIR                = System.getProperty("user.home") + "/solr-indexes";
	
	public static String DB_BACKUP_TEMP_DIR              = System.getProperty("user.home") + "/tales-tmp/db-backups";
	public static String DB_RESTORE_TEMP_DIR             = System.getProperty("user.home") + "/tales-tmp/db-restores";
	
	public static String BACKUP_S3_BUCKET_NAME           = "tales-backups";
	public static String TEMP_S3_BUCKET_NAME             = "tales-temp";
	public static String FILES_S3_BUCKET_NAME            = "tales-files-";
	
	public static int GIT_SYNC_REFESH_INTERVAL           = 5000;
	public static int DOWNLOADER_MAX_TIMEOUT_INTERVAL    = 10000;
	public static int REDIS_TIMEOUT                      = 0;
	public static int DIR_SYNC_REFESH_INTERVAL           = 1000;
	public static int SOLR_COMMIT_WITHIN_INTERNAL        = 5000;	
	public static int RELOAD_CONFIG_INTERNAL             = 1000;
	public static int DASHBOARD_MAX_ERRORS               = 30;
	public static int DOCUMENT_NAME_MAX_LENGTH           = 2000;
	public static int DOCUMENT_ATTRIBUTE_DATA_MAX_LENGTH = 20000;
	
	public static String WEB_DIR                         = System.getProperty("user.home") + "/tales-core/core/web";
	public static String WEB_DASHBOARD                   = "dashboard.html";
		
}
