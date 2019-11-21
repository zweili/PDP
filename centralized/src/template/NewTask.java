package template;

import logist.task.Task;

public class NewTask {
	
	public Task	task;
	public boolean isPickup;
	
	
	public NewTask(Task task, boolean isPickup) {
		this.task = task;
		this.isPickup = isPickup;
	}

}
