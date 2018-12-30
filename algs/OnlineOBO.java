package algs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jgrapht.graph.SimpleWeightedGraph;

import graph.InternetLink;
import graph.Node;
import simulation.Parameters;
import simulation.SamplePlacementSimulator;
import system.DataCenter;
import system.Dataset;
import system.Query;
import utils.RanNum;

public class OnlineOBO {
	
	/************* parameters *****************/
	private SamplePlacementSimulator simulator = null;

	// the number of admitted for different trials, where each trial has a different set of queries. 
	private List<Integer> admittedNumOfQueriesTrials = new ArrayList<Integer>();

	private List<Double> averageErrorTrials = new ArrayList<Double>();
	
	private Map<Integer, List<Double>> costTrialsAccumulated = new HashMap<Integer, List<Double>>();
	private Map<Integer, List<Double>> storageCostTrialsAccumulated = new HashMap<Integer, List<Double>>();// the cost of a query (average)
	private Map<Integer, List<Double>> updateCostTrialsAccumulated = new HashMap<Integer, List<Double>>();
	private Map<Integer, List<Double>> accessCostTrialsAccumulated = new HashMap<Integer, List<Double>>();
	private Map<Integer, List<Double>> processCostTrialsAccumulated = new HashMap<Integer, List<Double>>();
	
	// prediction parameters:
	private int K = 5; // parameter for AR part of the prediction model
	private double [] varsAR = null; 
	private int Q = 5; // parameter for MA part of the prediction model
	private double [] varsMA = null; 
		
	public OnlineOBO(SamplePlacementSimulator simulator, int numOfTrials) {
		
		this.simulator = simulator;
		for (int i = 0; i < numOfTrials; i++) {
			this.admittedNumOfQueriesTrials.add(i, 0);
//			this.costTrials.add(i, 0.0);
//			this.storageCostTrials.add(i, 0.0);
//			this.updateCostTrials.add(i, 0.0);
//			this.accessCostTrials.add(i, 0.0);
//			this.processCostTrials.add(i, 0.0);
			this.averageErrorTrials.add(i, 0.0);
		}
		
		//TODO double check the setting of this one.. 
		for (int timeslot = 0; timeslot < Parameters.maxNumOfQueriesPerTS; timeslot ++) {
			this.costTrialsAccumulated.put(timeslot, new ArrayList<Double>());
			this.storageCostTrialsAccumulated.put(timeslot, new ArrayList<Double>());
			this.updateCostTrialsAccumulated.put(timeslot, new ArrayList<Double>());
			this.accessCostTrialsAccumulated.put(timeslot, new ArrayList<Double>());
			this.processCostTrialsAccumulated.put(timeslot, new ArrayList<Double>());
			for (int i = 0; i < numOfTrials; i++) {
				this.costTrialsAccumulated.get(timeslot).add(i, 0d);
				this.storageCostTrialsAccumulated.get(timeslot).add(i, 0.0);
				this.updateCostTrialsAccumulated.get(timeslot).add(i, 0.0);
				this.accessCostTrialsAccumulated.get(timeslot).add(i, 0.0);
				this.processCostTrialsAccumulated.get(timeslot).add(i, 0.0);
			}
		}
		
		this.varsAR = new double [this.K];
		for (int i = 0; i < this.K; i ++) {
			this.varsAR[i] = 1/Math.pow(2, i + 1);
		}
		
		this.varsMA = new double [this.Q];
		for (int i = 0; i < this.K; i ++) {
			this.varsMA[i] = 1/Math.pow(2, i + 1);
		}
	}
	
	public void run(int numOfTrials, int periodLength) {
		
		SimpleWeightedGraph<Node, InternetLink> datacenterNetwork = this.simulator.getDatacenterNetwork();
		setEdgeWeightDataCenterNetwork(datacenterNetwork);

		for (int trial = 0; trial < numOfTrials; trial++) {
			
			if (trial > 0) {
				this.simulator.modifyCosts();// double check this.
				//this.resetDataCenterNetwork(datacenterNetwork);
			}
			
			List<Query> queries = simulator.getQueries().get(trial);//get all queries at time slot "timeslot"
						
			List<Dataset> allDatasets = new ArrayList<Dataset>();
			for (Entry<Integer, List<Dataset>> entry : simulator.getDatasets().entrySet()){
				for (Dataset ds : entry.getValue())
					allDatasets.add(ds);
			}
			//List<Dataset> existingDatasets = new ArrayList<Dataset>();
			
			Map<Integer, Map<Dataset, Double>> historicalPopularities = new HashMap<Integer, Map<Dataset, Double>>();
			for (int i = 0; i < this.K; i ++) {// initialize the popularity data.  
				if (null == historicalPopularities.get(i))
					historicalPopularities.put(i, new HashMap<Dataset, Double>());
				
				Map<Dataset, Double> popularityTS = historicalPopularities.get(i);
				for (Dataset ds : allDatasets) {
					popularityTS.put(ds, periodLength * RanNum.getRandomDoubleRange(1, 0.2));
				}
			}
			
			//Map<Integer, Map<Dataset, Double>> predictedPopularities = new HashMap<Integer, Map<Dataset, Double>>();
			
			double threshold = periodLength * 0.9;// dataset is requested by 50% of the requests in each period. 
			int periodCount = 0;// starts with 0
			int timeslot = 0; // starts with 0
			
			Double accumulatedCost = 0d;
			Double accumulatedAccessCost = 0d;
			Double accumulatedStorageCost = 0d;
			Double accumulatedUpdateCost = 0d;
			Double accumulatedProcessCost = 0d;
			
			Map<Dataset, Double> currentPopularity = null;
			
			for (Query query : queries) {// process queries one by one	
				
				if (0 == timeslot % periodLength) {// prediction needed.
					periodCount ++; 
					// update the popularities result. 
					currentPopularity = predictPopularity(allDatasets, historicalPopularities, timeslot, periodCount);
				} 
				
				if (null == currentPopularity)
					System.out.println("currently predicted popularity should not be null");
				
				// update historical popularities
				if (null == historicalPopularities.get(periodCount + this.K - 1))
					historicalPopularities.put(periodCount + this.K - 1, new HashMap<Dataset, Double>());
				
				Set<Dataset> DSsWithUpdatedPop = new HashSet<Dataset>();
				for (Dataset ds : query.getDatasets()) {
					DSsWithUpdatedPop.add(ds);
					if (null == historicalPopularities.get(periodCount + this.K - 1).get(ds))
						historicalPopularities.get(periodCount + this.K - 1).put(ds, 0d);
					
					historicalPopularities.get(periodCount + this.K - 1).put(ds, historicalPopularities.get(periodCount + this.K - 1).get(ds) + 1d);
				}
				
				for (Dataset ds : allDatasets) {
					if(!DSsWithUpdatedPop.contains(ds))
						historicalPopularities.get(periodCount + this.K - 1).put(ds, 0d);// no queries access these datasets.
				}

				ApproOneQuery approOneQuery = new ApproOneQuery(this.simulator, Parameters.numOfTrials);
				approOneQuery.runWithOneQuery(query, datacenterNetwork, this.simulator.getDataCenters(), trial, currentPopularity, threshold);
				
				accumulatedCost += approOneQuery.getCostTrials().get(trial); 
				this.costTrialsAccumulated.get(timeslot).add(trial, accumulatedCost);
				accumulatedAccessCost +=  approOneQuery.getAccessCostTrials().get(trial);
				this.accessCostTrialsAccumulated.get(timeslot).add(trial, accumulatedAccessCost);
				accumulatedStorageCost += approOneQuery.getStorageCostTrials().get(trial);
				this.storageCostTrialsAccumulated.get(timeslot).add(trial, accumulatedStorageCost);
				accumulatedUpdateCost += approOneQuery.getUpdateCostTrials().get(trial);
				this.updateCostTrialsAccumulated.get(timeslot).add(trial, accumulatedUpdateCost);
				accumulatedProcessCost += approOneQuery.getProcessCostTrials().get(trial);
				this.processCostTrialsAccumulated.get(timeslot).add(trial, accumulatedProcessCost);
				
				if (timeslot == queries.size() - 1)
					this.averageErrorTrials.add(trial, approOneQuery.getAverageErrorTrials().get(trial));
				
				timeslot ++; // each time slot has one query
				
				// don't need to reset data centers here. 
			}
			
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
	}
	
	public Map<Dataset, Double> predictPopularity(
			List<Dataset> datasets, 
			Map<Integer, Map<Dataset, Double>> historicalPopularities, 
			int currentTimeSlot, 
			int currentPeriod) {// predict the popularity of each dataset in the beginning of each period.
		//
		Map<Dataset, Double> popularityCurrentP = new HashMap<Dataset, Double>();
		for (Dataset ds : datasets) {
			double popularity = 0d;
			for (int i = 1; i <= this.K; i ++) {
				popularity += this.varsAR[i - 1] * historicalPopularities.get(this.K - 1 + currentPeriod - i).get(ds);
			}
			popularityCurrentP.put(ds, popularity);
		}
		//historicalPopularities.put(currentPeriod + this.K - 1, popularityCurrentP);
		return popularityCurrentP; 
	}
	
	private void setEdgeWeightDataCenterNetwork(SimpleWeightedGraph<Node, InternetLink> datacenterNetwork) {
		for (InternetLink il : datacenterNetwork.edgeSet()) {
			datacenterNetwork.setEdgeWeight(il, il.getCost());
		}
	}

	public Map<Integer, List<Double>> getCostTrialsAccumulated() {
		return costTrialsAccumulated;
	}

	public void setCostTrialsAccumulated(Map<Integer, List<Double>> costTrialsAccumulated) {
		this.costTrialsAccumulated = costTrialsAccumulated;
	}

	public Map<Integer, List<Double>> getStorageCostTrialsAccumulated() {
		return storageCostTrialsAccumulated;
	}

	public void setStorageCostTrialsAccumulated(Map<Integer, List<Double>> storageCostTrialsAccumulated) {
		this.storageCostTrialsAccumulated = storageCostTrialsAccumulated;
	}

	public Map<Integer, List<Double>> getUpdateCostTrialsAccumulated() {
		return updateCostTrialsAccumulated;
	}

	public void setUpdateCostTrialsAccumulated(Map<Integer, List<Double>> updateCostTrialsAccumulated) {
		this.updateCostTrialsAccumulated = updateCostTrialsAccumulated;
	}

	public Map<Integer, List<Double>> getAccessCostTrialsAccumulated() {
		return accessCostTrialsAccumulated;
	}

	public void setAccessCostTrialsAccumulated(Map<Integer, List<Double>> accessCostTrialsAccumulated) {
		this.accessCostTrialsAccumulated = accessCostTrialsAccumulated;
	}

	public Map<Integer, List<Double>> getProcessCostTrialsAccumulated() {
		return processCostTrialsAccumulated;
	}

	public void setProcessCostTrialsAccumulated(Map<Integer, List<Double>> processCostTrialsAccumulated) {
		this.processCostTrialsAccumulated = processCostTrialsAccumulated;
	}

	public List<Double> getAverageErrorTrials() {
		return averageErrorTrials;
	}

	public void setAverageErrorTrials(List<Double> averageErrorTrials) {
		this.averageErrorTrials = averageErrorTrials;
	}
	
}
