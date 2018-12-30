package algs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.SimpleWeightedGraph;

import flow.Commodity;
import flow.MCMC;
import flow.MinCostFlowEdge;
import graph.InternetLink;
import graph.Node;
import simulation.Parameters;
import simulation.SamplePlacementSimulator;
import system.DataCenter;
import system.Dataset;
import system.Query;
import system.Sample;

public class ApproOneQuery {
	
	/************* parameters *****************/
	private SamplePlacementSimulator simulator = null;

	// the number of admitted for different trials, where each trial has a different set of queries. 
	private List<Integer> admittedNumOfQueriesTrials = new ArrayList<Integer>();

	private List<Double> costTrials = new ArrayList<Double>();
	private List<Double> storageCostTrials = new ArrayList<Double>();// the cost of a query (average)
	private List<Double> updateCostTrials = new ArrayList<Double>();
	private List<Double> accessCostTrials = new ArrayList<Double>();
	private List<Double> processCostTrials = new ArrayList<Double>();

	private List<Double> averageErrorTrials = new ArrayList<Double>();
	private List<Double> averageDelayTrials = new ArrayList<Double>();
	
	private int lowestErrorIndex = 0;
	
	private double epsilon = 0.23;
	
	/************* construction function *****************/
	public ApproOneQuery(SamplePlacementSimulator simulator, int numOfTrials) {
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

	/************* void run function *****************/
	public void run(int numOfTrials) {
		SimpleWeightedGraph<Node, InternetLink> datacenterNetwork = this.simulator.getDatacenterNetwork();
		setEdgeWeightDataCenterNetwork(datacenterNetwork);

		for (int trial = 0; trial < numOfTrials; trial++) {
			
//			if (trial > 0) {
//				this.simulator.modifyCosts();// double check this.
//				this.resetDataCenterNetwork(datacenterNetwork);
//			}
			
			List<Query>	queries = simulator.getQueries().get(trial);//get all queries at time slot "timeslot"
			
			int numOfSuccessAdmissions = 0;
			for (Query query : queries) {
				
				boolean admitted = runWithOneQuery(query, datacenterNetwork, this.simulator.getDataCenters(), trial, null, Double.MAX_VALUE);
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
			}
			
			this.getCostTrials().set(trial, this.getCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getAccessCostTrials().set(trial, this.getAccessCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getStorageCostTrials().set(trial, this.getStorageCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getUpdateCostTrials().set(trial, this.getUpdateCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getProcessCostTrials().set(trial, this.getProcessCostTrials().get(trial)/numOfSuccessAdmissions);
			this.getAverageErrorTrials().set(trial, this.getAverageErrorTrials().get(trial)/numOfSuccessAdmissions);
			this.getAverageDelayTrials().set(trial, this.getAverageDelayTrials().get(trial) / numOfSuccessAdmissions);
		}// end for trials;
	}
	
	public boolean runWithOneQuery(Query query,
			SimpleWeightedGraph<Node, InternetLink> datacenterNetwork, 
			List<DataCenter> datacenters, 
			int trial, 
			Map<Dataset, Double> popularity, // used in online algorithms. 
			double popularityThreshold
			) {
		
		List<Commodity> commodities = new ArrayList<Commodity>();
		List<DataCenter> virtualDCs = new ArrayList<DataCenter>();
		List<Sample> virtualSamples = new ArrayList<Sample>();
		
		double totalAvailComputing = 0d; 
		for (DataCenter dc : datacenters) {
			totalAvailComputing += dc.randomReGenerateCapacity();
		}
		
		double computingNeededForSmallestError = 0d; 
		List<Dataset> dss = query.getDatasets();
		for (Dataset ds : dss) {
			computingNeededForSmallestError += ds.getSample(0).getVolume() * Parameters.computingAllocatedToUnitData;
		}
		
		double gammaFactor = (totalAvailComputing > computingNeededForSmallestError) ? 1d : (totalAvailComputing / computingNeededForSmallestError);
		
		////find the right error bound of samples to evaluate query. 
		// first scale down the available computing resource
		List<Sample> selectedSamples = new ArrayList<Sample>();//selected sample for each dataset.
		
		for (int i = 0; i < dss.size(); i ++) {
			Dataset ds = dss.get(i);
			double targetVolume = ds.getSample(0).getVolume() * gammaFactor;
			double minDiff = Double.MAX_VALUE;
			Sample minDiffSample = null;
			for (Sample sample : ds.getSamples()) {
				if ((targetVolume - sample.getVolume() >=0 ) && (targetVolume - sample.getVolume() < minDiff)) {
					minDiff = targetVolume - sample.getVolume();
					minDiffSample = sample;
					if (0 == minDiff)
						break;
				}
			}
			if (null != minDiffSample)
				selectedSamples.add(minDiffSample);
			else {
				//this query cannot be accepted. 
				return false;
			}
		}
		
		// lowest error bound check
		for (int i = 0; i < dss.size(); i ++) {
			Dataset ds = dss.get(i);
			Sample sam = selectedSamples.get(i);
			double errorSam = sam.getError();
			int indexSam = 0; 
			for (int j = 0; j < ds.getSamples().size(); j ++) {
				if (errorSam == ds.getSamples().get(j).getError()) {
					indexSam = j; 
					break; 
				}
			}
						
			if (indexSam < this.getLowestErrorIndex()) {
				selectedSamples.set(i, ds.getSamples().get(this.getLowestErrorIndex()));
			}
		}
		
		SimpleWeightedGraph<Node, MinCostFlowEdge> flowNet = this.initializeFlowNetwork(
				datacenterNetwork, selectedSamples,
				commodities, virtualDCs, virtualSamples,
				query, trial);
		
		if (null != popularity) { // adjust the flow network for online algorithms. 
			
			Set<MinCostFlowEdge> toBeRemovedEdges = new HashSet<MinCostFlowEdge>();
			for (Sample vSample : virtualSamples) {
				Sample sample = vSample.getRealSample();
				
				Set<DataCenter> DCsWithPlacedThisSample = new HashSet<DataCenter>();
				Set<DataCenter> DCsThisSampleIsConnectedTo = new HashSet<DataCenter>();
				
				for (MinCostFlowEdge edge : flowNet.edgeSet()) {
					if (edge.getSource() instanceof Sample && edge.getTarget() instanceof DataCenter) {
						
						DataCenter dc = ((DataCenter) edge.getTarget()).getParent();
					
						if (edge.getSource().equals(vSample)) {
							DCsThisSampleIsConnectedTo.add(dc);
							
							for (Sample admittedSample : dc.getAdmittedSamples()){
								if (admittedSample.equals(sample.getRealSample()) || 
										admittedSample.getParentDataset().equals(sample.getParentDataset())){
									DCsWithPlacedThisSample.add(dc);
									break;
								}
							}
						}
					}
				}
				
				// check the popularity
				double popularityDataset = popularity.get(sample.getParentDataset());
				
				if (popularityDataset > popularityThreshold) {// prefers to place this dataset to new data centers
					// check whether all data centers have 
					if (DCsWithPlacedThisSample.size() < DCsThisSampleIsConnectedTo.size()) {// not all data centers have a sample of this dataset
						for (MinCostFlowEdge edge : flowNet.edgeSet()) {
							if (edge.getSource() instanceof Sample && edge.getTarget() instanceof DataCenter)
								if (((Sample)edge.getSource()).equals(vSample) && DCsWithPlacedThisSample.contains((DataCenter)edge.getTarget()))
									toBeRemovedEdges.add(edge);
						}
					}
				} else {// prefers to place this dataset to the data centers with placed samples. 
					if (DCsWithPlacedThisSample.size() > 0) {// not all data centers have a sample of this dataset
						for (MinCostFlowEdge edge : flowNet.edgeSet()) {
							if (edge.getSource() instanceof Sample && edge.getTarget() instanceof DataCenter)
								if (((Sample)edge.getSource()).equals(vSample) && 
										!DCsWithPlacedThisSample.contains((DataCenter) edge.getTarget()) &&
										DCsThisSampleIsConnectedTo.contains((DataCenter) edge.getTarget()))
									toBeRemovedEdges.add(edge);
						}
					}
				}
			}
			
			for (MinCostFlowEdge toBeRemoved : toBeRemovedEdges)
				flowNet.removeEdge(toBeRemoved);
			
			ConnectivityInspector<Node, MinCostFlowEdge> connectivityInspector = new ConnectivityInspector<Node, MinCostFlowEdge>(flowNet);
			
			if (!connectivityInspector.isGraphConnected())
				System.out.println("The pruned auxiliary graph should be connected!!!");
			
		}
		
		ConnectivityInspector<Node, MinCostFlowEdge> connectivityInspector = new ConnectivityInspector<Node, MinCostFlowEdge>(flowNet);
		
		if (!connectivityInspector.isGraphConnected())
			System.out.println("The auxiliary graph should be connected!!!");
		
		MCMC minCostFlowAlg = new MCMC(flowNet, this.epsilon, commodities, commodities.size());
		SimpleWeightedGraph<Node, MinCostFlowEdge> afterFlowNetwork = minCostFlowAlg.calcMinCostFlow(); 
		
		// obtain and adjust the assignment result for this query 
		
		Map<Sample, Set<DataCenter>> assignedDCsSample = new HashMap<Sample, Set<DataCenter>>();
		
		int numEdgesPositiveFlow = 0; 
		for (MinCostFlowEdge flowEdge : afterFlowNetwork.edgeSet()) {	
			if (flowEdge.getSource() instanceof Sample && flowEdge.getTarget() instanceof DataCenter) {
				Sample sam = ((Sample) flowEdge.getSource()).getRealSample();
				DataCenter dcForThisSam = ((DataCenter) flowEdge.getTarget()).getParent();
				double flowOnEdge = flowEdge.getFlows();
				if (flowOnEdge > 0) {
					numEdgesPositiveFlow ++; 
					if (null == assignedDCsSample.get(sam))
						assignedDCsSample.put(sam, new HashSet<DataCenter>());
					
					assignedDCsSample.get(sam).add(dcForThisSam);
				}
			}
		}
		
		System.out.println("" + numEdgesPositiveFlow);
		
		if (0 == numEdgesPositiveFlow)
			return false; 
		
		double totalCostQuery = 0d; 
		
		Map<Sample, DataCenter> finalAssignment = new HashMap<Sample, DataCenter>();
		double totalAccessCostTrial = 0d;
		double totalStorageCostTrial = 0d;
		double totalUpdateCostTrial = 0d;
		double totalProcessCostTrial = 0d; 
		double totalSampleVolume = 0d; 
		double maxDelay = 0; 
		
		for (Sample sample : selectedSamples) {
			
			totalSampleVolume += sample.getVolume();
			
			double minCost = Double.MAX_VALUE;
			double accessCostAssigned = 0d; 
			double storageCostAssigned = 0d; 
			double updateCostAssigned = 0d; 
			double processCostAssigned = 0d; 
			DataCenter dcMinCost = null; 
			double delayThisSample = 0d; 
			
			for (DataCenter dc : assignedDCsSample.get(sample)) {
				
				double storageCost = sample.getVolume() * dc.getStorageCost();
				double processCost = sample.getVolume() * dc.getProcessingCost();

				double updateCostUnit = 0d; 
				double sampleReplicationdelay = 0d;
				
				if (!sample.getParentDataset().getDatacenter().equals(dc)) {
					DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(datacenterNetwork, sample.getParentDataset().getDatacenter(), dc);
					updateCostUnit = Double.MAX_VALUE;
					sampleReplicationdelay = Double.MAX_VALUE;
					for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
						if (0 == i ) {
							updateCostUnit = 0d;
							sampleReplicationdelay = 0d; 
						}
						updateCostUnit += datacenterNetwork.getEdgeWeight(shortestPath.getPathEdgeList().get(i));
						sampleReplicationdelay += shortestPath.getPathEdgeList().get(i).getDelay();
					}
				}
				
				sampleReplicationdelay *= sample.getVolume();
				
				double updateCost = 0d; 
				if (Double.MAX_VALUE != updateCostUnit)
					updateCost = updateCostUnit * sample.getVolume(); 
				else 
					System.out.println("ERROR: path should exist!!");
				
				double accessCostUnit = 0d;
				double resultAccessDelay = 0d; 
				if (!query.getHomeDataCenter().equals(dc)) {
					DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(datacenterNetwork, query.getHomeDataCenter(), dc);
					accessCostUnit = Double.MAX_VALUE;
					resultAccessDelay = Double.MAX_VALUE;
					for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
						if (0 == i ) {
							accessCostUnit = 0d;
							resultAccessDelay = 0d; 
						}
						accessCostUnit += datacenterNetwork.getEdgeWeight(shortestPath.getPathEdgeList().get(i));
						resultAccessDelay += shortestPath.getPathEdgeList().get(i).getDelay();
					}
				}
				
				resultAccessDelay *= (sample.getVolume() * Parameters.resultToSampleRatio);
				
				double accessCost = 0d; 
				if (Double.MAX_VALUE != accessCostUnit)
					accessCost += accessCostUnit * sample.getVolume(); 
				else 
					System.out.println("ERROR: path should exist!!");
				
				double costQuery = storageCost + updateCost + processCost + accessCost; 
				
				if (costQuery <= minCost) {
					minCost = costQuery; 
					dcMinCost = dc; 
					accessCostAssigned = accessCost; 
					storageCostAssigned = storageCost;
					updateCostAssigned = updateCost;
					processCostAssigned = processCost;
					delayThisSample = (sampleReplicationdelay + resultAccessDelay);
				}
			}
			
			finalAssignment.put(sample, dcMinCost);
			dcMinCost.admitSample(sample, query);
			
			totalAccessCostTrial += accessCostAssigned; 
			totalStorageCostTrial += storageCostAssigned;
			totalUpdateCostTrial += updateCostAssigned;
			totalProcessCostTrial += processCostAssigned;
			totalCostQuery += minCost;
			if (maxDelay <= delayThisSample)
				maxDelay = delayThisSample; 
		}
		
		double errorQ = 0d; 
		for (Sample sam : selectedSamples)
			errorQ += (sam.getError() * sam.getVolume() / totalSampleVolume);
		
		this.getCostTrials().set(trial, this.getCostTrials().get(trial) + totalCostQuery);
		this.getAccessCostTrials().set(trial, this.getAccessCostTrials().get(trial) + totalAccessCostTrial);
		this.getStorageCostTrials().set(trial, this.getStorageCostTrials().get(trial) + totalStorageCostTrial);
		this.getUpdateCostTrials().set(trial, this.getUpdateCostTrials().get(trial) + totalUpdateCostTrial);
		this.getProcessCostTrials().set(trial, this.getProcessCostTrials().get(trial) + totalProcessCostTrial);
		this.getAverageErrorTrials().set(trial, this.getAverageErrorTrials().get(trial) + errorQ);
		this.getAverageDelayTrials().set(trial, this.getAverageDelayTrials().get(trial) + maxDelay);
		
		return true; 
		
	}
	
	// functions
	// return the constructed flow network (edges and vertice in datacenter
	// network + the auxiliaty virtual-sink node)
	private SimpleWeightedGraph<Node, MinCostFlowEdge> initializeFlowNetwork (
			SimpleWeightedGraph<Node, InternetLink> dcNetwork,
			List<Sample> selectedSamples, 
			List<Commodity> commodities,
			List<DataCenter> virtualDataCenters, 
			List<Sample> virtualSamples, 
			Query query,
			int trial//, 
			//double error
			//Map<Dataset, Double> popularity
			) {

		SimpleWeightedGraph<Node, MinCostFlowEdge> flowNetwork = new SimpleWeightedGraph<Node, MinCostFlowEdge>(
				MinCostFlowEdge.class);
		// create a virtual sink node, and add it to the flownetwork
		Node virtualSink = new Node(SamplePlacementSimulator.idAllocator.nextId(), "Virtual-Sink");
		flowNetwork.addVertex(virtualSink);

		// calculate the basic sample size
		double basicSampleVolume = Parameters.sizePerDatasetMin * (1 - Parameters.errorBounds[Parameters.errorBounds.length - 1]);
		double computingResourceForBasicSample = basicSampleVolume * Parameters.computingAllocatedToUnitData;
		
		List<DataCenter> dcNodes = simulator.getDataCenters();//get all datacenter nodes of the datacenter network
		
		for (Sample selSample : selectedSamples) {
			// create a number of virtual samples 
			int vSampleCount = (int) Math.ceil(selSample.getVolume() / basicSampleVolume);
			for (int ii = 0; ii < vSampleCount - 1; ii ++) {
				Sample vSample = new Sample(selSample, basicSampleVolume, "Virtual Sample");
				virtualSamples.add(vSample);
				flowNetwork.addVertex(vSample);
				Commodity comm = new Commodity(vSample, virtualSink, 1);
				commodities.add(comm);
			}
			// add the last virtual sample
			Sample vSample = new Sample(selSample, selSample.getVolume() - (vSampleCount - 1)*basicSampleVolume, "Virtual Sample");
			virtualSamples.add(vSample);
			flowNetwork.addVertex(vSample);
			
			// create a new commodity that needs to be routed to virtualSink. 
			Commodity comm = new Commodity(vSample, virtualSink, 1d);
			commodities.add(comm);
		}
		
		//create virtual datacenter nodes and add them into the flow network
		List<DataCenter> virtualDCNodes = new ArrayList<DataCenter>();
		for(DataCenter dc : dcNodes) {
			if (dc.getAvailableComputing() < computingResourceForBasicSample) {
				DataCenter vDC = new DataCenter(SamplePlacementSimulator.idAllocator.nextId(), "Virtual Data Center", dc);
				vDC.setAvailableComputing(dc.getAvailableComputing());
				virtualDCNodes.add(vDC);
				virtualDataCenters.add(vDC);
				flowNetwork.addVertex(vDC);
			} else {
				int numVDCs = (int) Math.ceil(dc.getAvailableComputing()/computingResourceForBasicSample);
				for (int ii = 0; ii < numVDCs - 1; ii ++) {
					DataCenter vDC = new DataCenter(SamplePlacementSimulator.idAllocator.nextId(), "Virtual Data Center", dc);
					vDC.setAvailableComputing(computingResourceForBasicSample);
					virtualDCNodes.add(vDC);
					virtualDataCenters.add(vDC);
					flowNetwork.addVertex(vDC);
				}
				DataCenter vDC = new DataCenter(SamplePlacementSimulator.idAllocator.nextId(), "Virtual Data Center", dc);
				vDC.setAvailableComputing(dc.getAvailableComputing() - (numVDCs - 1) * computingResourceForBasicSample);
				virtualDCNodes.add(vDC);
				virtualDataCenters.add(vDC);
				flowNetwork.addVertex(vDC);
			}
		}
		////////////set edges in the auxiliary graph///////////////
		for (Node vSampleNode : virtualSamples) {
			Sample vSample = (Sample) vSampleNode; 
			
			for (Node vDCNode : virtualDCNodes) {
				DataCenter vDC = (DataCenter) vDCNode; 
				
				double delay = 0d; 
				double cost = 0d; 
				if (!vSample.getRealSample().getParentDataset().getDatacenter().equals(vDC.getParent())) {
					DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(dcNetwork, 
							vSample.getRealSample().getParentDataset().getDatacenter(), 
							vDC.getParent());
					
					delay = Double.MAX_VALUE;
					for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
						if (0 == i ) {
							delay = 0d;
						}
						delay += shortestPath.getPathEdgeList().get(i).getDelay();
						cost += shortestPath.getPathEdgeList().get(i).getCost();
					}
				}
				
				delay *= vSample.getRealSample().getVolume();
				cost *= vSample.getVolume();
				
				if (delay <= query.getDelayRequirement()) {
					MinCostFlowEdge edge = flowNetwork.addEdge(vSampleNode, vDCNode);
					edge.setCost(cost);
					edge.setCapacity(1d);// capacity is one.
					flowNetwork.setEdgeWeight(edge, cost);// set edge weight
				}
			}
		}
		
		for (Node vDCNode : virtualDCNodes) {
			MinCostFlowEdge edge = flowNetwork.addEdge(vDCNode, virtualSink);
			edge.setCost(0d);
			edge.setCapacity(10 * virtualSamples.size());// capacity is the maximum number of virtual samples. 
			flowNetwork.setEdgeWeight(edge, 0d);// set edge weight
		}
		
		return flowNetwork;
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

	public int getLowestErrorIndex() {
		return lowestErrorIndex;
	}

	public void setLowestErrorIndex(int lowestErrorIndex) {
		this.lowestErrorIndex = lowestErrorIndex;
	}

	public List<Double> getAverageDelayTrials() {
		return averageDelayTrials;
	}

	public void setAverageDelayTrials(List<Double> averageDelayTrials) {
		this.averageDelayTrials = averageDelayTrials;
	}

}