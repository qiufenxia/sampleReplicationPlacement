package system;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import graph.Node;
import simulation.Parameters;
import simulation.SamplePlacementSimulator;
import utils.RanNum;

public class Dataset {
	private double ID;
	private Node datacenter;
	private double volume;
	private List<Sample> samples = null; 
	private Set<Sample> placedSamples = null;
	
	public Dataset (List<DataCenter> dcList, boolean randomVolume) {
		this.ID = SamplePlacementSimulator.idAllocator.nextId();
		
		int indexOfDCDatasetLocated = RanNum.getRandomIntRange(dcList.size(), 0);
		this.datacenter = dcList.get(indexOfDCDatasetLocated);
		// set the volume of this dataset
		if (randomVolume) {
			this.volume = RanNum.getRandomDoubleRange(Parameters.sizePerDatasetMax, Parameters.sizePerDatasetMin);
		} else {
			Random ran = new Random();
			if (ran.nextBoolean()){
				this.volume = Parameters.sizePerDatasetMin;
			} else {
				this.volume = Parameters.sizePerDatasetMax;
			}
		}
		this.setSamples(new ArrayList<Sample>());
		this.setPlacedSamples(new HashSet<Sample>());
	}
	
//	public boolean equals (Object another){
//		if (this == another)
//			return true;
//		
//		if (!(another instanceof Dataset))
//			return false;
		//?????????????????????????whether all the conditions are satisfied
//		if (this.user.equals(((Dataset) another).getUser()) )
//			return true;
//		else 
//			return false;
//	}
	
	public void reset(){
		this.setPlacedSamples(new HashSet<Sample>());
	}
	
	public Sample getSample(double error) {
		for (Sample sample : this.getSamples()){
			if (error == sample.getError())
				return sample; 
		}
		return null; 
	}
	
	// eee = 1 : largest error; eee = 0 : smallest error
	public Sample getSample(int eee) {
		
		Sample samLargestError = null;
		double largestError = -1;
		Sample samSmallestError = null; 
		double smallestError = Double.MAX_VALUE;

		for (Sample sample : this.getSamples()){
			if (largestError <= sample.getError()){
				largestError = sample.getError();
				samLargestError = sample; 
			}
			
			if (smallestError >= sample.getError()){
				smallestError = sample.getError();
				samSmallestError = sample; 
			}
		}
		
		if (eee == 1) {
			return samLargestError;
		} else if (eee == 0){
			return samSmallestError; 
		} else {
			return null;
		}
	}
	
	public double getID() {
		return ID;
	}
	public void setID(double iD) {
		ID = iD;
	}
	public Node getDatacenter() {
		return datacenter;
	}
	public void setDatacenter(Node datacenter) {
		this.datacenter = datacenter;
	}
	public double getVolume() {
		return volume;
	}
	public void setVolume(double volume) {
		this.volume = volume;
	}

	public List<Sample> getSamples() {
		return samples;
	}

	public void setSamples(List<Sample> samples) {
		this.samples = samples;
	}

	public Set<Sample> getPlacedSamples() {
		return placedSamples;
	}

	public void setPlacedSamples(Set<Sample> placedSamples) {
		this.placedSamples = placedSamples;
	} 
}
