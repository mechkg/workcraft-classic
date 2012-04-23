package workcraft.gate;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import workcraft.DuplicateIdException;
import workcraft.InvalidConnectionException;
import workcraft.ModelBase;
import workcraft.ModelValidationException;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.common.DefaultSimControls;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.editor.EditorPane;
import workcraft.petri.EditablePetriPlace;
import workcraft.petri.EditablePetriTransition;
import workcraft.petri.PetriModel.SimThread;
import workcraft.util.Colorf;

public class GateModel extends ModelBase {
	public static final UUID _modeluuid = UUID.fromString("6f704a28-e691-11db-8314-0800200c9a66");
	public static final String _displayname = "Gate-level circuit";

	private EditorPane editor = null; 
	private int name_cnt = 0;
	private int input_cnt = 0;
	private int env_cnt = 0;
	private int output_cnt = 0;
	private int contact_cnt = 0;
	
	private static Colorf connectionColor = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);

//	static float rotator = 0.0f; 

	LinkedList<BasicGate> gates;
	LinkedList<BasicGate> excitedGates;
	LinkedList<DefaultConnection> connections;

	protected boolean allowNextStep = false;
	LinkedList <EnvironmentBox> environments = new LinkedList<EnvironmentBox>();


	public void restoreState(Object state) {
		Hashtable<String, Integer> marking = (Hashtable<String, Integer>)state;
		for (DefaultConnection c : connections) {
			GateContact c1 = (GateContact) c.getFirst();
			GateContact c2 = (GateContact) c.getSecond();               
			Integer n = marking.get(c1.getId());
			if (n==null)
				JOptionPane.showMessageDialog(null, "Model structure inconsistent with state data: node "+c1.getId() +" does not exist.", "Warning", JOptionPane.ERROR_MESSAGE);
			else {
				GateContact.StateType res = GateContact.StateType.values()[n];
				if(c1.getIOType()==GateContact.IOType.input) {
					c1.setState(res);
				}
				else if(c1.getParentGate() instanceof Joint) {
					Joint j = (Joint) c1.getParentGate();
					j.setOutState(res);
				}
			}
			n = marking.get(c2.getId());
			if (n==null)
				JOptionPane.showMessageDialog(null, "Model structure inconsistent with state data: node "+c2.getId() +" does not exist.", "Warning", JOptionPane.ERROR_MESSAGE);
			else {
				GateContact.StateType res = GateContact.StateType.values()[n];
				if(c2.getIOType()==GateContact.IOType.input) {
					c2.setState(res);
				}
				else if(c2.getParentGate() instanceof Joint) {
					Joint j = (Joint) c2.getParentGate();
					j.setOutState(res);
				}
			}
		}
		for(BasicGate g : gates) {
			if(!(g instanceof Input || g instanceof Output))
				continue;
			Integer n = marking.get(g.getId());
			if (n==null)
				JOptionPane.showMessageDialog(null, "Model structure inconsistent with state data: node "+g.getId() +" does not exist.", "Warning", JOptionPane.ERROR_MESSAGE);
			else {
				if(g instanceof Input)
					((Input)g).setState(n);
				else if(g instanceof Output)
					((Output)g).setState(n);
			}
		}
	}

	@Override
	public Object saveState() {
		Hashtable<String, Integer> marking = new Hashtable<String, Integer>();
		for (DefaultConnection c : connections) {
			GateContact c1 = (GateContact) c.getFirst();
			GateContact c2 = (GateContact) c.getSecond();               
			if(c1!=null && !marking.containsKey(c1.getId()))
				marking.put(c1.getId(), c1.getWireState().ordinal());
			if(c2!=null && !marking.containsKey(c2.getId()))
				marking.put(c2.getId(), c2.getWireState().ordinal());
		}
		for(BasicGate g : gates) {
			if(g instanceof Input)
				marking.put(g.getId(), ((Input)g).getState());
			else if(g instanceof Output)
				marking.put(g.getId(), ((Output)g).getState());
		}
		return marking;
	}

	public class SimThread extends Thread {
		HashSet<BasicGate> enabled = new HashSet<BasicGate>();
		List<String> traceEvents = null;
		Iterator<String> iter = null;
		private void spreadSignals() {
			for(BasicGate g :  gates) {
				g.reset();
			}
			boolean ch;
			do {
				ch = false;
				for(DefaultConnection c : connections) {
					GateContact c1 = (GateContact) c.getFirst();
					GateContact c2 = (GateContact) c.getSecond();
					GateContact.StateType res =
						(c1.getWireState()==GateContact.StateType.set || c2.getWireState()==GateContact.StateType.set)?
								GateContact.StateType.set:GateContact.StateType.reset;
					if(c1.getIOType()==GateContact.IOType.input) {
						c1.setState(res);
					}
					else if(c1.getParentGate() instanceof Joint) {
						Joint j = (Joint) c1.getParentGate();
						j.setOutState(res);
					}
					if(c2.getIOType()==GateContact.IOType.input) {
						c2.setState(res);
					}
					else if(c2.getParentGate() instanceof Joint) {
						Joint j = (Joint) c2.getParentGate();
						j.setOutState(res);
					}
				}
				for(BasicGate g : gates)
					if(g instanceof Joint) {
						Joint j = (Joint) g;
						ch |= j.refreshJoint();
					}
			} while(ch);
		}

		public void run() {
			if (panelSimControls.isTraceReplayEnabled()) {
				traceEvents = panelSimControls.getTrace().getEvents();
				iter = traceEvents.iterator();
			}

			SimLoop: while (true) {
				try {
					// check enabled gates
					enabled.clear();
					for(BasicGate g :  gates) {
						if(g instanceof Joint) {
							continue;
						}
						g.refresh();
						if(g.canFire)
							enabled.add(g);
					}

					if (!panelSimControls.isUserInteractionEnabled())				
						sleep( (long)(100 / panelSimControls.getSpeedFactor()));
					else
						sleep(30);

					if (panelSimControls.isStepByStepEnabled())
						if (!allowNextStep)
							continue;

					// select gate to fire
					BasicGate chosen = null;	

					if (panelSimControls.isUserInteractionEnabled()) {
						for (BasicGate g : enabled)
							if (g.canWork) {
								chosen = g;
								break;
							}
					} else {
						if (panelSimControls.isTraceReplayEnabled()) {
							if (iter.hasNext()) {
								String nextId = iter.next();
								for (BasicGate g : gates) {
									if (g.getId().equals(nextId) && (g.canFire || g instanceof Input)) {
										chosen = g;
										break;
									}
								}
								if (chosen == null) {
									JOptionPane.showMessageDialog(null, "Gate which is listed in the trace, "+nextId+", is not present in the model or not enabled at this point. Replay terminated.", "End of replay", JOptionPane.WARNING_MESSAGE);
									break SimLoop;
								}
							} else {
								JOptionPane.showMessageDialog(null, "Replay has finished.", "End of replay", JOptionPane.INFORMATION_MESSAGE);
								break SimLoop;
							}
						} else if(!enabled.isEmpty()) {
							int s = (int)Math.floor(Math.random()*enabled.size());
							chosen = (BasicGate)(enabled.toArray()[s]);
						}
					}

					if (chosen!=null) {
						if(chosen instanceof Input) {
							((Input)chosen).switchState();
							chosen.refresh();
						}
						else
							chosen.fire();
						if (panelSimControls.isTraceWriteEnabled())
							panelSimControls.addTraceEvent(chosen.getId());
						spreadSignals();
						allowNextStep = false;
						server.execPython("_redraw()");
					} else {
						if (panelSimControls.isTraceReplayEnabled()) {
							if (iter.hasNext())
								JOptionPane.showMessageDialog(null, "The net is dead: no gate can fire.", "Simulation stopped", JOptionPane.INFORMATION_MESSAGE);
							else
								JOptionPane.showMessageDialog(null, "Replay has finished.", "End of replay", JOptionPane.INFORMATION_MESSAGE);
							break SimLoop;
						}
						else {
							spreadSignals();
							allowNextStep = false;
							server.execPython("_redraw()");
						}
					}
				} catch (InterruptedException e) { break SimLoop; }
			}

			simReset();
		}
	}

	public void addTraceEvent(String id) {
		if (panelSimControls.isTraceWriteEnabled())
			panelSimControls.addTraceEvent(id);
	}

	SimThread sim_thread = null;
	public static DefaultSimControls panelSimControls = null;

	public GateModel() {
		gates = new LinkedList<BasicGate>();
		excitedGates = new LinkedList<BasicGate>();
		connections = new LinkedList<DefaultConnection>();
	}

	public void addComponent(BasicEditable c, boolean auto_name)
	throws UnsupportedComponentException {
		if(c instanceof Input) {
			BasicGate gate = (BasicGate)c;
			gates.add(gate);
			gate.setOwnerDocument(this);
			if (auto_name)
				for (;;) {
					try {
						gate.setId("input"+input_cnt++);
						break;
					} catch (DuplicateIdException e) {
					}
				}
		}
		else 	if(c instanceof Output) {
			BasicGate gate = (BasicGate)c;
			gates.add(gate);
			gate.setOwnerDocument(this);
			if (auto_name)
				for (;;) {
					try {
						gate.setId("output"+output_cnt++);
						break;
					} catch (DuplicateIdException e) {
					}
				}
		}
		else if(c instanceof BasicGate) {
			BasicGate gate = (BasicGate)c;
			gates.add(gate);
			gate.setOwnerDocument(this);
			if (auto_name)
				for (;;) {
					try {
						gate.setId("g"+name_cnt++);
						break;
					} catch (DuplicateIdException e) {
					}
				}
		}
		else if(c instanceof GateContact) {
			GateContact gate = (GateContact)c;
			gate.setOwnerDocument(this);
			if (auto_name)
				for (;;) {
					try {
						gate.setId("con"+contact_cnt++);
						break;
					} catch (DuplicateIdException e) {
					}
				}
		}
		else if (c instanceof EnvironmentBox) {
			EnvironmentBox env = (EnvironmentBox)c;
			environments.add(env);
			env.setOwnerDocument(this);
			if (auto_name)
				for (;;) {
					try {
						env.setId("env"+env_cnt++);
						break;
					} catch (DuplicateIdException e) {
					}
				}
		}
		else throw new UnsupportedComponentException();

		super.addComponent(c, auto_name);
	}

	public EditableConnection createConnection(BasicEditable first,
			BasicEditable second) throws InvalidConnectionException {
		GateContact cfirst = null;
		GateContact csecond = null;

		if(first instanceof GateContact) {
			cfirst = (GateContact) first;
		}
		else {
			BasicGate gate = (BasicGate) first;
			try {
				cfirst = gate.getFreeOutput();
			}
			catch(UnsupportedComponentException err) {
				throw new InvalidConnectionException ("Unsupported component exception!");
			}
		}

		if(second instanceof GateContact) {
			csecond = (GateContact) second;
		}
		else {
			BasicGate gate = (BasicGate) second;
			try {
				csecond = gate.getFreeInput();
			}
			catch(UnsupportedComponentException err) {
				throw new InvalidConnectionException ("Unsupported component exception!");
			}
		}

		if(cfirst==null || csecond==null)
			throw new InvalidConnectionException ("Contacts are full!");
		if (cfirst == csecond)
			throw new InvalidConnectionException ("Can't connect to self!");
		if(cfirst.getParentGate()==csecond.getParentGate() && cfirst.getIOType()==csecond.getIOType()
				&& (cfirst.connections.size()>0 || csecond.connections.size()>0)) {
			EditableConnection con1 = (cfirst.connections.size()>0)?cfirst.connections.getFirst():null;
			EditableConnection con2 = (csecond.connections.size()>0)?csecond.connections.getFirst():null;
			cfirst.connections.clear();
			if(con2!=null) {
				cfirst.connections.add(con2);
				if(con2.getFirst()==csecond)
					con2.setFirst(cfirst);
				else
					con2.setSecond(cfirst);
			}
			csecond.connections.clear();
			if(con1!=null) {
				csecond.connections.add(con1);
				if(con1.getFirst()==cfirst)
					con1.setFirst(csecond);
				else
					con1.setSecond(csecond);
			}
			return null;
		}

		if(cfirst.connections.size()>0 || csecond.connections.size()>0)
			throw new InvalidConnectionException ("Contact is already bound!");

		DefaultConnection con = new DefaultConnection(cfirst, csecond);
		con.connectionType = DefaultConnection.Type.polylineConnection;
		con.drawArrow = false;
		connections.add(con);
		cfirst.connections.add(con);
		csecond.connections.add(con);
		return con;
	}

	public void removeComponent(BasicEditable c)
	throws UnsupportedComponentException {
		if(c instanceof GateContact) {
			c.selected = false;
			if(c.connections.isEmpty())
				return;
			EditableConnection con = c.connections.getFirst();
			con.deselect();
			con.getSecond().removeFromConnections(con);
			con.getFirst().removeFromConnections(con);
			removeConnection(con);
		} else if (c instanceof BasicGate) {
			BasicGate gate = (BasicGate) c;
			gate.disposeContacts();
			gates.remove(gate);
		} else if (c instanceof EnvironmentBox) {
			environments.remove(c);
		}
		
		super.removeComponent(c);
	}

	public void removeConnection(EditableConnection con)
	throws UnsupportedComponentException {
		GateContact c = (GateContact) con.getFirst();
		c.getParentGate().removeContact(c);
		c = (GateContact) con.getSecond();
		c.getParentGate().removeContact(c);
		connections.remove(con);
	}


	public void getComponents(List<BasicEditable> out) {
		for (BasicEditable n: gates)
			out.add(n);
	}

	public List<EditableConnection> getConnections() {
		return (List<EditableConnection>)((List)connections);	
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

	public JPanel getSimulationControls() {
		if (panelSimControls == null) {
			panelSimControls = new DefaultSimControls(_modeluuid.toString());
		}
		return panelSimControls;
	}

	public void prepareMap() throws ModelValidationException {

		ModelValidationException err =  new ModelValidationException();
		for(BasicGate g : gates) {
			for(GateContact c : g.in) {
				c.srcContact = null;
			}
			if(g instanceof Joint) {
				Joint j = (Joint) g;
				j.srcContact = null;
			}
		}
		LinkedList<DefaultConnection> lstcon = (LinkedList<DefaultConnection>) connections.clone();
		boolean req = false;
		boolean chg = false;
		do {
			req = false;
			chg = false;
			LinkedList<DefaultConnection> lstcon_copy = (LinkedList<DefaultConnection>) lstcon.clone();
			for(DefaultConnection con : lstcon_copy) {
				GateContact c1 = (GateContact) con.getFirst();
				Joint j1 = (c1.getParentGate() instanceof Joint)?(Joint)c1.getParentGate():null;
				GateContact c2 = (GateContact) con.getSecond();
				Joint j2 = (c2.getParentGate() instanceof Joint)?(Joint)c2.getParentGate():null;
				if(j1==null && j2==null) {
					if(c1.getIOType()==c2.getIOType()) {
						lstcon.remove(con);
					}
					else if(c1.getIOType()==GateContact.IOType.output) {
						c2.srcContact = c1;
						chg = true;
						lstcon.remove(con);
					}
					else if(c2.getIOType()==GateContact.IOType.output) {
						c1.srcContact = c2;
						chg = true;
						lstcon.remove(con);
					}
				}
				else if(j1==null) {
					if(c1.getIOType()==GateContact.IOType.output) {
						if(j2.srcContact!=null && j2.srcContact!=c1) {
							err.addError("["+j2.getId()+"] Implicit joint-ORs not allowed. Use OR gate instead.");
							throw err;
						}
						j2.srcContact = c1;
						chg = true;
						lstcon.remove(con);
					}
					else {
						if(j2.srcContact!=null) {
							c1.srcContact = j2.srcContact;
							chg = true;
							lstcon.remove(con);
						}
						else {
							req = true;
						}
					}
				}
				else if(j2==null) {
					if(c2.getIOType()==GateContact.IOType.output) {
						if(j1.srcContact!=null && j1.srcContact!=c2) {
							err.addError("["+j1.getId()+"] Implicit joint-ORs not allowed. Use OR gate instead.");
							throw err;
						}
						j1.srcContact = c2;
						chg = true;
						lstcon.remove(con);
					}
					else {
						if(j1.srcContact!=null) {
							c2.srcContact = j1.srcContact;
							chg = true;
							lstcon.remove(con);
						}
						else {
							req = true;
						}
					}
				}
				else {
					if(j1.srcContact!=null && j2.srcContact!=null && j1.srcContact!=j2.srcContact ) {
						err.addError("["+j1.getId()+","+j2.getId()+"] Implicit joint-ORs not allowed. Use OR gate instead.");
						throw err;
					}
					else if(j1.srcContact!=null) {
						j2.srcContact = j1.srcContact;
						chg = true;
						lstcon.remove(con);
					}
					else if(j2.srcContact!=null) {
						j1.srcContact = j2.srcContact;
						chg = true;
						lstcon.remove(con);
					}
					else {
						req = true;
					}
				}
			}
		} while(req && chg);
	}

	public void simBegin() {
		if (sim_thread==null) {
			simReset();
			sim_thread = new SimThread();
			sim_thread.start();
		}
		if (panelSimControls.isTraceWriteEnabled())
			panelSimControls.clearTrace();
	}

	public void simFinish() {
		if (sim_thread!=null) {
			sim_thread.interrupt();
			sim_thread = null;
		}
	}

	public boolean simIsRunning() {
		return (sim_thread != null);
	}

	public void simReset() {
		for(BasicGate g : gates) {
			g.canFire = false;
			g.canWork = false;
			g.lostSignal = false;
		}
	}

	public void simStep() {
		allowNextStep = true;
	}

	@Override
	public void getGuideNodes(List<BasicEditable> out) {
		for (BasicGate g : gates) {
			for (GateContact gc : g.getInputContacts())
				out.add(gc);
			for (GateContact gc : g.getOutputContacts())
				out.add(gc);
		}
	}

	public void validate() throws ModelValidationException {
	}

	public void setActiveInterface(EnvironmentBox e) {
		for (EnvironmentBox ee : environments)
		{
			if (ee == e) continue;
			ee.setActive(false);
		}
	}

	public String getActiveInterfacePath() {
		for (EnvironmentBox e : environments)
			if (e.isActive())
				return e.getEnvironmentStg();
		return "";
	}
}