package graph;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

import org.json.simple.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Starter class. Instantiates connections to SQL and neo4j databases.
 * @author i2scmg
 *
 */
public class Main {
    private static final File DB_Folder = new File("C:\\i2S-devenv\\workspace\\BPMN-graph\\db");
    
    public static class JSONEdge extends JSONObject{
        @Override
        public boolean equals(Object o){
        if (o instanceof JSONEdge) {
            JSONEdge c = (JSONEdge) o;
            return this.get("source").equals(c.get("source")) && this.get("target").equals(c.get("target"));
          }
          return false;  
        }
        
        @Override
        public int hashCode(){
            return Objects.hash(this.get("source"), this.get("target"));
        }
    }
    
    
    private static Connection dbConnect(String username, String password, String url){
        Connection connection = null;
        
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection(url, username, password);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return connection;
    }
    
    
    public static void main(String[] args) {
        GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();
        GraphDatabaseService graphDb = graphDbFactory.newEmbeddedDatabase(DB_Folder);
        System.out.println("Connected to neo4j database");
        
        
        String username = "i2sflowbam";
        String password = "i2sflowbampass";
        String url = "jdbc:sqlserver://srvsqlax.asterix.local\\WFPOC;databaseName=WF";
        Connection connection = dbConnect(username, password, url);
        SQLDB sql = new SQLDB(connection); 
        
        Scanner scanner = new Scanner(System.in);
        String input;
        String[] split;
        HashMap<String, Command> commands = new HashMap<String, Command>();
        
        commands.put("db", new GraphDatabase(graphDb, sql));
        commands.put("build", new ExportManager(graphDb, sql));
        
        while(!(input = scanner.nextLine()).equals("end")){
            System.out.println(input);
            split = input.split(" ", 2);
            
            try{
                System.out.println(commands.get(split[0]).execute(split[1]));
            } 
            catch(Exception e) {
                e.printStackTrace(); 
            }
        }
        
        graphDb.shutdown();
        if(connection!= null) try { connection.close(); } catch(Exception e) {}
        }

}
