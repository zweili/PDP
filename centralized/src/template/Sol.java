package template;

import java.util.ArrayList;
import java.util.List;

import logist.simulation.Vehicle;
//import logist.task.Task;


public class Sol {
	
	List<NewTask> nextTask = new ArrayList<NewTask>();
	int[] time;
	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	
	
	public Sol(List<NewTask> nextTask, int[] time, List<Vehicle> v) {
		this.nextTask = nextTask;
		this.time = time;
		this.vehicles = v;
	}
		
	public Sol copy() {
		return new Sol(this.nextTask,this.time,this.vehicles);
	}
}
