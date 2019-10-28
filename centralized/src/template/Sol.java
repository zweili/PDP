package template;

import java.util.ArrayList;
import java.util.List;

import logist.simulation.Vehicle;
import logist.task.Task;


public class Sol {
	
	List<Task> nextTask = new ArrayList<Task>();
	int[] time;
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	
	
	public Sol(List<Task> nextTask, int[] time, List<Vehicle> v) {
		this.nextTask = nextTask;
		this.time = time;
		this.vehicles = v;
	}
		
	public Sol copy() {
		return new Sol(this.nextTask,this.time,this.vehicles);
	}
}
