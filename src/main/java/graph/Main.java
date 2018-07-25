package graph;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;
import java.util.Stack;

import org.json.simple.JSONObject;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
public class Main {
    private static HashMap<String, Integer> id = new HashMap<String, Integer>();
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
    
    private  void topologicalSort(Node node, boolean[] visited, Stack<Node> stack) {
        visited[(Integer) node.getProperty("id")] = true;
        Iterator<Relationship> edges = node.getRelationships().iterator();
        
        while(edges.hasNext()) {
            Node currentNode = edges.next().getEndNode();
            if(!visited[(Integer) currentNode.getProperty("id")]) {
                topologicalSort(currentNode, visited, stack);
            }
        }
        stack.push(node);
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
                
        
//        PathFinder <WeightedPath> finder = GraphAlgoFactory.dijkstra(
//                PathExpanders.forTypeAndDirection(RelTypes.KNOWS, Direction.OUTGOING), "weight");
//        
//        WeightedPath path = finder.findSinglePath(nodes[0], nodes[9]);
//        
//        for(Node node : path.nodes()){
//            System.out.print(node.getProperty("id") + "->");
//        }
//        System.out.println();
//        
//        //topological order
//        Stack<Node> stack = new Stack<Node>();
//        boolean[] visited = new boolean[10];
//        for(int i = 0; i < 10; i++){
//            visited[i] = false;
//        }
//        
//        for(int i = 0; i < 10; i++){
//            if(visited[i] == false){
//                topologicalSort(nodes[i], visited, stack);
//            }
//        }
//        
//        int i = 0;
//        Node element;
//        Node[] inOrderNodes = new Node[10];
//        while(!stack.isEmpty()){
//            element = stack.pop();
//            element.setProperty("order", i);
//            inOrderNodes[i] = element;
//            i++;
//        }
//        
//        for(i = 0; i < 10; i++){
//            nodes[i].setProperty("timeToComplete", 0);
//        }
//        
//        int maxSoFar = 0;
//        int weightOfEdgeFromHighestSoFar= 0;
//        Relationship currentRelationship;
//        Node currentNode;
//        Node currentNeighbor;
//        for(int j = 0; j < 10; j++){
//            currentNode = inOrderNodes[j];
//            Iterator<Relationship> relationships = currentNode.getRelationships().iterator();
//            maxSoFar = 0;
//            
//            while(relationships.hasNext()){
//               currentRelationship = relationships.next();
//               currentNeighbor = currentRelationship.getOtherNode(currentNode);
////               System.out.println((Integer) currentNeighbor.getProperty("order"));
////               System.out.println((Integer) currentNode.getProperty("order"));
//               if((Integer) currentNeighbor.getProperty("order") < (Integer) currentNode.getProperty("order")){
//                   if((Integer) currentNeighbor.getProperty("timeToComplete") >= maxSoFar){
//                       maxSoFar = (Integer) currentNeighbor.getProperty("timeToComplete");
//                       weightOfEdgeFromHighestSoFar = (Integer) currentRelationship.getProperty("weight");
//                   }
//               }
//            }
//            
//            currentNode.setProperty("timeToComplete", maxSoFar + weightOfEdgeFromHighestSoFar);
//        }
//        
//        for(i = 0; i < 10; i++)
//            System.out.println(nodes[i].getProperty("timeToComplete"));
//        graphDb.execute("MATCH (n), ()-[r]-() DELETE n,r");
//        graphDb.shutdown();
        }

}
