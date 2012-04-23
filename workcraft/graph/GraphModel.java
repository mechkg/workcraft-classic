package workcraft.graph;

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

public class GraphModel extends ModelBase {
	public static final UUID _modeluuid = UUID.fromString("dc03e740-a705-11db-befa-0800200c9a66");
	public static final String _displayname = "Graph";

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

	int v_name_cnt = 0;
	
	private int state = 0;
	private boolean loading;

	SimThread sim_thread = null;
	public static DefaultSimControls panelSimControls = null;

	LinkedList<EditableGraphVertex> vertices;
	LinkedList<DefaultConnection> connections;

	LinkedList<EditableGraphVertex> readyVertices;

	public GraphModel() {
		vertices = new LinkedList<EditableGraphVertex>();
		connections = new LinkedList<DefaultConnection>();
		readyVertices = new LinkedList<EditableGraphVertex>();
	}

	public String getNextVertexID() {
		return "v"+v_name_cnt++;
	}

	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException {
		if (c instanceof EditableGraphVertex) {
			EditableGraphVertex p = (EditableGraphVertex)c;
			vertices.add(p);
			p.setOwnerDocument(this);
			if (auto_name)
				for (;;) {
					try {
						p.setId(getNextVertexID());
						break;
					} catch (DuplicateIdException e) {
					}
				}		
		} else throw new UnsupportedComponentException();
		
		super.addComponent(c, auto_name);
	}

	public void removeComponent(BasicEditable c) throws UnsupportedComponentException {
		super.removeComponent(c);
		if (c instanceof EditableGraphVertex) {
			EditableGraphVertex v = (EditableGraphVertex)c;
			for (EditableGraphVertex t : v.getIn()) {
				t.removeOut(v);				
			}
			for (EditableGraphVertex t : v.getOut()) {
				t.removeIn(v);
			}

			vertices.remove(c);
		
		} else throw new UnsupportedComponentException();
		
		super.removeComponent(c);
	}


	public EditableConnection createConnection(BasicEditable first, BasicEditable second) throws InvalidConnectionException {
		
		if (!(first instanceof EditableGraphVertex) || !(second instanceof EditableGraphVertex))
			throw new InvalidConnectionException ("Invalid connection.");
		
		EditableGraphVertex p, q;

		p = (EditableGraphVertex)first;
		q = (EditableGraphVertex)second;
		DefaultConnection con = new DefaultConnection(p, q);
		if (p.addOut(con) && q.addIn(con)) {
			connections.add(con);
			return con;
		}
		
		return null;
	}

	public void removeConnection(EditableConnection con) throws UnsupportedComponentException {
		EditableGraphVertex p,q;
	
		p = (EditableGraphVertex)con.getFirst();
		q = (EditableGraphVertex)con.getSecond();
		
		p.removeOut(q);
		q.removeIn(p);
		connections.remove(con);
	}

	public void simReset() {
		state = 0;
		for(EditableGraphVertex v : vertices) v.setColor(0);
		readyVertices.clear();
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
			readyVertices.clear();
			for (EditableGraphVertex v : vertices)
			{
				LinkedList<EditableGraphVertex> prev = v.getIn();

				boolean canfire = true;
				for (EditableGraphVertex p : prev) {
					if (p.getColor()<1)
					{
						canfire = false;
						break;
					}
				}
				
				if (v.getColor()>0) canfire = false;
				
				if (canfire) readyVertices.add(v);
				v.canFire = canfire;
			}
			state = 1;
			break;
		case 1:
			if (readyVertices.isEmpty())
			{
				state = 0;
				break;
			}

			if (panelSimControls.isUserInteractionEnabled()) {
				EditableGraphVertex w = null;				
				for (EditableGraphVertex t : readyVertices) {
					if (t.canWork) {
						w = t;
						break;
					}
				}
				if (w!=null)
				{
					w.setColor(1);
					w.canFire = false;
					w.canWork = false;
					
					state = 0;
				}
			} else {
				int s = (int)Math.floor(Math.random()*readyVertices.size());
				EditableGraphVertex t = readyVertices.get(s);
				
				t.setColor(1);
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
		for (BasicEditable n: vertices)
			out.add(n);
	}
}