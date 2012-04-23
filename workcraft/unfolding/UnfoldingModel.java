package workcraft.unfolding;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.JPanel;

import workcraft.DuplicateIdException;
import workcraft.InvalidConnectionException;
import workcraft.Model;
import workcraft.ModelBase;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.common.DefaultSimControls;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.editor.EditorPane;

public class UnfoldingModel extends ModelBase {
	public static final UUID _modeluuid = UUID.fromString("23a72f18-5c90-11dc-8314-0800200c9a66");
	public static final String _displayname = "Unfolding with time annotation";

	public class SimThread extends Thread {
		public void run() {
			while (true) {
				try {
					sleep( (long)(100 / panelSimControls.getSpeedFactor()));
					simStep();
					server.execPython("_redraw()");
				} catch (InterruptedException e) { break; }
			}
		}
	}

	int t_name_cnt = 0;
	int p_name_cnt = 0;

	private int state = 0;
	private boolean loading;
	
	private boolean timingAttached = false;

	SimThread sim_thread = null;
	public static DefaultSimControls panelSimControls = null;

	LinkedList<EditableCondition> conditions;
	LinkedList<EditableEvent> events;
	LinkedList<DefaultConnection> connections;

	LinkedList<EditableEvent> readyTransitions;

	public UnfoldingModel() {
		conditions = new LinkedList<EditableCondition>();
		events = new LinkedList<EditableEvent>();
		connections = new LinkedList<DefaultConnection>();
		readyTransitions = new LinkedList<EditableEvent>();
	}

	public String getNextTransitionID() {
		return "e"+t_name_cnt++;
	}

	public String getNextPlaceID() {
		return "c"+p_name_cnt++;
	}

	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException {
		if (c instanceof EditableCondition) {
			EditableCondition p = (EditableCondition)c;
			conditions.add(p);
			p.setOwnerDocument(this);
			if (auto_name)
				for (;;) {
					try {
						p.setId(getNextPlaceID());
						break;
					} catch (DuplicateIdException e) {
					}
				}
		}
		else if (c instanceof EditableEvent) {
			EditableEvent t = (EditableEvent)c;
			events.add(t);
			t.setOwnerDocument(this);
			if (auto_name)
				for (;;) {
					try {
						t.setId(getNextTransitionID());
						break;
					} catch (DuplicateIdException e) {
					}
				}			
		} else throw new UnsupportedComponentException();
		
		super.addComponent(c, auto_name);
	}

	public void removeComponent(BasicEditable c) throws UnsupportedComponentException {
		super.removeComponent(c);
		if (c instanceof EditableCondition) {
			EditableCondition p = (EditableCondition)c;
			for (EditableEvent t : p.getIn()) {
				t.removeOut(p);				
			}
			for (EditableEvent t : p.getOut()) {
				t.removeIn(p);
			}

			conditions.remove(c);
		}

		else if (c instanceof EditableEvent) {
			EditableEvent t = (EditableEvent)c;
			for (EditableCondition p : t.getIn()) {
				t.removeOut(p);				
			}
			for (EditableCondition p : t.getOut()) {
				t.removeIn(p);			
			}

			events.remove(c);

		} else throw new UnsupportedComponentException();	
		
		super.removeComponent(c);
	}


	public EditableConnection createConnection(BasicEditable first, BasicEditable second) throws InvalidConnectionException {
		if (first == second)
			throw new InvalidConnectionException ("Can't connect to self!");

		if (!(
				(first instanceof EditableCondition && second instanceof EditableEvent) ||
				(first instanceof EditableEvent && second instanceof EditableCondition)
		)) throw new InvalidConnectionException ("Invalid connection (only place-transition and transition-place connections allowed)");
		EditableCondition p;
		EditableEvent t;
		if (first instanceof EditableCondition) {
			p = (EditableCondition)first;
			t = (EditableEvent)second;
			DefaultConnection con = new DefaultConnection(p, t);
			if (p.addOut(con) && t.addIn(con)) {
				connections.add(con);
				return con;
			}
		} else {
			p = (EditableCondition)second;
			t = (EditableEvent)first;
			DefaultConnection con = new DefaultConnection(t, p);
			if (p.addIn(con) && t.addOut(con)) {
				connections.add(con);
				return con;
			}
		}
		return null;
	}

	public void removeConnection(EditableConnection con) throws UnsupportedComponentException {
		EditableCondition p;
		EditableEvent t;
		if (con.getFirst() instanceof EditableCondition) {
			p = (EditableCondition)con.getFirst();
			t = (EditableEvent)con.getSecond();
			p.removeOut(t);
			t.removeIn(p);
			connections.remove(con);
			return;
		}
		if (con.getFirst() instanceof EditableEvent) {
			p = (EditableCondition)con.getSecond();
			t = (EditableEvent)con.getFirst();
			p.removeIn(t);
			t.removeOut(p);
			connections.remove(con);
			return;
		}
		throw new UnsupportedComponentException();
	}

	public void simReset() {
		for (EditableEvent t : events ) {
			t.canFire = false;
		}
		state = 0;
		readyTransitions.clear();
	}

	public void simBegin() {
		if (sim_thread==null) {
			simReset();
			sim_thread = new SimThread();
			sim_thread.start();
		}
	}

	public void simStep() {
		switch (state) {
		case 0:
			readyTransitions.clear();
			for (EditableEvent t : events) {
				LinkedList<EditableCondition> pl = t.getIn();

				if (pl.isEmpty())
					break;
				if (t.getOut().isEmpty())
					break;
				boolean canfire = true;
				for (EditableCondition p : pl) {
					if (p.getTokens()<1) {
						canfire = false;
						break;
					}
				}
				if (canfire)
				{
					t.canFire = true;
					readyTransitions.add(t);
				} else
					t.canFire = false;
			}
			state = 1;
			break;
		case 1:
			if (readyTransitions.isEmpty()) {
				state = 0;
				break;
			}

			if (panelSimControls.isUserInteractionEnabled()) {
				EditableEvent w = null;				
				for (EditableEvent t : readyTransitions) {
					if (t.canWork) {
						w = t;
						break;
					}
				}
				if (w!=null) {
					LinkedList<EditableCondition> in = w.getIn();
					LinkedList<EditableCondition> out = w.getOut();
					
					for (EditableCondition p : in) {
						p.setTokens(p.getTokens()-1);
					}
					for (EditableCondition p : out) {
						p.setTokens(p.getTokens()+1);
					}
					w.canFire = false;
					w.canWork = false;
					state = 0;
				}
			} else {
				int s = (int)Math.floor(Math.random()*readyTransitions.size());
				EditableEvent t = readyTransitions.get(s);
				LinkedList<EditableCondition> in = t.getIn();
				LinkedList<EditableCondition> out = t.getOut();
				
				for (EditableCondition p : in) {
					p.setTokens(p.getTokens()-1);
				}
				for (EditableCondition p : out) {
					p.setTokens(p.getTokens()+1);
				}


				t.canFire = false;	
				state = 0;
			}

			break;
		}

	}

	public boolean simIsRunning() {
		return (sim_thread != null);
	}

	public void simFinish() {
		if (sim_thread!=null) {
			sim_thread.interrupt();
			sim_thread = null;
		}
	}

	public List<EditableConnection> getConnections() {
		return (List<EditableConnection>)((List)connections);		
	}

	public void validate() {
	}

	public JPanel getSimulationControls() {
		if (panelSimControls == null) {
			panelSimControls = new DefaultSimControls(_modeluuid.toString());
		}
		return panelSimControls;
	}

	public EditorPane getEditor() {
		return editor;
	}

	public void setEditor(EditorPane editor) {
		this.editor = editor;
	}

	public WorkCraftServer getServer() {
		return server;
	}

	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	public boolean isLoading() {
		return loading;
	}

	public void getComponents(List<BasicEditable> out) {
		for (BasicEditable n: conditions)
			out.add(n);
		for (BasicEditable n: events)
			out.add(n);
	}

	public boolean isTimingAttached() {
		return timingAttached;
	}

	public void setTimingAttached(boolean timingAttached) {
		this.timingAttached = timingAttached;
	}
	
	public void calculateTiming() {
		
	}
}