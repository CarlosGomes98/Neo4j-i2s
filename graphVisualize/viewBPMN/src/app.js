// we use stringify to inline an example XML document
import diagram from '../resources/XMLBPMN.bpmn';
import JSONnodes from '../resources/nodes.json';

// make sure you added bpmn-js to your your project
// dependencies via npm install --save bpmn-js
import BpmnViewer from 'bpmn-js';
import $ from "jquery";


var viewer = new BpmnViewer({
  container: '#canvas'
});

viewer.importXML(diagram, function (err) {
  "use strict";
  var nodes = JSONnodes;
  var nodeTypes = ["bpmn:Task", "bpmn:EndEvent", "bpmn:StartEvent"];
  var overlays = viewer.get('overlays');
  var elementRegistry = viewer.get('elementRegistry');
  var elements = {};
  var color = "";
  console.log(elementRegistry.getAll());
  // elementRegistry.filter(element => nodeTypes.includes(element.type))
  //  .forEach(element => {
  //   if(element.businessObject.name in nodes)
  //     nodes[element.businessObject.name][3] = element;
  //  });
  var totalInstances = nodes["total_instances"];
  var connectionObject;

  for (var node in nodes) {
    if (nodes.hasOwnProperty(node)) {
      nodes[node].forEach(function(connection){
        if (connection.instances < totalInstances * 0.1)
          color = "rgba(0,255,0,1)";
        else if (connection.instances < totalInstances * 0.25)
          color = "rgba(255,255,0,1)";
        else if (connection.instances < totalInstances * 0.5)
          color = "rgba(255,69,0,1)";
        else
          color = "rgba(255,0,0,1)";
        console.log(node);
        console.log(connection.instances);
        connectionObject = elementRegistry.filter(element => element.type == "bpmn:SequenceFlow").find(element => element.businessObject.sourceRef.name === node && element.businessObject.targetRef.name === connection.target)
        console.log(connectionObject);
        overlays.add(connectionObject, {
          position: {
            top: 0,
            left: 0
          },
          html: $('<div class="highlight-overlay">')
            .css({
              width: Math.max(10, Math.abs(connectionObject.waypoints[connectionObject.waypoints.length - 1].x - connectionObject.waypoints[0].x)),
              height: Math.max(5, Math.abs(connectionObject.waypoints[connectionObject.waypoints.length - 1].y - connectionObject.waypoints[0].y)),
              "background-color": color
            })
        })
      });
    }
  }

  if (!err) {
    console.log('success!');
    viewer.get('canvas').zoom('fit-viewport');
  } else {
    console.log('something went wrong:', err);
  }
});