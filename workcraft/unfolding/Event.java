package workcraft.unfolding;

public class Event {
	protected EditableEvent e = null;
	protected int number;
	protected int origTransN;
	protected String origTransId;
	
	public Event (int number, int origTransN) {
		this.number = number;
		this.origTransN = origTransN;
	}
}
