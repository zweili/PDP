package template;


import logist.task.TaskSet;
import logist.topology.Topology.City;

public class States implements Comparable<States>{
	
	public City CurrCity; // Current city.
	public TaskSet aTask; //Available Tasks remaining in the topology.
	public TaskSet cTask; //Carried Tasks.
	
	// attributes to help search algorithm, not really used to describe a state.
	public double minTotalCost; // the min cost from initial state to current state.
	public States BestDaddy; // Parents with the minimum cost.

	
	public States(City city, TaskSet available, TaskSet carried, double mc, States daddy) {
		
		this.CurrCity = city;
		this.aTask = available;
		this.cTask = carried;
		this.minTotalCost = mc;
		this.BestDaddy = daddy;
	}
	
	
	// Helper function to sort a list of states in function of f = g + h (heuristic) TODO change using cost.
	public int compareTo(States s){
		double f_other = s.minTotalCost + (s.cTask.rewardSum() + s.aTask.rewardSum());
		double f = minTotalCost + (cTask.rewardSum() + aTask.rewardSum());
		if(f>f_other) {
			return 1;
		}
		else if (f<f_other){
			return -1;
		}
		else {
			return 0;
		}
		
		
	}

}
