package tales.services;




import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import tales.config.Globals;
import tales.templates.TemplateConnectionInterface;
import tales.templates.TemplateMetadataInterface;
import tales.utils.DBUtils;

import com.mysql.jdbc.Statement;




public class TalesDB {




	private String dbName;
	private Connection conn;




	// index
	private final static HashMap<String, Integer> index = new HashMap<String, Integer>();

	// connections
	private final static HashMap<String, ArrayList<Connection>> conns = new HashMap<String, ArrayList<Connection>>();

	// cache tables
	private final static HashMap<String, ArrayList<String>> cachedTables = new HashMap<String, ArrayList<String>>();



	
	public TalesDB(final int threads, final TemplateConnectionInterface connMetadata, final TemplateMetadataInterface metadata) throws TalesException{

		this.dbName = metadata.getNamespace();

		try{

			// db conn
			conn = TalesDB.connect(threads, connMetadata, metadata);  

		}catch(final Exception e){
			final String[] args = {"dbName: " + dbName};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	private synchronized final static Connection connect(final int threads, final TemplateConnectionInterface connMetadata, final TemplateMetadataInterface metadata) throws TalesException{

		String dbName = metadata.getNamespace();

		try{


			// database and memcache connections
			if(!conns.containsKey(dbName)){


				// checks if mysql is up
				DBUtils.waitUntilMysqlIsReady(connMetadata.getDataDBHost(), 
						connMetadata.getDataDBPort(),
						connMetadata.getDataDBUsername(),
						connMetadata.getDataDBPassword());


				// init connection holders
				conns.put(dbName, new ArrayList<Connection>());
				cachedTables.put(dbName, new ArrayList<String>());


				// checks if the database exists, if not create it
				DBUtils.checkDatabase(connMetadata.getDataDBHost(), 
						connMetadata.getDataDBPort(), 
						connMetadata.getDataDBUsername(), 
						connMetadata.getDataDBPassword(), 
						metadata.getNamespace());


				// creates the conns
				Logger.log(new Throwable(), "[" + dbName + "] openning " + threads + " connections to host \"" + connMetadata.getDataDBHost() + "\" database \"" + dbName + "\"");

				Connection conn = null;
				for(int i = 0; i < threads; i++){

					Class.forName("com.mysql.jdbc.Driver");
					conn = DriverManager.getConnection("jdbc:mysql://"+
							connMetadata.getDataDBHost()+":"+connMetadata.getDataDBPort()+"/"+
							Globals.DATABASE_NAMESPACE + dbName +
							"?user="+connMetadata.getDataDBUsername() +
							"&password="+connMetadata.getDataDBPassword() +
							"&useUnicode=true&characterEncoding=UTF-8" +
							"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
							);
					conns.get(dbName).add(conn);

				}


				// checks if the doc table exists
				Logger.log(new Throwable(), "[" + dbName + "] checking documents table...");
				if(!TalesDB.documentsTableExists(conn)){
					TalesDB.createDocumentsTable(conn);
				}


				// adds the first document if none
				Logger.log(new Throwable(), "[" + dbName + "] checking required documents...");
				if(metadata.getRequiredDocuments() != null && metadata.getRequiredDocuments().size() > 0){
					
					for(final String document : metadata.getRequiredDocuments()){
						
						if(!new TalesDB(threads, connMetadata, metadata).documentExists(document)){
							Logger.log(new Throwable(), "-adding: " + document);
							new TalesDB(threads, connMetadata, metadata).addDocument(document);
						}
						
					}
				
				}else if(metadata.getRequiredDocuments() == null || metadata.getRequiredDocuments().size() == 0){
					
					if(!new TalesDB(threads, connMetadata, metadata).documentExists("/")){
						Logger.log(new Throwable(), "-adding: /");
						new TalesDB(threads, connMetadata, metadata).addDocument("/");
					}
					
				}


				// checks if the ignored document table exists
				Logger.log(new Throwable(), "[" + dbName + "] checking ignored documents table...");
				if(!TalesDB.ignoredDocumentsTableExists(conn)){
					TalesDB.createIgnoredDocumentsTable(conn);
				}

			}


			// index
			if(!index.containsKey(dbName)){
				index.put(dbName, 0);
			}else{
				index.put(dbName, index.get(dbName) + 1);
			}

			if(index.get(dbName) >= conns.get(dbName).size()){
				index.put(dbName, 0);
			}

			return conns.get(dbName).get(index.get(dbName));


		}catch(final Exception e){
			final String[] args = {"dbName: " + dbName};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final String getDBName(){
		return this.dbName;
	}




	public synchronized final int addDocument(final String name) throws TalesException{
		
		try{


			// db query
			final PreparedStatement statement = conn.prepareStatement("INSERT INTO documents (name) values (?)", Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, name);
			statement.executeUpdate();


			final ResultSet rs = statement.getGeneratedKeys();
			rs.next();


			final int id = rs.getInt(1);


			rs.close();
			statement.close();


			return id;


		}catch(final Exception e){

			final String[] args = {"name: " + name};
			throw new TalesException(new Throwable(), e, args);

		}

	}




	public synchronized final boolean documentExists(final String name) throws TalesException{

		try {


			boolean exists = false;


			final PreparedStatement statement = conn.prepareStatement("SELECT count(*) FROM documents WHERE name=? LIMIT 1");
			statement.setString(1, name);


			final ResultSet rs = statement.executeQuery();
			rs.next();


			if(rs.getInt(1) > 0){
				exists = true;
			}


			rs.close();
			statement.close();


			return exists;


		}catch(final Exception e){
			final String[] args = {"name: " + name};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public synchronized final boolean documentIdExists(final int documentId) throws TalesException{

		try {


			boolean exists = false;


			final PreparedStatement statement = conn.prepareStatement("SELECT count(*) FROM documents WHERE id=? LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs = statement.executeQuery();
			rs.next();


			if(rs.getInt(1) > 0){
				exists = true;
			}


			rs.close();
			statement.close();


			return exists;


		}catch(final Exception e){
			final String[] args = {"documentId: " + documentId};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final int getDocumentId(final String name) throws TalesException{

		try {


			final PreparedStatement statement = conn.prepareStatement("SELECT id FROM documents WHERE name=? LIMIT 1");
			statement.setString(1, name);


			final ResultSet rs = statement.executeQuery();
			rs.next();


			int id = rs.getInt("id");


			rs.close();
			statement.close();


			return id;


		}catch(final Exception e){
			final String[] args = {"name: " + name};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final Document getAndUpdateLastCrawledDocument() throws TalesException{

		try {
			return getAndUpdateLastCrawledDocuments(1).get(0);

		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public final ArrayList<Document> getAndUpdateLastCrawledDocuments(final int number) throws TalesException{


		try {


			final ArrayList<Document> list      = new ArrayList<Document>();
			final PreparedStatement statement   = conn.prepareStatement("SELECT *,lastUpdate FROM documents WHERE active = 1 ORDER BY lastUpdate ASC LIMIT 0,?");
			statement.setInt(1, number);


			final ResultSet rs                  = statement.executeQuery();

			while(rs.next()){

				final Document document = new Document();
				document.setId(rs.getInt("id"));
				document.setName(rs.getString("name"));
				document.setAdded(rs.getTimestamp("added"));
				document.setLastUpdate(rs.getTimestamp("lastUpdate"));
				document.setActive(rs.getBoolean("active"));

				list.add(document);

				// update
				updateDocumentLastUpdate(document.getId());

			}


			rs.close();
			statement.close();


			return list;


		}catch(final Exception e){
			final String[] args = {"number: " + number};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final ArrayList<Document> getMostRecentCrawledDocuments(final int number) throws TalesException{


		try {


			final ArrayList<Document> list      = new ArrayList<Document>();
			final PreparedStatement statement   = conn.prepareStatement("SELECT *,lastUpdate FROM documents WHERE active = 1 ORDER BY lastUpdate DESC LIMIT 0,?");
			statement.setInt(1, number);


			final ResultSet rs                  = statement.executeQuery();

			while(rs.next()){

				final Document document = new Document();
				document.setId(rs.getInt("id"));
				document.setName(rs.getString("name"));
				document.setAdded(rs.getTimestamp("added"));
				document.setLastUpdate(rs.getTimestamp("lastUpdate"));
				document.setActive(rs.getBoolean("active"));

				list.add(document);

			}


			rs.close();
			statement.close();


			return list;


		}catch(final Exception e){
			final String[] args = {"number: " + number};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final void updateDocumentLastUpdate(final int documentId) throws TalesException{


		try {


			final PreparedStatement statement = conn.prepareStatement("UPDATE documents SET lastUpdate=? WHERE id=?");
			statement.setTimestamp(1, new Timestamp(new Date().getTime()));
			statement.setInt(2, documentId);
			statement.executeUpdate();
			statement.close();


		}catch(final Exception e){
			final String[] args = {"documentId: " + documentId};
			throw new TalesException(new Throwable(), e, args);
		}	

	}




	public final void disableDocument(final int documentId) throws TalesException{


		try {


			final PreparedStatement statement = conn.prepareStatement("UPDATE documents SET active=? WHERE id=?");
			statement.setBoolean(1, false);
			statement.setInt(2, documentId);
			statement.executeUpdate();
			statement.close();


		}catch(final Exception e){
			final String[] args = {"documentId: " + documentId};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final Document getDocument(final int documentId) throws TalesException{


		try{


			final PreparedStatement statement = conn.prepareStatement("SELECT *,id FROM documents WHERE id = ? LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs                = statement.executeQuery();
			rs.next();


			final Document document = new Document();
			document.setId(rs.getInt("id"));
			document.setName(rs.getString("name"));
			document.setAdded(rs.getTimestamp("added"));
			document.setLastUpdate(rs.getTimestamp("lastUpdate"));
			document.setActive(rs.getBoolean("active"));


			rs.close();
			statement.close();


			return document;


		}catch(final Exception e){
			final String[] args = {"documentId: " + documentId};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public synchronized final int addAttribute(final Attribute attribute) throws TalesException{


		try {


			// checks if the db row xists
			TalesDB.checkAttributeTable(conn, dbName, attribute.getName());


			String tbName                 = Globals.ATTRIBUTE_TABLE_NAMESPACE + attribute.getName();
			tbName                        = tbName.replace(".", "_");


			final PreparedStatement statement = conn.prepareStatement("INSERT INTO " + tbName + " (documentId, data) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
			statement.setInt(1, attribute.getDocumentId());
			statement.setString(2, attribute.getData());
			statement.executeUpdate(); 


			final ResultSet rs = statement.getGeneratedKeys();
			rs.next();


			final int id = rs.getInt(1);


			rs.close();
			statement.close();


			return id;


		}catch(final Exception e){
			final String[] args = {"id: " + attribute.getId(),
					"documentId: " + attribute.getDocumentId(),
					"data: " + attribute.getData(),
					"attributeName: " + attribute.getName()};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public synchronized final void updateAttribute(final Attribute attribute) throws TalesException{


		try {


			// checks if the db row xists
			TalesDB.checkAttributeTable(conn, dbName, attribute.getName());


			// update
			final boolean attributeExists = attributeExist(attribute.getName(), attribute.getDocumentId());

			if(attributeExists){

				final String lastestData = getAttributeLastestStateData(attribute.getName(), attribute.getDocumentId());

				if (
						((lastestData == null && attribute.getData() != null) || (lastestData != null && attribute.getData() == null))
						|| (lastestData != null && attribute.getData() != null && !lastestData.equals(attribute.getData()))
						){

					addAttribute(attribute);

				}

			}else if(!attributeExists){
				addAttribute(attribute);
			}



		}catch(final Exception e){
			final String[] args = {"id: " + attribute.getId(),
					"documentId: " + attribute.getDocumentId(),
					"data: " + attribute.getData(),
					"attributeName: " + attribute.getName()};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public synchronized final boolean attributeExist(final String attributeName, final int documentId) throws TalesException{


		if(!TalesDB.attributeTableExists(conn, dbName, attributeName)){
			return false;
		}


		try {


			boolean exists                = false;


			String tbName                 = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                        = tbName.replace(".", "_");


			final PreparedStatement statement = conn.prepareStatement("SELECT count(*) FROM " + tbName + " WHERE documentId = ? LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs = statement.executeQuery();
			rs.next();


			if(rs.getInt(1) > 0){
				exists  = true;
			}


			rs.close();
			statement.close();


			return exists;


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName, "documentId: " + documentId};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final String getAttributeLastestStateData(final String attributeName, final int documentId) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");

			final PreparedStatement statement  = conn.prepareStatement("SELECT data FROM " + tbName + " WHERE documentId = ? ORDER BY id DESC LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs                 = statement.executeQuery();
			rs.next();


			final String data = rs.getString("data");


			rs.close();
			statement.close();


			return data;


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName, "documentId: " + documentId};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final Attribute getAttributeLastestState(final String attributeName, final int documentId) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");


			final PreparedStatement statement  = conn.prepareStatement("SELECT * FROM " + tbName + " WHERE documentId = ? ORDER BY id DESC LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs                 = statement.executeQuery();
			rs.next();


			final Attribute attribute     = new Attribute(documentId, attributeName);
			attribute.setId(rs.getInt("id"));
			attribute.setData(rs.getString("data"));
			attribute.setAdded(rs.getTimestamp("added"));


			rs.close();
			statement.close();


			return attribute;


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName, "documentId: " + documentId};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final Attribute getAttributeFirstState(final String attributeName, final int documentId) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");


			final PreparedStatement statement  = conn.prepareStatement("SELECT * FROM " + tbName + " WHERE documentId = ? ORDER BY id ASC LIMIT 1");
			statement.setInt(1, documentId);


			final ResultSet rs                 = statement.executeQuery();
			rs.next();


			final Attribute attribute     = new Attribute(documentId, attributeName);
			attribute.setId(rs.getInt("id"));
			attribute.setData(rs.getString("data"));
			attribute.setAdded(rs.getTimestamp("added"));


			rs.close();
			statement.close();


			return attribute;


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName, "documentId: " + documentId};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final ArrayList<Attribute> getAttributeStates(final String attributeName, final int documentId, final int limit) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");


			final PreparedStatement statement  = conn.prepareStatement("SELECT data, added FROM " + tbName + " WHERE documentId = ? ORDER BY id DESC LIMIT ?");
			statement.setInt(1, documentId);
			statement.setInt(2, limit);


			final ResultSet rs                 = statement.executeQuery();

			ArrayList<Attribute> attributes    = new ArrayList<Attribute>();
			while(rs.next()){

				final Attribute attribute      = new Attribute(documentId, attributeName);
				attribute.setData(rs.getString("data"));
				attribute.setAdded(rs.getTimestamp("added"));
				attributes.add(attribute);

			}


			rs.close();
			statement.close();


			return attributes;


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName, "documentId: " + documentId, "limit: " + limit};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	private synchronized static final void checkAttributeTable(final Connection conn, final String dbName, final String attributeName) throws TalesException{
		if(!attributeTableExists(conn, dbName, attributeName)){
			createAttributeTable(conn, dbName, attributeName);
		}
	}




	private synchronized static final boolean attributeTableExists(final Connection conn, final String dbName, final String attributeName) throws TalesException{


		boolean exists = false;


		try {


			String tbName           = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                  = tbName.replace(".", "_");


			if(!cachedTables.get(dbName).contains(tbName)){


				final ResultSet tables = conn.getMetaData().getTables(null, null, tbName, null);
				if(tables.next()){
					exists = true;
					cachedTables.get(dbName).add(tbName);
				}


			}else{
				exists = true;
			}


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName};
			throw new TalesException(new Throwable(), e, args);
		}


		return exists;

	}




	private synchronized static final void createAttributeTable(final Connection conn, final String dbName, final String attributeName) throws TalesException{


		try {


			String tbName       = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName              = tbName.replace(".", "_");

			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate("CREATE TABLE " + tbName + " (id INT NOT NULL AUTO_INCREMENT, documentId INT NOT NULL, data TEXT CHARACTER SET utf8 COLLATE utf8_unicode_ci NULL, added TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (id), KEY documentId (documentId)) ENGINE = MYISAM DEFAULT CHARSET=utf8");
			statement.executeUpdate("OPTIMIZE TABLE " + tbName);
			statement.close();


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName};
			throw new TalesException(new Throwable(), e, args);
		}
		
	}




	public final int getDocumentsCount() throws TalesException{


		try {


			final PreparedStatement statement = conn.prepareStatement("SELECT COUNT(*) FROM documents");


			final ResultSet rs            = statement.executeQuery();
			rs.next();


			final int count               = rs.getInt(1);


			rs.close();
			statement.close();            


			return count;


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}

	
	
	
	private synchronized final static void createDocumentsTable(Connection conn) throws TalesException{


		try {


			final String sql = "CREATE TABLE documents (id int(11) NOT NULL AUTO_INCREMENT,"
					+ "name varchar(" + Globals.DOCUMENT_NAME_MAX_LENGTH + ") NOT NULL,"
					+ "added timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,"
					+ "lastUpdate timestamp NOT NULL DEFAULT '1999-12-31 17:00:00',"
					+ "active int(2) NOT NULL DEFAULT '1',"
					+ "PRIMARY KEY (id),"
					+ "KEY lastUpdate (lastUpdate),"
					+ "KEY active (active)"
					+ ") ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";


			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate(sql);
			statement.close();


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private synchronized final static boolean documentsTableExists(Connection conn) throws TalesException{


		boolean exists          = false;


		try {


			final ResultSet tables = conn.getMetaData().getTables(null, null, "documents", null);
			if(tables.next()){
				exists = true;
			}


		}catch(final Exception e){
			new TalesException(new Throwable(), e);
		}


		return exists;

	}




	private synchronized final static void createIgnoredDocumentsTable(Connection conn) throws TalesException{


		try {


			final String sql = "CREATE TABLE ignoredDocuments ("
					+ "id INT NOT NULL AUTO_INCREMENT, "
					+ "name VARCHAR(1000) NOT NULL, "
					+ "added timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, "
					+ "PRIMARY KEY (id)" 
					+ ") ENGINE = MYISAM DEFAULT CHARSET=utf8 AUTO_INCREMENT=1;";


			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate(sql);
			statement.close();


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	private synchronized final static boolean ignoredDocumentsTableExists(Connection conn) throws TalesException{


		boolean exists          = false;


		try {


			final ResultSet tables = conn.getMetaData().getTables(null, null, "ignoredDocuments", null);
			if(tables.next()){
				exists = true;
			}


		}catch(final Exception e){
			new TalesException(new Throwable(), e);
		}


		return exists;

	}




	public synchronized final void addIgnoredDocument(final String name) throws TalesException{


		try{


			final PreparedStatement statement = conn.prepareStatement("INSERT INTO ignoredDocuments (name) values (?)");
			statement.setString(1, name);
			statement.executeUpdate();
			statement.close();


		}catch(final Exception e){
			final String[] args = {"name: " + name};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public synchronized final boolean ignoredDocumentExists(final String name) throws TalesException{


		try{


			final PreparedStatement statement  = conn.prepareStatement("SELECT count(*) FROM ignoredDocuments WHERE name=? LIMIT 1");
			statement.setString(1, name);


			final ResultSet rs                 = statement.executeQuery();
			rs.next();


			boolean exists                     = false;
			if(rs.getInt(1) > 0){
				exists                         = true;
			}


			rs.close();
			statement.close();


			return exists;


		}catch(final Exception e){
			final String[] args = {"name: " + name};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final static void deleteAll(final TemplateConnectionInterface connMetadata, final TemplateMetadataInterface metadata) throws TalesException {


		try {


			// checks db
			DBUtils.checkDatabase(connMetadata.getDataDBHost(), 
					connMetadata.getDataDBPort(), 
					connMetadata.getDataDBUsername(), 
					connMetadata.getDataDBPassword(), 
					metadata.getNamespace());


			// conn
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection("jdbc:mysql://"+
					connMetadata.getDataDBHost()+":"+connMetadata.getDataDBPort()+"/"+
					Globals.DATABASE_NAMESPACE + metadata.getNamespace() +
					"?user="+connMetadata.getDataDBUsername() +
					"&password="+connMetadata.getDataDBPassword() +
					"&useUnicode=true&characterEncoding=UTF-8" +
					"&autoReconnect=true&failOverReadOnly=false&maxReconnects=10"
					);

			// db
			Logger.log(new Throwable(), "[" + metadata.getNamespace() + "] dropping database");

			final Statement statement = (Statement) conn.createStatement();
			statement.executeUpdate("drop database " + Globals.DATABASE_NAMESPACE + metadata.getNamespace());
			statement.close();

			conn.close();


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public synchronized final static void closeDBConnections(final String dbName) throws TalesException{


		try {


			// closes the connections
			if(conns.containsKey(dbName)){

				int i = conns.get(dbName).size();

				for(final Connection conn : conns.get(dbName)){
					conn.close();
				}

				conns.remove(dbName);

				Logger.log(new Throwable(), "[" + dbName + "] connections closed " + i);

			}


		}catch(final Exception e){
			throw new TalesException(new Throwable(), e);
		}

	}




	public final ArrayList<Document> getAllDocumentsWithAttributeOrderedByDocumentLastUpdateDesc(final String attributeName) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");


			final PreparedStatement statement  = conn.prepareStatement("SELECT DISTINCT " + tbName + ".documentId, documents.* FROM " + tbName + ", documents WHERE " + tbName + ".documentId = documents.id ORDER BY lastUpdate DESC;");

			final ResultSet rs                 = statement.executeQuery();

			final ArrayList<Document> documents   = new ArrayList<Document>();
			while(rs.next()){

				final Document document = new Document();
				document.setId(rs.getInt("id"));
				document.setName(rs.getString("name"));
				document.setAdded(rs.getTimestamp("added"));
				document.setLastUpdate(rs.getTimestamp("lastUpdate"));
				document.setActive(rs.getBoolean("active"));

				documents.add(document);

			}


			rs.close();
			statement.close();


			return documents;


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final ArrayList<Document> getAndUpdateLastCrawledDocumentsWithAttributeAndQuery(final String attributeName, final String query, final int number) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");
			PreparedStatement statement;


			if(query != null){	
				statement = conn.prepareStatement("SELECT documents.id, documents.name, documents.added, documents.lastUpdate, documents.active FROM " + tbName + " INNER JOIN documents ON " + tbName + ".documentId = documents.id WHERE " + tbName + ".data LIKE \"" + query + "\" ORDER BY documents.lastUpdate ASC LIMIT 0,?;");

			}else{
				statement = conn.prepareStatement("SELECT documents.id, documents.name, documents.added, documents.lastUpdate, documents.active FROM " + tbName + " INNER JOIN documents ON " + tbName + ".documentId = documents.id ORDER BY documents.lastUpdate ASC LIMIT 0,?;");
			}

			statement.setInt(1, number);


			final ResultSet rs                 = statement.executeQuery();

			final ArrayList<Document> documents = new ArrayList<Document>();
			while(rs.next()){

				final Document document = new Document();
				document.setId(rs.getInt("id"));
				document.setName(rs.getString("name"));
				document.setAdded(rs.getTimestamp("added"));
				document.setLastUpdate(rs.getTimestamp("lastUpdate"));
				document.setActive(rs.getBoolean("active"));

				documents.add(document);

				// update
				updateDocumentLastUpdate(document.getId());

			}


			rs.close();
			statement.close();


			return documents;


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName};
			throw new TalesException(new Throwable(), e, args);
		}

	}




	public final ArrayList<Document> getMostRecentCrawledDocumentsWithAttributeAndQuery(final String attributeName, final String query, int number) throws TalesException{


		try {


			String tbName                      = Globals.ATTRIBUTE_TABLE_NAMESPACE + attributeName;
			tbName                             = tbName.replace(".", "_");
			PreparedStatement statement;


			if(query != null){
				statement = conn.prepareStatement("SELECT documents.id, documents.name, documents.added, documents.lastUpdate, documents.active FROM " + tbName + " INNER JOIN documents ON " + tbName + ".documentId = documents.id WHERE " + tbName + ".data LIKE \"" + query + "\" ORDER BY documents.lastUpdate DESC LIMIT 0,?;");

			}else{
				statement = conn.prepareStatement("SELECT documents.id, documents.name, documents.added, documents.lastUpdate, documents.active FROM " + tbName + " INNER JOIN documents ON " + tbName + ".documentId = documents.id ORDER BY documents.lastUpdate DESC LIMIT 0,?;");
			}

			statement.setInt(1, number);


			final ResultSet rs                 = statement.executeQuery();

			final ArrayList<Document> documents = new ArrayList<Document>();
			while(rs.next()){

				final Document document = new Document();
				document.setId(rs.getInt("id"));
				document.setName(rs.getString("name"));
				document.setAdded(rs.getTimestamp("added"));
				document.setLastUpdate(rs.getTimestamp("lastUpdate"));
				document.setActive(rs.getBoolean("active"));

				documents.add(document);

			}


			rs.close();
			statement.close();


			return documents;


		}catch(final Exception e){
			final String[] args = {"attributeName: " + attributeName};
			throw new TalesException(new Throwable(), e, args);
		}

	}
	
}