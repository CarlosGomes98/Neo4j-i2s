package qwqwee;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import javax.swing.JFrame;

import org.jgrapht.Graph;
import org.jgrapht.ListenableGraph;
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.ListenableDirectedWeightedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.Label;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.swing.mxGraphComponent;
public class Main {
    
    private static final File DB_Folder = new File("C:\\i2S-devenv\\workspace\\neo4jTest\\db");
    
    public static class JSONEdge extends JSONObject{
        @Override
        public boolean equals(Object o){
        if (o instanceof JSONEdge) {
            JSONEdge c = (JSONEdge) o;
            return this.get("source") == c.get("source") && this.get("target") == c.get("target");
          }
          return false;  
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
    
    private static JSONObject buildGraphJSON(GraphDatabaseService graphDb){
    
        ResourceIterator<Node> nodes = graphDb.getAllNodes().iterator();
        
        JSONObject jsonGraph = new JSONObject();
        JSONArray jsonNodes = new JSONArray();
        JSONArray jsonEdges = new JSONArray();
        JSONObject jsonNode;
        int activityIndex = 1;
        HashMap<String, Integer> activityIds = new HashMap<String, Integer>();
        String activityName;
        while(nodes.hasNext()) {
            jsonNode = new JSONObject();
            activityName = nodes.next().getLabels().iterator().next().toString();
            activityIds.put(activityName, activityIndex);
            jsonNode.put("id", activityIndex++);
            jsonNode.put("name", activityName);
            jsonNodes.add(jsonNode);
        }
        
        ResourceIterator<Relationship> edges = graphDb.getAllRelationships().iterator();
        JSONEdge edgeToAdd;
        Iterator edgeIterator;
        boolean found = false;
        JSONEdge edgeObject = null;
        
        while(edges.hasNext()){
            Relationship edge = edges.next();
            String source = edge.getStartNode().getLabels().iterator().next().toString();
            String target = edge.getEndNode().getLabels().iterator().next().toString();
            edgeToAdd = new JSONEdge();
            edgeToAdd.put("source", activityIds.get(source));
            edgeToAdd.put("target", activityIds.get(target));
            edgeToAdd.put("weight", 1);
            
            if(!jsonEdges.contains(edgeToAdd)){
                jsonEdges.add(edgeToAdd);
//            if(g.containsEdge(source, target)){
//                double weight = g.getEdgeWeight(g.getEdge(source, target));
//                g.setEdgeWeight(g.getAllEdges(source, target).iterator().next(), weight + 1);
            }
            else{
                int index = 0;
                edgeIterator = jsonEdges.iterator();
                found = false;
                while(edgeIterator.hasNext() && !found){
                    edgeObject = (JSONEdge) edgeIterator.next();
                    if(edgeObject.equals(edgeToAdd)){
                        edgeObject.put("weight", (Integer) edgeObject.get("weight") + 1);
                        jsonEdges.set(index, edgeObject);
                    }
                    index++;
//                g.setEdgeWeight(g.addEdge(source, target), 1);
                }
            }
        }

        jsonGraph.put("nodes", jsonNodes);
        jsonGraph.put("edges", jsonEdges);
        
        return jsonGraph;
    }
//        JGraphXAdapter<String, DefaultWeightedEdge> graphAdapter = 
//              new JGraphXAdapter<String, DefaultWeightedEdge>(g);
//      
//        mxIGraphLayout layout = new mxCircleLayout(graphAdapter);
//        layout.execute(graphAdapter.getDefaultParent());
//
//        frame.add(new mxGraphComponent(graphAdapter));
//
//        frame.pack();
//        frame.setLocationByPlatform(true);
//        frame.setVisible(true);
//    
    
    
    public static void main(String[] args) {
        GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();
        GraphDatabaseService graphDb = graphDbFactory.newEmbeddedDatabase(DB_Folder);;
        String username = "i2sflowbam";
        String password = "i2sflowbampass";
        
        String url = "jdbc:sqlserver://srvsqlax.asterix.local\\WFPOC;databaseName=WF";
        
        Connection connection = dbConnect(username, password, url);
        Statement statement = null;   
        ResultSet resultSet = null;  
        try{
            
            String query = "SELECT DISTINCT"
                    + "[CURRENT_ACT_ID] "
                    + "FROM [WF].[i2sflowbam].[PROC_GRAPH] ";
            statement = connection.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE, 
                    ResultSet.CONCUR_READ_ONLY);
            resultSet = statement.executeQuery(query);
            
            
            graphDb.beginTx();
            Node node;
            Label label;
            HashMap<String, Label> labels = new HashMap<String, Label>();
            //create nodes
            while (resultSet.next()) {
                label = Label.label(resultSet.getString(1));
                
                if(!labels.containsKey(resultSet.getString(1))) {
                    labels.put(resultSet.getString(1), label);
                    node = graphDb.createNode(label);
                }
                
                
                
            } 
            Node targetNode;
            Relationship rel;
            resultSet.close();
            query = "SELECT "
                    + "[ID_NODE], "
                    + "[PROC_GRAPH].[PROCESS_KEY], "
                    + "[CURRENT_ACT_ID], "
                    + "[NEXT_ACT_ID], "
                    + "[REL_TIMESTAMP], "
                    + "[DURATION] "
                    + "FROM [WF].[i2sflowbam].[PROC_GRAPH] INNER JOIN [WF].[i2sflowbam].[PROC_EXECS] "
                    + "ON [PROC_GRAPH].[PROCESS_KEY] = PROCESS_DEF_KEY AND CURRENT_ACT_ID = ACTIVITY_KEY "
                    + "WHERE [PROC_GRAPH].[PROCESS_KEY] <= 5101"
                    + "ORDER BY [PROC_GRAPH].[PROCESS_KEY]";
            
            statement = connection.createStatement(
                    ResultSet.TYPE_SCROLL_INSENSITIVE, 
                    ResultSet.CONCUR_READ_ONLY);
            resultSet = statement.executeQuery(query);
            
            //create relationships which represent each instance
            // estes tempos sao tempos totais ate ao fim do processo
            while (resultSet.next()) {                
                if(!(resultSet.getString(3).compareTo(resultSet.getString(4)) == 0)){
                    node = graphDb.findNodes(labels.get(resultSet.getString(3))).next();
                    targetNode = graphDb.findNodes(labels.get(resultSet.getString(4))).next();
                    rel = node.createRelationshipTo(targetNode, RelationshipType.withName(resultSet.getString(2)));
//                    try{
//                        parsedNodeDate = Timestamp.valueOf((String) node.getProperty("TIMESTAMP"));
//                        parsedTargetNodeDate = Timestamp.valueOf((String) targetNode.getProperty("TIMESTAMP"));
//                    }
//                    catch (Exception e){
//                        e.printStackTrace();
//                        parsedNodeDate = new Timestamp(0);
//                        parsedTargetNodeDate = parsedNodeDate;
//                    }
//                    long timeDifference = parsedTargetNodeDate.getTime() - parsedNodeDate.getTime();
//                    //time in ms is node weight
                    rel.setProperty("TIME_DIF", resultSet.getLong(6));
                    rel.setProperty("START", resultSet.getString(5));
                }
                
            } 
            
//            ResourceIterator<Node> nodes = graphDb.getAllNodes().iterator();
//            while(nodes.hasNext()) {
//                System.out.println(nodes.next().getLabels().iterator().next());
//            }
            Result result = graphDb.execute("MATCH (n1)-[r]->(n2) "
                            + "WITH labels(n1) as l1, labels(n2) as l2, "
                            + "r.PROCESS_KEY as process, r.TIME_DIF as time "
                            + "ORDER BY r.TIME_DIF DESC "
                            + "RETURN l1, l2, process, time LIMIT 20");
            
            while(result.hasNext()){
                System.out.println(result.next());
            }
            
//            JFrame frame = new JFrame("Graph");
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            
//            ListenableDirectedWeightedGraph<String, DefaultWeightedEdge> g = new ListenableDirectedWeightedGraph<>(DefaultWeightedEdge.class);
            
            JSONObject jsonGraph = buildGraphJSON(graphDb);
            try (FileWriter file = new FileWriter("/Users/i2scmg/Documents/graphVisualize/jsonGraph.json")) {
                file.write(jsonGraph.toJSONString());
                System.out.println("Successfully Copied JSON Object to File...");
                System.out.println("\nJSON Object: " + jsonGraph);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            graphDb.execute("MATCH (n), ()-[r]-() DELETE n,r");
            graphDb.shutdown();
            if(connection!= null) try { connection.close(); } catch(Exception e) {}
        }
//        GraphDatabaseFactory graphDbFactory = new GraphDatabaseFactory();        
//        GraphDatabaseService graphDb = graphDbFactory.newEmbeddedDatabase(DB_Folder);
//        graphDb.beginTx();
        
//        Node[] nodes = new Node[10];
//        for(int i = 0; i < 10; i++){
//            nodes[i] = graphDb.createNode();
//            nodes[i].setProperty("id", i);
//        }
//        
//        nodes[0].createRelationshipTo(nodes[1], RelTypes.KNOWS).setProperty("weight", 10);
//        nodes[0].createRelationshipTo(nodes[2], RelTypes.KNOWS).setProperty("weight", 7);
//        nodes[0].createRelationshipTo(nodes[3], RelTypes.KNOWS).setProperty("weight", 11);
//        nodes[1].createRelationshipTo(nodes[4], RelTypes.KNOWS).setProperty("weight", 3);
//        nodes[2].createRelationshipTo(nodes[5], RelTypes.KNOWS).setProperty("weight", 2);
//        nodes[3].createRelationshipTo(nodes[5], RelTypes.KNOWS).setProperty("weight", 8);
//        nodes[3].createRelationshipTo(nodes[6], RelTypes.KNOWS).setProperty("weight", 3);
//        nodes[5].createRelationshipTo(nodes[7], RelTypes.KNOWS).setProperty("weight", 5);
//        nodes[4].createRelationshipTo(nodes[8], RelTypes.KNOWS).setProperty("weight", 4);
//        nodes[6].createRelationshipTo(nodes[8], RelTypes.KNOWS).setProperty("weight", 3);
//        nodes[7].createRelationshipTo(nodes[9], RelTypes.KNOWS).setProperty("weight", 2);
//        nodes[8].createRelationshipTo(nodes[9], RelTypes.KNOWS).setProperty("weight", 7);
        
        
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
