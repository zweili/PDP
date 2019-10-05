package template;

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

	private Random random;
	private Agent myAgent;
	
	
	//V values for each state
	private HashMap<State, Double> Vvalues = new HashMap<State, Double>();
	
	private HashMap<State,City> actions = new HashMap<State, City>();
	
	
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
		
		boolean betterValue = false;
		ArrayList<City> allTaskDestinations = new ArrayList<City>(topology.cities()); 
		
		//no task aka no taskDestination
		allTaskDestinations.add(null);
		
		
		while(betterValue) {
			
			for (City city: topology) {
				
				//all task destinations from city (remove city)
				ArrayList<City> taskDestinations = new ArrayList<City>(allTaskDestinations);
				taskDestinations.remove(city);
				
				//all Neighbors of city (available actions)
				ArrayList<City> neighbors = new ArrayList<City>(city.neighbors());
				
				for (City destination: taskDestinations) {
					
					State state = new State(city, destination);
					
					for(City action: neighbors) {
						
						double Q = reward(state, action, td, v);
						
						ArrayList<City> neighborsOfAction = new ArrayList<City>(action.neighbors());
						
						// all task destination from the city "action"
						ArrayList<City> taskNextDestinations = new ArrayList<City>(allTaskDestinations);
						taskNextDestinations.remove(action);
						
						for( City nextDestination : taskNextDestinations ) {
						
							State nextState = new State(action,nextDestination);
							
							Q += gamma*TransProba(state,action,nextState,td)*getVvalue(nextState);
							
						
						}
						
					}
					
					
					
					
					
				}
			}
		};
		
	}
	
	public double TransProba(State s, City a, State nextS,TaskDistribution td) {
		
		if( || s.currentCity.equals(nextS.currentCity)
				|| !nextS.currentCity.equals(a)) {
			return 0.0;
		} else {
			return td.probability(nextS.currentCity, nextS.taskDestination);
			}
		}
	
	public double getVvalue(State s) {
		return Vvalues.getOrDefault(s, 0.0);
	}
	
	
	

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			currentCity.
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}

		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}
