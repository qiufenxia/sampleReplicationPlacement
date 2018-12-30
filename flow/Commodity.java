package flow;

import java.util.ArrayList;
import graph.Node;
import simulation.SamplePlacementSimulator;

// demand node
public class Commodity {

	private double ID;
	private Node source;
	private Node sink;
	private double demand;
		
	//used in the steps of each iteration in the Minimum cost multicommodity flow algorithm
	private double currDemand;
	
	private ArrayList<FlowPath> flowPaths = new ArrayList<FlowPath>();
	
	public Commodity(Node source, Node sink, double demand) {
		this.ID = SamplePlacementSimulator.idAllocator.nextId();
		this.source = source;
		this.sink = sink;
		this.setDemand(demand);
		this.currDemand = demand;
	}
	
	public Node getSource() {
		return source;
	}
	
	public void setSource(Node source) {
		this.source = source;
	}
	
	public Node getSink() {
		return sink;
	}
	
	public void setSink(Node sink) {
		this.sink = sink;
	}

	public double getCurrDemand() {
		return currDemand;
	}

	public void setCurrDemand(double currDemand) {
		this.currDemand = currDemand;
	}

	public double getDemand() {
		return demand;
	}

	public void setDemand(double demand) {
		this.demand = demand;
	}

	public double getID() {
		return ID;
	}

	public void setID(double iD) {
		ID = iD;
	}
	
	@Override
	public boolean equals(Object another) {
		if (this == another)
			return true;
		
		if (!(another instanceof Node))
			return false;
		
		if (this.ID == ((Node) another).getID())
			return true;
		else
			return false;
	}
	
	public void addPath(FlowPath path){
		this.flowPaths.add(path);
	}

	public ArrayList<FlowPath> getFlowPaths() {
		return flowPaths;
	}

	public void setFlowPaths(ArrayList<FlowPath> flowPaths) {
		this.flowPaths = flowPaths;
	}
	
}
