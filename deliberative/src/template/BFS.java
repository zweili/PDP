package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.topology.Topology.City;
import logist.task.TaskSet;
import logist.task.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;

public class BFS {
// Breadth First Search
	private Vehicle vehicle;
	private TaskSet tasks;
	
	
	public BFS(Vehicle vehicle, TaskSet tasks) {
		this.vehicle = vehicle;
		this.tasks = tasks;
	}
	

	public Plan BFSPlan(int capacity) {
	
		long BFS_start = System.currentTimeMillis();
		System.out.println("Planning with Breadth First Search... \n");
		
		// Initialize
		City current_city = vehicle.getCurrentCity();
		Plan bfs_plan = new Plan(current_city);
		
		// variable to hold the cumulative cost (re-calculated every loop)
		double curr_total_cost = 0.0;
		
		// Get initial state
		States curr_state = new States(current_city, tasks, vehicle.getCurrentTasks(), curr_total_cost, null);

		// initialize Q - add current state
		Queue<States> Q = new LinkedList<States>();
		Q.add(curr_state);

		// initialize C - empty
		ArrayList<States> C = new ArrayList<States>();
		
		while (true) {

			// check failure condition
			if(Q.peek() == null) {
				System.out.println("Error: State queue empty! \n");
				return null;
			}
			
			// get first element in Q as current state
			curr_state = Q.poll();

			City curr_city = curr_state.CurrCity;
			
			TaskSet picked_tasks = curr_state.cTask;
			TaskSet available_tasks = curr_state.aTask;

			// get total cost
			curr_total_cost = curr_state.minTotalCost;
			
			// break loop condition
			// found a goal state if there are no more tasks in the topology and also none carried by the vehicle
			if((picked_tasks.rewardSum() == 0) && (available_tasks.rewardSum() == 0)) {
				break;
			}
			
			// check if the current state was visited before
			States found_state = findState(C,curr_state);
			
			if (found_state == null) { // || found_state.minTotalCost > curr_total_cost) {
				
				// add current state to checked states
				C.add(curr_state);
				
				// get and add current state's successor states
				// 1 - neighboring cities
				List<City> neighbors = curr_city.neighbors();
				int n_neighs = neighbors.size();

				for(int n_ind = 0; n_ind < n_neighs; n_ind++) {
					City neigh_city = neighbors.get(n_ind);
					double next_total_cost = curr_total_cost + curr_city.distanceUnitsTo(neigh_city)*vehicle.costPerKm();
					States succ_state = new States(neigh_city, available_tasks, picked_tasks, next_total_cost, curr_state);
					
					Q.add(succ_state);
				}
				
				// 2 - picking up tasks (check if exceeds total allowed weight)
				// check if there is a task in current city
				
				for(Task task : available_tasks) {
					if(task.pickupCity == curr_city) {
						// add picked task action as successor state
						if(picked_tasks.weightSum() + task.weight <capacity) { // + the actual task.weight right ?
							
							// update(add task) picked_tasks of current vehicle and update(remove task) available_tasks of the topology
							TaskSet new_picked_tasks = picked_tasks.clone();
							new_picked_tasks.add(task);
							TaskSet new_available_tasks = available_tasks.clone();
							new_available_tasks.remove(task);
							States succ_state = new States(curr_city, new_available_tasks, new_picked_tasks, curr_total_cost, curr_state);
							
							Q.add(succ_state); 							
						}
						break;
					}					
				}
				
				// 3 - delivering tasks
				// check if there is a task to be delivered here
				for(Task task : picked_tasks) {
					
					if(task.deliveryCity == curr_city) {
						
						// update(remove task) picked_tasks of current vehicle
						TaskSet new_picked_tasks = picked_tasks.clone();
						new_picked_tasks.remove(task);
						States succ_state = new States(curr_city, available_tasks, new_picked_tasks, curr_total_cost, curr_state);
						
						Q.add(succ_state);
							
						}
					break;			
				}
			}
			else {
			// if current state was visited before
			
				// check if the current total cost is smaller
				double prev_total_cost = found_state.minTotalCost; 
				if(prev_total_cost > curr_total_cost) {
				
					// update total cost and parent city if total cost is smaller than previous value
					// or equivalently, old state in C and add current state to it
					C.remove(found_state);
					C.add(curr_state);
					
				}
				// otherwise, do nothing
				
			}
		}
		// generate plan from final state
		generatePlan(curr_state, bfs_plan);
		long BFS_end = System.currentTimeMillis();
		System.out.print("Execution Time: " + (BFS_end - BFS_start)+ "\n");
		return bfs_plan;
	}
	
	// generate a plan recursively starting from current state and following the parent states
	public void generatePlan(States curr_state, Plan plan) {

		States parent_state = curr_state.BestDaddy;

		if(parent_state != null) {	// reached the first state if it is null
			// recursion
			generatePlan(parent_state, plan);
			// check the action that was done
			
			// moving
			if(parent_state.CurrCity != curr_state.CurrCity) {

				plan.appendMove(curr_state.CurrCity);
			}
			// delivery
			else if(parent_state.cTask.rewardSum() > curr_state.cTask.rewardSum() ) { // TODO change using cost
				
				TaskSet diff_picked = parent_state.cTask.clone();
				diff_picked.removeAll(curr_state.cTask);
				Task taskToPickup = diff_picked.iterator().next();

				plan.appendDelivery(taskToPickup);
			}
			// pickup
			else if( parent_state.cTask.rewardSum() < curr_state.cTask.rewardSum() ) { // TODO change using cost
				
				
				TaskSet diff_picked = curr_state.cTask.clone();
				diff_picked.removeAll(parent_state.cTask);
				Task taskToPickup = diff_picked.iterator().next();

				plan.appendPickup(taskToPickup);
			}
		}

	}

	public States findState(ArrayList<States> allStates, States stateToFind) {
		
		
		//return the state from allStates that is similar(the first 3 parameters) to stateTofFind
		for (States s: allStates) {
			if(s.CurrCity.equals(stateToFind.CurrCity) && s.aTask.equals(stateToFind.aTask) && s.cTask.equals(stateToFind.cTask)){

				return s;
				
			}
		}

		return null;
	}
}


