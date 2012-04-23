package workcraft.unfolding;

import java.util.LinkedList;
import java.util.List;

public class Condition {
	protected EditableCondition c = null;
	protected int origPlaceN;
	protected String origPlaceId;
	protected Event presetEvent = null;
	protected List<Event> postset = new LinkedList<Event>();
};
