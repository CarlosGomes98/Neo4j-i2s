alchemy.getNodes().all().forEach(function(node){
    node.addEventListener("click", function(node){
        alert(node.version);
    })
})