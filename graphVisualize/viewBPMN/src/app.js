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

viewer.importXML(diagram, function(err) {
  "use strict";
  var nodes = JSONnodes;
  var nodeTypes = ["bpmn:Task", "bpmn:EndEvent", "bpmn:StartEvent"];
  var overlays = viewer.get('overlays');
  var elementRegistry = viewer.get('elementRegistry');
  var elements = {};
  var color = "";
  console.log(elementRegistry.getAll());
  elementRegistry.filter(element => nodeTypes.includes(element.type))
                 .forEach(element => nodes[element.businessObject.name]["object"] = element);

  for (var node in nodes) {
    if (nodes.hasOwnProperty(node)) {
      if (node.instances <= 500)
        color = "rgba(0,255,0,1)";
      else if (node.instances <= 1500)
        color = "rgba(255,255,0,1)";
      else
        color = "rgba(255, 0, 0, 1)";

      overlays.add(node.object, {
        position: {
          top: 0,
          left: 0
      },
      html:  $('<div class="highlight-overlay">')
            .css({
              width: node.object.width,
              height: node.object.height,
              "background-color": color
            })
      })
    }
  }

  if (!err) {
    console.log('success!');
    viewer.get('canvas').zoom('fit-viewport');
  } else {
    console.log('something went wrong:', err);
  }
});