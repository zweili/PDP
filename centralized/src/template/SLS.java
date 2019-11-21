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
	public List<NewTask> newTasks;
	Random rand = new Random();	 
	
	// number of tasks and vehicles
	public int NT;
	public int NV;

	public SLS(List<Vehicle> vehicles, TaskSet tasks) {
		this.vehicles = vehicles;
		this.tasks = new ArrayList<Task>(tasks);
		this.NT = tasks.size()*2;
		this.NV = vehicles.size();
		
		List<NewTask> nt = new ArrayList<NewTask>();
		for(Task t: tasks) {
			NewTask newP = new NewTask(t,true); // pickup
			NewTask newD = new NewTask(t,false); // deliver
			nt.add(newP);
			nt.add(newD);
		}
		
		this.newTasks = nt;
	}

	public List<Plan> BuildPLans(){
		System.out.print("Building Plans...");
		
		Sol bestSol = SelectInitialSolution();
		int max_iter = 10000;
		int n_iter = 0;
		while(n_iter < max_iter){
			
			Sol oldSol = bestSol.copy();
			List<Sol> neighborsSol = ChooseNeighbors(oldSol);
			bestSol = localChoice(neighborsSol,oldSol);
			//converge until max_iteration is reached
			n_iter += 1;
			if(n_iter % 100 == 0) {
				System.out.print("iteration: " + n_iter+ "\n");
			}
			
		}
		System.out.print("bestSol NT: "+ bestSol.nextTask+" size: "+bestSol.nextTask.size()+ "\n");
		System.out.print("time: \n");
		for(int i: bestSol.time) {
			System.out.print(i+" ");
		}
		System.out.print("endtime\n");
		System.out.print(bestSol.vehicles+"\n");
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
				
			List<NewTask> nextTask = new ArrayList<NewTask>();
			int[] time = new int[tasks.size()*2];
			List<Vehicle> vehicle = new ArrayList<Vehicle>();
			
			// add nextTask for all tasks
			Task firstTask = tasks.get(0);

			for(Task t: tasks) {
				
				if(t.equals(firstTask)) {
					//vehicle.add(biggest);
					NewTask firstnewdel= new NewTask(firstTask,false); // deliver
					nextTask.add(firstnewdel);

				}
				else {
				NewTask newPick = new NewTask(t,true); // pickup
				NewTask newDel = new NewTask(t,false); // deliver
				nextTask.add(newPick);
				nextTask.add(newDel);
				}

				// set vehicles for all tasks
				vehicle.add(biggest);
			}
			
			// add last task null nextTask as there is no more task
			nextTask.add(null);
			
			for(int i=0;i < tasks.size()*2; i++) {// set time for all tasks
				time[i] = i+1;
			}
			
			// add initial nextTask for all vehicles
			for(Vehicle v: vehicles) {
				if(v.equals(biggest)) {
					NewTask firstnewPick = new NewTask(firstTask,true); // pickup
					nextTask.add(firstnewPick);

				}
				else {
					nextTask.add(null);
				}
			}

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

			vehicle_ind = rand.nextInt(vehicles.size());
			v = vehicles.get(vehicle_ind);
			// check validity
			if (s.nextTask.get(NT+vehicle_ind)!=null) {

				rand_vehicle_invalid = false;	// break loop if valid
			}
		}
		

		// Applying the changing vehicle operator
		for(Vehicle v1: vehicles) {

			// get current weight on vehicle v
			double v1_load = 0;
			for(Task t: tasks) {
				int task_ind = tasks.indexOf(t);
				if(s.vehicles.get(task_ind).equals(v1)) {
					
					//TODO Check weight in isConsistent ?
					v1_load += t.weight;
				}
			}

			int other_vehicle_ind = vehicles.indexOf(v1);
			// generate a new solution for each vehicle
			NewTask t = s.nextTask.get(NT + other_vehicle_ind);
			// check invalidity: capacity exceeded or task null
			double weig = 0;
			
			if(t != null) {
				weig = t.task.weight;
			}
			if((v1.capacity() > (v1_load + weig))) {
				System.out.print("changevehicle\n");
				Vehicle other_v = v1;

				Sol new_s = changingVehicle(s,v,other_v);

				sols.add(new_s);
				
			}
		}

		// Applying the changing task order operator
		// compute the number of tasks of the vehicle
		int length = 0;
		int next_task_ind = NT + vehicle_ind;
		
		while(s.nextTask.get(next_task_ind) != null) {

			NewTask next_task = s.nextTask.get(next_task_ind);
			int pickUpNT = next_task.isPickup ? 0:1;
			length += 1;
			// get the index of the next task
			next_task_ind = tasks.indexOf(next_task.task)*2+(pickUpNT);

		}
		
		// apply the operator
		if(length >= 2) {
			for (int tidx1 = 0; tidx1 < length; tidx1++) {
				for (int tidx2 = 0; tidx2 < length; tidx2++) {
					
					//System.out.print("ChangingT nextTask s: "+ s.nextTask +"\n");
					//System.out.print("ChangingT vehicles s: "+ s.vehicles +"\n");
					Sol new_s = changingTaskOrder(s,v,tidx1,tidx2);
					//System.out.print("ChangingT nextTask new_s: "+ s.nextTask +"\n");
					//System.out.print("ChangingT vehicles new_s: "+ s.vehicles +"\n");
					sols.add(new_s);
				}
			}
		}

		return sols;
	}
	
	// Function for changing vehicle
		public Sol changingVehicle(Sol s,Vehicle v,Vehicle other_v) {

			Sol s1 = s.copy();	// good or copying better?
			int v_index = vehicles.indexOf(v);
			int other_v_index = vehicles.indexOf(other_v);
			NewTask t = s.nextTask.get(NT+v_index);
			
			if(t != null) {
				
				int pickUpT = t.isPickup ? 0:1;
				int t_index = tasks.indexOf(t.task)*2+(pickUpT);
				s1.nextTask.set(NT+v_index,s1.nextTask.get(t_index));
				s1.nextTask.set(t_index, s1.nextTask.get(NT+other_v_index));
				s1.nextTask.set(NT+other_v_index,t);
				System.out.print("enter first update\n");
				UpdateTime(s1,v);
				System.out.print("enter second update\n");
				UpdateTime(s1,other_v);
				s1.vehicles.set(tasks.indexOf(t.task),other_v);

			}

			return s1;
			
		}
		
		// Function for changing task order
		public Sol changingTaskOrder(Sol s,Vehicle v,int tidx1, int tidx2) {

			Sol s1 = s.copy();
			int v_index = vehicles.indexOf(v);
			int tPre1_ind = NT+v_index;	// previous task of task 1
			NewTask t1 = s.nextTask.get(tPre1_ind);	// task 1
			int count = 1;
			int pickUp1 = t1.isPickup ? 0:1;
			while(count < tidx1) {
				
				tPre1_ind = tasks.indexOf(t1.task)*2+(pickUp1);
				t1 = s1.nextTask.get(tPre1_ind);
				pickUp1 = t1.isPickup ? 0:1; // mapping true to 0 and false to 1
				count++;
			}
			
			int tPre2_ind = tasks.indexOf(t1.task)*2+(pickUp1);	// previous task of task2
			NewTask t2 = s1.nextTask.get(tPre2_ind);	// task2
			int pickUp2 = t2.isPickup ? 0:1;
			count++;
			
			while(count < tidx2) {
				
				//System.out.print(count+"\n");
				tPre2_ind = tasks.indexOf(t2.task)*2+(pickUp2);
				t2 = s1.nextTask.get(tPre2_ind);
				if(t2 != null) {
					pickUp2 = t2.isPickup ? 0:1; // mapping true to 0 and false to 1
				}
				count++;
			}
			
			
			//Task tPost2 = null;
			NewTask nTPost2 = null;
			System.out.print("taskindexof: "+(tasks.indexOf(t2.task)*2+(pickUp2))+" t2.task: "+t2.task+"\n");
			NewTask nT2 = s1.nextTask.get(tasks.indexOf(t2.task)*2+(pickUp2));
			if(nT2 != null) {
				
				int tPost2_ind = tasks.indexOf(nT2.task);	// the task delivered after t2
				Task tPost2 = tasks.get(tPost2_ind);
				nTPost2 = new NewTask(tPost2,nT2.isPickup);
			} 
			
			NewTask nT1 = s1.nextTask.get(tasks.indexOf(t1.task)*2+(pickUp1));
			int tPost1_ind = tasks.indexOf(nT1.task); // the task delivered after t1
			Task tPost1 = tasks.get(tPost1_ind);
			
			NewTask nTPost1 = new NewTask(tPost1,nT1.isPickup);
			

			// exchanging two tasks
			if(tPost1_ind == tasks.indexOf(t2.task)) {
				// the task t2 is delivered immediately after t1
				s1.nextTask.set(tPre1_ind,t2);
				s1.nextTask.set(tasks.indexOf(t2.task)*2+(pickUp2),t1);
				s1.nextTask.set(tasks.indexOf(t1.task)*2+(pickUp1),nTPost2);
			}
			else {
				s1.nextTask.set(tPre1_ind,t2);
				s1.nextTask.set(tPre2_ind,t1);
				s1.nextTask.set(tasks.indexOf(t2.task)*2+(pickUp2),nTPost1);
				s1.nextTask.set(tasks.indexOf(t1.task)*2+(pickUp1),nTPost2);
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

		boolean c1,c2,c3,c4,c5,c6,c7,c8;
		c1 = c2 = c3 = c4 = c5 = c6 = c7 = c8 = true;
		int n_null = 0; 
		int j=0;
		int v_ind = -1;
		for(NewTask nt: s.nextTask) {

			int i=0;
			
			// for sixth constraint

			if(nt == null) {
				System.out.print("NULL");
				n_null++;

			}else {
				System.out.print(nt);
				i = newTasks.indexOf(nt); //TODO WHY -1 ??
			}
			
			if(j > NT-1) {
				v_ind += 1; // vehicle number
			}
			
			if((j > NT-1) && nt != null) {

				//second constraint on initial nextTask of vehicles
				if(s.time[i]!=1) {
					System.out.print("AAA2\n");
					c2 = false;
				}
				//fourth constraint
				if(vehicles.get(v_ind) != s.vehicles.get((i-i%2)/2)) {
					System.out.print("AAA4");
					c4 = false;
				}

				
			} else if((j <=NT-1) && (nt != null)){ // if j<= NT aka we are still in tasks part of nextTask
				
				// first constraint
				//System.out.print("taskj: "+tasks.get(j/2)+" nt_task: "+nt.task+"\n");
				if(newTasks.get(j).equals(nt)){
					System.out.print("AAA1");
					c1 = false;
				}
				
				//third constraint
				System.out.print("i: "+i+" j: "+j+"\n");
				if(s.time[i] != (s.time[j]+1)) {
					System.out.print("AAA3");
					c3 = false;
				}

				//fifth constraint
				if(s.vehicles.get(j) != s.vehicles.get((i-i%2)/2)) {
					System.out.print("AAA5");
					c5 = false;
				}
				
			}	
			j +=1;
		}
		
		//for the sixth
		List<Task> taskOfnTDeliverOnly = new ArrayList<Task>();
		for(NewTask NT: s.nextTask) {
			if(NT != null) {
				if(!NT.isPickup) {
					taskOfnTDeliverOnly.add(NT.task);
				}
			}
		}
		// end of the sixth constraint
		
		if((!taskOfnTDeliverOnly.containsAll(tasks)) && (n_null != s.vehicles.size())) {
				System.out.print("AAA6");
				c6 = false;
		}
		
		// seventh constraint
		
		int v_ind2 = 0;
		for(Vehicle v: vehicles) {
				double v_weight = 0.0;
				NewTask next_task = s.nextTask.get(NT+v_ind2);
				int pickUp = next_task.isPickup ? 0:1;
				while(next_task != null) {
				
					if(next_task.isPickup) {
						v_weight += next_task.task.weight; 
					}
					else {
						v_weight -= next_task.task.weight;
					}
					if(v_weight > v.capacity()) {
						c7 = false;
					}
					next_task = s.nextTask.get(tasks.indexOf(next_task.task)*2+(pickUp));
				}
				v_ind2++;
		}

		
		// eight constraint
		List<Task> taskOfNT = new ArrayList<Task>();
		for(NewTask NT: s.nextTask) {
			taskOfNT.add(NT.task);
		}
		for(Task t: tasks) { // check if first occurrence of t in nexTask is a pickup;
			int ind = taskOfNT.indexOf(t);
			NewTask nt = s.nextTask.get(ind);
			if(!nt.isPickup) {
				c8 = false;
			}
				
		}
		
		return c1&&c2&&c4&&c3&&c5&&c6&&c7&&c8;
	}
	
	// Function for updating times
	public void UpdateTime(Sol s, Vehicle v) {

		int v_index = vehicles.indexOf(v);
		NewTask ti = s.nextTask.get(NT+v_index);

		if(ti != null) {
			int pickUpti = ti.isPickup ? 0:1;
			int ti_ind = tasks.indexOf(ti.task)*2+(pickUpti);
			s.time[ti_ind] = 1;

			while(true) {
				
				pickUpti = ti.isPickup ? 0:1;
				ti_ind = tasks.indexOf(ti.task)*2+(pickUpti);
				int ti_NT_ind = tasks.indexOf(ti.task)*2+(pickUpti);
				NewTask tj = s.nextTask.get(ti_NT_ind);
				
				if(tj != null) {
					int pickUptj = tj.isPickup ? 0:1;
					int tj_ind = tasks.indexOf(tj.task)*2+(pickUptj);
					//System.out.print("ti: "+ti+" ti_ind: "+ti_ind+" tj: "+tj+" tj_ind: "+tj_ind+"\n");
					s.time[tj_ind] = s.time[ti_ind]+1;
					
					ti=tj;
				} else {
					break;
				}
			}
		}
	}
	
	
public double costFunction(Sol s) {
		System.out.print("costFunction\n");
		double sumTaskCost = 0;
		for(int t_ind = 0; t_ind < NT; t_ind++) {
			//int t_ind = tasks.indexOf(t);
			NewTask t_next = s.nextTask.get(t_ind);
			NewTask t = s.nextTask.get(NT+vehicles.indexOf(s.vehicles.get((t_ind - t_ind % 2)/2)));
			if(t!= null) { // sum while there is still a nextTask
				if(t.isPickup) {
					if(t_next.isPickup) {
						sumTaskCost += (t.task.pickupCity.distanceTo(t_next.task.pickupCity))*s.vehicles.get(t_ind).costPerKm();
					}
					else {
						sumTaskCost += (t.task.pickupCity.distanceTo(t_next.task.deliveryCity))*s.vehicles.get(t_ind).costPerKm();
					}
				}
				else { // delivery task
					if(t_next.isPickup) {
						sumTaskCost += (t.task.deliveryCity.distanceTo(t_next.task.pickupCity))*s.vehicles.get(t_ind).costPerKm();
					}
					else {
						sumTaskCost += (t.task.deliveryCity.distanceTo(t_next.task.deliveryCity))*s.vehicles.get(t_ind).costPerKm();
					}
				}
			}
		}

		double sumVehCost = 0;
		for(Vehicle v: vehicles) {
			int t_ind = vehicles.indexOf(v);
			
			if(s.nextTask.get(NT+t_ind) != null) {
				sumVehCost += (v.homeCity().distanceTo(s.nextTask.get(NT+t_ind).task.pickupCity))*v.costPerKm();
			}
			
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
				NewTask t = s.nextTask.get(NT+v_index);
				if(t != null) { // if vehicle has no initial task there is no plan to do
		
					
					City pickup_city = t.task.pickupCity;
					
					List<City> path_list = home_city.pathTo(pickup_city);
					
					for (City city : path_list) {
						v_plan.appendMove(city);
					}
					v_plan.appendPickup(t.task);
					while(t!=null) {
						
						// delivering the task
						City deliver_city = t.task.deliveryCity;
						path_list = pickup_city.pathTo(deliver_city);
						for (City city : path_list) {
							v_plan.appendMove(city);
						}
						v_plan.appendDelivery(t.task);
						// picking up the new task
						int pickUpT = t.isPickup ? 0:1;
						t = s.nextTask.get(tasks.indexOf(t.task)*2+(1+pickUpT));
						if(t!=null) {
							
							pickup_city = t.task.pickupCity;
							path_list = deliver_city.pathTo(pickup_city);
							for (City city : path_list) {
								v_plan.appendMove(city);
							}
							v_plan.appendPickup(t.task);
						}
					}
					// add plan to list
					plans.add(v_plan);
					System.out.print(v_plan);
				} else { // if no initial task then empty plan.
					
					plans.add(v_plan);
				}
			}
			
			return plans;
		}
	
}
