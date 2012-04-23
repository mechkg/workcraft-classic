package workcraft.chetri;

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

public class ChetriModel extends ModelBase{
	public static final UUID _modeluuid = UUID.fromString("e9137910-8535-11db-b606-0800200c9a66");
	public static final String _displayname = "Binary Petri Net (chEEtah's invention)";

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
	

	SimThread sim_thread = null;
	public static DefaultSimControls panelSimControls = null;

	LinkedList<EditableChetriPlace> places;
	LinkedList<EditableChetriTransition> transitions;
	LinkedList<DefaultConnection> connections;

	LinkedList<EditableChetriTransition> readyTransitions;

	public ChetriModel() {
		places = new LinkedList<EditableChetriPlace>();
		transitions = new LinkedList<EditableChetriTransition>();
		connections = new LinkedList<DefaultConnection>();
		readyTransitions = new LinkedList<EditableChetriTransition>();
	}

	public String getNextTransitionID() {
		return "t"+t_name_cnt++;
	}

	public String getNextPlaceID() {
		return "p"+p_name_cnt++;
	}

	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException {
		if (c instanceof EditableChetriPlace) {
			EditableChetriPlace p = (EditableChetriPlace)c;
			places.add(p);
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
		else if (c instanceof EditableChetriTransition) {
			EditableChetriTransition t = (EditableChetriTransition)c;
			transitions.add(t);
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
	}

	public void removeComponent(BasicEditable c) throws UnsupportedComponentException {
		super.removeComponent(c);
		if (c instanceof EditableChetriPlace) {
			EditableChetriPlace p = (EditableChetriPlace)c;
			for (EditableChetriTransition t : p.getIn()) {
				t.removeOut(p);				
			}
			for (EditableChetriTransition t : p.getOut()) {
				t.removeIn(p);
			}

			places.remove(c);
		}

		else if (c instanceof EditableChetriTransition) {
			EditableChetriTransition t = (EditableChetriTransition)c;
			for (EditableChetriPlace p : t.getIn()) {
				t.removeOut(p);				
			}
			for (EditableChetriPlace p : t.getOut()) {
				t.removeIn(p);			
			}

			transitions.remove(c);

		} else throw new UnsupportedComponentException();
	}


	public EditableConnection createConnection(BasicEditable first, BasicEditable second) throws InvalidConnectionException {
		if (first == second)
			throw new InvalidConnectionException ("Can't connect to self!");

		if (!(
				(first instanceof EditableChetriPlace && second instanceof EditableChetriTransition) ||
				(first instanceof EditableChetriTransition && second instanceof EditableChetriPlace)
		)) throw new InvalidConnectionException ("Invalid connection (only place-transition and transition-place connections allowed)");
		EditableChetriPlace p;
		EditableChetriTransition t;
		if (first instanceof EditableChetriPlace) {
			p = (EditableChetriPlace)first;
			t = (EditableChetriTransition)second;
			DefaultConnection con = new DefaultConnection(p, t);
			if (p.addOut(con) && t.addIn(con)) {
				connections.add(con);
				return con;
			}
		} else {
			p = (EditableChetriPlace)second;
			t = (EditableChetriTransition)first;
			DefaultConnection con = new DefaultConnection(t, p);
			if (p.addIn(con) && t.addOut(con)) {
				connections.add(con);
				return con;
			}
		}
		return null;
	}

	public void removeConnection(EditableConnection con) throws UnsupportedComponentException {
		EditableChetriPlace p;
		EditableChetriTransition t;
		if (con.getFirst() instanceof EditableChetriPlace) {
			p = (EditableChetriPlace)con.getFirst();
			t = (EditableChetriTransition)con.getSecond();
			p.removeOut(t);
			t.removeIn(p);
			connections.remove(con);
			return;
		}
		if (con.getFirst() instanceof EditableChetriTransition) {
			p = (EditableChetriPlace)con.getSecond();
			t = (EditableChetriTransition)con.getFirst();
			p.removeIn(t);
			t.removeOut(p);
			connections.remove(con);
			return;
		}
		throw new UnsupportedComponentException();
	}

	public void simReset() {
		for (EditableChetriTransition t : transitions ) {
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
			for (EditableChetriTransition t : transitions) {
				LinkedList<EditableChetriPlace> pl = t.getIn();

				if (pl.isEmpty())
					break;
				if (t.getOut().isEmpty())
					break;
				boolean canfire = true;
				if (t.token<1)
				{
					for (EditableChetriPlace p : pl) {
						if (p.getTokens()<1) {
							canfire = false;
							break;
						}
					}
				}
				else
				{
					for (EditableChetriPlace p : pl) {
						if (p.getTokens()>0) {
							canfire = false;
							break;
						}
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
				EditableChetriTransition w = null;				
				for (EditableChetriTransition t : readyTransitions) {
					if (t.canWork) {
						w = t;
						break;
					}
				}
				if (w!=null) {
					LinkedList<EditableChetriPlace> in = w.getIn();
					LinkedList<EditableChetriPlace> out = w.getOut();
					
					w.token = 1 - w.token;
					
					server.python.exec(w.script);
						
					for (EditableChetriPlace p : out) {
						p.setTokens(1 - p.getTokens());
					}

					w.canFire = false;
					w.canWork = false;
					state = 0;
				}
			} else {
				int s = (int)Math.floor(Math.random()*readyTransitions.size());
				EditableChetriTransition t = readyTransitions.get(s);
				LinkedList<EditableChetriPlace> in = t.getIn();
				LinkedList<EditableChetriPlace> out = t.getOut();
				
				t.token = 1 - t.token;
				
				for (EditableChetriPlace p : out) {
					p.setTokens(1 - p.getTokens());
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

	public void bind(WorkCraftServer server) {
		this.server = server;
		server.python.set("_document", this);
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
		for (BasicEditable n: places)
			out.add(n);
		for (BasicEditable n: transitions)
			out.add(n);
	}

}