var weight;
var config = {
    dataSource: json,
    edgeCaption: function(node){
        return "Instances: " + node.instances + " " + "Average Time: " + Math.round(node.avgTime * 100)/100
    },
    directedEdges: true,
    nodeCaptionsOnByDefault: true,
    
    forceLocked: false,

    cluster: true,
    clusterKey: "version",
    zoomControls: true,
    rootNodeRadius: 30,
    linkDistancefn : function(edge, k){
        return (Math.floor(Math.log10(edge.instances)) + 1) * k * 10;
    },
    nodeStyle: {
        "all": {
            "borderColor": "#127DC1"
        }
    },
    edgeStyle: {
        "all": {
            "width": function (d) {
                return Math.max(4, d.getProperties().instances / 250)
            },
            "color": function (d) {
                instances = d.getProperties().instances
                if (instances <= 500)
                    return "rgba(0,255,0,1)"
                else if (instances <= 1500)
                    return "rgba(255,255,0,1)"
                else
                    return "rgba(255, 0, 0, 1)"
            }
        }
    }
};

alchemy.begin(config);