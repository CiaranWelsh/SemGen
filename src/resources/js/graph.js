/**
 * Defines graph functionality
 * 
 * Adapted from: http://stackoverflow.com/questions/11400241/updating-links-on-a-force-directed-graph-from-dynamic-json-data
 */

function Graph() {

	// Add a node to the graph
	this.addNode = function (nodeData) {
		if(!nodeData)
			throw "Invalid node data";
		
		if(typeof nodeData.id != "string")
			throw "Node id must be a string";
		
		if(typeof nodeData.r != "number")
			throw "Node radius must be a number";
		
		if(typeof nodeData.className != "string")
			throw "Node className must be a string";
		
	    nodes.push(nodeData);
	    update();
	};

	// Set up the D3 visualisation in the specified element
	var w = 500,
	    h = 500;
	
	// Get the stage and style it
	var vis = d3.select("#stage")
	    .append("svg:svg")
	    .attr("width", w)
	    .attr("height", h)
	    .attr("id", "svg")
	    .attr("pointer-events", "all")
	    .attr("viewBox","0 0 "+ w +" "+ h)
	    .attr("perserveAspectRatio", "xMinYMid")
	    .append('svg:g');

	var force = d3.layout.force();

	var nodes = force.nodes(),
	    links = force.links();

	/** 
	 * Updates the graph
	 */
	var update = function () {
		var link = vis.selectAll("line")
			.data(links);

	    link.enter().append("line")
	        .attr("class","link");	

	    link.exit().remove();
	    
	    var node = vis.selectAll("g.node")
	        .data(nodes, function(d) { return d.id; });

	    var nodeEnter = node.enter().append("g")
	        .attr("class", function (d) {return d.className; })
	        .call(force.drag);

	    nodeEnter.append("svg:circle")
		    .attr("r", function(d) { return d.r; })
		    .attr("id",function(d) { return "Node;"+d.id;})
		    .attr("class","nodeStrokeClass");

	    nodeEnter.append("svg:text")
		    .attr("class","text")
		    .attr("x", 20)
            .attr("y", ".31em")
		    .text( function(d) { return d.id; }) ;

	    // Tell each node to initialize its element
	    node.each(function (d) {
	    	d.initialize(this);
	    });
	    
	    node.exit().remove();
	    
	    force.on("tick", function() {
	    	// Execute the tick handler for each node
	    	node.each(function (d) {
	    		d.tickHandler(this);
	    	});

	        link.attr("x1", function(d) { return d.source.x; })
	          .attr("y1", function(d) { return d.source.y; })
	          .attr("x2", function(d) { return d.target.x; })
	          .attr("y2", function(d) { return d.target.y; });
	    });

	    // Restart the force layout.
	    force
		    .gravity(.05)
		    .distance(50)
		    .linkDistance( 50 )
		    .size([w, h])
		    .start();
	};

	// Run it
	update();
}