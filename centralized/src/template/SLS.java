package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;



public class SLS {

	public List<Vehicle> vehicles = new ArrayList<Vehicle>();
	public List<Task> tasks;
	Random rand = new Random();
	
	// number of tasks and vehicles
	public int NT;
	public int NV;

	public SLS(List<Vehicle> vehicles, TaskSet tasks) {
		this.vehicles = vehicles;
		this.tasks = new ArrayList<Task>(tasks);
		this.NT = tasks.size()-1;
		this.NV = vehicles.size()-1;
	}

	public List<Plan> BuildPLans(){
		System.out.print("Building Plans...");
		
		Sol bestSol = SelectInitialSolution();
		int max_iter = 10000;
		int n_iter = 0;
		while(n_iter < max_iter){
			System.out.print("entering while");
			Sol oldSol = bestSol.copy();
			List<Sol> neighborsSol = ChooseNeighbors(oldSol);
			System.out.print("pass ChooseNeigbors");
			bestSol = localChoice(neighborsSol,oldSol);
			System.out.print("pass localChoice");
			//converge until max_iteration is reached
			n_iter += 1;
			if(n_iter % 100 == 0) {
				System.out.print("iteration: " + n_iter);
			}
			
		}
		System.out.print("Building Plans...");
		return GenerateListPlanFrom(bestSol); // TODO return list of plans
	}
	
	public Sol SelectInitialSolution() {
		
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
				
			List<Task> nextTask = new ArrayList<Task>();
			int[] time = new int[tasks.size()];
			List<Vehicle> vehicle = new ArrayList<Vehicle>();
			
			// add nextTask for all tasks
			Task firstTask = tasks.get(0);
			for(Task t: tasks) {
				int i = 0;
				if(t.equals(firstTask)) {
					vehicle.add(biggest);
					continue;
				}
				else {
				nextTask.add(t);
				}
				// set time for all tasks
				time[i] = i+1;
				i+= 1;
				
				// set vehicles for all tasks
				vehicle.add(biggest);
			}
			
			// add initial nextTask for all vehicles
			for(Vehicle v: vehicles) {
				if(v.equals(biggest)) {
					nextTask.add(firstTask);
				}
				nextTask.add(null);
			}
			
			System.out.print(nextTask.size() +" "+ time.length+" " +vehicle.size());
			return new Sol(nextTask,time,vehicle);
				
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
			System.out.print("entering ChooseNeighbors while \n");
			vehicle_ind = rand.nextInt(vehicles.size());
			v = vehicles.get(vehicle_ind);
			// check validity
			if (s.nextTask.get(NT+vehicle_ind)!=null) {
				System.out.print("exiting ChooseNeighbors while \n");
				rand_vehicle_invalid = false;	// break loop if valid
			}
		}
		
		// get current weight on vehicle v
		double v_load = 0;
		for(Task t: tasks) {
			int task_ind = tasks.indexOf(t);
			if(s.vehicles.get(task_ind).equals(v)) {
				System.out.print("passed first get except for" + task_ind+"\n");
				v_load += t.weight;
			}
		}
		
		System.out.print("passed v_load \n");
		// Applying the changing vehicle operator
		for(Vehicle v1: vehicles) {
			int other_vehicle_ind = vehicles.indexOf(v1);
			// generate a new solution for each vehicle
			Task t = s.nextTask.get(NT + other_vehicle_ind);
			// check invalidity: capacity exceeded or task null
			if((t!=null) && (v.capacity() > v_load + t.weight)) {
				Vehicle other_v = v1;
				Sol new_s = changingVehicle(s,v,other_v);
				sols.add(new_s);
				
			}
		}
		System.out.print("passed v1 \n");
		// Applying the changing task order operator
		// compute the number of tasks of the vehicle
		int length = 0;
		int next_task_ind = NT + vehicle_ind;
		
		while(s.nextTask.get(next_task_ind) != null) { //TODO never quitting
			System.out.print(next_task_ind+"\n");
			System.out.print(s.nextTask+"\n");
			System.out.print(s.nextTask.get(next_task_ind)+"\n");
			Task next_task = s.nextTask.get(next_task_ind);
			length += 1;
			// get the index of the next task
			next_task_ind = tasks.indexOf(next_task);

		}
		System.out.print("passed while null \n");
		
		// apply the operator
		if(length >= 2) {
			for (int tidx1 = 0; tidx1 < length; tidx1++) {
				for (int tidx2 = 0; tidx2 < length; tidx2++) {
					Sol new_s = changingTaskOrder(s,v,tidx1,tidx2);
					sols.add(new_s);
				}
			}
		}
		System.out.print("passed length \n");
			
		return sols;
	}
	
	// Function for changing vehicle
		public Sol changingVehicle(Sol s,Vehicle v,Vehicle other_v) {
			
			Sol s1 = s;	// good or copying better?
			int v_index = vehicles.indexOf(v);
			int other_v_index = vehicles.indexOf(other_v);
			Task t = s.nextTask.get(NT+v_index);
			int t_index = tasks.indexOf(t);
			s1.nextTask.add(NT+v_index,s1.nextTask.get(t_index));
			s1.nextTask.add(t_index, s1.nextTask.get(NT+other_v_index));
			s1.nextTask.add(NT+other_v_index,t);
			UpdateTime(s1,v);
			UpdateTime(s1,other_v);
			return s1;
		}
		
		// Function for changing task order
		public Sol changingTaskOrder(Sol s,Vehicle v,int tidx1, int tidx2) {
			
			Sol s1 = s;
			int v_index = vehicles.indexOf(v);
			int tPre1_ind = NT+v_index;	// previous task of task 1
			Task t1 = s.nextTask.get(tPre1_ind);	// task 1
			int count = 1;
			while(count < tidx1) {
				tPre1_ind = tasks.indexOf(t1);
				t1 = s1.nextTask.get(tPre1_ind);
				count++;
			}
			
			int tPost1_ind = tasks.indexOf(s1.nextTask.get(tasks.indexOf(t1)));	// the task delivered after t1
			int tPre2_ind = tasks.indexOf(t1);	// previous task of task2
			Task t2 = s1.nextTask.get(tPre2_ind);	// task2
			count++;
			
			while(count < tidx2) {
				tPre2_ind = tasks.indexOf(t2);
				t2 = s1.nextTask.get(tPre2_ind);
				count++;
			}
			int tPost2_ind = tasks.indexOf(s1.nextTask.get(tasks.indexOf(t2)));	// the task delivered after t2
			
			// exchanging two tasks
			if(tPost1_ind == tasks.indexOf(t2)) {
				// the task t2 is delivered immediately after t1
				s1.nextTask.add(tPre1_ind,t2);
				s1.nextTask.add(tasks.indexOf(t2),t1);
				s1.nextTask.add(tasks.indexOf(t1),tasks.get(tPost2_ind));
			}
			else {
				s1.nextTask.add(tPre1_ind,t2);
				s1.nextTask.add(tPre2_ind,t1);
				s1.nextTask.add(tasks.indexOf(t2),tasks.get(tPost1_ind));
				s1.nextTask.add(tasks.indexOf(t1),tasks.get(tPost2_ind));
			}
			UpdateTime(s1,v);
			return s1;
		}
	
	
	// Choose the best Solution between Neighbors with potential
	public Sol localChoice(List<Sol> n, Sol oldSol) {
		
		//probability 1-p to explore other solutions.
		double p = 0.5;
		double r = rand.nextDouble();
		
		if(r<p){
			
			Sol bestSol = n.get(0);
			for(Sol s: n) {
				
				if(isConsistent(s)){
					if(costFunction(s) < costFunction(bestSol)) {
						bestSol = s;
					}
				}
				else{ 
					continue; 
				}
			}
			return bestSol;
			}
			
			else {
				return oldSol;
			}
	}
	
	public Boolean isConsistent(Sol s) {
		
		boolean c1,c2,c3,c4,c5,c6,c7;
		c1 = c2 = c3 = c4 = c5 = c6 = c7 = true;
		
		for(Task nt: s.nextTask) {
			
			int j = s.nextTask.indexOf(nt); // index of tj in algo.
			int i = tasks.indexOf(nt); // index of ti in algo
			
			// first constraint
			if(nt.equals(tasks.get(j))){
				c1 = false;
			}
			
			int v = 0;
			if(j > NT) {
				
				v += 1; // vehicle number
				//second constraint on initial nextTask of vehicles
				if(s.time[i]!=1) {
					c2 = false;
				}
				//fourth constraint
				if(vehicles.get(v) == s.vehicles.get(i)) {
					c4 = false;
				}
				
			} else { // if j<= NT aka we are still in tasks part of nextTask
				
				//third constraint
				if(s.time[j] != s.time[i]+1) {
					c3 = false;
				}
				//fifth constraint
				if(s.vehicles.get(j) != s.vehicles.get(i)) {
					c5 = false;
				}
			}
				
			// TODO c6 and c7
				
		}
		return c1&&c2&&c4&&c3&&c5&&c6&&c7;
	}
	
	// Function for updating times
	public void UpdateTime(Sol s, Vehicle v) {
		
		int v_index = vehicles.indexOf(v);
		Task ti = s.nextTask.get(NT+v_index);
		if(ti != null) {
			int ti_ind = tasks.indexOf(ti);
			s.time[ti_ind] = 1;
			
			while(true) {
				Task tj = s.nextTask.get(tasks.indexOf(ti));
				if(tj != null) {
					int tj_ind = tasks.indexOf(tj);
					s.time[tj_ind] = s.time[ti_ind]+1;
					ti=tj;
				} else {
					break;
				}
			}
		}
	}
	
	public double costFunction(Sol s) {
		
		double sumTaskCost = 0;
		for(Task t: tasks) {
			int t_ind = tasks.indexOf(t);
			sumTaskCost += t.deliveryCity.distanceTo(s.nextTask.get(t_ind).pickupCity);
			sumTaskCost *= s.vehicles.get(t_ind).costPerKm();
		}
		
		double sumVehCost = 0;
		for(Vehicle v: vehicles) {
			int t_ind = vehicles.indexOf(v);
			sumVehCost += v.homeCity().distanceTo(s.nextTask.get(t_ind).pickupCity);
			sumVehCost *= s.vehicles.get(t_ind).costPerKm();
			
		}
		return sumTaskCost + sumVehCost;
	}
	
	
	//generate the list of plans from Solution s
		public List<Plan> GenerateListPlanFrom(Sol s){
			
			List<Plan> plans = new ArrayList<Plan>();
			
			for (Vehicle v: vehicles) {
				
				int v_index = vehicles.indexOf(v);
				City  home_city = v.homeCity();	// initial city is just the pickup city
				Plan v_plan = new Plan(home_city);
				// add initial task moves (outside of loop because initially at home city)
				Task t = s.nextTask.get(NT+v_index);
				City pickup_city = t.pickupCity;
				List<City> path_list = home_city.pathTo(pickup_city);
				for (City city : path_list) {
					v_plan.appendMove(city);
				}
				v_plan.appendPickup(t);
				while(t!=null) {
					
					// delivering the task
					City deliver_city = t.deliveryCity;
					path_list = pickup_city.pathTo(deliver_city);
					for (City city : path_list) {
						v_plan.appendMove(city);
					}
					v_plan.appendDelivery(t);
					// picking up the new task
					t = s.nextTask.get(tasks.indexOf(t));
					if(t!=null) {
						pickup_city = t.pickupCity;
						path_list = deliver_city.pathTo(pickup_city);
						for (City city : path_list) {
							v_plan.appendMove(city);
						}
						v_plan.appendPickup(t);
					}
				}
				// add plan to list
				plans.add(v_plan);
			}
			
			return plans;
		}
	
}
