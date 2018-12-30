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

public class BenchmarkOneQuery {
	
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
	
	private int lowestErrorIndex = 0;
	
	/************* construction function *****************/
	public BenchmarkOneQuery(SamplePlacementSimulator simulator, int numOfTrials) {
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
				boolean admitted = runWithOneQuery(query, datacenterNetwork, trial);
				
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
			this.getAverageDelayTrials().set(trial, this.getAverageDelayTrials().get(trial)/numOfSuccessAdmissions);
		}// end for trials;
	}
	
	private boolean runWithOneQuery(
			Query query, 
			SimpleWeightedGraph<Node, InternetLink> datacenterNetwork, 
			int trial
			) {
		
		List<Commodity> commodities = new ArrayList<Commodity>();
		//List<Commodity> rejectedCommodities = new ArrayList<Commodity>();
		int errorIndexForUnAdmitted = this.getLowestErrorIndex();
//		for (int i = 0; i < Parameters.errorBounds.length; i ++) {
//			if (this.getLowestErrorIndex() == Parameters.errorBounds[i]) {
//				errorIndexForUnAdmitted = i;
//				break;
//			}
//		}
		
		SimpleWeightedGraph<Node, MinCostFlowEdge> flowNet = this.initializeFlowNetwork(datacenterNetwork, query, commodities, trial, errorIndexForUnAdmitted);
		boolean increaseAdmittedErrors = false;
		
		while(!commodities.isEmpty()) {
			
			int numOfDCsHaveSampleErrorIncreased = 0;
			if (increaseAdmittedErrors) {
				
				for (DataCenter dc : this.simulator.getDataCenters()) {
					Set<Sample> adjustedSamples = new HashSet<Sample>();
					Map<Sample, Set<Query>> sampleQueries = new HashMap<Sample, Set<Query>>();
					
					boolean haveSampleErrorsIncreased = false; 
					for (Sample admittedSample : dc.getAdmittedSamples()) {
						
						int newErrorIndex = -1;
						
						for (int i = 0; i < Parameters.errorBounds.length - 1; i ++) {
							
							if (admittedSample.getError() == admittedSample.getParentDataset().getSamples().get(i).getError() ) {
								newErrorIndex = i + 1; 
								break;
							}
						}
						
						if (-1 != newErrorIndex) {
							
							Sample newSample = admittedSample.getParentDataset().getSamples().get(newErrorIndex);
							
							adjustedSamples.add(newSample);
							sampleQueries.put(newSample, dc.getAdmittedQueriesSamples().get(admittedSample));
							haveSampleErrorsIncreased = true; 
						} else {
							adjustedSamples.add(admittedSample);
							sampleQueries.put(admittedSample, dc.getAdmittedQueriesSamples().get(admittedSample));
						}
					}
					
					if (haveSampleErrorsIncreased)
						numOfDCsHaveSampleErrorIncreased ++; 
					
					dc.setAdmittedSamples(adjustedSamples);
					dc.setAdmittedQueriesSamples(sampleQueries);
				}
				
				flowNet = adjustFlowNetwork(datacenterNetwork, flowNet, commodities, errorIndexForUnAdmitted);
			}
			
			//double error = Parameters.errorBounds[errorIndexForUnAdmitted];
			
			for (Iterator<Commodity> iter = commodities.iterator(); iter.hasNext(); ) {
				Commodity comm = iter.next();
				DemandNode deNode = (DemandNode) comm.getSource();
				Query toBeAdmittedQuery = deNode.getQuery();
				Sample toBeAdmittedSample = deNode.getDataset().getSamples().get(errorIndexForUnAdmitted);
				
				this.updateEdgeCostAndCapacities(datacenterNetwork, flowNet, errorIndexForUnAdmitted);
				
				DijkstraShortestPath<Node, MinCostFlowEdge> shortestPath = new DijkstraShortestPath<Node, MinCostFlowEdge>(flowNet, comm.getSource(), comm.getSink());
				
				if (null == shortestPath.getPath() || shortestPath.getPathEdgeList().isEmpty()){
					//System.out.println("ERROR: shortest path should not be empty!!");
					//System.exit(0);
					if (increaseAdmittedErrors && (numOfDCsHaveSampleErrorIncreased == 0)){
						return false;
					}
					continue;
				}
				
				DataCenter targetDC = null;
				for (MinCostFlowEdge pathEdge : shortestPath.getPathEdgeList()){
					Node edgeS = flowNet.getEdgeSource(pathEdge); 
					Node edgeT = flowNet.getEdgeTarget(pathEdge); 
					if ( edgeS instanceof DataCenter &&  edgeT instanceof DataCenter) {
						if (((DataCenter) edgeS).getParent() == null){
							targetDC = (DataCenter) edgeS;
							break;
						} else {
							targetDC = (DataCenter) edgeT;
							break;
						}
					}
				}
				
				// admit this sample into "targetDC"
				targetDC.admitSample(toBeAdmittedSample, toBeAdmittedQuery);
				iter.remove();//  admit this commodity and remove it from the list. 
			}
			
			if (!commodities.isEmpty()) {
				if (errorIndexForUnAdmitted < Parameters.errorBounds.length - 1) {
					errorIndexForUnAdmitted ++;
					flowNet = adjustFlowNetwork(datacenterNetwork, flowNet, commodities, errorIndexForUnAdmitted);
					increaseAdmittedErrors = false;
				} else 
					increaseAdmittedErrors = true;
			}
		}// end while
		
		double totalStorageCostTrial = 0d;
		double totalUpdateCostTrial = 0d;
		double totalAccessCostTrial = 0d;
		double totalProcessCostTrial = 0d;
		
		Map<Sample, Query> queryPlacedThisSample = new HashMap<Sample, Query>();
		Map<Query, List<ReturnPair<Sample, DataCenter>>> queriesSampleLocations = new HashMap<Query, List<ReturnPair<Sample, DataCenter>>>();
		for (DataCenter dc : this.simulator.getDataCenters()) {
			
			if (dc.getAdmittedSamples().isEmpty())
				continue; 
			
			// storage cost for all placed samples
			for (Sample admittedSample : dc.getAdmittedSamples()) {
				totalStorageCostTrial += admittedSample.getVolume() * dc.getStorageCost();
				totalProcessCostTrial += admittedSample.getVolume() * dc.getProcessingCost();

				double updateCost = 0d; 
				if (!admittedSample.getParentDataset().getDatacenter().equals(dc)) {
					DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(datacenterNetwork, admittedSample.getParentDataset().getDatacenter(), dc);
					updateCost = Double.MAX_VALUE;
					for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++) {
						if (0 == i ) 
							updateCost = 0d;
						updateCost += datacenterNetwork.getEdgeWeight(shortestPath.getPathEdgeList().get(i));
					}
				}
				
				if (Double.MAX_VALUE != updateCost)
					totalUpdateCostTrial += updateCost * admittedSample.getVolume(); 
				else 
					System.out.println("ERROR: path should exist!!");
			}
			
			for (Entry<Sample, Set<Query>> entry : dc.getAdmittedQueriesSamples().entrySet()) {
				Sample admittedSample = entry.getKey();
				
				boolean querySelected = false;
				for (Query accessQuery : entry.getValue()) {
					
					if (!querySelected) {
						queryPlacedThisSample.put(admittedSample, accessQuery);
						querySelected = true; 
					}
					
					ReturnPair<Sample, DataCenter> pair = new ReturnPair<Sample, DataCenter>(admittedSample, dc);
					
					if (null == queriesSampleLocations.get(accessQuery)) {
						queriesSampleLocations.put(accessQuery, new ArrayList<ReturnPair<Sample, DataCenter>>());
					}
					
					queriesSampleLocations.get(accessQuery).add(pair);
					
					double accessCost = 0d;
					if (!accessQuery.getHomeDataCenter().equals(dc)) {
						DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(datacenterNetwork, accessQuery.getHomeDataCenter(), dc);
						accessCost = Double.MAX_VALUE;
						for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
							if (0 == i ) 
								accessCost = 0d;
							accessCost += datacenterNetwork.getEdgeWeight(shortestPath.getPathEdgeList().get(i));
						}
					}
					
					if (Double.MAX_VALUE != accessCost)
						totalAccessCostTrial += accessCost * admittedSample.getVolume(); 
					else 
						System.out.println("ERROR: path should exist!!");
				}
			}
		}
		
		this.getCostTrials().set(trial, this.getCostTrials().get(trial) + totalAccessCostTrial + totalStorageCostTrial + totalUpdateCostTrial + totalProcessCostTrial);
		this.getAccessCostTrials().set(trial, this.getAccessCostTrials().get(trial) + totalAccessCostTrial);
		this.getStorageCostTrials().set(trial, this.getStorageCostTrials().get(trial) + totalStorageCostTrial);
		this.getUpdateCostTrials().set(trial, this.getUpdateCostTrials().get(trial) + totalUpdateCostTrial);
		this.getProcessCostTrials().set(trial, this.getProcessCostTrials().get(trial) + totalProcessCostTrial);
		
		Map<Query, Set<Sample>> queryPlacedSamples = new HashMap<Query, Set<Sample>>();
		for (DataCenter dc : this.simulator.getDataCenters()) {
			
			for (Entry<Sample, Set<Query>> entry : dc.getAdmittedQueriesSamples().entrySet()) {
				
				for (Query query1 : entry.getValue()) {
					if (null == queryPlacedSamples.get(query1))
						queryPlacedSamples.put(query1, new HashSet<Sample>());
					queryPlacedSamples.get(query1).add(entry.getKey());					
				}
			}
		}
				
		double averageError = 0d; 
		for (Entry<Query, Set<Sample>> entry : queryPlacedSamples.entrySet()) {
			
			double totalSampleVolume = 0d; 
			for (Sample sam : entry.getValue())
				totalSampleVolume += sam.getVolume();
			
			double errorQ = 0d; 
			for (Sample sam : entry.getValue()) {
				errorQ += (sam.getError() * sam.getVolume() / totalSampleVolume);
			}
			
			averageError += errorQ;
		}
		//averageError /= queryPlacedSamples.size(); 
		
		this.getAverageErrorTrials().set(trial, this.getAverageErrorTrials().get(trial) + averageError);
		
		for (Entry<Query, List<ReturnPair<Sample, DataCenter>>> entry : queriesSampleLocations.entrySet()) {
			Query q = entry.getKey();
			
			double sampleReplicationDelay = 0;
			double accessDelay = 0; 
			for (ReturnPair<Sample, DataCenter> pair : entry.getValue()) {
				Sample sam = pair.getValue1();
				DataCenter dc = pair.getValue2(); 
				
				if (queryPlacedThisSample.get(sam).equals(q)) {
					// count the replcation delay. 
					double delay = 0d; 
					if (!sam.getParentDataset().getDatacenter().equals(dc)) {
						DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(datacenterNetwork, 
								sam.getParentDataset().getDatacenter(), 
								dc);
						
						delay = Double.MAX_VALUE;
						for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
							if (0 == i ) {
								delay = 0d;
							}
							delay += shortestPath.getPathEdgeList().get(i).getDelay();
						}
					}
					
					delay *= sam.getVolume(); 
					
					if (sampleReplicationDelay < delay) {
						sampleReplicationDelay = delay;
					}
				} 
				
				// calculate the access delay
				double accDelay = 0d;
				if (!q.getHomeDataCenter().equals(dc)) {
					DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(datacenterNetwork, 
							q.getHomeDataCenter(),
							dc);
					
					accDelay = Double.MAX_VALUE;
					for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
						if (0 == i ) {
							accDelay = 0d;
						}
						accDelay += shortestPath.getPathEdgeList().get(i).getDelay();
					}
				}
				accDelay *= (sam.getVolume() * Parameters.resultToSampleRatio);
				
				if (accessDelay < accDelay)
					accessDelay = accDelay;
			}
			
			this.getAverageDelayTrials().set(trial, this.getAverageDelayTrials().get(trial) + accessDelay + sampleReplicationDelay);
		}
		
		return true;
		
	}
	
	private void updateEdgeCostAndCapacities(SimpleWeightedGraph<Node, InternetLink> dcNetwork, 
			SimpleWeightedGraph<Node, MinCostFlowEdge> flowNetwork, 
			int errorIndexForUnAdmitted) {
		
		double totalAvailableCapacity = 0d;
		
		for (MinCostFlowEdge flowEdge : flowNetwork.edgeSet()) {
			Node edgeS = flowNetwork.getEdgeSource(flowEdge);
			Node edgeT = flowNetwork.getEdgeTarget(flowEdge);
			DataCenter dc = null; 
			
			if ((edgeS instanceof DataCenter)
					&& (edgeT instanceof DataCenter) 
					&& (((DataCenter) edgeS).getParent() == null)) {
				dc = (DataCenter) edgeS; 
			} else if ((edgeS instanceof DataCenter)
					&& (edgeT instanceof DataCenter) 
					&& (((DataCenter) edgeT).getParent() == null)) {
				dc = (DataCenter) edgeT; 
			}
			
			if (null == dc)
				continue;
			
			flowEdge.setCost(dc.getProcessingCost());
			//flowNetwork.setEdgeWeight(flowEdge, dc.getProcessingCost() * (1 / dc.getAvailableComputing()));
			double edgeWeight = 0; 
			if (dc.getAvailableComputing() < 0 )
				edgeWeight = 1 / 0.0002;
			else 
				edgeWeight = 1 / dc.getAvailableComputing();
			
			flowNetwork.setEdgeWeight(flowEdge, edgeWeight);
			totalAvailableCapacity += dc.getAvailableComputing() / Parameters.computingAllocatedToUnitData;
			flowEdge.setCapacity(dc.getAvailableComputing() / Parameters.computingAllocatedToUnitData);
		}
		
		for (MinCostFlowEdge flowEdge : flowNetwork.edgeSet()) {
			Node edgeS = flowNetwork.getEdgeSource(flowEdge);
			Node edgeT = flowNetwork.getEdgeTarget(flowEdge);
			DemandNode deNode = null;
			Query query = null; 
			Sample sample = null; 
			DataCenter dc = null; 
			
			if ((edgeS instanceof DemandNode) && (edgeT instanceof DataCenter)){
				deNode = (DemandNode) edgeS; 
				
				sample = deNode.getDataset().getSamples().get(errorIndexForUnAdmitted);
				dc = (DataCenter) edgeT;
			} else if ((edgeT instanceof DemandNode)&&(edgeS instanceof DataCenter)) {
				deNode = (DemandNode) edgeT; 
				sample = deNode.getDataset().getSamples().get(errorIndexForUnAdmitted); 
				dc = (DataCenter) edgeS;
			} 
			if (edgeS.getName().equals("Virtual-Sink") || edgeT.getName().equals("Virtual-Sink")){
				flowEdge.setCost(0);
				flowEdge.setCapacity(totalAvailableCapacity);
				flowNetwork.setEdgeWeight(flowEdge, 0d);
			}
			
			if (null == deNode)
				continue;
			
			query = deNode.getQuery();
			
			double accessCost = 0d; 
			if (!query.getHomeDataCenter().equals(dc)) {
				DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(dcNetwork, query.getHomeDataCenter(), dc);
				double cost = Double.MAX_VALUE;
				for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
					if (0 == i ) 
						cost = 0d;
					cost += dcNetwork.getEdgeWeight(shortestPath.getPathEdgeList().get(i));
				}
				accessCost = cost; 
			}
			
			double updateCost = 0d; 
			double storageCost = 0d; 

			if (!dc.isSampleAdmitted(sample)) {
				// cost = update cost + access cost + storage cost 
				// update cost
				if (!sample.getParentDataset().getDatacenter().equals(dc)) {
					DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(dcNetwork, sample.getParentDataset().getDatacenter(), dc);
					double cost = Double.MAX_VALUE;
					for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
						if (0 == i ) 
							cost = 0d;
						cost += dcNetwork.getEdgeWeight(shortestPath.getPathEdgeList().get(i));
					}
					updateCost = cost; 
				}
				storageCost = dc.getStorageCost();
			}
			
			double totalCost = accessCost + updateCost + storageCost;
			flowEdge.setCost(totalCost);
			//flowNetwork.setEdgeWeight(flowEdge, totalCost * (1 / dc.getAvailableComputing()));
			
			double edgeWeight = 0; 
			if (dc.getAvailableComputing() < 0 )
				edgeWeight = 1 / 0.0002;
			else 
				edgeWeight = 1 / dc.getAvailableComputing();
			
			flowNetwork.setEdgeWeight(flowEdge, edgeWeight);
			flowEdge.setCapacity(totalAvailableCapacity);
			
		}
	}
	
	private SimpleWeightedGraph<Node, MinCostFlowEdge> adjustFlowNetwork(
			SimpleWeightedGraph<Node, InternetLink> dcNetwork,
			SimpleWeightedGraph<Node, MinCostFlowEdge> flowNetwork,
			List<Commodity> commodities,
			int errorIndexForUnAdmitted
			) {
		
		for (Commodity comm : commodities) {
			DemandNode demandNode = (DemandNode) comm.getSource();
			
			for (Node dcNode : simulator.getDataCenters()) {
				
				MinCostFlowEdge previousEdge = flowNetwork.getEdge(demandNode, dcNode);
				if (null != previousEdge)
					flowNetwork.removeEdge(previousEdge);
				
				//check the delay from the home datacenter of the query to dcNode, if yes, there is an edge
				double delay = 0d;
				if (demandNode.getDataset().getDatacenter().equals(dcNode)){
					DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(dcNetwork, demandNode.getDataset().getDatacenter(), dcNode);
					delay = Double.MAX_VALUE;
					for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
						if (0 == i ) {
							delay = 0d;
						}
						delay += shortestPath.getPathEdgeList().get(i).getDelay();
					}
				}
				
				Sample sample = demandNode.getDataset().getSamples().get(errorIndexForUnAdmitted); 
				
				delay *= sample.getVolume();
				
				if (((DataCenter) dcNode).isSampleAdmitted(sample)) {
					if (delay <= demandNode.getQuery().getDelayRequirement()) {
						DataCenter dc = ((DataCenter) dcNode); 
						double edgeWeight = 1 / dc.getAvailableComputing();
						if (edgeWeight > 0) {
							MinCostFlowEdge addedEdge = flowNetwork.addEdge(comm.getSource(), dcNode);
							flowNetwork.setEdgeWeight(addedEdge, edgeWeight);
						} else 
							continue;
					}
				} else {
					if (delay <= demandNode.getQuery().getDelayRequirement() && 
							(demandNode.getDataset().getSamples().get(errorIndexForUnAdmitted).getVolume() * Parameters.computingAllocatedToUnitData < ((DataCenter) dcNode).getAvailableComputing())) {
						DataCenter dc = ((DataCenter) dcNode); 
						double edgeWeight = 1 / dc.getAvailableComputing();
						if (edgeWeight > 0) {
							MinCostFlowEdge addedEdge = flowNetwork.addEdge(comm.getSource(), dcNode);
							flowNetwork.setEdgeWeight(addedEdge, edgeWeight);
						}
					}
				}
			}
		}
		
		return flowNetwork;
	}
	// functions
	// return the constructed flow network (edges and vertice in datacenter
	// network + the auxiliaty virtual-sink node)
	private SimpleWeightedGraph<Node, MinCostFlowEdge> initializeFlowNetwork(
			SimpleWeightedGraph<Node, InternetLink> dcNetwork,
			Query query, 
			List<Commodity> commodities,
			int trial, 
			int errorIndexForUnAdmitted
			) {

		SimpleWeightedGraph<Node, MinCostFlowEdge> flowNetwork = new SimpleWeightedGraph<Node, MinCostFlowEdge>(
				MinCostFlowEdge.class);
		// create a virtual sink node, and add it to the flownetwork
		Node virtualSink = new Node(SamplePlacementSimulator.idAllocator.nextId(), "Virtual-Sink");
		flowNetwork.addVertex(virtualSink);

		List<DataCenter> dcNodes = simulator.getDataCenters();//get all datacenter nodes of the datacenter network
		for (DataCenter dcNode : dcNodes)
			dcNode.randomReGenerateCapacity();// randomly generate available resources of each data center. 
		
		//List<Query> queriesAccessData = simulator.getQueries().get(trial);//get all queries at time slot "timeslot"
		List<DemandNode> demandNodes = new ArrayList<DemandNode>();//create demand nodes and add all demand nodes into the flow network
		
		
		List<Dataset> dsListOfAQuery = query.getDatasets();//get the list of datasets that query will access
		for (Dataset dsQuery : dsListOfAQuery) {
			DemandNode demandNode = new DemandNode(SamplePlacementSimulator.idAllocator.nextId(), "Demand Node", query, dsQuery);
			demandNodes.add(demandNode);
			flowNetwork.addVertex(demandNode);
				// create a new commodity that needs to be routed to virtualSink. 
			Commodity comm = new Commodity(demandNode, virtualSink, dsQuery.getVolume());
			commodities.add(comm);
		}
		
		//create virtual datacenter nodes and add them into the flow network
		List<DataCenter> virtualDCNodes = new ArrayList<DataCenter>();
		for(DataCenter dc : dcNodes) {
			flowNetwork.addVertex(dc);
			DataCenter vDC = new DataCenter(SamplePlacementSimulator.idAllocator.nextId(), "Virtual Data Center", dc);
			virtualDCNodes.add(vDC);
			flowNetwork.addVertex(vDC);
			flowNetwork.addEdge(dc, vDC);
			flowNetwork.addEdge(vDC, virtualSink);			
		}
		
		for (Node deNode : demandNodes) {
			DemandNode demandNode = (DemandNode) deNode;
			for (Node dcNode : dcNodes) {
				//check the delay from the home datacenter of the query to dcNode, if yes, there is an edge
				double delay = 0d; 
				if (!demandNode.getDataset().getDatacenter().equals(dcNode)) {
					DijkstraShortestPath<Node, InternetLink> shortestPath = new DijkstraShortestPath<Node, InternetLink>(dcNetwork, demandNode.getDataset().getDatacenter(), dcNode);
					delay = Double.MAX_VALUE;
					for (int i = 0; i < shortestPath.getPathEdgeList().size(); i ++){
						if (0 == i ) {
							delay = 0d;
						}
						delay += shortestPath.getPathEdgeList().get(i).getDelay();
					}
				}
							
				delay *= demandNode.getDataset().getSamples().get(errorIndexForUnAdmitted).getVolume();
				
				if (delay <= demandNode.getQuery().getDelayRequirement() && 
						(demandNode.getDataset().getSamples().get(errorIndexForUnAdmitted).getVolume() * Parameters.computingAllocatedToUnitData < ((DataCenter) dcNode).getAvailableComputing())) {
					flowNetwork.addEdge(deNode, dcNode);
				}
			}
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