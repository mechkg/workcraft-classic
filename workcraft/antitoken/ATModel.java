package workcraft.antitoken;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.JPanel;

import workcraft.DuplicateIdException;
import workcraft.ModelBase;
import workcraft.ModelValidationException;
import workcraft.InvalidConnectionException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.common.DefaultSimControls;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.editor.EditorPane;
import workcraft.sdfs.SDFSLogicBase;
import workcraft.sdfs.SDFSModelBase;
import workcraft.sdfs.SDFSNode;
import workcraft.sdfs.SDFSRegisterBase;

public class ATModel extends SDFSModelBase {
	public static final UUID _modeluuid = UUID.fromString("5e947520-7af7-11db-9fe1-0800200c9a66");
	public static final String _displayname = "SDFS (Antitoken)";

	public static final String py_feval_function = 
		"def lfe(o):\n" +
		"\treturn o.isForwardEvaluated();\n" +
		"\n";
	
	public static final String py_beval_function = 
		"def lbe(o):\n" +
		"\treturn o.isBackwardEvaluated();\n" +
		"\n";

	public static final String py_freset_function = 
		"def lfr(o):\n" +
		"\treturn o.isForwardReset();\n" +
		"\n";
	
	public static final String py_breset_function = 
		"def lbr(o):\n" +
		"\treturn o.isBackwardReset();\n" +
		"\n";
	
	public static final String py_marked_function = 
		"def m(o):\n" +
		"\treturn o.isMarked();\n" +
		"\n";
	
	public static final String py_antimarked_function = 
		"def am(o):\n" +
		"\treturn o.isAntiMarked();\n" +
		"\n";
	
	public static final String py_fenabled_function = 
		"def rfe(o):\n" +
		"\treturn o.isForwardEnabled();\n" +
		"\n";
	
	public static final String py_benabled_function = 
		"def rbe(o):\n" +
		"\treturn o.isBackwardEnabled();\n" +
		"\n";

	int t_name_cnt = 0;
	int p_name_cnt = 0;

	private int state = 0;
	private boolean loading = false;
	private SimThread sim_thread = null;

	LinkedList<DefaultConnection> connections;

	public ATModel() {
		registers = new LinkedList<SDFSRegisterBase>();
		logic = new LinkedList<SDFSLogicBase>();
		connections = new LinkedList<DefaultConnection>();
	}

	public String getNextTransitionID() {
		return "l"+t_name_cnt++;
	}

	public String getNextPlaceID() {
		return "r"+p_name_cnt++;
	}

	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException {
		if (c instanceof ATRegister) {
			ATRegister p = (ATRegister)c;
			p.setOwnerDocument(this);
			registers.add(p);
			if (auto_name) {
				for (;;) {
					try {
						p.setId(getNextPlaceID());
						break;
					} catch (DuplicateIdException e) 	{						
					}
				}
			}
		}
		
		else if (c instanceof ATLogic) {
			ATLogic t = (ATLogic)c;
			t.setOwnerDocument(this);			
			logic.add(t);
			if (auto_name) {

				for (;;)
					try {
						t.setId(getNextTransitionID());
						break;
					} catch (DuplicateIdException e) {}
					
			}
		} else throw new UnsupportedComponentException();
		
		super.addComponent(c, auto_name);
	}

	public void removeComponent(BasicEditable c) throws UnsupportedComponentException {
		super.removeComponent(c);
		if (c instanceof ATRegister) {
			ATRegister p = (ATRegister)c;
			registers.remove(p);
		}
		else if (c instanceof ATLogic) {
			ATLogic t = (ATLogic)c;
			logic.remove(t);
		} else throw new UnsupportedComponentException();
		
		super.removeComponent(c);
	}

	public EditableConnection createConnection(BasicEditable first, BasicEditable second) throws InvalidConnectionException {
		if (first == second)
			throw new InvalidConnectionException ("Can't connect to self!");
		/*if (
				(first instanceof EditableSTRegister && second instanceof EditableSTRegister)
		) throw new InvalidConnectionException ("Invalid connection (direct place-to-place connections not allowed)");*/
		ATRegister p,pp;
		ATLogic t, tt;
		if (first instanceof ATRegister) {
			if (second instanceof ATLogic) {
				p = (ATRegister)first;
				t = (ATLogic)second;
				DefaultConnection con = new DefaultConnection(p, t);
				if (p.addOut(con) && t.addIn(con)) {
					connections.add(con);
					return con;
				}
			} else {
				p = (ATRegister)first;
				pp = (ATRegister)second;
				DefaultConnection con = new DefaultConnection(p, pp);
				if (p.addOut(con) && pp.addIn(con)) {
					connections.add(con);
					return con;
				}
			}
		} else {
			if (second instanceof ATRegister) {
				p = (ATRegister)second;
				t = (ATLogic)first;
				DefaultConnection con = new DefaultConnection(t, p);
				if (p.addIn(con) && t.addOut(con)) {
					connections.add(con);
					return con;
				}
			} else {
				t = (ATLogic)first;
				tt = (ATLogic)second;
				DefaultConnection conn = new DefaultConnection(t, tt);
				if (tt.addIn(conn) && t.addOut(conn)) {
					connections.add(conn);
					return conn;
				}
			}
		}
		return null;
	}

	public void removeConnection(EditableConnection con) throws UnsupportedComponentException {
		SDFSNode first = (SDFSNode)con.getFirst();		
		SDFSNode second = (SDFSNode)con.getSecond();
		first.removeOut(second);
		second.removeIn(first);
		connections.remove(con);
	}

	public void simReset() {
	}

	public void simBegin() {
		if (sim_thread == null) {
			sim_thread = new SimThread();
			sim_thread.start();
		}
	}

	public void simStep() {
		// TODO Auto-generated method stub
	}

	public boolean simIsRunning() {
		return (sim_thread!=null);
	}

	public void simFinish() {
		if (sim_thread != null) {
			sim_thread.interrupt();
			sim_thread = null;
		}
	}

	public List<EditableConnection> getConnections() {
		return (List<EditableConnection>)((List)connections);		
	}


	public void validate() throws ModelValidationException {
		ModelValidationException errors =  new ModelValidationException();
		boolean good = true;
		for (SDFSLogicBase node : logic) {
			if (node.getIn().isEmpty() || node.getOut().isEmpty()) {
				errors.addError(ATLogic._displayname+" ["+node.getId()+"]: combinational logic with empty preset or postset is not allowed");
				good = false;
			}
		}
		if (!good)
			throw errors;
	}

	public boolean isUserInteractionEnabled() {
		if (panelSimControls == null)
			return false;
		else
			return panelSimControls.isUserInteractionEnabled();
	}

	public JPanel getSimulationControls() {
		if (panelSimControls == null) {
			panelSimControls = new DefaultSimControls(_modeluuid.toString());
		}
		return panelSimControls;
	}

	public void bind(WorkCraftServer server) {
		super.bind(server);
		server.python.exec(py_feval_function);
		server.python.exec(py_beval_function);
		server.python.exec(py_freset_function);
		server.python.exec(py_breset_function);
		server.python.exec(py_marked_function);
		server.python.exec(py_antimarked_function);
		server.python.exec(py_fenabled_function);
		server.python.exec(py_benabled_function);
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
		for (BasicEditable n: logic)
			out.add(n);
		for (BasicEditable n: registers)
			out.add(n);
		
	}
	
	
}