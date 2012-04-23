package workcraft.mg;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

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
import workcraft.util.Colorf;

public class MGModel extends ModelBase{
	public static final UUID _modeluuid = UUID.fromString("4fbdb830-dba3-11db-8314-0800200c9a66");
	public static final String _displayname = "Marked Graph";
	
	protected static Colorf criticalColor = new Colorf(0.8f, 0.26f, 0.0f, 1.0f);
	
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

	LinkedList<MGPlace> places;
	LinkedList<MGTransition> transitions;
	LinkedList<MGConnection> connections;

	LinkedList<MGTransition> readyTransitions;

	public MGModel() {
		places = new LinkedList<MGPlace>();
		transitions = new LinkedList<MGTransition>();
		connections = new LinkedList<MGConnection>();
		readyTransitions = new LinkedList<MGTransition>();
	}

	public String getNextTransitionID() {
		return "t"+t_name_cnt++;
	}

	public String getNextPlaceID() {
		return "p"+p_name_cnt++;
	}

	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException {
		if (c instanceof MGPlace) {
			MGPlace p = (MGPlace)c;
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
		else if (c instanceof MGTransition) {
			MGTransition t = (MGTransition)c;
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
		if (c instanceof MGPlace) {
			MGPlace p = (MGPlace)c;
			for (MGTransition t : p.getIn()) {
				t.removeOut(p);				
			}
			for (MGTransition t : p.getOut()) {
				t.removeIn(p);
			}

			places.remove(c);
		}

		else if (c instanceof MGTransition) {
			MGTransition t = (MGTransition)c;
			for (MGPlace p : t.getIn()) {
				t.removeOut(p);				
			}
			for (MGPlace p : t.getOut()) {
				t.removeIn(p);			
			}

			transitions.remove(c);

		} else throw new UnsupportedComponentException();	
		
		super.removeComponent(c);
	}


	public EditableConnection createConnection(BasicEditable first, BasicEditable second) throws InvalidConnectionException {
		if (first == second)
			throw new InvalidConnectionException ("Can't connect to self!");

		if (first instanceof MGPlace && ((MGPlace)first).getOut().size() > 0)
		{
			throw new InvalidConnectionException ("Invalid connection (choice places are not allowed)");
		}
		
		if (second instanceof MGPlace && ((MGPlace)second).getIn().size() > 0)
		{
			throw new InvalidConnectionException ("Invalid connection (merge places are not allowed)");
		}
		
		if (!(
				(first instanceof MGPlace && second instanceof MGTransition) ||
				(first instanceof MGTransition && second instanceof MGPlace)
		)) throw new InvalidConnectionException ("Invalid connection (only place-transition and transition-place connections allowed)");
		MGPlace p;
		MGTransition t;
		if (first instanceof MGPlace) {
			p = (MGPlace)first;
			t = (MGTransition)second;
			MGConnection con = new MGConnection(p, t);
			if (p.addOut(con) && t.addIn(con)) {
				connections.add(con);
				return con;
			}
		} else {
			p = (MGPlace)second;
			t = (MGTransition)first;
			MGConnection con = new MGConnection(t, p);
			if (p.addIn(con) && t.addOut(con)) {
				connections.add(con);
				return con;
			}
		}
		return null;
	}

	public void removeConnection(EditableConnection con) throws UnsupportedComponentException {
		MGPlace p;
		MGTransition t;
		if (con.getFirst() instanceof MGPlace) {
			p = (MGPlace)con.getFirst();
			t = (MGTransition)con.getSecond();
			p.removeOut(t);
			t.removeIn(p);
			connections.remove(con);
			return;
		}
		if (con.getFirst() instanceof MGTransition) {
			p = (MGPlace)con.getSecond();
			t = (MGTransition)con.getFirst();
			p.removeIn(t);
			t.removeOut(p);
			connections.remove(con);
			return;
		}
		throw new UnsupportedComponentException();
	}

	private void resetFlags()
	{
		for(MGPlace p : places)
		{
			p.flag = 0;
			p.critical = false;
		}
		for(MGTransition t : transitions)
		{
			t.flag = 0;
			t.critical = false;
		}
		for(MGConnection c : connections)
		{
			c.critical = false;
		}
	}
	
	private boolean in_dead_cycle(MGPlace p, MGPlace x, boolean first)
	{
		if (x.getTokens() != 0) return false;
		
		if (!first && p == x) return true;
		
		if (x.flag > 0) return false;
		
		x.flag = 1;

		if (x.getOut().size() > 0)
		{
			MGTransition t = x.getOut().getFirst();
			for(MGPlace next : t.getOut()) if (in_dead_cycle(p, next, false)) return true;				
		}
		return false;
	}
	
	public boolean isLive()
	{
		for(MGPlace p : places)
		{
			resetFlags();
			if (in_dead_cycle(p, p, true)) return false;
		}
		return true;
	}
	
	private boolean reached;
	
	private boolean in_safe_cycle(MGPlace p, MGPlace x, int seenToken, boolean first)
	{
		if (!first && p == x)
		{
			reached = true;
			return seenToken < 2;
		}

		seenToken += x.getTokens();
		if (seenToken > 1) return false;
		
		if (seenToken == 0)
			if (x.flag % 2 == 1) return false;
			else x.flag++;
		else
			if (x.flag / 2 == 1) return false;
			else x.flag += 2;
		
		if (x.getOut().size() > 0)
		{
			MGTransition t = x.getOut().getFirst();
			for(MGPlace next : t.getOut()) if (in_safe_cycle(p, next, seenToken, false)) return true;				
		}
		
		return false;
	}
	
//	private boolean reach_unsafe_place(MGPlace p)
//	{
//		p.flag = 1;
//		if (p.getOut().size() > 0)
//		{
//			MGTransition t = p.getOut().getFirst();
//			
//			if (!testLiveness(t)) return false;
//			
//			for(MGPlace x : t.getOut())
//			if (x.flag == 0)
//			{
//				if (x.getTokens() > 0) return true;
//				if (reach_unsafe_place(x)) return true;
//			}
//		}
//		return false;
//	}
//	
//	private boolean testLiveness(MGTransition t)
//	{
//		if (t.flag != 0) return t.flag == 2;
//		
//		t.flag = 1;
//		
//		for(MGPlace p : t.getIn())
//		{
//			if (p.getTokens() > 0) continue;
//			if (p.getIn().size() == 0)
//			{
//				t.flag = 1;
//				return false;
//			}
//			
//			MGTransition prev = p.getIn().getFirst();
//			
//			if (testLiveness(prev))
//			{			
//				t.flag = 1;
//				return false;
//			}
//		}
//		
//		t.flag = 2;
//		
//		return true;
//	}
	
	public boolean isSafe()
	{
		for(MGPlace p : places)
		{
			resetFlags();
			reached = false;
			if (in_safe_cycle(p, p, 0, true)) continue;
			if (reached) return false;
		}
//		
//		for(MGPlace p : places)
//		if (p.getTokens() > 0)
//		{
//			if (p.getTokens() > 1) return false;
//			resetFlags();
//			if (reach_unsafe_place(p)) return false;
//		}
//		
		return true;
	}	
	
	public void simReset() {
		for (MGTransition t : transitions ) {
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
			for (MGTransition t : transitions) {
				LinkedList<MGPlace> pl = t.getIn();

				if (pl.isEmpty())
					break;
				if (t.getOut().isEmpty())
					break;
				boolean canfire = true;
				for (MGPlace p : pl) {
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
				MGTransition w = null;				
				for (MGTransition t : readyTransitions) {
					if (t.canWork) {
						w = t;
						break;
					}
				}
				if (w!=null) {
					LinkedList<MGPlace> in = w.getIn();
					LinkedList<MGPlace> out = w.getOut();
					
					for (MGPlace p : in) {
						p.setTokens(p.getTokens()-1);
					}
					for (MGPlace p : out) {
						p.setTokens(p.getTokens()+1);
					}
					w.canFire = false;
					w.canWork = false;
					state = 0;
				}
			} else {
				int s = (int)Math.floor(Math.random()*readyTransitions.size());
				MGTransition t = readyTransitions.get(s);
				LinkedList<MGPlace> in = t.getIn();
				LinkedList<MGPlace> out = t.getOut();
				
				for (MGPlace p : in) {
					p.setTokens(p.getTokens()-1);
				}
				for (MGPlace p : out) {
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
	
	public void calculateCriticalCycle(LinkedList<MGPlace> cp, LinkedList<MGTransition> ct)
	{
		int n = transitions.size();
		
		HashMap<MGTransition, Integer> t2id = new HashMap<MGTransition, Integer>();
		MGTransition id2t[] = new MGTransition[n];
	
		int k = 0;
		for(MGTransition t : transitions)
		{
			t2id.put(t, k);
			id2t[k++] = t;
		}		
				
		Vector<Integer> from = new Vector<Integer>();
		Vector<Integer> to = new Vector<Integer>();
		Vector<Double> weight = new Vector<Double>();
		Vector<Double> delay = new Vector<Double>();
		Vector<Integer> tokens = new Vector<Integer>();
		
		double sum = 0.0;
		double min_delay = 1.0;
		
		for(MGTransition t : transitions)
			for(MGPlace p : t.getOut())
			{
				MGTransition next = p.getOut().getFirst();
				from.add(t2id.get(t));
				to.add(t2id.get(next));
				delay.add(p.getDelayMean());
				weight.add(0.0);
				tokens.add(p.getTokens());
				
				if (min_delay > p.getDelayMean()) min_delay = p.getDelayMean();
				sum += p.getTokens();
			}
		
		int m = from.size();
		
		double d[][] = new double[n][n + 1];

		if (min_delay < 1e-6) min_delay = 1e-6;
		
		double lower = 0.0, upper = sum / min_delay; 
		
		while(upper - lower > 1e-9)
		{
			double x = (lower + upper) / 2.0;
			
			for(int i = 0; i < m; i++) weight.set(i, tokens.get(i) - x * delay.get(i)); 
			
			for(int i = 0; i < n; i++) d[i][0] = 0.0;
			
			for(int i = 1; i <= n; i++)
			{
				for(int j = 0; j < n; j++) d[j][i] = d[j][i - 1];
				for(int j = 0; j < m; j++)
					if (d[to.get(j)][i] > d[from.get(j)][i - 1] + weight.get(j))
						d[to.get(j)][i] = d[from.get(j)][i - 1] + weight.get(j);
			}
			
			boolean neg_cycle = false;
			
			for(int j = 0; j < m; j++)
				if (d[to.get(j)][n] > 1e-6 + d[from.get(j)][n] + weight.get(j))
				{
					neg_cycle = true;
					break;
				}
			
			if (!neg_cycle) lower = x; else upper = x;
		}
		
		int p[][] = new int[n][n + 1];
		for(int i = 0; i < n; i++) d[i][0] = 0.0;
		
		for(int i = 1; i <= n; i++)
		{
			for(int j = 0; j < n; j++) { d[j][i] = d[j][i - 1]; p[j][i] = p[j][i - 1]; }
			for(int j = 0; j < m; j++)
				if (d[to.get(j)][i] > d[from.get(j)][i - 1] + weight.get(j))
				{
					d[to.get(j)][i] = d[from.get(j)][i - 1] + weight.get(j);
					p[to.get(j)][i] = from.get(j);
				}
		}
		
		for(int i = 0; i < n; i++)
			if (d[i][n] + 1e-7 < d[i][n - 1])
			{
				boolean [] seen = new boolean[n];
				int j = i, nt = 0;
				
				k = 0;
				
				while(!seen[j])
				{
					seen[j] = true;
					j = p[j][n - k];
					k++;
					nt++;
				}
				
				k = 0;
				while(i != j)
				{
					i = p[i][n - k];
					k++;
					nt--;
				}
				
				do
				{
					ct.add(id2t[i]);
					MGTransition prev = id2t[p[i][n - k]];
					
					for(MGPlace pl : prev.getOut())
						if (pl.getOut().getFirst() == ct.getLast())
						{
							cp.add(pl);
							break;
						}
					
					i = p[i][n - k];
					k++;
				} while(i != j);
					
				break;
			}
	}

	public void updateConnectionsColor()
	{
		for(MGConnection c : connections)
		{
			if (c.critical && MGConnection.show_critical)
				c.colorOverride = MGModel.criticalColor;
			else
				c.colorOverride = null;
		}
	}

	public LinkedList<MGPlace> getPlaces() {
		return places;
	}

	public LinkedList<MGTransition> getTransitions() {
		return transitions;
	}
}