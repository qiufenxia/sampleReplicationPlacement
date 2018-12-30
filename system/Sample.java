package system;

import graph.Node;
import simulation.Parameters;
import simulation.SamplePlacementSimulator;

public class Sample extends Node {
	
	private double error;
	private double volume;
	private Dataset parentDataset;
	//private double lifeCycle;//how many time slots that this sample can live in the system
	
	private Sample realSample = null;// used in "virtual samples"

	public Sample(Dataset parent, int errorIndex, int scaleFactor) {
		super(SamplePlacementSimulator.idAllocator.nextId(), "Sample");
		this.parentDataset = parent;
		if (-1 == scaleFactor) {
			//int choice = RanNum.getRandomIntRange(Parameters.errorBounds.length - 1, 0);		
			this.error = Parameters.errorBounds[errorIndex];
			this.volume = parentDataset.getVolume() * (1 - this.error);
			//this.lifeCycle = RanNum.getRandomIntRange(Parameters.lifeCycleMax, Parameters.lifeCycleMin);
		} else {
			double largestVolume = parentDataset.getVolume() * (1 - Parameters.errorBounds[errorIndex]);
			double smallestVolume = largestVolume / Parameters.errorBounds.length;
			this.volume = smallestVolume * scaleFactor;
			//this.error = 1 - this.volume / parentDataset.getVolume();
			this.error = 1/(6.3 * Math.sqrt(this.volume));
		}
	}
	
	public Sample(Sample realSample, double volume, String name){
		super(SamplePlacementSimulator.idAllocator.nextId(), name);
		this.volume = volume; 
		this.realSample = realSample; 
	}
	
	//get the computing demand of processing this sample
	public double getComputingDemands(Sample sample){
		double dems = 0d;
		dems = sample.volume * Parameters.computingAllocatedToUnitData;
		return dems; 
	}
	
	//getter and setter
	public double getError() {
		return error;
	}

	public void setError(double error) {
		this.error = error;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public Dataset getParentDataset() {
		return parentDataset;
	}

	public void setParentDataset(Dataset parentDataset) {
		this.parentDataset = parentDataset;
	}

	public Sample getRealSample() {
		return realSample;
	}

	public void setRealSample(Sample realSample) {
		this.realSample = realSample;
	}

}
