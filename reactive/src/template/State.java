package template;

import logist.topology.Topology.City;

public class State {
	
	//if no task then taskDestination == null
	public City currentCity;
	public City taskDestination;
	
	public State(City city, City taskDestination) {
		this.currentCity = city;
		this.taskDestination = taskDestination;
	}
	

}
