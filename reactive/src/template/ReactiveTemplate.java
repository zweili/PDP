package template;


import java.util.Collections;
import java.util.Random;
import java.util.HashMap;
import java.util.ArrayList;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Action.Delivery;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Agent myAgent;
	private int numActions = 0;
	
	//V values for each state
	private HashMap<State, Double> Vvalues = new HashMap<State, Double>();
		
	// action mappings from states
	private HashMap<State,City> bestActions = new HashMap<State, City>();
		
	// list of all states
	private ArrayList<State> allStates = new ArrayList<State>(); 
		
	// list of possible next actions
	private HashMap<State, ArrayList<City>> possibleActions = new HashMap<State, ArrayList<City>>();
	
	
	//reward function return expected reward from state to action.
	public double reward(State state, City action, TaskDistribution td, Vehicle v) {
		
		double taskReward = 0;
		
		if(state.taskDestination == action) {
			taskReward += td.reward(state.currentCity,action);
		} 
		
		double drivingCost = state.currentCity.distanceTo(action)*v.costPerKm();
		
		return taskReward - drivingCost;
	}
	
	//Compute the Optimal strategy for the reactive agent.
	public void computeStrategy(Topology topology, TaskDistribution td, Vehicle v, double gamma) {
		
		boolean betterValue = true;
		
		
		ArrayList<City> allCities = new ArrayList<City>(topology.cities()); 
		
	 
		for (City city: allCities) {
			
			//all task destinations from city (possible task destinations)
			ArrayList<City> taskDestinations = new ArrayList<City>(allCities);
			// except current city
			taskDestinations.remove(city);
			// and add no task destination (means no available task)
			taskDestinations.add(null);
			
			for (City destination: taskDestinations) {
				

				State state = new State(city, destination);
				// add state to the state list
				allStates.add(state);
				// initialize state V value to zero
				Vvalues.put(state, 0.0);
				
				// set possible actions for this state
				// if there is an available task, then you can go to neighboring cities (refuse task) or the task city by shortest path (means picking up the task)
				// if there are no tasks available, then you can only go to neighboring cities
				ArrayList<City> stateActions = new ArrayList<City>(city.neighbors());
				if(destination != null) { 
					if(!stateActions.contains(destination)) {
						stateActions.add(destination);	
					}		
				}
				possibleActions.put(state, stateActions);
				System.out.printf("current state actions: \n");
				System.out.print(stateActions);
				
				
			}
		}
		
		
		System.out.print("States initialized");
		
		int loop_n = 0;
		
		System.out.println("Initial V values: " + Vvalues + "\n");
		
		while(betterValue) {
			
			
			
			loop_n += 1;
			
			betterValue = false;
			
			for (State currstate : allStates) {
				
				
				ArrayList<City> stateActions = possibleActions.get(currstate);
				ArrayList<Double> Qs = new ArrayList<Double>();
				
				// debug
				//System.out.println("CurrState : " + currstate.currentCity + " " + currstate.taskDestination + "\n");
				//System.out.println("Actions : " + stateActions + "\n");
				
				for (City possibleAction : stateActions) {
					
					double Q = reward(currstate, possibleAction, td, v);
												
					// all task destination from the city "possibleAction"
					ArrayList<City> possibleDestinations = new ArrayList<City>(allCities);
					possibleDestinations.remove(possibleAction);
					// also possibility of no new task
					possibleDestinations.add(null);
						
					for( City nextDestination : possibleDestinations ) {
						
						//System.out.println("cp1 V values: " + Vvalues + "\n");
						System.out.printf("a" + possibleAction + "d" + nextDestination);
						State nextState = findState(Vvalues, possibleAction, nextDestination);
						System.out.println("contains" + allStates.contains(nextState));
						//System.out.println("Next state: " + nextState.currentCity + nextState.taskDestination + "\n");
						
						double V_ns = getVvalue(nextState);
						
						System.out.println("next state V value : " + V_ns + "\n");
							
						Q += gamma*TransProba(currstate,possibleAction,nextState,td)*V_ns;
						
						System.out.println("delta Q value : " + gamma*TransProba(currstate,possibleAction,nextState,td)*V_ns + "\n");
						
						//System.out.println("cp2 V values: " + Vvalues + "\n");
						
					}
					Qs.add(Q);		
				}
				
				
				
				double V = Collections.max(Qs);
				double OldV = Vvalues.put(currstate,V);
				
				System.out.println("Old V: " + OldV + " New V: " + V + "\n");
			
				
				
				if (Math.abs(V-OldV)>Math.abs(OldV)*0.01) {
					
					betterValue = true;
					
				}	
				bestActions.put(currstate, stateActions.get(Qs.indexOf(V)));
	
			}
			System.out.println("\n loop: " + loop_n);
	
		} 
	}
	
	public State findState(HashMap<State, Double> Vvalues, City c,City t) {
		String city;
		String dest;
		String stateCity;
		String stateDest;
		
		if ( c == null) {
			city = "NULL";
		} else {
			city = c.toString();
		}
		
		if (t == null) {
			dest = "NULL";
		} else {
			dest = t.toString();
		}
		
		for (State s: Vvalues.keySet()) {
			
			if (s.currentCity == null) {
				stateCity = "NULL";
			} else {
				stateCity = (String)s.currentCity.toString();
			}
			
			if (s.taskDestination == null) {
				stateDest = "NULL";
			} else {
				stateDest = s.taskDestination.toString();
				
			}
			
			System.out.println("findstate2" + stateCity + stateDest);
			if ((city.equals(stateCity)) && (dest.equals(stateDest))){
				System.out.printf("hello world");
				System.out.print(s);
				return s;
			}
		
			
		}
		
		return null;
	}
	
	public double TransProba(State s, City a, State nextS,TaskDistribution td) {
		
		return td.probability(a,nextS.taskDestination); 
		
		/* if( || s.currentCity.equals(nextS.currentCity)
				|| !nextS.currentCity.equals(a)) {
			return 0.0;
		} else {
			return td.probability(nextS.currentCity, nextS.taskDestination);
			} */
		}
	
	
	public double getVvalue(State s) {
		return Vvalues.getOrDefault(s, 0.0);
	}
	
	public City getBestAction(State s) {
		return bestActions.get(s);
	}
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		
		System.out.printf("running setup /n");
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.myAgent = agent;
		
		//get first vehicle
		Vehicle v = agent.vehicles().get(0);
		
		computeStrategy(topology,td,v,discount);
		System.out.printf("setup completed/n");
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		System.out.printf("running action \n");
		//get current state
		City currentCity = vehicle.getCurrentCity();
		
		if (availableTask != null) {
			
			System.out.printf("availableTask");
			
			State state = findState(Vvalues,currentCity,availableTask.deliveryCity);
			
			System.out.println("contains" + allStates.contains(state));
			//get best action of state
			City bestAction = getBestAction(state);
			
			if(bestAction.equals(availableTask.deliveryCity)) {
				action = new Pickup(availableTask);
			} else {
				action = new Move(bestAction);
			}
			
		} else {
			
			System.out.printf("availableTask_null");
			
			State state = findState(Vvalues,currentCity,null);

			System.out.println("contains" + allStates.contains(state));
			//get best action of state
			City bestAction = getBestAction(state);
			
			action = new Move(bestAction);
		}
		

		

		
		//print every 10 actions the total and the mean profit of the agent
		if ((numActions > 0) && (numActions % 10 == 0)) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}
