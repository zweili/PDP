package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;



public class SLS {

	List<Vehicle> vehicles = new ArrayList<Vehicle>();
	List<Task> tasks = new ArrayList<Task>();
	Random rand = new Random();
	
	// number of tasks and vehicles
	int NT;
	int NV;
	
	public SLS(List<Vehicle> v,List<Task> t) {
		this.vehicles = v;
		this.tasks = t;
		// get number of tasks and vehicles
		NT = tasks.size();
		NV = vehicles.size();
	}

	public List<Plan> BuildPLans(){
		
		Sol bestSol = SelectInitialSolution();
		double epsilon = 1e-3;
		
		while(true){
			
			Sol oldSol = bestSol.copy();
			List<Sol> neighborsSol = ChooseNeighbors(oldSol);
			bestSol = localChoice(neighborsSol);
			
			
			// converge until no more improvement
			if(bestSol.cost - oldSol.cost > epsilon) {
				break;
			}
			
		}
		
		return GenerateListPlanFrom(bestSol); // TODO return list of plans
	}
	
	public Sol SelectInitialSolution() {
		
		List<List<Object>> x = new ArrayList<List<Object>>();
		List<Boolean> c = new ArrayList<Boolean>();
		Vehicle biggest = vehicles.get(0);
		Task heaviest = tasks.get(0);
		
		// Select biggest vehicle
		for(Vehicle v: vehicles) {
			if(v.capacity() > biggest.capacity()) {
				biggest = v;
			}
		}
		
		// Select heaviest task
		for(Task t: tasks) {
			if(t.weight > heaviest.weight) {
				heaviest = t;
			}
		}
		
		// assert an error if biggest vehicle is not big enough
		if(biggest.capacity() < heaviest.weight) {
			throw new RuntimeException("Impossible to plan: vehicles are not big enough");
		}
		else {
			
;			for(Vehicle v: vehicles) {
	
				if(v.equals(biggest)) {
					
					List<Object> xi = new ArrayList<Object>();
					xi.add(tasks.get(0));
					xi.add(1);
					xi.add(biggest);
					
				} else {
					
				List<Object> xi = new ArrayList<Object>();
				xi.add(null); //nextTask
				xi.add(1); // time
				xi.add(v); // vehicle
				x.add(xi);
				}
			}

		
			return new Sol();
		}
	}
	
	// Select a list of neighbors with high potential 
	public List<Sol> ChooseNeighbors(Sol s) {
		
		// N - list of solution neighbors
		List<Sol> sols = new ArrayList<Sol>();
		
		// get random vehicle, check if valid (next task != null)
		boolean rand_vehicle_invalid = true;
		int vehicle_ind = 0;
		Vehicle v = vehicles.get(vehicle_ind);
		while(rand_vehicle_invalid) {
			vehicle_ind = rand.nextInt(vehicles.size());
			v = vehicles.get(vehicle_ind);
			// check validity
			if (s.nextTask.get(NT+vehicle_ind)!=null) {
				rand_vehicle_invalid = false;	// break loop if valid
			}
		}
		
		// get current weight on vehicle v
		double v_load = 0;
		for(Task t: tasks) {
			int task_ind = tasks.indexOf(t);
			if(s.vehicles.get(task_ind).equals(v)) {
				Task curr_task = tasks.get(task_ind);
				v_load += curr_task.weight;
			}
		}
		
		
		// Applying the changing vehicle operator
		for (int other_vehicle_ind = 0; other_vehicle_ind<vehicles.size(); other_vehicle_ind++) {
			// generate a new solution for each vehicle
			Task t = s.nextTask.get(NT+other_vehicle_ind);	// get first task of vehicle
			// check invalidity: capacity exceeded or task null
			if( (s.nextTask.get(NT+other_vehicle_ind)!=null) && (v.capacity() > v_load + t.weight()) ) {
				Vehicle other_v = vehicles.get(other_vehicle_ind);
				Sol new_s = changingVehicle(s,v,other_v);
				sols.add(new_s);
			}
		}
		
		// Applying the changing task order operator
		
		// compute the number of tasks of the vehicle
		int length = 0;
		int next_task_ind = NT + vehicle_ind;
		while(s.nextTask.get(next_task_ind) != null) {
			Task next_task = s.nextTask.get(next_task_ind);
			length = length + 1;
			// get the index of the next task
			next_task_ind = tasks.indexOf(next_task);

		}
		
		// apply the operator
		if(length >= 2) {
			for (int tidx1 = 0; tidx1 < length; tidx1++) {
				for (int tidx2 = 0; tidx2 < length; tidx2++) {
					Sol new_s = changingTaskOrder(s,v,tidx1,tidx2);
					sols.add(new_s);
				}
			}
		}
			
		return sols;
	}
	
	// Choose the best Solution between Neighbors with potential
	public Sol localChoice(List<Sol> n) {
		
		//probability 1-p to explore other solutions.
		double p = 0.95;
		
		Sol bestSol = n.get(0);
		
		return bestSol;
	}
	
	
	//generate the list of plans from Solution s
	public List<Plan> GenerateListPlanFrom(Sol s){
		
		List<Plan> plans = new ArrayList<Plan>();
		return plans;
	}
}