package simulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleWeightedGraph;

import algs.ApproOneQuery;
import algs.Benchmark1;
import algs.BenchmarkOneQuery;
import algs.BenchmarkOnlineBatch;
import algs.BenchmarkOnlineOBO;
import algs.OnlineBatch;
import algs.OnlineOBO;
import algs.ProposedApproximationAlg;
import algs.ProposedHeuristicAlg;
import graph.InternetLink;
import graph.NetworkGenerator;
import graph.Node;
import system.DataCenter;
import system.Dataset;
import system.Query;
import system.Sample;
import utils.IdAllocator;
import utils.RanNum;

public class SamplePlacementSimulator {
	private SimpleWeightedGraph<Node, InternetLink> datacenterNetwork = null;
	public static IdAllocator idAllocator = new IdAllocator();
	
	private List<DataCenter> dataCenterList = new ArrayList<DataCenter>();
	// <trial, list of datasets>
	private Map<Integer, List<Dataset>> datasets = new HashMap<Integer, List<Dataset>>();
	private Map<Integer, List<Sample>> samples = new HashMap<Integer, List<Sample>>();
	// <trial, list of queries>
	private Map<Integer, List<Query>> queries = new HashMap<Integer,List<Query>>();
	private Map<Integer, Map<Integer, List<Query>>> queriesOnlineBatch = new HashMap<Integer, Map<Integer, List<Query>>>();

	public SamplePlacementSimulator() {
		
	}

	public static void main(String[] s) {
		
		// 1. performance with different network sizes;
		performanceSingleQuery();
		// 2. performance by varying maximum delay requirement of queries
		//performanceSingleQueryMaxDelayReq();
		// 3. performance by varying maximum number of datasets of queries.
		//performanceSingleQueryMaxDSNumPerQuery();
		// 4. performance by varying error bounds
		//performanceSingleQueryLowestErrorBound();
		
		// 5. performance of heuristic
		//performanceMultiQueries(true);
		// 6. performance of heuristic with different network sizes; 
		//performanceMultiQueries(false);
		// 7. performance by varying maximum delay requirement of queries
		//performanceMultiQueriesMaxDelayReq();
		// 8. performance by varying maximum number of datasets of queries.
		//performanceMultiQueriesMaxDSNumPerQuery();
		// 9. performance by varying error bounds
		//performanceMultiQueriesLowestErrorBound();
		
	}
	
	public static void performanceSingleQuery() {
		
		int numOfAlgs = 2;// the proposed algorithm and a benchmark algorithm. 
		
		//int [] network_sizes = {20, 30, 40, 50, 100, 150, 200};
		int [] network_sizes = {20, 50, 100, 150, 200};
		double [][] aveCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveStorageCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveUpdateCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveAccessCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveProcessCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveError = new double [numOfAlgs][network_sizes.length];
		double [][] aveDelay = new double [numOfAlgs][network_sizes.length];
		
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			
			for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
				String networkIndexPostFix = "";
				if (round > 0) 
					networkIndexPostFix = "-" + round;
				
				SamplePlacementSimulator simulator = new SamplePlacementSimulator();
				simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, network_size);//get the data center network (cloud network)			
				simulator.InitializeDatasetsAndSamples(true, Parameters.numOfTrials);// scaleFactor = 2; TODO: calculate the range for scaleFactor
				simulator.InitializeQueries(Parameters.numOfTrials, false,  Parameters.numOfDatasetPerQueryMax);
				ApproOneQuery approAlgOneQuery = new ApproOneQuery(simulator, Parameters.numOfTrials);
				approAlgOneQuery.run(Parameters.numOfTrials);
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d;
				double averageUpdateCostT = 0d;
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				double averageErrorT = 0d; 
				double averageDelayT = 0d;
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (approAlgOneQuery.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (approAlgOneQuery.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (approAlgOneQuery.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (approAlgOneQuery.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (approAlgOneQuery.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (approAlgOneQuery.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
					averageDelayT += (approAlgOneQuery.getAverageDelayTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[0][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[0][i] += (averageDelayT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				BenchmarkOneQuery benchmarkOneQueryAlg = new BenchmarkOneQuery(simulator, Parameters.numOfTrials);
				benchmarkOneQueryAlg.run(Parameters.numOfTrials);
				
				averageCostT = 0d;
				averageStorageCostT = 0d;
				averageUpdateCostT = 0d;
				averageAccessCostT = 0d;
				averageProcessCostT = 0d;
				int numOfInvalidTrials = 0;	
				averageErrorT = 0d; 
				averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					if (benchmarkOneQueryAlg.getCostTrials().get(t) == 0d) numOfInvalidTrials ++; 
					averageCostT += (benchmarkOneQueryAlg.getCostTrials().get(t));
					averageStorageCostT += (benchmarkOneQueryAlg.getStorageCostTrials().get(t));
					averageUpdateCostT += (benchmarkOneQueryAlg.getUpdateCostTrials().get(t));
					averageAccessCostT += (benchmarkOneQueryAlg.getAccessCostTrials().get(t));
					averageProcessCostT += (benchmarkOneQueryAlg.getProcessCostTrials().get(t));
					averageErrorT += (benchmarkOneQueryAlg.getAverageErrorTrials().get(t));
					averageDelayT += (benchmarkOneQueryAlg.getAverageDelayTrials().get(t));
				}
				
				averageCostT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageStorageCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageUpdateCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageAccessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageProcessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageErrorT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageDelayT /= (Parameters.numOfTrials - numOfInvalidTrials);
				
				aveCost[1][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[1][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[1][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[1][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[1][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[1][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[1][i] += (averageDelayT / Parameters.roundNum);
				//aveCost[2][i] += (averageCostLowerboundT / Parameters.roundNum);
			}
		}
		
		System.out.println("total costs and lower bound");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("average errors");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("average delay");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveDelay[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
	}
	
	public static void performanceSingleQueryMaxDelayReq() {
		
		int numOfAlgs = 2;// the proposed algorithm and a benchmark algorithm. 
		
		//int [] network_sizes = {20, 30, 40, 50, 100, 150, 200};
		//double [] maxDelayReqs = {550, 600, 650, 700, 750};
		double [] maxDelayReqs = {550, 650, 750, 850, 950};
		double [][] aveCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveStorageCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveUpdateCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveAccessCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveProcessCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveError = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveDelay = new double [numOfAlgs][maxDelayReqs.length];

		for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
			String networkIndexPostFix = "";
			if (round > 0) 
				networkIndexPostFix = "-" + round;
				
			SamplePlacementSimulator simulator = new SamplePlacementSimulator();
			simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, Parameters.numOfDataCenters);//get the data center network (cloud network)			
			simulator.InitializeDatasetsAndSamples(true, Parameters.numOfTrials);// scaleFactor = 2; TODO: calculate the range for scaleFactor
			simulator.InitializeQueries(Parameters.numOfTrials, false, Parameters.numOfDatasetPerQueryMax);
				
			for (int i = 0; i < maxDelayReqs.length; i ++) {
				
				Parameters.queryDelayRequirementMax = maxDelayReqs[i];
				
				for (Entry<Integer, List<Query>> entry : simulator.getQueries().entrySet()) {
					for (Query q : entry.getValue()) {
						q.setDelayRequirement(RanNum.getRandomDoubleRange(Parameters.queryDelayRequirementMax, Parameters.queryDelayRequirementMin));
						//TODO reset query ??????
					}
				}
				
				ApproOneQuery approAlgOneQuery = new ApproOneQuery(simulator, Parameters.numOfTrials);
				approAlgOneQuery.run(Parameters.numOfTrials);
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d;
				double averageUpdateCostT = 0d;
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				double averageErrorT = 0d; 
				double averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (approAlgOneQuery.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (approAlgOneQuery.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (approAlgOneQuery.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (approAlgOneQuery.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (approAlgOneQuery.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (approAlgOneQuery.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
					averageDelayT += (approAlgOneQuery.getAverageDelayTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[0][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[0][i] += (averageDelayT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				BenchmarkOneQuery benchmarkOneQueryAlg = new BenchmarkOneQuery(simulator, Parameters.numOfTrials);
				benchmarkOneQueryAlg.run(Parameters.numOfTrials);
				
				averageCostT = 0d;
				averageStorageCostT = 0d;
				averageUpdateCostT = 0d;
				averageAccessCostT = 0d;
				averageProcessCostT = 0d;
				int numOfInvalidTrials = 0;	
				averageErrorT = 0d; 
				averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					if (benchmarkOneQueryAlg.getCostTrials().get(t) == 0d) numOfInvalidTrials ++; 
					averageCostT += (benchmarkOneQueryAlg.getCostTrials().get(t));
					averageStorageCostT += (benchmarkOneQueryAlg.getStorageCostTrials().get(t));
					averageUpdateCostT += (benchmarkOneQueryAlg.getUpdateCostTrials().get(t));
					averageAccessCostT += (benchmarkOneQueryAlg.getAccessCostTrials().get(t));
					averageProcessCostT += (benchmarkOneQueryAlg.getProcessCostTrials().get(t));
					averageErrorT += (benchmarkOneQueryAlg.getAverageErrorTrials().get(t));
					averageDelayT += (benchmarkOneQueryAlg.getAverageDelayTrials().get(t));
				}
				
				averageCostT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageStorageCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageUpdateCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageAccessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageProcessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageErrorT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageDelayT /= (Parameters.numOfTrials - numOfInvalidTrials);
				
				aveCost[1][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[1][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[1][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[1][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[1][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[1][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[1][i] += (averageDelayT / Parameters.roundNum);
				//aveCost[2][i] += (averageCostLowerboundT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
			}
		}
		
		System.out.println("total costs and lower bound");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("average errors");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("average delay");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveDelay[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
	}
	
	public static void performanceSingleQueryMaxDSNumPerQuery() {
		
		int numOfAlgs = 2;// the proposed algorithm and a benchmark algorithm. 
		
		//int [] network_sizes = {20, 30, 40, 50, 100, 150, 200};
		int [] maxNumOfDSPerQuery = {5, 10, 15, 30, 50};
		double [][] aveCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveStorageCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveUpdateCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveAccessCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveProcessCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveError = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveDelay = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		
		for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
			String networkIndexPostFix = "";
			if (round > 0) 
				networkIndexPostFix = "-" + round;
				
			SamplePlacementSimulator simulator = new SamplePlacementSimulator();
			simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, Parameters.numOfDataCenters);//get the data center network (cloud network)			
			simulator.InitializeDatasetsAndSamples(true, Parameters.numOfTrials);// scaleFactor = 2; TODO: calculate the range for scaleFactor
				
			for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
				
				Parameters.numOfDatasetPerQueryMax = maxNumOfDSPerQuery[i];
				
				simulator.InitializeQueries(Parameters.numOfTrials, false, Parameters.numOfDatasetPerQueryMax);
				
				ApproOneQuery approAlgOneQuery = new ApproOneQuery(simulator, Parameters.numOfTrials);
				approAlgOneQuery.run(Parameters.numOfTrials);
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d;
				double averageUpdateCostT = 0d;
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				double averageErrorT = 0d; 
				double averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (approAlgOneQuery.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (approAlgOneQuery.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (approAlgOneQuery.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (approAlgOneQuery.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (approAlgOneQuery.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (approAlgOneQuery.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
					averageDelayT += (approAlgOneQuery.getAverageDelayTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[0][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[0][i] += (averageDelayT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				BenchmarkOneQuery benchmarkOneQueryAlg = new BenchmarkOneQuery(simulator, Parameters.numOfTrials);
				benchmarkOneQueryAlg.run(Parameters.numOfTrials);
				
				averageCostT = 0d;
				averageStorageCostT = 0d;
				averageUpdateCostT = 0d;
				averageAccessCostT = 0d;
				averageProcessCostT = 0d;
				int numOfInvalidTrials = 0;	
				averageErrorT = 0d; 
				averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					if (benchmarkOneQueryAlg.getCostTrials().get(t) == 0d) numOfInvalidTrials ++; 
					averageCostT += (benchmarkOneQueryAlg.getCostTrials().get(t));
					averageStorageCostT += (benchmarkOneQueryAlg.getStorageCostTrials().get(t));
					averageUpdateCostT += (benchmarkOneQueryAlg.getUpdateCostTrials().get(t));
					averageAccessCostT += (benchmarkOneQueryAlg.getAccessCostTrials().get(t));
					averageProcessCostT += (benchmarkOneQueryAlg.getProcessCostTrials().get(t));
					averageErrorT += (benchmarkOneQueryAlg.getAverageErrorTrials().get(t));
					averageDelayT += (benchmarkOneQueryAlg.getAverageDelayTrials().get(t));
				}
				
				averageCostT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageStorageCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageUpdateCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageAccessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageProcessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageErrorT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageDelayT /= (Parameters.numOfTrials - numOfInvalidTrials);
				
				aveCost[1][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[1][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[1][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[1][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[1][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[1][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[1][i] += (averageDelayT / Parameters.roundNum);
				//aveCost[2][i] += (averageCostLowerboundT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				simulator.setQueries(new HashMap<Integer, List<Query>>());
			}
		}
		
		System.out.println("total costs and lower bound");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("average errors");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("average delay");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveDelay[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
	}
	
	public static void performanceSingleQueryLowestErrorBound() {
		
		int numOfAlgs = 2;// the proposed algorithm and a benchmark algorithm. 
		
		//double [] lowestErrors = {0.05, 0.075, 0.1, 0.125, 0.15};
		//int [] lowestErrorIndex = {Parameters.errorBounds.length - 1, Parameters.errorBounds.length - 3, Parameters.errorBounds.length - 5, Parameters.errorBounds.length - 7}; //, Parameters.errorBounds.length - 5};
		int [] lowestErrorIndex = {0, 2, 4, 6}; //, Parameters.errorBounds.length - 5};
		double [][] aveCost = new double [numOfAlgs][lowestErrorIndex.length];
		double [][] aveStorageCost = new double [numOfAlgs][lowestErrorIndex.length];
		double [][] aveUpdateCost = new double [numOfAlgs][lowestErrorIndex.length];
		double [][] aveAccessCost = new double [numOfAlgs][lowestErrorIndex.length];
		double [][] aveProcessCost = new double [numOfAlgs][lowestErrorIndex.length];
		double [][] aveError = new double [numOfAlgs][lowestErrorIndex.length];
		double [][] aveDelay = new double [numOfAlgs][lowestErrorIndex.length];
		
		for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
			String networkIndexPostFix = "";
			if (round > 0) 
				networkIndexPostFix = "-" + round;
				
			SamplePlacementSimulator simulator = new SamplePlacementSimulator();
			simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, Parameters.numOfDataCenters);//get the data center network (cloud network)			
			simulator.InitializeDatasetsAndSamples(true, Parameters.numOfTrials);// scaleFactor = 2; TODO: calculate the range for scaleFactor
			simulator.InitializeQueries(Parameters.numOfTrials, false, Parameters.numOfDatasetPerQueryMax);

			for (int i = 0; i < lowestErrorIndex.length; i ++) {
				
				int lowestErrorI = lowestErrorIndex[i];
				
				ApproOneQuery approAlgOneQuery = new ApproOneQuery(simulator, Parameters.numOfTrials);
				approAlgOneQuery.setLowestErrorIndex(lowestErrorI);
				approAlgOneQuery.run(Parameters.numOfTrials);
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d;
				double averageUpdateCostT = 0d;
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				double averageErrorT = 0d; 
				double averageDelayT = 0d;
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (approAlgOneQuery.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (approAlgOneQuery.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (approAlgOneQuery.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (approAlgOneQuery.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (approAlgOneQuery.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (approAlgOneQuery.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
					averageDelayT += (approAlgOneQuery.getAverageDelayTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[0][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[0][i] += (averageDelayT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				BenchmarkOneQuery benchmarkOneQueryAlg = new BenchmarkOneQuery(simulator, Parameters.numOfTrials);
				benchmarkOneQueryAlg.setLowestErrorIndex(lowestErrorI);
				benchmarkOneQueryAlg.run(Parameters.numOfTrials);
				
				averageCostT = 0d;
				averageStorageCostT = 0d;
				averageUpdateCostT = 0d;
				averageAccessCostT = 0d;
				averageProcessCostT = 0d;
				int numOfInvalidTrials = 0;	
				averageErrorT = 0d; 
				averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					if (benchmarkOneQueryAlg.getCostTrials().get(t) == 0d) numOfInvalidTrials ++; 
					averageCostT += (benchmarkOneQueryAlg.getCostTrials().get(t));
					averageStorageCostT += (benchmarkOneQueryAlg.getStorageCostTrials().get(t));
					averageUpdateCostT += (benchmarkOneQueryAlg.getUpdateCostTrials().get(t));
					averageAccessCostT += (benchmarkOneQueryAlg.getAccessCostTrials().get(t));
					averageProcessCostT += (benchmarkOneQueryAlg.getProcessCostTrials().get(t));
					averageErrorT += (benchmarkOneQueryAlg.getAverageErrorTrials().get(t));
					averageDelayT += (benchmarkOneQueryAlg.getAverageDelayTrials().get(t));
				}
				
				averageCostT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageStorageCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageUpdateCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageAccessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageProcessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageErrorT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageDelayT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				
				aveCost[1][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[1][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[1][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[1][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[1][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[1][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[1][i] += (averageDelayT / Parameters.roundNum);
				//aveCost[2][i] += (averageCostLowerboundT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				//simulator.setQueries(new HashMap<Integer, List<Query>>());	
			}
		}
		
		System.out.println("total costs and lower bound");
		for (int i = 0; i < lowestErrorIndex.length; i ++) {
			int errorIndex = lowestErrorIndex[i] ;
			double error = Parameters.errorBounds[errorIndex];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++) {
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + error + " " + out);
		}
		
		System.out.println("average errors");
		for (int i = 0; i < lowestErrorIndex.length; i ++) {
			int errorIndex = lowestErrorIndex[i] ;
			double error = Parameters.errorBounds[errorIndex];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + error + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < lowestErrorIndex.length; i ++) {
			int errorIndex = lowestErrorIndex[i] ;
			double error = Parameters.errorBounds[errorIndex];
			
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + error + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < lowestErrorIndex.length; i ++) {
			int errorIndex = lowestErrorIndex[i] ;
			double error = Parameters.errorBounds[errorIndex];
			
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + error + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < lowestErrorIndex.length; i ++) {
			int errorIndex = lowestErrorIndex[i] ;
			double error = Parameters.errorBounds[errorIndex];
			
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + error + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < lowestErrorIndex.length; i ++) {
			int errorIndex = lowestErrorIndex[i] ;
			double error = Parameters.errorBounds[errorIndex];
			
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + error + " " + out);
		}
		
		System.out.println("average delay");
		for (int i = 0; i < lowestErrorIndex.length; i ++) {
			int errorIndex = lowestErrorIndex[i] ;
			double error = Parameters.errorBounds[errorIndex];
			
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++) {
				out += aveDelay[j][i] + " ";
			}
			
			System.out.println("" + error + " " + out);
		}
	}
	
	
	public static void performanceOnlineBatch() {
		
		int numOfAlgs = 2;// the proposed algorithm and a benchmark algorithm. 
		
		//int [] network_sizes = {20, 30, 40, 50, 100, 150, 200};
		int [] network_sizes = {20};
		double [][][] aveAccumulatedCost = new double [numOfAlgs][network_sizes.length][Parameters.max_timeslots];
		double [][][] aveAccumulatedStorageCost = new double [numOfAlgs][network_sizes.length][Parameters.max_timeslots];
		double [][][] aveAccumulatedUpdateCost = new double [numOfAlgs][network_sizes.length][Parameters.max_timeslots];
		double [][][] aveAccumulatedAccessCost = new double [numOfAlgs][network_sizes.length][Parameters.max_timeslots];
		double [][][] aveAccumulatedProcessCost = new double [numOfAlgs][network_sizes.length][Parameters.max_timeslots];
		double [][] aveError = new double [numOfAlgs][network_sizes.length];
		
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
				
				String networkIndexPostFix = "";
				if (round > 0) 
					networkIndexPostFix = "-" + round;
				
				SamplePlacementSimulator simulator = new SamplePlacementSimulator();
				simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, network_size);//get the data center network (cloud network)			
				simulator.InitializeDatasetsAndSamples(true, Parameters.numOfTrials);// scaleFactor = 2; TODO: calculate the range for scaleFactor
				simulator.InitializeQueries(Parameters.numOfTrials, Parameters.max_timeslots);
				OnlineBatch onlineBatchAlg = new OnlineBatch(simulator, Parameters.numOfTrials);
				onlineBatchAlg.run(Parameters.numOfTrials, 5);// length of a period is 5. 
				
				for (int timeslot = 0; timeslot < Parameters.max_timeslots; timeslot ++) {
					
					double averageCostT = 0d;
					double averageStorageCostT = 0d;
					double averageUpdateCostT = 0d;
					double averageAccessCostT = 0d;
					double averageProcessCostT = 0d;
					double averageErrorT = 0d; 
					for (int trial = 0; trial < Parameters.numOfTrials; trial++) {
						averageCostT += (onlineBatchAlg.getCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageStorageCostT += (onlineBatchAlg.getStorageCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageUpdateCostT += (onlineBatchAlg.getUpdateCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageAccessCostT += (onlineBatchAlg.getAccessCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageProcessCostT += (onlineBatchAlg.getProcessCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						if (timeslot == Parameters.max_timeslots - 1)
							averageErrorT += (onlineBatchAlg.getAverageErrorTrials().get(trial) / Parameters.numOfTrials);
					}
					
					aveAccumulatedCost[0][i][timeslot] += (averageCostT / Parameters.roundNum);
					aveAccumulatedStorageCost[0][i][timeslot] += (averageStorageCostT / Parameters.roundNum);
					aveAccumulatedUpdateCost[0][i][timeslot] += (averageUpdateCostT / Parameters.roundNum);
					aveAccumulatedAccessCost[0][i][timeslot] += (averageAccessCostT / Parameters.roundNum);
					aveAccumulatedProcessCost[0][i][timeslot] += (averageProcessCostT / Parameters.roundNum);
					if (timeslot == Parameters.max_timeslots - 1)
						aveError[0][i] = averageErrorT;
				}
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				BenchmarkOnlineBatch benchmarkBatchAlg = new BenchmarkOnlineBatch(simulator, Parameters.numOfTrials);
				benchmarkBatchAlg.run(Parameters.numOfTrials);
				
				for (int timeslot = 0; timeslot < Parameters.max_timeslots; timeslot ++) {
					
					double averageCostT = 0d;
					double averageStorageCostT = 0d;
					double averageUpdateCostT = 0d;
					double averageAccessCostT = 0d;
					double averageProcessCostT = 0d;
					double averageErrorT = 0d;
					
					for (int trial = 0; trial < Parameters.numOfTrials; trial++) {
						averageCostT += (benchmarkBatchAlg.getCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageStorageCostT += (benchmarkBatchAlg.getStorageCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageUpdateCostT += (benchmarkBatchAlg.getUpdateCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageAccessCostT += (benchmarkBatchAlg.getAccessCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageProcessCostT += (benchmarkBatchAlg.getProcessCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						if (timeslot == Parameters.max_timeslots - 1)
							averageErrorT += (benchmarkBatchAlg.getAverageErrorTrials().get(trial) / Parameters.numOfTrials);
					}
					
					aveAccumulatedCost[1][i][timeslot] += (averageCostT / Parameters.roundNum);
					aveAccumulatedStorageCost[1][i][timeslot] += (averageStorageCostT / Parameters.roundNum);
					aveAccumulatedUpdateCost[1][i][timeslot] += (averageUpdateCostT / Parameters.roundNum);
					aveAccumulatedAccessCost[1][i][timeslot] += (averageAccessCostT / Parameters.roundNum);
					aveAccumulatedProcessCost[1][i][timeslot] += (averageProcessCostT / Parameters.roundNum);
					
					if (timeslot == Parameters.max_timeslots - 1)
						aveError[1][i] = averageErrorT;
					
				}
			}
		}
		
		int skipPointsNum = 5; 
		
		System.out.println("total costs and lower bound");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.max_timeslots; timeslot++) {
				
				if (0 != timeslot % skipPointsNum)
					continue; 
				
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.max_timeslots; timeslot++) {
				
				if (0 != timeslot % skipPointsNum)
					continue; 
				
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedStorageCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		}
		
		System.out.println("update costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.max_timeslots; timeslot++) {
				if (0 != timeslot % skipPointsNum)
					continue; 
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedUpdateCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		}
		
		System.out.println("access costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.max_timeslots; timeslot++) {
				
				if (0 != timeslot % skipPointsNum)
					continue; 
				
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedAccessCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		}
		
		System.out.println("process costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.max_timeslots; timeslot++) {
				
				if (0 != timeslot % skipPointsNum)
					continue; 
				
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedProcessCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		}
		
		System.out.println("average errors");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
	}
	
	public static void performanceOnlineOBO() {
		
		int numOfAlgs = 2;// the proposed algorithm and a benchmark algorithm. 
		
		//int [] network_sizes = {20, 30, 40, 50, 100, 150, 200};
		int [] network_sizes = {20};
		
		Parameters.maxNumOfQueriesPerTS = 1000;// double check here. 
		
		double [][][] aveAccumulatedCost = new double [numOfAlgs][network_sizes.length][Parameters.maxNumOfQueriesPerTS];
		double [][][] aveAccumulatedStorageCost = new double [numOfAlgs][network_sizes.length][Parameters.maxNumOfQueriesPerTS];
		double [][][] aveAccumulatedUpdateCost = new double [numOfAlgs][network_sizes.length][Parameters.maxNumOfQueriesPerTS];
		double [][][] aveAccumulatedAccessCost = new double [numOfAlgs][network_sizes.length][Parameters.maxNumOfQueriesPerTS];
		double [][][] aveAccumulatedProcessCost = new double [numOfAlgs][network_sizes.length][Parameters.maxNumOfQueriesPerTS];
		double [][] aveError = new double [numOfAlgs][network_sizes.length];
		
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			
			for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
				String networkIndexPostFix = "";
				if (round > 0) 
					networkIndexPostFix = "-" + round;
				
				SamplePlacementSimulator simulator = new SamplePlacementSimulator();
				simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, network_size);//get the data center network (cloud network)			
				simulator.InitializeDatasetsAndSamples(true, Parameters.numOfTrials);// scaleFactor = 2; TODO: calculate the range for scaleFactor
				simulator.InitializeQueries(Parameters.numOfTrials, true, Parameters.numOfDatasetPerQueryMax);
				OnlineOBO onlineOBOAlg = new OnlineOBO(simulator, Parameters.numOfTrials);
				onlineOBOAlg.run(Parameters.numOfTrials, 5);// length of a period is 5. 
				
				for (int timeslot = 0; timeslot < Parameters.maxNumOfQueriesPerTS; timeslot ++) {
					
					double averageCostT = 0d;
					double averageStorageCostT = 0d;
					double averageUpdateCostT = 0d;
					double averageAccessCostT = 0d;
					double averageProcessCostT = 0d;
					double averageErrorT = 0d; 
					for (int trial = 0; trial < Parameters.numOfTrials; trial++) {
						averageCostT += (onlineOBOAlg.getCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageStorageCostT += (onlineOBOAlg.getStorageCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageUpdateCostT += (onlineOBOAlg.getUpdateCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageAccessCostT += (onlineOBOAlg.getAccessCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageProcessCostT += (onlineOBOAlg.getProcessCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						if (timeslot == Parameters.maxNumOfQueriesPerTS - 1)
							averageErrorT += (onlineOBOAlg.getAverageErrorTrials().get(trial) / Parameters.numOfTrials);
					}
					
					aveAccumulatedCost[0][i][timeslot] += (averageCostT / Parameters.roundNum);
					aveAccumulatedStorageCost[0][i][timeslot] += (averageStorageCostT / Parameters.roundNum);
					aveAccumulatedUpdateCost[0][i][timeslot] += (averageUpdateCostT / Parameters.roundNum);
					aveAccumulatedAccessCost[0][i][timeslot] += (averageAccessCostT / Parameters.roundNum);
					aveAccumulatedProcessCost[0][i][timeslot] += (averageProcessCostT / Parameters.roundNum);
					if (timeslot == Parameters.maxNumOfQueriesPerTS - 1)
						aveError[0][i] += (averageErrorT / Parameters.roundNum);
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
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				BenchmarkOnlineOBO benchmarkOBOAlg = new BenchmarkOnlineOBO(simulator, Parameters.numOfTrials);
				benchmarkOBOAlg.run(Parameters.numOfTrials);
				
				for (int timeslot = 0; timeslot < Parameters.maxNumOfQueriesPerTS; timeslot ++) {
					double averageCostT = 0d;
					double averageStorageCostT = 0d;
					double averageUpdateCostT = 0d;
					double averageAccessCostT = 0d;
					double averageProcessCostT = 0d;
					double averageErrorT = 0d; 
					for (int trial = 0; trial < Parameters.numOfTrials; trial++) {
						averageCostT += (benchmarkOBOAlg.getCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageStorageCostT += (benchmarkOBOAlg.getStorageCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageUpdateCostT += (benchmarkOBOAlg.getUpdateCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageAccessCostT += (benchmarkOBOAlg.getAccessCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						averageProcessCostT += (benchmarkOBOAlg.getProcessCostTrialsAccumulated().get(timeslot).get(trial) / Parameters.numOfTrials);
						if (timeslot == Parameters.maxNumOfQueriesPerTS - 1)
							averageErrorT += (benchmarkOBOAlg.getAverageErrorTrials().get(trial) / Parameters.numOfTrials);
					}
					
					aveAccumulatedCost[1][i][timeslot] += (averageCostT / Parameters.roundNum);
					aveAccumulatedStorageCost[1][i][timeslot] += (averageStorageCostT / Parameters.roundNum);
					aveAccumulatedUpdateCost[1][i][timeslot] += (averageUpdateCostT / Parameters.roundNum);
					aveAccumulatedAccessCost[1][i][timeslot] += (averageAccessCostT / Parameters.roundNum);
					aveAccumulatedProcessCost[1][i][timeslot] += (averageProcessCostT / Parameters.roundNum);
					if (timeslot == Parameters.maxNumOfQueriesPerTS - 1)
						aveError[1][i] += (averageErrorT / Parameters.roundNum);
				}
			}
		}
		
		
		int skipPointsNum = 100; 
		System.out.println("total costs and lower bound");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.maxNumOfQueriesPerTS; timeslot++) {
				
				if (0 != timeslot % skipPointsNum)
					continue; 
				
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		} 
		
		System.out.println("storage costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.maxNumOfQueriesPerTS; timeslot++) {
				
				if (0 != timeslot % skipPointsNum)
					continue; 
				
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedStorageCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		}
		
		System.out.println("update costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.maxNumOfQueriesPerTS; timeslot++) {
				
				if (0 != timeslot % skipPointsNum)
					continue; 
				
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedUpdateCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		}
		
		System.out.println("access costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.maxNumOfQueriesPerTS; timeslot++) {
				
				if (0 != timeslot % skipPointsNum)
					continue; 
				
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedAccessCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		}
		
		System.out.println("process costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			
			int network_size = network_sizes[i];
			
			System.out.println("" + network_size + ":");	
			
			for (int timeslot = 0; timeslot < Parameters.maxNumOfQueriesPerTS; timeslot++) {
				
				if (0 != timeslot % skipPointsNum)
					continue; 
				
				String out = "" + timeslot + " ";
				for (int j = 0; j < numOfAlgs; j ++) {
					out += aveAccumulatedProcessCost[j][i][timeslot] + " "; 
				}
				System.out.println(out);
			}
		}
		
		System.out.println("average errors");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
	}
	
	
	public static void performanceHeuAppro() {
		
		int numOfAlgs = 2; 
		//int [] network_sizes = {20, 30, 40, 50, 100, 150, 200}; 
		int [] network_sizes = {50};
		double [][] aveCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveStorageCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveUpdateCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveAccessCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveProcessCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveError = new double [numOfAlgs][network_sizes.length];

		
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			
			for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
				String networkIndexPostFix = "";
				if (round > 0) 
					networkIndexPostFix = "-" + round;
				
				SamplePlacementSimulator simulator = new SamplePlacementSimulator();
				simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, network_size);//get the data center network (cloud network)			
				simulator.InitializeDatasetsAndSamples(false, Parameters.numOfTrials);
				simulator.InitializeQueries(Parameters.numOfTrials, false, Parameters.numOfDatasetPerQueryMax);
				ProposedHeuristicAlg heuAlg = new ProposedHeuristicAlg(simulator);
				heuAlg.run(null, Double.MAX_VALUE, -1, Parameters.numOfTrials);
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d;
				double averageUpdateCostT = 0d;
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				double averageErrorT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (heuAlg.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (heuAlg.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (heuAlg.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (heuAlg.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (heuAlg.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (heuAlg.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[0][i] += (averageErrorT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				ProposedApproximationAlg approAlg = new ProposedApproximationAlg(simulator);
				approAlg.run();
				
				averageCostT = 0d;
				averageStorageCostT = 0d;
				averageUpdateCostT = 0d;
				averageAccessCostT = 0d;
				averageProcessCostT = 0d;
				int numOfInvalidTrials = 0;	
				averageErrorT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					if (approAlg.getCostTrials().get(t) == 0d) numOfInvalidTrials ++; 
					averageCostT += (approAlg.getCostTrials().get(t));
					averageStorageCostT += (approAlg.getStorageCostTrials().get(t));
					averageUpdateCostT += (approAlg.getUpdateCostTrials().get(t));
					averageAccessCostT += (approAlg.getAccessCostTrials().get(t));
					averageProcessCostT += (approAlg.getProcessCostTrials().get(t));
					averageErrorT += (approAlg.getAverageErrorTrials().get(t));
				}
				
				averageCostT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageStorageCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageUpdateCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageAccessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageProcessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageErrorT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				
				aveCost[1][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[1][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[1][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[1][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[1][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[1][i] += (averageErrorT / Parameters.roundNum);
				//aveCost[2][i] += (averageCostLowerboundT / Parameters.roundNum);
			}
		}
		
		System.out.println("total costs and lower bound");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("average errors");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
	}

	
	
	
	public static void performanceMultiQueries(boolean singleNetSize) {
		
		int numOfAlgs = 2; 
		//int [] network_sizes = {20, 30, 40, 50, 100};
		int [] network_sizes = {20, 50, 100, 150, 200};
		//int [] network_sizes = {20, 30};
		if (singleNetSize) {
			network_sizes = new int [1];
			network_sizes[0] = Parameters.numOfDataCenters;
		}
		
		double [][] aveCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveStorageCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveUpdateCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveAccessCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveProcessCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveError = new double [numOfAlgs][network_sizes.length];
		double [][] aveDelay = new double [numOfAlgs][network_sizes.length];
		
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			
			for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
				String networkIndexPostFix = "";
				if (round > 0) 
					networkIndexPostFix = "-" + round;
				
				SamplePlacementSimulator simulator = new SamplePlacementSimulator();
				simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, network_size);//get the data center network (cloud network)			
				simulator.InitializeDatasetsAndSamples(false, Parameters.numOfTrials);
				simulator.InitializeQueries(Parameters.numOfTrials, false, Parameters.numOfDatasetPerQueryMax);
				ProposedHeuristicAlg heuAlg = new ProposedHeuristicAlg(simulator);
				heuAlg.run(null, Double.MAX_VALUE, -1, Parameters.numOfTrials);
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d; 
				double averageUpdateCostT = 0d; 
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				double averageErrorT = 0d; 
				double averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (heuAlg.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (heuAlg.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (heuAlg.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (heuAlg.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (heuAlg.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (heuAlg.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
					averageDelayT += (heuAlg.getAverageDelayTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[0][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[0][i] += (averageDelayT / Parameters.roundNum);
				
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				Benchmark1 benchmarkAlg = new Benchmark1(simulator, Parameters.numOfTrials);
				benchmarkAlg.run(null);
				
				averageCostT = 0d;
				averageStorageCostT = 0d;
				averageUpdateCostT = 0d;
				averageAccessCostT = 0d;
				averageProcessCostT = 0d;
				int numOfInvalidTrials = 0;	
				averageErrorT = 0d; 
				averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					if (benchmarkAlg.getCostTrials().get(t) == 0d) numOfInvalidTrials ++; 
					averageCostT += (benchmarkAlg.getCostTrials().get(t));
					averageStorageCostT += (benchmarkAlg.getStorageCostTrials().get(t));
					averageUpdateCostT += (benchmarkAlg.getUpdateCostTrials().get(t));
					averageAccessCostT += (benchmarkAlg.getAccessCostTrials().get(t));
					averageProcessCostT += (benchmarkAlg.getProcessCostTrials().get(t));
					averageErrorT += (benchmarkAlg.getAverageErrorTrials().get(t));
					averageDelayT += (benchmarkAlg.getAverageDelayTrials().get(t));
				}
				
				averageCostT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageStorageCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageUpdateCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageAccessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageProcessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageErrorT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageDelayT /= (Parameters.numOfTrials - numOfInvalidTrials);
				
				aveCost[1][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[1][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[1][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[1][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[1][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[1][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[1][i] += (averageDelayT / Parameters.roundNum);
				//aveCost[2][i] += (averageCostLowerboundT / Parameters.roundNum);
				
			}
		}
		
		System.out.println("total costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("average error");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("average delay");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveDelay[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
	}
	
	public static void performanceMultiQueriesMaxDelayReq() {
		
		int numOfAlgs = 2;// the proposed algorithm and a benchmark algorithm. 
		
		//int [] network_sizes = {20, 30, 40, 50, 100, 150, 200};
		double [] maxDelayReqs = {550, 650, 750, 850, 950};
		double [][] aveCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveStorageCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveUpdateCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveAccessCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveProcessCost = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveError = new double [numOfAlgs][maxDelayReqs.length];
		double [][] aveDelay = new double [numOfAlgs][maxDelayReqs.length];
		
		Parameters.numOfTrials = 3; 
		for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
			String networkIndexPostFix = "";
			if (round > 0) 
				networkIndexPostFix = "-" + round;
				
			SamplePlacementSimulator simulator = new SamplePlacementSimulator();
			simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, Parameters.numOfDataCenters);//get the data center network (cloud network)			
			simulator.InitializeDatasetsAndSamples(false, Parameters.numOfTrials);// scaleFactor = 2; TODO: calculate the range for scaleFactor
			simulator.InitializeQueries(Parameters.numOfTrials, false, Parameters.numOfDatasetPerQueryMax);
				
			for (int i = 0; i < maxDelayReqs.length; i ++) {
				
				Parameters.queryDelayRequirementMax = maxDelayReqs[i];
				
				for (Entry<Integer, List<Query>> entry : simulator.getQueries().entrySet()) {
					for (Query q : entry.getValue()) {
						q.setDelayRequirement(RanNum.getRandomDoubleRange(Parameters.queryDelayRequirementMax, Parameters.queryDelayRequirementMin));
						//TODO reset query ??????
					}
				}
				
				ProposedHeuristicAlg heuAlg = new ProposedHeuristicAlg(simulator);
				heuAlg.run(null, Double.MAX_VALUE, -1, Parameters.numOfTrials);
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d;
				double averageUpdateCostT = 0d;
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				double averageErrorT = 0d; 
				double averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (heuAlg.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (heuAlg.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (heuAlg.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (heuAlg.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (heuAlg.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (heuAlg.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
					averageDelayT += (heuAlg.getAverageDelayTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[0][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[0][i] += (averageDelayT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				Benchmark1 benchmarkAlg = new Benchmark1(simulator, Parameters.numOfTrials);
				benchmarkAlg.run(null);
				
				averageCostT = 0d;
				averageStorageCostT = 0d;
				averageUpdateCostT = 0d;
				averageAccessCostT = 0d;
				averageProcessCostT = 0d;
				int numOfInvalidTrials = 0;	
				averageErrorT = 0d; 
				averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					if (benchmarkAlg.getCostTrials().get(t) == 0d) numOfInvalidTrials ++; 
					averageCostT += (benchmarkAlg.getCostTrials().get(t));
					averageStorageCostT += (benchmarkAlg.getStorageCostTrials().get(t));
					averageUpdateCostT += (benchmarkAlg.getUpdateCostTrials().get(t));
					averageAccessCostT += (benchmarkAlg.getAccessCostTrials().get(t));
					averageProcessCostT += (benchmarkAlg.getProcessCostTrials().get(t));
					averageErrorT += (benchmarkAlg.getAverageErrorTrials().get(t));
					averageDelayT += (benchmarkAlg.getAverageDelayTrials().get(t));
				}
				
				averageCostT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageStorageCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageUpdateCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageAccessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageProcessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageErrorT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageDelayT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				
				aveCost[1][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[1][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[1][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[1][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[1][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[1][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[1][i] += (averageErrorT / Parameters.roundNum);
				//aveCost[2][i] += (averageCostLowerboundT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
			}
		}
		
		System.out.println("total costs and lower bound");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("average errors");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
		
		System.out.println("average delay");
		for (int i = 0; i < maxDelayReqs.length; i ++) {
			double maxDelayReq  = maxDelayReqs[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveDelay[j][i] + " ";
			}
			System.out.println("" + maxDelayReq + " " + out);
		}
	}
	
	
	public static void performanceMultiQueriesMaxDSNumPerQuery() {
		
		int numOfAlgs = 2;// the proposed algorithm and a benchmark algorithm. 
		
		//int [] network_sizes = {20, 30, 40, 50, 100, 150, 200};
		int [] maxNumOfDSPerQuery = {5, 10, 15, 30, 50};
		double [][] aveCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveStorageCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveUpdateCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveAccessCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveProcessCost = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveError = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		double [][] aveDelay = new double [numOfAlgs][maxNumOfDSPerQuery.length];
		
		Parameters.numOfTrials = 3; 
		
		for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
			String networkIndexPostFix = "";
			if (round > 0) 
				networkIndexPostFix = "-" + round;
				
			SamplePlacementSimulator simulator = new SamplePlacementSimulator();
			simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, Parameters.numOfDataCenters);//get the data center network (cloud network)			
			simulator.InitializeDatasetsAndSamples(false, Parameters.numOfTrials);// scaleFactor = 2; TODO: calculate the range for scaleFactor
				
			for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
				
				Parameters.numOfDatasetPerQueryMax = maxNumOfDSPerQuery[i];
				
				simulator.InitializeQueries(Parameters.numOfTrials, false, Parameters.numOfDatasetPerQueryMax);
				
				ProposedHeuristicAlg heuAlg = new ProposedHeuristicAlg(simulator);
				heuAlg.run(null, Double.MAX_VALUE, -1, Parameters.numOfTrials);
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d;
				double averageUpdateCostT = 0d;
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				double averageErrorT = 0d; 
				double averageDelayT = 0d;
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (heuAlg.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (heuAlg.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (heuAlg.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (heuAlg.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (heuAlg.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (heuAlg.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
					averageDelayT += (heuAlg.getAverageDelayTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[0][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[0][i] += (averageDelayT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				Benchmark1 benchmarkAlg = new Benchmark1(simulator, Parameters.numOfTrials);
				benchmarkAlg.run(null);
				
				averageCostT = 0d;
				averageStorageCostT = 0d;
				averageUpdateCostT = 0d;
				averageAccessCostT = 0d;
				averageProcessCostT = 0d;
				int numOfInvalidTrials = 0;	
				averageErrorT = 0d; 
				averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					if (benchmarkAlg.getCostTrials().get(t) == 0d) numOfInvalidTrials ++; 
					averageCostT += (benchmarkAlg.getCostTrials().get(t));
					averageStorageCostT += (benchmarkAlg.getStorageCostTrials().get(t));
					averageUpdateCostT += (benchmarkAlg.getUpdateCostTrials().get(t));
					averageAccessCostT += (benchmarkAlg.getAccessCostTrials().get(t));
					averageProcessCostT += (benchmarkAlg.getProcessCostTrials().get(t));
					averageErrorT += (benchmarkAlg.getAverageErrorTrials().get(t));
					averageDelayT += (benchmarkAlg.getAverageDelayTrials().get(t));
				}
				
				averageCostT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageStorageCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageUpdateCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageAccessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageProcessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageErrorT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageDelayT /= (Parameters.numOfTrials - numOfInvalidTrials);
				
				aveCost[1][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[1][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[1][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[1][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[1][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[1][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[1][i] += (averageDelayT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
			}
		}
		
		System.out.println("total costs and lower bound");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("average errors");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
		
		System.out.println("average delay");
		for (int i = 0; i < maxNumOfDSPerQuery.length; i ++) {
			double maxNumDSQuery  = maxNumOfDSPerQuery[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveDelay[j][i] + " ";
			}
			System.out.println("" + maxNumDSQuery + " " + out);
		}
	}
	
	public static void performanceMultiQueriesLowestErrorBound() {
		int numOfAlgs = 2; 
		//int [] network_sizes = {20, 30, 40, 50, 100, 150, 200}; 
		double [] lowestErrors = {0.05, 0.075, 0.1, 0.125, 0.15};
		
		double [][] aveCost = new double [numOfAlgs][lowestErrors.length];
		double [][] aveStorageCost = new double [numOfAlgs][lowestErrors.length];
		double [][] aveUpdateCost = new double [numOfAlgs][lowestErrors.length];
		double [][] aveAccessCost = new double [numOfAlgs][lowestErrors.length];
		double [][] aveProcessCost = new double [numOfAlgs][lowestErrors.length];
		double [][] aveError = new double [numOfAlgs][lowestErrors.length];
		double [][] aveDelay = new double [numOfAlgs][lowestErrors.length];
		
		for (int i = 0; i < lowestErrors.length; i ++) {
			int network_size = 50;
			
			double lowestError = lowestErrors[i];
			
			for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
				String networkIndexPostFix = "";
				if (round > 0) 
					networkIndexPostFix = "-" + round;
				
				SamplePlacementSimulator simulator = new SamplePlacementSimulator();
				simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, network_size);//get the data center network (cloud network)			
				simulator.InitializeDatasetsAndSamples(false, Parameters.numOfTrials);
				simulator.InitializeQueries(Parameters.numOfTrials, false, Parameters.numOfDatasetPerQueryMax);
				ProposedHeuristicAlg heuAlg = new ProposedHeuristicAlg(simulator);
				heuAlg.setLowestError(lowestError);
				heuAlg.run(null, Double.MAX_VALUE, -1, Parameters.numOfTrials);
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d;
				double averageUpdateCostT = 0d;
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				double averageErrorT = 0d; 
				double averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (heuAlg.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (heuAlg.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (heuAlg.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (heuAlg.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (heuAlg.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (heuAlg.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
					averageDelayT += (heuAlg.getAverageDelayTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[0][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[0][i] += (averageDelayT / Parameters.roundNum);
				
				for (Node node : simulator.getDatacenterNetwork().vertexSet()) {
					if (node instanceof DataCenter) {
						((DataCenter) node).reset();
					}
				}
				for (InternetLink il : simulator.getDatacenterNetwork().edgeSet()) {
					il.clear();
				}
				
				for (int t = 0; t < Parameters.numOfTrials; t ++){
					for (Dataset ds : simulator.getDatasets().get(t)){
						ds.reset();
					}
				}
				
				Benchmark1 benchmarkAlg = new Benchmark1(simulator, Parameters.numOfTrials);
				benchmarkAlg.setLowestError(lowestError);
				benchmarkAlg.run(null);
				
				averageCostT = 0d;
				averageStorageCostT = 0d;
				averageUpdateCostT = 0d;
				averageAccessCostT = 0d;
				averageProcessCostT = 0d;
				averageErrorT = 0d; 
				averageDelayT = 0d; 
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					averageCostT += (benchmarkAlg.getCostTrials().get(t) / Parameters.numOfTrials);
					averageStorageCostT += (benchmarkAlg.getStorageCostTrials().get(t) / Parameters.numOfTrials);
					averageUpdateCostT += (benchmarkAlg.getUpdateCostTrials().get(t) / Parameters.numOfTrials);
					averageAccessCostT += (benchmarkAlg.getAccessCostTrials().get(t) / Parameters.numOfTrials);
					averageProcessCostT += (benchmarkAlg.getProcessCostTrials().get(t) / Parameters.numOfTrials);
					averageErrorT += (benchmarkAlg.getAverageErrorTrials().get(t) / Parameters.numOfTrials);
					averageDelayT += (benchmarkAlg.getAverageDelayTrials().get(t) / Parameters.numOfTrials);
				}
				
				aveCost[1][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[1][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[1][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[1][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[1][i] += (averageProcessCostT / Parameters.roundNum);
				aveError[1][i] += (averageErrorT / Parameters.roundNum);
				aveDelay[1][i] += (averageDelayT / Parameters.roundNum);
				//aveCost[2][i] += (averageCostLowerboundT / Parameters.roundNum);
			}
		}
		
		System.out.println("total costs and lower bound");
		for (int i = 0; i < lowestErrors.length; i ++) {
			double lowestError = lowestErrors[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + lowestError + " " + out);
		}
		
		System.out.println("average errors");
		for (int i = 0; i < lowestErrors.length; i ++) {
			double lowestError = lowestErrors[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveError[j][i] + " ";
			}
			System.out.println("" + lowestError + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < lowestErrors.length; i ++) {
			double lowestError = lowestErrors[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + lowestError + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < lowestErrors.length; i ++) {
			double lowestError = lowestErrors[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + lowestError + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < lowestErrors.length; i ++) {
			double lowestError = lowestErrors[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + lowestError + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < lowestErrors.length; i ++) {
			double network_size = lowestErrors[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("average delays");
		for (int i = 0; i < lowestErrors.length; i ++) {
			double network_size = lowestErrors[i];
			String out = "";
			for (int j = 0; j < numOfAlgs ; j ++){
				out += aveDelay[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
	}
	
	public static void performanceApproximation() {
		int numOfAlgs = 1; 
		int [] network_sizes = {20, 30}; // 100, 150, 200
		double [][] aveCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveStorageCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveUpdateCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveAccessCost = new double [numOfAlgs][network_sizes.length];
		double [][] aveProcessCost = new double [numOfAlgs][network_sizes.length];
		
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			
			for(int round = 0; round < Parameters.roundNum; round ++) {// different network toplolgies.
				String networkIndexPostFix = "";
				if (round > 0) 
					networkIndexPostFix = "-" + round;
			
				SamplePlacementSimulator simulator = new SamplePlacementSimulator();
				simulator.InitializeDataCenterNetwork(simulator.getDatacenterNetwork(), networkIndexPostFix, network_size);//get the data center network (cloud network)			
				simulator.InitializeDatasetsAndSamples(false, Parameters.numOfTrials);
				simulator.InitializeQueries(Parameters.numOfTrials, false, Parameters.numOfDatasetPerQueryMax);
				ProposedApproximationAlg approAlg = new ProposedApproximationAlg(simulator);
				approAlg.run();
				
				double averageCostT = 0d;
				double averageStorageCostT = 0d;
				double averageUpdateCostT = 0d;
				double averageAccessCostT = 0d;
				double averageProcessCostT = 0d;
				int numOfInvalidTrials = 0;				
				for (int t = 0; t < Parameters.numOfTrials; t++) {
					if (approAlg.getCostTrials().get(t) == 0d) numOfInvalidTrials ++; 
					averageCostT += (approAlg.getCostTrials().get(t));
					averageStorageCostT += (approAlg.getStorageCostTrials().get(t));
					averageUpdateCostT += (approAlg.getUpdateCostTrials().get(t));
					averageAccessCostT += (approAlg.getAccessCostTrials().get(t));
					averageProcessCostT += (approAlg.getProcessCostTrials().get(t));
				}
				
				averageCostT /= (Parameters.numOfTrials - numOfInvalidTrials);
				averageStorageCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageUpdateCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageAccessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				averageProcessCostT /= (Parameters.numOfTrials - numOfInvalidTrials); 
				
				aveCost[0][i] += (averageCostT / Parameters.roundNum);
				aveStorageCost[0][i] += (averageStorageCostT / Parameters.roundNum);
				aveUpdateCost[0][i] += (averageUpdateCostT / Parameters.roundNum);
				aveAccessCost[0][i] += (averageAccessCostT / Parameters.roundNum);
				aveProcessCost[0][i] += (averageProcessCostT / Parameters.roundNum);
			}
		}
		
		System.out.println("total costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("storage costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveStorageCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("update costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveUpdateCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("access costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveAccessCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
		
		System.out.println("process costs");
		for (int i = 0; i < network_sizes.length; i ++) {
			int network_size = network_sizes[i];
			String out = "";
			for (int j = 0; j < numOfAlgs; j ++){
				out += aveProcessCost[j][i] + " ";
			}
			System.out.println("" + network_size + " " + out);
		}
	}

	/************************************
	 * Initialization functions
	 * 
	 * @return
	 ************************************/
	
	public SimpleWeightedGraph<Node, InternetLink> InitializeDataCenterNetwork(
			SimpleWeightedGraph<Node, InternetLink> dcNet, String networkIndexPostfix, int numOfDCs) {

		if (null == dcNet) {

			ConnectivityInspector<Node, InternetLink> connect = null;
			do {
				// Initialize the data center network
				datacenterNetwork = new SimpleWeightedGraph<Node, InternetLink>(InternetLink.class);
				NetworkGenerator<Node, InternetLink> networkGenerator = new NetworkGenerator<Node, InternetLink>();

				networkGenerator.setSize(numOfDCs);
				networkGenerator.setGenerateType(0);// generate data center
													// networks
				networkGenerator.setNetworkIndexPostFix(networkIndexPostfix);
				networkGenerator.generateGraph(datacenterNetwork, null, null);
				// displayGraph(substrateNetwork);
				connect = new ConnectivityInspector<Node, InternetLink>(datacenterNetwork);
			} while (!connect.isGraphConnected());

			List<DataCenter> dcs = this.getDataCenters();

			for (DataCenter dc1 : dcs) {
				for (DataCenter dc2 : dcs) {
					InternetLink il = datacenterNetwork.getEdge(dc1, dc2);
					if (null != il) {
						il.setEdgeSource(dc1);
						il.setEdgeTarget(dc2);
					}
				}
			}
		} else {// clear some parameters of dcNet.
			// Initialize parameters of sNet.
			// modify.
			for (Node node : dcNet.vertexSet()) {
				if (node instanceof DataCenter) {
					DataCenter dc = (DataCenter) node;
					dc.clearAdmittedSamples();
				} // TODO double check whether no need for front end servers.
			}
			for (InternetLink il : dcNet.edgeSet()) {
				il.clearUsers();
			}

			this.datacenterNetwork = dcNet;
		}
		return datacenterNetwork;
	}
	
	public Map<Integer, List<Dataset>> InitializeDatasetsAndSamples(
			boolean specialCase, // specialCase: with a minimal size of samples, which is set for the approximation algorithm with a single query.
			int numOfTrials//, 
			//int scaleFactor // for the creation of samples in the first approximation algorithm. 
			) { 
		
		if (this.datasets.isEmpty()) {
			
			if (!specialCase) {
				for(int i = 0; i < numOfTrials; i ++) {
					int numOfDatasetsPerTS = RanNum.getRandomIntRange(Parameters.maxNumOfDatasetsPerTS, Parameters.minNumOfDatasetsPerTS);
					List<Dataset> dss = new ArrayList<Dataset>();
					List<Sample> sams = new ArrayList<Sample>();
					for(int j = 0; j < numOfDatasetsPerTS; j ++) {
						Dataset ds = new Dataset(this.getDataCenters(), specialCase);
						dss.add(ds);
						for (int s = 0; s < Parameters.errorBounds.length; s ++){
							Sample sample = new Sample(ds, s, -1);
							sams.add(sample);
							ds.getSamples().add(sample);
						}
					}
					this.datasets.put(i, dss);
					this.samples.put(i, sams);
				}
			} else {
				for(int i = 0; i < numOfTrials; i ++) {
					int numOfDatasetsPerTS = RanNum.getRandomIntRange(Parameters.maxNumOfDatasetsPerTS, Parameters.minNumOfDatasetsPerTS);
					List<Dataset> dss = new ArrayList<Dataset>();
					List<Sample> sams = new ArrayList<Sample>();
					for(int j = 0; j < numOfDatasetsPerTS; j ++) {
						Dataset ds = new Dataset(this.getDataCenters(), !specialCase);// in the special case volume of each dataset is not randomly generated. 
						dss.add(ds);
						//int [] scale_factors = {1, 2, 3};
						for (int s = 0; s < Parameters.errorBounds.length; s ++) {
							Sample sample = new Sample(ds, s, Parameters.errorBounds.length - s);
							sams.add(sample);
							ds.getSamples().add(sample);
							//Parameters.errorBounds[s] = sample.getError();
						}
					}
					this.datasets.put(i, dss);
					this.samples.put(i, sams);
				}
			}
		}
		
		return this.datasets;
	}
	
	public Map<Integer, Map<Integer, List<Query>>> InitializeQueries(int numOfTrials, int maxNumTimeslots){
		
		if (this.queriesOnlineBatch.isEmpty()) {
			for (int timeslot = 0; timeslot < maxNumTimeslots; timeslot ++) {
				
				if (null == queriesOnlineBatch.get(timeslot))
					queriesOnlineBatch.put(timeslot, new HashMap<Integer, List<Query>>());
				
				for(int i = 0; i < numOfTrials; i ++) {
					int numOfQueriesPerTS = RanNum.getRandomIntRange(Parameters.maxNumOfQueriesPerTS, Parameters.minNumOfQueriesPerTS);
					
					List<Query> qus = new ArrayList<Query>();
					Set<Sample> samplesToBePlaced = new HashSet<Sample>();
					
					for(int j = 0; j < numOfQueriesPerTS; j ++) {
						Query quer = new Query(this.getDataCenters(), this.getDatasets().get(i), Parameters.numOfDatasetPerQueryMax);
						qus.add(quer);
						
						for (Dataset ds : quer.getDatasets()) {
							samplesToBePlaced.add(ds.getSamples().get(Parameters.errorBounds.length - 2));
						}
					}
					
					queriesOnlineBatch.get(timeslot).put(i, qus);
					
					// make sure the total computing resource is more than that required by all queries. 
					double totalComputingDemand = 0d;
					for (Sample sample : samplesToBePlaced) {
						totalComputingDemand += sample.getVolume() * Parameters.computingAllocatedToUnitData; 
					}
					
					double totalComputingAvailable = 0d; 
					for (DataCenter dc : this.dataCenterList){
						totalComputingAvailable += dc.getAvailableComputing();
					}
					
					if (totalComputingDemand > totalComputingAvailable) {
						
						double ratio = totalComputingAvailable / (totalComputingDemand + 10d);
						
						System.out.println("scalling down the size of all datasets by ratio: " + ratio);

						for (Dataset ds : this.datasets.get(i)) {
							ds.setVolume(ds.getVolume() * ratio);
							for (Sample sam : ds.getSamples()){
								sam.setVolume(sam.getVolume() * ratio);
							}
						}
					}
				}
				//System.out.println(numOfQueries);
			}
		}
		
		return this.queriesOnlineBatch;
	}
	
	
	public Map<Integer, List<Query>> InitializeQueries(int numOfTrials, boolean fixNumPerTrial, int numOfDatasetPerQueryMax) {
		
		//String numOfQueries = "";
		if (this.queries.isEmpty()){// generate queries
			
			for(int i = 0; i < numOfTrials; i ++) {
				
				int numOfQueriesPerTS = 0; 
				if (!fixNumPerTrial)
					numOfQueriesPerTS = RanNum.getRandomIntRange(Parameters.maxNumOfQueriesPerTS, Parameters.minNumOfQueriesPerTS);
				else 
					numOfQueriesPerTS = Parameters.maxNumOfQueriesPerTS; 
				
				//numOfQueries += numOfQueriesPerTS + " ";
				List<Query> qus = new ArrayList<Query>();
				Set<Sample> samplesToBePlaced = new HashSet<Sample>();
				//double targetError = Parameters.errorBounds[1];
				
				for(int j = 0; j < numOfQueriesPerTS; j ++) {
					Query quer = new Query(this.getDataCenters(), this.getDatasets().get(i), numOfDatasetPerQueryMax);
					qus.add(quer);
					
					for (Dataset ds : quer.getDatasets()) {
						samplesToBePlaced.add(ds.getSamples().get(Parameters.errorBounds.length - 2));
					}
				}
				
				queries.put(i, qus);
				
				// make sure the total computing resource is more than that required by all queries. 
				double totalComputingDemand = 0d;
				for (Sample sample : samplesToBePlaced) {
					totalComputingDemand += sample.getVolume() * Parameters.computingAllocatedToUnitData; 
				}
				
				double totalComputingAvailable = 0d; 
				for (DataCenter dc : this.dataCenterList){
					totalComputingAvailable += dc.getAvailableComputing();
				}
				
				if (totalComputingDemand > totalComputingAvailable){
					double ratio = totalComputingAvailable / (totalComputingDemand + 10d);
					
					System.out.println("scalling down the size of all datasets by ratio: " + ratio);

					for (Dataset ds : this.datasets.get(i)) {
						ds.setVolume(ds.getVolume() * ratio);
						for (Sample sam : ds.getSamples()){
							sam.setVolume(sam.getVolume() * ratio);
						}
					}
				}
			}
			//System.out.println(numOfQueries);
		}
		return this.queries;
	}

	public void modifyCosts() {

		for (Node node : this.datacenterNetwork.vertexSet()) {

			if (node instanceof DataCenter) {
				double processingCost = RanNum.getRandomDoubleRange(Parameters.processCostUnitDataMax,
						Parameters.processCostUnitDataMin);
				double storageCost = RanNum.getRandomDoubleRange(Parameters.storageCostUnitDataMax,
						Parameters.storageCostUnitDataMin);

				DataCenter dc = (DataCenter) node;
				dc.setProcessingCost(processingCost);
				dc.setStorageCost(storageCost);
			}
		}

		for (InternetLink il : this.datacenterNetwork.edgeSet()) {
			il.setCost(RanNum.getRandomDoubleRange(Parameters.maxBandwidthCost, Parameters.minBandwidthCost));
		}
	}
	
	public List<Sample> samplesOfDataset(int trial, Dataset ds){
		
		List<Sample> foundSamples = new ArrayList<Sample>();
		for (Sample sample : this.getSamples().get(trial)){
			if (sample.getParentDataset().equals(ds))
				foundSamples.add(sample);
		}
		
		return foundSamples;
	}

	public List<DataCenter> getDataCenters() {
		if (!this.dataCenterList.isEmpty())
			return this.dataCenterList;

		for (Node dc : this.datacenterNetwork.vertexSet()) {
			if (dc instanceof DataCenter)
				this.dataCenterList.add((DataCenter) dc);
		}

		return this.dataCenterList;
	}

	public int getNumberOfDataCenters() {
		int numDC = 0;
		for (Node dc : this.datacenterNetwork.vertexSet()) {
			if (dc instanceof DataCenter)
				numDC++;
		}
		return numDC;
	}

	/******** getter and setter ************/
	public SimpleWeightedGraph<Node, InternetLink> getDatacenterNetwork() {
		return datacenterNetwork;
	}

	public void setDatacenterNetwork(SimpleWeightedGraph<Node, InternetLink> datacenterNetwork) {
		this.datacenterNetwork = datacenterNetwork;
	}

	public Map<Integer, List<Dataset>> getDatasets() {
		return datasets;
	}

	public void setDatasets(Map<Integer, List<Dataset>> datasets) {
		this.datasets = datasets;
	}

	public Map<Integer, List<Sample>> getSamples() {
		return samples;
	}

	public void setSamples(Map<Integer, List<Sample>> samples) {
		this.samples = samples;
	}

	public Map<Integer, List<Query>> getQueries() {
		return queries;
	}

	public void setQueries(Map<Integer, List<Query>> queries) {
		this.queries = queries;
	}

	public Map<Integer, Map<Integer, List<Query>>> getQueriesOnlineBatch() {
		return queriesOnlineBatch;
	}

	public void setQueriesOnlineBatch(Map<Integer, Map<Integer, List<Query>>> queriesOnlineBatch) {
		this.queriesOnlineBatch = queriesOnlineBatch;
	}

}
