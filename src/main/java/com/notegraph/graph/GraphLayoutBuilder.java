package com.notegraph.graph;

import java.util.*;

public class GraphLayoutBuilder {
    public static GraphData build(Map<String, Set<String>> links){
        Map<String,GraphNode> map=new HashMap<>();
        Random r=new Random();
        for(String id:links.keySet()){
            map.put(id,new GraphNode(
                    id,
                    r.nextDouble()*800,
                    r.nextDouble()*600
            ));
        }

        List<GraphEdge> edges=new ArrayList<>();
        for(String a:links.keySet()){
            for(String b:links.get(a)){
                GraphNode n1=map.get(a);
                GraphNode n2=map.get(b);
                if(n1!=null && n2!=null)
                    edges.add(new GraphEdge(n1,n2));
            }
        }

        return new GraphData(
                new ArrayList<>(map.values()),
                edges
        );
    }
}