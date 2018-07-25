package graph;

import graph.Main.JSONEdge;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.WeightedPath;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PathExpanders;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import static org.neo4j.driver.v1.Values.parameters;

public class GraphDatabase implements Command{
    private HashMap<String, Label> labels;
    private GraphDatabaseService graphDb;
    private SQLDB sql;
    public GraphDatabase(GraphDatabaseService graphDb, SQLDB sql){
        this.graphDb = graphDb;
        this.sql = sql;
    }
    
    public String execute(String args){
        String[] split = args.split(" ", 2);
        switch(split[0]){
        case "populate":
            return populate();
        case "query":
            return query(split[1]);
        case "analyse":
            return analyse();
        }
        return "No method found";
    }
    
    public String analyse(){
        
        Result result = graphDb.execute("MATCH ()-[r]->() RETURN type(r) as label, avg(r.TIME_DIF) as time ORDER BY time DESC");
        StringBuilder resultBuilder = new StringBuilder();
        Map<String, Object> row;
        while(result.hasNext()){
            row = result.next();
            resultBuilder.append(row.get("label") + ": " + row.get("time") + "\n");
        }
        
        return resultBuilder.toString();
    }
    
    public String query(String query){
        Result result = graphDb.execute(query);
        StringBuilder resultBuilder = new StringBuilder();
        while(result.hasNext()){
            resultBuilder.append(result.next());
            resultBuilder.append("\n");
        }
        return resultBuilder.toString();
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
    
    public String critical(){
        
        HashSet<String> versions = new HashSet<String>();
        ResourceIterator<Node> nodes = graphDb.getAllNodes().iterator();
        ArrayList<Node> allNodes = new ArrayList<Node>();
        Scanner scanner = new Scanner(System.in);
        Node node;
        while(nodes.hasNext()){
            node = nodes.next();
            versions.add((String) node.getProperty("version")); 
            allNodes.add(node);
        }
            
        Iterator<String> versionsIterator = versions.iterator();
        while(versionsIterator.hasNext()){
            System.out.println(versionsIterator.next());
        }
        System.out.println("Choose version");
        String versionChoice = scanner.nextLine();
          
//        Node startNode = graphDb.findNode(labels.get("start"), "version", versionChoice);
//        Node endNode = graphDb.findNode(labels.get("end"), "version", versionChoice);
        
        
          
          //topological order
          Stack<Node> stack = new Stack<Node>();
          boolean[] visited = new boolean[allNodes.size()];
          for(int i = 0; i < visited.length; i++){
              visited[i] = false;
          }
          
          for(int i = 0; i < visited.length; i++){
              if(visited[i] == false){
                  topologicalSort(allNodes.get(i), visited, stack);
              }
          }
          
          ArrayList<Node> inOrderNodes = new ArrayList<Node>();
          while(!stack.isEmpty()){
              node = stack.pop();
              node.setProperty("order", inOrderNodes.size());
              inOrderNodes.add(node);
          }
          
          for(Node nodex : allNodes){
              nodex.setProperty("timeToComplete", 0);
          }
          
          int maxSoFar = 0;
          int weightOfEdgeFromHighestSoFar = 0;
          LinkedList<Node> criticalPath = new LinkedList<Node>();
          criticalPath.add(inOrderNodes.get(0));
          Relationship currentRelationship;
          Node currentNeighbor;
          Node nodeInCritPath;
          for(Node nodex : inOrderNodes){
              Iterator<Relationship> relationships = nodex.getRelationships().iterator();
              maxSoFar = 0;

              nodeInCritPath = null;
              while(relationships.hasNext()){
                 currentRelationship = relationships.next();
                 currentNeighbor = currentRelationship.getOtherNode(nodex);
                 System.out.println((Integer) currentNeighbor.getProperty("order"));
                 System.out.println((Integer) nodex.getProperty("order"));
                 if((Integer) currentNeighbor.getProperty("order") < (Integer) nodex.getProperty("order")){
                     if((Integer) currentNeighbor.getProperty("timeToComplete") >= maxSoFar){
                         maxSoFar = (Integer) currentNeighbor.getProperty("timeToComplete");
                         nodeInCritPath = currentNeighbor;
                         weightOfEdgeFromHighestSoFar = (Integer) currentRelationship.getProperty("instances");
                     }
                 }
              }
          criticalPath.add(nodeInCritPath);
          nodex.setProperty("timeToComplete", maxSoFar + weightOfEdgeFromHighestSoFar);
          }
          
          String cpath = "";
          Iterator<Node> critPathIterator = criticalPath.iterator();
          while(critPathIterator.hasNext())
              cpath += critPathIterator.next() + " -> ";
          
          cpath = cpath.substring(0, cpath.length()-4);
          scanner.close();
          return (String) inOrderNodes.get(inOrderNodes.size()).getProperty("timeToComplete") + "\n"
                  + cpath;
          
        }
    
    
    public String populate(){
        Transaction tx = graphDb.beginTx();
        try{
            Node node;
            Label label;
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            labels = new HashMap<String, Label>();
            labels.put("starter", Label.label("starter"));
            labels.put("ender", Label.label("ender"));
            labels.put("null", Label.label("null"));
            //create nodes
            ResultSet resultSet = sql.getActivities();
            while (resultSet.next()) {
                label = Label.label(resultSet.getString(1));
                
                if(!labels.containsKey(resultSet.getString(1))) {
                    labels.put(resultSet.getString(1), label);
                    node = graphDb.createNode(label);
                    if(resultSet.getString(1).contains("Start")){
                        node.addLabel(labels.get("starter"));
                    }
                    if(resultSet.getString(1).contains("End")){
                        node.addLabel(labels.get("ender"));
                    }
                    node.setProperty("version", date);
                    node.setProperty("activity", resultSet.getString(1));
                }
                
            }             
//            Node startNode = graphDb.createNode(labels.get("start"));
//            startNode.setProperty("activity", "start");
//            startNode.setProperty("version", date);
//            ResourceIterator<Node> starterNodes = graphDb.findNodes(labels.get("starter"), "version", date);
//            Relationship r;
//            while(starterNodes.hasNext()){
//                r = startNode.createRelationshipTo(starterNodes.next(), RelationshipType.withName("starting"));
//                r.setProperty("weight", 0);
//                r.setProperty("PROCESS",-1);
//            }
            
//            Node endNode = graphDb.createNode(labels.get("end"));
//            endNode.setProperty("activity", "end");
//            endNode.setProperty("version", date);
//            ResourceIterator<Node> enderNodes = graphDb.findNodes(labels.get("starter"), "version", date);
//            while(enderNodes.hasNext()){
//                r = enderNodes.next().createRelationshipTo(endNode, RelationshipType.withName("ending"));
//                r.setProperty("WEIGHT", 0);
//                r.setProperty("PROCESS",-1);
//            }
            Node nullNode = graphDb.createNode(labels.get("null"));
            nullNode.setProperty("activity", "null");
            nullNode.setProperty("version", date);
            
            Node targetNode;
            Relationship rel;
            resultSet.close();
            
            resultSet = sql.getProcesses();
            
            //create relationships which represent each instance
            // estes tempos sao tempos totais ate ao fim do processo
            while (resultSet.next()){
                if(!(resultSet.getString(3) == null)){// && !(resultSet.getString(4)==null)){// && !(resultSet.getString(3).compareTo(resultSet.getString(4)) == 0)){
                    node = graphDb.findNodes(labels.get(resultSet.getString(3)), "version", date).next();
                    if(resultSet.getString(4) == null){
                        targetNode = nullNode;
                    }
                    else{
                        targetNode = graphDb.findNodes(labels.get(resultSet.getString(4)), "version", date).next();
                    }
                    
                    if(!(node.getProperty("activity").equals(targetNode.getProperty("activity")))){
                        rel = node.createRelationshipTo(targetNode, RelationshipType.withName(resultSet.getString(3) + "->" + resultSet.getString(4)));
                        
                        //time in ms is node weight
                        rel.setProperty("TIME_DIF", resultSet.getLong(6));
                        rel.setProperty("START", resultSet.getString(5));
                        rel.setProperty("PROCESS", resultSet.getLong(2));
                        rel.setProperty("VERSION", date);
     
//                        if(node.hasLabel(labels.get("starter"))){
//                            rel = startNode.createRelationshipTo(node, RelationshipType.withName("Starting"));
//                            rel.setProperty("PROCESS",-1);
//                            rel.setProperty("VERSION", date);
//                        }
                    //make relationship from start or to end if its a starter or ender

                    }
                    
//                    if(node.hasLabel(labels.get("ender"))){
//                        rel = node.createRelationshipTo(endNode, RelationshipType.withName("Ending"));
//                        rel.setProperty("PROCESS",-1);
//                        rel.setProperty("VERSION", date);
//                    }
                }
            }
            tx.success();
            return "Success";
        }
        catch(Exception e){
            tx.failure();
            e.printStackTrace();
            return "failure";
        }
        finally{
            tx.close();
        }
    }
                 
    
}
