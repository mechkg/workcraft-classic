package workcraft.petri;

import java.util.HashMap;
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
import workcraft.Model;
import workcraft.ModelBase;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.common.DefaultSimControls;
import workcraft.editor.BasicEditable;
import workcraft.editor.BoundingBox;
import workcraft.editor.EditableConnection;
import workcraft.editor.EditorPane;
import workcraft.util.Vec2;

public class PetriModel extends ModelBase {
	@Override
	public void restoreState(Object state) {
		Hashtable<String, Integer> marking = (Hashtable<String, Integer>)state;
		for (EditablePetriPlace p : places) {
			Integer t = marking.get(p.getId());
			if (t==null)
				JOptionPane.showMessageDialog(null, "Model structure inconsistent with state data: node "+p.getId() +" does not exist.", "Warning", JOptionPane.ERROR_MESSAGE);
			else
				p.setTokens(t);
		}
	}

	@Override
	public Object saveState() {
		Hashtable<String, Integer> marking = new Hashtable<String, Integer>();
		for (EditablePetriPlace p : places)
			marking.put(p.getId(), p.getTokens());
		return marking;
	}

	public static final UUID _modeluuid = UUID.fromString("65f89260-641d-11db-bd13-0800200c9a66");
	public static final String _displayname = "Petri Net";

	protected boolean allowNextStep = false;

	public class SimThread extends Thread {
		HashSet<EditablePetriTransition> enabled = new HashSet<EditablePetriTransition>();
		HashSet<EditablePetriTransition> affected = new HashSet<EditablePetriTransition>();
		List<String> traceEvents = null;
		Iterator<String> iter = null;

		void check(EditablePetriTransition t) {
			LinkedList<EditablePetriPlace> pl = t.getIn();
			
			/*
			 * SG06102008
			 * transition with no input places is always activated 
			 */
			//if (pl.isEmpty())
			//	return;

			boolean canfire = true;
			for (EditablePetriPlace p : pl) {
				if (p.getTokens()<1) {
					canfire = false;
					break;
				}
			}
			if (canfire) {
				t.canFire = true;
				enabled.add(t);
			} else {
				t.canFire = false;
				enabled.remove(t);
			}
		}

		void fire(EditablePetriTransition t) {
			affected.clear();

			for (EditablePetriPlace p : t.getIn()) {
				p.setTokens(p.getTokens()-1);
				for (EditablePetriTransition ot : p.getOut())
					affected.add(ot);
			}

			for (EditablePetriPlace p : t.getOut()) {
				p.setTokens(p.getTokens()+1);
				for (EditablePetriTransition ot : p.getOut())
					affected.add(ot);
			}

			for (EditablePetriTransition at : affected)
				check(at);

			if (panelSimControls.isTraceWriteEnabled())
				panelSimControls.addTraceEvent(t.getId());

			t.canWork = false;
		}

		public void run() {
			enabled.clear();

			for (EditablePetriTransition t: transitions)
				check(t);

			if (panelSimControls.isTraceReplayEnabled()) {
				traceEvents = panelSimControls.getTrace().getEvents();
				iter = traceEvents.iterator();
			}

			while (true) {
				try {
					if (!panelSimControls.isUserInteractionEnabled())				
						sleep( (long)(100 / panelSimControls.getSpeedFactor()));
					else
						sleep(30);

					if (panelSimControls.isStepByStepEnabled())
						if (!allowNextStep)
							continue;

					if (!enabled.isEmpty()) {		
						EditablePetriTransition chosen = null;	

						if (panelSimControls.isUserInteractionEnabled()) {

							for (EditablePetriTransition t : enabled)
								if (t.canWork) {
									chosen = t;
									break;
								}
						} else {
							if (panelSimControls.isTraceReplayEnabled()) {
								if (iter.hasNext()) {
									String nextId = iter.next();
									for (EditablePetriTransition t : enabled) {

										if (t.getId().equals(nextId)) {
											chosen = t;
											break;
										}

									}

									if (chosen == null) {
										JOptionPane.showMessageDialog(null, "Transition which is listed in the trace, "+nextId+", is not present in the model or not enabled at this point. Replay terminated.", "End of replay", JOptionPane.WARNING_MESSAGE);
										break;
									}
								} else {
									JOptionPane.showMessageDialog(null, "Replay has finished.", "End of replay", JOptionPane.INFORMATION_MESSAGE);
									break;
								}
							} else {
								int s = (int)Math.floor(Math.random()*enabled.size());
								chosen = (EditablePetriTransition)(enabled.toArray()[s]);
							}
						}

						if (chosen!=null) {
							fire(chosen);
							allowNextStep = false;
							server.execPython("_redraw()");
						}
					} else {
						if (panelSimControls.isTraceReplayEnabled())
							if (iter.hasNext())
								JOptionPane.showMessageDialog(null, "The net is dead: no transition can fire.", "Simulation stopped", JOptionPane.INFORMATION_MESSAGE);
							else
								JOptionPane.showMessageDialog(null, "Replay has finished.", "End of replay", JOptionPane.INFORMATION_MESSAGE);

						break;
					}
				} catch (InterruptedException e) { break; }
			}

			for (EditablePetriTransition t: transitions) {
				t.canWork = false;
				t.canFire = false;
			}
		}
	}


	int t_name_cnt = 0;
	int p_name_cnt = 0;

	private int state = 0;
	private boolean loading;

	SimThread sim_thread = null;
	public static DefaultSimControls panelSimControls = null;

	protected LinkedList<EditablePetriPlace> places;
	protected LinkedList<EditablePetriTransition> transitions;
	protected LinkedList<DefaultConnection> connections;
	private Boolean shorthandNotation = false;

	public PetriModel() {
		places = new LinkedList<EditablePetriPlace>();
		transitions = new LinkedList<EditablePetriTransition>();
		connections = new LinkedList<DefaultConnection>();
	}

	public String getNextTransitionID() {
		return "t"+t_name_cnt++;
	}

	public String getNextPlaceID() {
		return "p"+p_name_cnt++;
	}

	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException {
		if (c instanceof EditablePetriPlace) {
			EditablePetriPlace p = (EditablePetriPlace)c;
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
		else if (c instanceof EditablePetriTransition) {
			EditablePetriTransition t = (EditablePetriTransition)c;
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

		super.addComponent(c, auto_name);
	}

	public void removeComponent(BasicEditable c) throws UnsupportedComponentException {
		super.removeComponent(c);
		if (c instanceof EditablePetriPlace) {
			EditablePetriPlace p = (EditablePetriPlace)c;
			for (EditablePetriTransition t : p.getIn()) {
				t.removeOut(p);				
			}
			for (EditablePetriTransition t : p.getOut()) {
				t.removeIn(p);
			}

			places.remove(c);
		}

		else if (c instanceof EditablePetriTransition) {
			EditablePetriTransition t = (EditablePetriTransition)c;
			for (EditablePetriPlace p : t.getIn()) {
				t.removeOut(p);				
			}
			for (EditablePetriPlace p : t.getOut()) {
				t.removeIn(p);			
			}

			transitions.remove(c);

		} else throw new UnsupportedComponentException();

		super.removeComponent(c);
	}


	public EditableConnection createConnection(BasicEditable first, BasicEditable second) throws InvalidConnectionException {
		if (first == second)
			throw new InvalidConnectionException ("Can't connect to self!");

		if (first instanceof EditablePetriTransition && second instanceof EditablePetriTransition) {
			try {
				EditablePetriPlace p = new EditablePetriPlace(getRoot());

				Vec2 v1 = first.transform.getTranslation2d();
				v1.add(second.transform.getTranslation2d());
				v1.mul(0.5f);
				p.transform.translateAbs(v1);
				p.setTiny(true);

				DefaultConnection con = new DefaultConnection(first, p);
				if (p.addIn(con) && ((EditablePetriTransition)first).addOut(con)) {
					connections.add(con);
				}

				con = new DefaultConnection(p, second);
				if (p.addOut(con) && ((EditablePetriTransition)second).addIn(con)) {
					connections.add(con);
					return con;
				}
			} catch (UnsupportedComponentException e) {
				e.printStackTrace();
				return null;
			}
		} else if (first instanceof EditablePetriPlace && second instanceof EditablePetriPlace) {

			try {
				EditablePetriTransition t = new EditablePetriTransition(getRoot());
				Vec2 v1 = first.transform.getTranslation2d();
				v1.add(second.transform.getTranslation2d());
				v1.mul(0.5f);
				t.transform.translateAbs(v1);				

				DefaultConnection con = new DefaultConnection(first, t);
				if (t.addIn(con) && ((EditablePetriPlace)first).addOut(con)) {
					connections.add(con);
				}

				con = new DefaultConnection(t, second);
				if (t.addOut(con) && ((EditablePetriPlace)second).addIn(con)) {
					connections.add(con);
					return con;
				}
			} catch (UnsupportedComponentException e) {
				e.printStackTrace();
				return null;

			}
		} else {
			EditablePetriPlace p;
			EditablePetriTransition t;
			if (first instanceof EditablePetriPlace) {
				p = (EditablePetriPlace)first;
				t = (EditablePetriTransition)second;
				DefaultConnection con = new DefaultConnection(p, t);
				if (p.addOut(con) && t.addIn(con)) {
					connections.add(con);
					return con;
				}
			} else {
				p = (EditablePetriPlace)second;
				t = (EditablePetriTransition)first;
				DefaultConnection con = new DefaultConnection(t, p);
				if (p.addIn(con) && t.addOut(con)) {
					connections.add(con);
					return con;
				}
			}
		}
		return null;	
	}

	public void removeConnection(EditableConnection con) throws UnsupportedComponentException {
		EditablePetriPlace p;
		EditablePetriTransition t;
		if (con.getFirst() instanceof EditablePetriPlace) {
			p = (EditablePetriPlace)con.getFirst();
			t = (EditablePetriTransition)con.getSecond();
			p.removeOut(t);
			t.removeIn(p);
			connections.remove(con);
			return;
		}
		if (con.getFirst() instanceof EditablePetriTransition) {
			p = (EditablePetriPlace)con.getSecond();
			t = (EditablePetriTransition)con.getFirst();
			p.removeIn(t);
			t.removeOut(p);
			connections.remove(con);
			return;
		}
		throw new UnsupportedComponentException();
	}

	public void simReset() {

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

	public void simStep() {
		allowNextStep = true;
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

	public List<String>getEditableProperties()  {
		
		List<String> list = super.getEditableProperties(); 
		list.add("bool,Shorthand notation,getShorthandNotation,setShorthandNotation");
		
		return list;
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

//	SG10022008 : loading is defined in parent, no need to use others
//	public void setLoading(boolean loading) {
//		this.loading = loading;
//	}

//	public boolean isLoading() {
//		return loading;
//	}

	public void getComponents(List<BasicEditable> out) {
		for (BasicEditable n: places)
			out.add(n);
		for (BasicEditable n: transitions)
			out.add(n);
	}


	public void getTransitions(List<EditablePetriTransition> out) {
		for (EditablePetriTransition n: transitions)
			out.add(n);
	}

	public void getPlaces(List<EditablePetriPlace> out) {
		for (EditablePetriPlace n: places)
			out.add(n);
	}
	
	protected boolean isPairUsed(EditablePetriTransition T1, EditablePetriTransition T2,  HashMap<EditablePetriTransition, LinkedList<EditablePetriTransition>> used_pairs) {
		LinkedList<EditablePetriTransition> lst = null;
		lst = used_pairs.get(T1);
		if(lst!=null && lst.contains(T2))
			return true;
		lst = used_pairs.get(T2);
		if(lst!=null && lst.contains(T1))
			return true;
		return false;
	}

	protected String addUsedPair(EditablePetriTransition T1, EditablePetriTransition T2,  HashMap<EditablePetriTransition, LinkedList<EditablePetriTransition>>used_pairs) {
		//	if(output_formula!="")
		//	output_formula += "|";
		//		output_formula += "("+T1.getId()+","+T2.getId()+")";

		String out = "";

		boolean f = false;
		LinkedList<EditablePetriPlace> plst = T1.getIn();
		for(EditablePetriPlace p : plst) {
			out += (f?"&":"")+p.getId();
			f = true;
		}
		for(EditablePetriPlace p : T2.getIn()) {
			if(plst.contains(p))
				continue;
			out += (f?"&":"")+p.getId();
			f = true;
		}

		LinkedList<EditablePetriTransition> lst = null;
		lst = used_pairs.get(T1);
		if(lst==null) {
			lst = new LinkedList<EditablePetriTransition>();
			used_pairs.put(T1, lst);
		}
		lst.add(T2);

		return out;
	}

	public List<String> buildSemimodularityCheckClauses() {
		HashMap<EditablePetriTransition, LinkedList<EditablePetriTransition>> used_pairs = new HashMap<EditablePetriTransition, LinkedList<EditablePetriTransition>>();
		LinkedList<String> formulaClauses = new LinkedList<String>(); 

		for(EditablePetriPlace P : places) {
			LinkedList<EditablePetriTransition> tlst = P.getOut();
			for(int i=0; i<tlst.size(); i++) {
				EditablePetriTransition T1 = tlst.get(i);
				for(int j=i+1; j<tlst.size(); j++) {
					EditablePetriTransition T2 = tlst.get(j);
					if (T1.getCustomProperty("interface")!=null && T2.getCustomProperty("interface")!=null) {
						continue;
					}					
					
					if(isPairUsed(T1, T2, used_pairs))
						continue;
					if(T1.getOut().contains(P) && T2.getOut().contains(P))
						continue;
					
					String name1 = T1.getId();
					
					int k;
					
					if (Character.isDigit((name1.charAt(name1.length()-1)))) {
						k = name1.length()-1;
						while (Character.isDigit((name1.charAt(k))) && k > 0) k--;
						name1 = name1.substring(0,k+1);
					}
					
					String name2 = T2.getId();
					
					

					if (Character.isDigit((name2.charAt(name2.length()-1)))) {
						k = name2.length()-1;
						while (Character.isDigit((name2.charAt(k))) && k > 0) k--;
						name2 = name2.substring(0,k+1);
					}					
					
					if (name1.equals(name2))
						continue;
					
					// System.out.println (name1+ ","+name2);
					
					// !!!! ADDED BY CHEETAH TO EXCLUDE PAIRS OF TRANSITIONS FROM THE SAME MULTI-OUTPUT GATE
					if (name1.indexOf("_sig_") > 1 && name2.indexOf("_sig_") > 1)
					{
						String z1 = name1, z2 = name2;
						
						z1 = z1.substring(0, z1.indexOf("_sig_"));
						z2 = z2.substring(0, z2.indexOf("_sig_"));
						
						char l1 = z1.charAt(z1.length() - 1);
						char l2 = z2.charAt(z2.length() - 1);
						
						if (l1 >= 'A' && l1 <= 'Z' && l2 >= 'A' && l2 <= 'Z')
						{
							z1 = z1.substring(0, z1.length() - 1);
							z2 = z2.substring(0, z2.length() - 1);
							
							l1 = z1.charAt(z1.length() - 1);
							l2 = z2.charAt(z2.length() - 1);
							
							if (l1 == '_' && z1.equals(z2))
							{
								// System.out.println ("Skipped as a multioutput pair!");
								continue;
							}
						}
					}
					
					//System.out.println(P.getId()); 
					
					
					
					/*HashSet<EditablePetriPlace> post1 = new HashSet<EditablePetriPlace>();
					HashSet<EditablePetriPlace> post2 = new HashSet<EditablePetriPlace>();

					for (EditablePetriPlace p : T1.getOut())
						post1.add(p);
					for (EditablePetriPlace p : T1.getIn())
						post1.remove(p);

					for (EditablePetriPlace p : T2.getOut())
						post2.add(p);
					for (EditablePetriPlace p : T2.getIn())
						post2.remove(p);

					if (post1.equals(post2))
						continue;*/
					
					
					
					
					String be = addUsedPair(T1, T2, used_pairs);
					
					System.out.println("ADDED "+name1+" "+name2 +" common place:"+ P.getId() + " ; clause:" + be);

					formulaClauses.add(be);
				}
			}
		}

		return formulaClauses;
	}


	public void applyInterface(PetriModel iface)
	{
		Hashtable<EditablePetriPlace, EditablePetriPlace> p2p = new Hashtable<EditablePetriPlace, EditablePetriPlace>();
		Hashtable<EditablePetriTransition, EditablePetriTransition> t2t = new Hashtable<EditablePetriTransition, EditablePetriTransition>();

		// BoundingBox bb1 = root.getBoundingBoxInViewSpace();
		//  BoundingBox bb2 = iface.root.getBoundingBoxInViewSpace();

		for(EditablePetriPlace p: iface.places)
		{
			String id = p.getId();

			EditablePetriPlace new_p = null;
			if (id.startsWith("iface_"))
			{
				new_p = (EditablePetriPlace)getComponentById(id.substring(6));
				if (new_p == null)
					System.err.println ("*hysteric* *search* "+ id.substring(6) ); 
			}
			else
			{
				try
				{
					new_p = new EditablePetriPlace(getRoot());
					new_p.transform.copy(p.transform);
					// new_p.transform.translateRel(0.0f, -(bb1.getUpperRight().getY()+(bb2.getUpperRight().getY()-bb2.getLowerLeft().getY())), 0.0f);
					new_p.setTokens(p.getTokens());
					new_p.setId("iface_" + id);
				}
				catch (UnsupportedComponentException e)
				{
					e.printStackTrace();
				}
				catch (DuplicateIdException e)
				{
					e.printStackTrace();
					System.err.println("Could not create interface place due to id duplication: 'iface_" + id + "'.");
				}
			}
			p2p.put(p, new_p);
		}

		for(EditablePetriTransition t: iface.transitions)
		{
			String id = t.getId();

			EditablePetriTransition new_t = null;
			if (id.startsWith("iface_"))
			{
				id = id.substring(6);
				new_t = (EditablePetriTransition)getComponentById(id);
				if (new_t == null)
				{
					String new_id = null;
					if (id.indexOf("_plus") != -1)
					{
						new_id = id.substring(0, id.indexOf("_plus") + 5) + "1";
					}
					else
						if (id.indexOf("_minus") != -1)
						{
							new_id = id.substring(0, id.indexOf("_minus") + 6) + "1";
						}
						else
						{
							System.err.println("Confused while finding corresponding interface transition for '" + id + "'. *pardon*");
						}

					try
					{
						new_t = new EditablePetriTransition(getRoot());
						new_t.transform.copy(t.transform);
						new_t.setCustomProperty("interface", "true");
						new_t.setId(id);

						EditablePetriTransition original = (EditablePetriTransition)getComponentById(new_id);
						if (original == null)
						{
							System.err.println("Confused while finding original interface transition '" + new_id + "'. *pardon*");
						}

						new_t.transform.translateAbs(original.transform.getTranslation().getX(), original.transform.getTranslation().getY(), 0.0f);

						for(EditablePetriPlace prev: original.getIn()) createConnection(prev, new_t);
						for(EditablePetriPlace next: original.getOut()) createConnection(new_t, next);
					}
					catch (UnsupportedComponentException e)
					{
						e.printStackTrace();
					}
					catch (DuplicateIdException e)
					{
						e.printStackTrace();
						System.err.println("Could not create interface transition due to id duplication: 'iface_" + id + "'.");
					}
					catch (InvalidConnectionException e)
					{
						e.printStackTrace();
					}
				} else {
					new_t.setCustomProperty("interface", "true");
				}
			}
			else
			{
				try
				{
					new_t = new EditablePetriTransition(getRoot());
					new_t.setCustomProperty("interface", "true");
					new_t.transform.copy(t.transform);
					new_t.setId("iface_" + id);
				}
				catch (UnsupportedComponentException e)
				{
					e.printStackTrace();
				}
				catch (DuplicateIdException e)
				{
					e.printStackTrace();
					System.err.println("Could not create interface transition due to id duplication: 'iface_" + id + "'.");
				}
			}
			t2t.put(t, new_t);
		}

		for(EditablePetriPlace p: iface.places)
		{
			for(EditablePetriTransition t: p.getIn())
			{
				try
				{
					createConnection(t2t.get(t), p2p.get(p));
				}
				catch(InvalidConnectionException e)
				{
				}
			}
			for(EditablePetriTransition t: p.getOut())
			{
				try
				{
					createConnection(p2p.get(p), t2t.get(t));
				}
				catch (InvalidConnectionException e)
				{
				}
			}
		} 
	} 
	public Boolean getShorthandNotation() {
		return shorthandNotation;
	}
	
	public void setShorthandNotation(Boolean shorth) {
		this.shorthandNotation = shorth;
	}

} 	
