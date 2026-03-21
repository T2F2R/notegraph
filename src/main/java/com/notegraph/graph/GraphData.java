package com.notegraph.graph;

import java.util.List;

public class GraphData {
    public List<GraphNode> nodes;
    public List<GraphEdge> edges;

    public GraphData(List<GraphNode> nodes,List<GraphEdge> edges){
        this.nodes=nodes;
        this.edges=edges;
    }
}