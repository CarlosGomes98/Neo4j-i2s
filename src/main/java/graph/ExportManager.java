package graph;

import graph.Main.JSONEdge;

import java.io.File;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 * Deals with exporting data from neo4j to other formats.
 * @author i2scmg
 *
 */
public class ExportManager implements Command{
    private GraphDatabaseService graphDb;
    private SQLDB sql;
    
    public ExportManager(GraphDatabaseService graphDb, SQLDB sql){
        this.graphDb = graphDb;
        this.sql = sql;
    }
    
    /**
     * Parses the command line input and calls the correct function
     * @param args The command line argument given.
     * @return The result if successful, "failure" if unsuccessful or "No method found" if the command does not exist
     */
    public String execute(String args){
        String[] split = args.split(" ", 2);
        switch(split[0]){
            case "json":
                return buildJson();
            case "ml":
                return buildML();
            case "xml":
                return buildXML();
            case "bpmn":
                return bpmn();
            case "processResult":
                return processResult(Long.parseLong(split[1]));
            }
        return "No method found";
    }
    
    /**
     * Finds whether a process was accepted or not
     * @param processId The id of the process
     * @return "Fail" or "Accept"
     */
    public String processResult(long processId){
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("processId", processId);
        String endNode =  graphDb.execute("MATCH ()-[r]->(n:ender) WHERE r.PROCESS = $processId RETURN n.activity, r.VERSION", parameters).next().get("n.activity").toString();
        if(endNode.contains("Failure")){
            return "Fail";
        }
        return "Accept";
    }
    
    /**
     * Build a graphJson representation of the process graph.
     * @return A success or failure message.
     */
    public String buildJson(){
        JSONObject jsonGraph = buildGraphJSON(graphDb);
        try (FileWriter file = new FileWriter("\\i2S-devenv\\workspace\\BPMN-graph\\graphVisualize\\js\\jsonGraph.js")){
            file.write("var json = \n");
            file.write(jsonGraph.toJSONString());
            return "Successfully Copied JSON Object to File... \n" + jsonGraph;
        }
        catch(Exception e){
            return e.getMessage();
        }
    }
    
    /**
     * Starter and ender nodes have that as their label as well. Find their activity label.
     * @param labels Labels that the node has.
     * @return The label indicating its activity name.
     */
    public String getActivityLabel(ArrayList<String> labels){
        if(labels.get(0).equals("starter") || labels.get(0).equals("ender")){
            return labels.get(1);
        }
        return labels.get(0);
    }
    
    /**
     * Build the json file needed to enrich the bpmn.js.
     * @return Success or failure message.
     */
    public String bpmn(){
        Result result = graphDb.execute("MATCH (s)-[r]->(t) RETURN labels(s) as source, labels(t) as target, avg(r.TIME_DIF) as time, COUNT(r) as instances ORDER BY time DESC");
        JSONObject jsonNodes = new JSONObject();
        Map<String, Object> row;
        JSONObject node;
        String[] properties = {"target", "time", "instances"};
        String source;
        String target = "";
        while(result.hasNext()){
            row = result.next();
            node = new JSONObject();
            for(String property : properties){
                if(property.equals("target")){
                    target = getActivityLabel((ArrayList) row.get(property));
                    node.put(property, target);
                }
                else
                    node.put(property, row.get(property));
                
            }
            if(target.equals("null")) continue;
            source = getActivityLabel((ArrayList) row.get("source"));
            if(jsonNodes.containsKey(source)){
                ((JSONArray) jsonNodes.get(source)).add(node);
            }
            else{
                jsonNodes.put(source, new JSONArray());
                ((JSONArray) jsonNodes.get(source)).add(node);
            }
        }
        long noInstances = (long) graphDb.execute("MATCH ()-[r]-() RETURN COUNT(r) as num").next().get("num");
        jsonNodes.put("total_instances", noInstances);
        try (FileWriter file = new FileWriter("\\i2S-devenv\\workspace\\BPMN-graph\\graphVisualize\\viewBPMN\\resources\\nodes.json")){
                file.write(jsonNodes.toJSONString());
                return "Successfully copied JSON Object to File";
            }
        catch(Exception e){ 
            e.printStackTrace(); 
            return "Failure";
        }
    }
    
    /**
     * Build a text file with questionnaire data.
     * @return Success or failure message.
     */
    public String buildML(){
        ResultSet questions = sql.getQuestions();
        ResultSet answers = sql.getAnswers();
        try (FileWriter file = new FileWriter("\\i2S-devenv\\workspace\\BPMN-graph\\ml\\data.txt")){
            while(questions.next()){
                file.write(questions.getString(1));
                file.write("\n");
            }
            file.write("Result");
            file.write("\n");
            String currentProcess = "";
            if(answers.next()){
                file.write(answers.getString(1));
                file.write("\n");
                currentProcess = answers.getString(2);
            }
            while(answers.next()){
                if(!currentProcess.equals(answers.getString(2))){
                    file.write(processResult(Long.parseLong(currentProcess.substring(0, 6))));
                    file.write("\n");
                    currentProcess = answers.getString(2);
                }
                file.write(answers.getString(1));
                file.write("\n");
            }
            file.write(processResult(Long.parseLong(currentProcess.substring(0, 6))));
            file.write("\n");
        return "Success";
        }
        catch(Exception e){
            e.printStackTrace();
            return "Failure";
        }
    }
    
    /**
     * Build a BPMN 2.0 representation of the process graph.
     * @return Success or failure message.
     */
    public String buildXML(){
        try{
            BpmnModelInstance modelInstance = buildBPMNXML();
            File file = new File("\\i2S-devenv\\workspace\\BPMN-graph\\graphVisualize\\js\\XMLBPMN.bpmn");
            Bpmn.validateModel(modelInstance);
            Bpmn.writeModelToFile(file, modelInstance);
            return "Successfully Copied XML Object to File... \n";
        }
        catch(Exception e){
            return e.getMessage();
        }
    }
    
    
    protected <T extends BpmnModelElementInstance> T createElement(BpmnModelInstance modelInstance, BpmnModelElementInstance parentElement, String id, Class<T> elementClass) {
        T element = modelInstance.newInstance(elementClass);
        element.setAttributeValue("id", id, true);
        parentElement.addChildElement(element);
        return element;
      }
    
    public SequenceFlow createSequenceFlow(BpmnModelInstance modelInstance, Process process, FlowNode from, FlowNode to) {
        String identifier = from.getId() + "-" + to.getId();
        SequenceFlow sequenceFlow = createElement(modelInstance, process, identifier, SequenceFlow.class);
        process.addChildElement(sequenceFlow);
        sequenceFlow.setSource(from);
        from.getOutgoing().add(sequenceFlow);
        sequenceFlow.setTarget(to);
        to.getIncoming().add(sequenceFlow);
        return sequenceFlow;
      }
    
    /**
     * Helper method for doing the bulk of the work in building the BPMN 2.0 representation.
     * @return XML representation of the process graph.
     */
    private BpmnModelInstance buildBPMNXML(){
        Transaction tx = graphDb.beginTx();
        ResourceIterator<Node> nodes = graphDb.getAllNodes().iterator();
        
        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();
        Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace("http://bpmn.io/schema/bpmn");
        modelInstance.setDefinitions(definitions);
        
        Process process = modelInstance.newInstance(Process.class);
        process.setId("process");
        definitions.addChildElement(process);
        HashMap<String, FlowNode> nodeMap = new HashMap<String, FlowNode>();
        Iterator<Label> labelIterator;
        String activityName;
        String version;
        FlowNode event;
        while(nodes.hasNext()) {
            Node node = nodes.next();
            labelIterator = node.getLabels().iterator();
            activityName = labelIterator.next().toString();
            
            //Get activity name from label. Start and end nodes have a starter and ender label as well. Dont get that one
            if(activityName.equals("starter") || activityName.equals("ender")) activityName = labelIterator.next().toString();
            
            version = (String) node.getProperty("version");
            if(activityName.contains("Start")){
                event = createElement(modelInstance, process, activityName, StartEvent.class);
            }
            else if(activityName.contains("End")){
                event = createElement(modelInstance, process, activityName, EndEvent.class);
            }
            
            else{
                event = createElement(modelInstance, process, activityName, UserTask.class);
            }
            nodeMap.put(activityName + version, event);
        }
        
        ResourceIterator<Relationship> edges = graphDb.getAllRelationships().iterator();
        Edge edgeToAdd;
        HashSet<Edge> edgeSet = new HashSet<Edge>();
        String sourceActivityName;
        String targetActivityName;
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> results;
        while(edges.hasNext()){
            Relationship edge = edges.next();
            Node source = edge.getStartNode();
            Node target = edge.getEndNode();
            
            labelIterator = source.getLabels().iterator();
            sourceActivityName = labelIterator.next().toString();
            
            if(sourceActivityName.equals("starter") || sourceActivityName.equals("ender")) sourceActivityName = labelIterator.next().toString();
            
            labelIterator = target.getLabels().iterator();
            targetActivityName = labelIterator.next().toString();
            
            if(targetActivityName.equals("starter") || targetActivityName.equals("ender")) targetActivityName = labelIterator.next().toString();
            
            edgeToAdd = new Edge(sourceActivityName + (String) source.getProperty("version"), targetActivityName + (String) target.getProperty("version"));
            Result result;
            if(!edgeSet.contains(edgeToAdd)){
                System.out.println(edgeToAdd.getSource() + " " + edgeToAdd.getTarget());
                params.clear();
                params.put("version", target.getProperty("version"));
                params.put("source", sourceActivityName);
                params.put("target", targetActivityName);
                result = graphDb.execute("MATCH ({version: $version, activity: $source})-[r]->({version: $version, activity: $target}) RETURN avg(r.TIME_DIF) as time, COUNT(r) as instances",
                                                params);
                results = result.next();
                if(results.get("time") == null){
                    edgeToAdd.setAverageTime(0);
                }
                else{
                    edgeToAdd.setAverageTime((double) results.get("time"));
                }
                
                if(results.get("instances") == null){
                    edgeToAdd.setInstances(0);
                }
                else{
                    edgeToAdd.setInstances((long) results.get("instances"));
                }
                
                edgeSet.add(edgeToAdd);
            }
           
        }
        
        for(Edge e : edgeSet){
            createSequenceFlow(modelInstance, process, nodeMap.get(e.getSource()), nodeMap.get(e.getTarget()));
        }
        
        
        tx.success();
        tx.close();
        return modelInstance;
    }
    
    private JSONObject buildGraphJSON(GraphDatabaseService graphDb){
        
        Transaction tx = graphDb.beginTx();
        ResourceIterator<Node> nodes = graphDb.getAllNodes().iterator();
        
        JSONObject jsonGraph = new JSONObject();
        JSONArray jsonNodes = new JSONArray();
        JSONArray jsonEdges = new JSONArray();
        JSONObject jsonNode;
        
        Iterator<Label> labelIterator;
        String activityName;
        String version;
        
        while(nodes.hasNext()) {
            jsonNode = new JSONObject();
            Node node = nodes.next();
            labelIterator = node.getLabels().iterator();
            activityName = labelIterator.next().toString();
            
            //Get activity name from label. Start and end nodes have a starter and ender label as well. Dont get that one
            if(activityName.equals("starter") || activityName.equals("ender")) activityName = labelIterator.next().toString();
            
            version = (String) node.getProperty("version");
//            if(activityName.equals("start") || activityName.equals("end")){
//                jsonNode.put("root", true);
//            }
            jsonNode.put("id", activityName + version);
            jsonNode.put("caption", activityName);
            jsonNode.put("version", version);
            jsonNodes.add(jsonNode);
        }
        
        ResourceIterator<Relationship> edges = graphDb.getAllRelationships().iterator();
        JSONEdge edgeToAdd;
        String sourceActivityName;
        String targetActivityName;
        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> results;
        while(edges.hasNext()){
            Relationship edge = edges.next();
            Node source = edge.getStartNode();
            Node target = edge.getEndNode();
    
            edgeToAdd = new JSONEdge();
            
            labelIterator = source.getLabels().iterator();
            sourceActivityName = labelIterator.next().toString();
            
            if(sourceActivityName.equals("starter") || sourceActivityName.equals("ender")) sourceActivityName = labelIterator.next().toString();
            
            edgeToAdd.put("source", sourceActivityName + (String) source.getProperty("version"));
            
            labelIterator = target.getLabels().iterator();
            targetActivityName = labelIterator.next().toString();
            
            if(targetActivityName.equals("starter") || targetActivityName.equals("ender")) targetActivityName = labelIterator.next().toString();
            
            
            edgeToAdd.put("target", targetActivityName + (String) target.getProperty("version"));
            if(!jsonEdges.contains(edgeToAdd)){
                params.clear();
                params.put("version", target.getProperty("version"));
                params.put("source", sourceActivityName);
                params.put("target", targetActivityName);
                Result result = graphDb.execute("MATCH ({version: $version, activity: $source})-[r]->({version: $version, activity: $target}) RETURN avg(r.TIME_DIF) as time, COUNT(r) as instances",
                                                params);
                results = result.next();
                edgeToAdd.put("avgTime", results.get("time"));
                edgeToAdd.put("instances", results.get("instances"));
                jsonEdges.add(edgeToAdd);
            }
           
        }
    
        jsonGraph.put("nodes", jsonNodes);
        jsonGraph.put("edges", jsonEdges);
        tx.success();
        tx.close();
        return jsonGraph;
    }
}
