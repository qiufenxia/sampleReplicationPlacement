package algs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleWeightedGraph;

import flow.Commodity;
import flow.DemandNode;
import flow.MinCostFlowEdge;
import graph.InternetLink;
import graph.Node;
import simulation.Parameters;
import simulation.SamplePlacementSimulator;
import system.DataCenter;
import system.Dataset;
import system.Query;
import system.Sample;
import utils.ReturnPair;

public class BenchmarkOneQuery1 {
	
	/************* parameters *****************/
	private SamplePlacementSimulator simulator = null;

	// the number of admitted for different trials, where each trial has a different set of queries. 
	private List<Integer> admittedNumOfQueriesTrials = new ArrayList<Integer>();

	private List<Double> costTrials = new ArrayList<Double>();
	private List<Double> storageCostTrials = new ArrayList<Double>();
	private List<Double> updateCostTrials = new ArrayList<Double>();
	private List<Double> accessCostTrials = new ArrayList<Double>();
	private List<Double> processCostTrials = new ArrayList<Double>();

	private List<Double> averageErrorTrials = new ArrayList<Double>();
	private List<Double> averageDelayTrials = new ArrayList<Double>();
	
	private double lowestError = Parameters.errorBounds[0];
	
	/************* construction function *****************/
	public BenchmarkOneQuery1(SamplePlacementSimulator simulator, int numOfTrials) {
		this.simulator = simulator;
		for (int i = 0; i < numOfTrials; i++) {
			this.admittedNumOfQueriesTrials.add(i, 0);
			this.costTrials.add(i, 0.0);
			this.storageCostTrials.add(i, 0.0);
			this.updateCostTrials.add(i, 0.0);
			this.accessCostTrials.add(i, 0.0);
			this.processCostTrials.add(i, 0.0);
			this.averageErrorTrials.add(i, 0.0);
			this.averageDelayTrials.add(i, 0.0);
		}
	}

	/************* void function *****************/
	public void run(int numOfTrials) {
		
		SimpleWeightedGraph<Node, InternetLink> datacenterNetwork = this.simulator.getDatacenterNetwork();
		setEdgeWeightDataCenterNetwork(datacenterNetwork);

		for (int trial = 0; trial < numOfTrials; trial++) {
			if (trial > 0) {
				this.simulator.modifyCosts();// double check this.
				this.resetDataCenterNetwork(datacenterNetwork);
			}
			
			List<Query> queries = simulator.getQueries().get(trial);//get all queries at time slot "timeslot"
			int numOfSuccessAdmissions = 0;
			for (Query query : queries) {
				
				Benchmark1 benchmarkAlg = new Benchmark1(simulator, 1);
				benchmarkAlg.setLowestError(this.getLowestError());
				boolean admitted = benchmarkAlg.run(query);
								
				if (admitted)
					numOfSuccessAdmissions ++;
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++) {
					for (Dataset ds : simulator.getDatasets().get(t)) {
						ds.reset();
					}
				}
				
				
				this.getCostTrials().set(trial, this.getCostTrials().get(trial) + benchmarkAlg.getCostTrials().get(0));
				this.getAccessCostTrials().set(trial, this.getAccessCostTrials().get(trial) + benchmarkAlg.getAccessCostTrials().get(0));
				this.getStorageCostTrials().set(trial, this.getStorageCostTrials().get(trial) + benchmarkAlg.getStorageCostTrials().get(0));
				this.getUpdateCostTrials().set(trial, this.getUpdateCostTrials().get(trial) + benchmarkAlg.getUpdateCostTrials().get(0));
				this.getProcessCostTrials().set(trial, this.getProcessCostTrials().get(trial) + benchmarkAlg.getProcessCostTrials().get(0));
				this.getAverageErrorTrials().set(trial, this.getAverageErrorTrials().get(trial) + benchmarkAlg.getAverageErrorTrials().get(0));
				this.getAverageDelayTrials().set(trial, this.getAverageDelayTrials().get(trial) + benchmarkAlg.getAverageDelayTrials().get(0));
			}
			
			this.getCostTrials().set(trial, this.getCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getAccessCostTrials().set(trial, this.getAccessCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getStorageCostTrials().set(trial, this.getStorageCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getUpdateCostTrials().set(trial, this.getUpdateCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getProcessCostTrials().set(trial, this.getProcessCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getAverageErrorTrials().set(trial, this.getAverageErrorTrials().get(trial)/numOfSuccessAdmissions);
			this.getAverageDelayTrials().set(trial, this.getAverageDelayTrials().get(trial)/numOfSuccessAdmissions);
		}// end for trials;
	}
	
	private void setEdgeWeightDataCenterNetwork(SimpleWeightedGraph<Node, InternetLink> datacenterNetwork) {
		for (InternetLink il : datacenterNetwork.edgeSet()) {
			datacenterNetwork.setEdgeWeight(il, il.getCost());
		}
	}



	private void resetDataCenterNetwork(SimpleWeightedGraph<Node, InternetLink> dcNetwork) {
		for (Node node : dcNetwork.vertexSet()) {
			if (node instanceof DataCenter) {
				((DataCenter) node).reset();
			}
		}
		for (InternetLink il : dcNetwork.edgeSet()) {
			il.clear();
		}
	}

	/********* setter and getter **************/
	public SamplePlacementSimulator getSimulator() {
		return simulator;
	}

	public void setSimulator(SamplePlacementSimulator simulator) {
		this.simulator = simulator;
	}

	public List<Integer> getAdmittedNumOfQueriesTrials() {
		return admittedNumOfQueriesTrials;
	}

	public void setAdmittedNumOfQueriesTrials(List<Integer> admittedNumOfQueriesPerTS) {
		this.admittedNumOfQueriesTrials = admittedNumOfQueriesPerTS;
	}

	public List<Double> getCostTrials() {
		return costTrials;
	}

	public void setCostTrials(List<Double> costPerTS) {
		this.costTrials = costPerTS;
	}

	public List<Double> getStorageCostTrials() {
		return storageCostTrials;
	}

	public void setStorageCostTrials(List<Double> storageCostPerTS) {
		this.storageCostTrials = storageCostPerTS;
	}

	public List<Double> getUpdateCostTrials() {
		return updateCostTrials;
	}

	public void setUpdateCostTrials(List<Double> updateCostPerTS) {
		this.updateCostTrials = updateCostPerTS;
	}

	public List<Double> getAccessCostTrials() {
		return accessCostTrials;
	}

	public void setAccessCostTrials(List<Double> accessCostPerTS) {
		this.accessCostTrials = accessCostPerTS;
	}

	public List<Double> getProcessCostTrials() {
		return processCostTrials;
	}

	public void setProcessCostTrials(List<Double> processCostPerTS) {
		this.processCostTrials = processCostPerTS;
	}

	public List<Double> getAverageErrorTrials() {
		return averageErrorTrials;
	}

	public void setAverageErrorTrials(List<Double> averageErrorTrials) {
		this.averageErrorTrials = averageErrorTrials;
	}

	public double getLowestError() {
		return lowestError;
	}

	public void setLowestError(double lowestError) {
		this.lowestError = lowestError;
	}

	public List<Double> getAverageDelayTrials() {
		return averageDelayTrials;
	}

	public void setAverageDelayTrials(List<Double> averageDelayTrials) {
		this.averageDelayTrials = averageDelayTrials;
	}

}