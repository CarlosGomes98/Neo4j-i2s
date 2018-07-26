import diagram from '../resources/XMLBPMN.bpmn';
import JSONnodes from '../resources/nodes.json';
import BpmnViewer from 'bpmn-js';
import $ from "jquery";

var viewer = new BpmnViewer({
  container: '#canvas'
});


viewer.importXML(diagram, function (err) {
  "use strict";
  var nodes = JSONnodes;
  $("#total-instances").text(nodes.total_instances);
  var overlays = viewer.get('overlays');
  var elementRegistry = viewer.get('elementRegistry');
  var color = "";
  var totalInstances = nodes["total_instances"];
  var connectionObject;
  var i = 0;
  for (var node in nodes) {
    if (nodes.hasOwnProperty(node) && Array.isArray(nodes[node])) {
      nodes[node].forEach(function(connection){
        if (connection.instances < totalInstances * 0.1)
          color = "rgba(0,255,0,1)";
        else if (connection.instances < totalInstances * 0.25)
          color = "rgba(255,255,0,1)";
        else if (connection.instances < totalInstances * 0.5)
          color = "rgba(255,69,0,1)";
        else
          color = "rgba(255,0,0,1)";

        connectionObject = elementRegistry.filter(element => element.type == "bpmn:SequenceFlow").find(element => element.businessObject.sourceRef.name === node && element.businessObject.targetRef.name === connection.target)
        var width = 0;
        var height = 0;
        var directionx = 0;
        var offsety = 0;
        console.log(connectionObject);
        for(i = 0; i < connectionObject.waypoints.length - 1; i++){
          width = connectionObject.waypoints[i + 1].x - connectionObject.waypoints[i].x;
          height = connectionObject.waypoints[i + 1].y - connectionObject.waypoints[i].y;
          if(width === 0){
            width = 10;
          }
          if (width < 0){
            directionx = -1;
            width = Math.abs(width);
          }
          else{
            directionx = 1;
          }
          if(height === 0){
            height = 10;
          }
          if (height < 0){
            offsety = connectionObject.waypoints[i].x - connectionObject.waypoints[i+1].x;
            height = Math.abs(height);
          }
          else{
            offsety = 0;
          }
          console.log(i + " to " + (i+1) + " width: " + width + ", height: " + height);
          overlays.add(connectionObject, {
            position: {
              top: offsety,
              left: (connectionObject.waypoints[i].x - connectionObject.waypoints[0].x)
            },
            html: $('<div class="highlight-overlay">')
              .css({
                //a rectangle with width of and height as difference between first and last points of the connection
                width: width,
                height: height,
                "background-color": color
              })
          })
        }
      });
    }
  }

  var eventBus = viewer.get('eventBus');
  var source = "";
  var target = "";
  eventBus.on("element.hover", e => {
    if(e.element.type === "bpmn:SequenceFlow"){
      source = e.element.businessObject.sourceRef.name;
      target = e.element.businessObject.targetRef.name;
      node = nodes[source].find(node => node.target == target);
      $("#source").text(source);
      $("#target").text(target);
      $("#time").text(node.time);
      $("#instances").text(node.instances);
    }

  })

  if (!err) {
    console.log('success!');
    viewer.get('canvas').zoom('fit-viewport');
  } else {
    console.log('something went wrong:', err);
  }
});