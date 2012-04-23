package workcraft.spreadtoken;

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

public class STModel extends SDFSModelBase {
	public static final UUID _modeluuid = UUID.fromString("a57b3350-73d3-11db-9fe1-0800200c9a66");
	public static final String _displayname = "SDFS (Spread Token)";
		
	public static final String py_eval_function = 
		"def e(o):\n" +
		"\treturn o.isEvaluated();\n" +
		"\n";

	public static final String py_reset_function = 
		"def r(o):\n" +
		"\treturn o.isReset();\n" +
		"\n";
	
	public static final String py_marked_function = 
		"def m(o):\n" +
		"\treturn o.isMarked();\n" +
		"\n";
	
	public static final String py_enabled_function = 
		"def re(o):\n" +
		"\treturn o.isEnabled();\n" +
		"\n";
	
		

	int t_name_cnt = 0;
	int p_name_cnt = 0;

	private boolean loading = false;

	LinkedList<DefaultConnection> connections;


	public STModel() {
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
		if (c instanceof STRegister) {
			STRegister p = (STRegister)c;
			p.setOwnerDocument(this);
			registers.add(p);
			if (auto_name)
				for (;;) {
					try {
						p.setId(getNextPlaceID());
						break;
					} catch (DuplicateIdException e) {
					}
				}
		}
		else if (c instanceof STLogic) {
			STLogic t = (STLogic)c;
			t.setOwnerDocument(this);
			logic.add(t);
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
		if (c instanceof STRegister) {
			STRegister p = (STRegister)c;
			registers.remove(p);
		}
		else if (c instanceof STLogic) {
			STLogic t = (STLogic)c;
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
		STRegister p,pp;
		STLogic t, tt;
		if (first instanceof STRegister) {
			if (second instanceof STLogic) {
				p = (STRegister)first;
				t = (STLogic)second;
				DefaultConnection con = new DefaultConnection(p, t);
				if (p.addOut(con) && t.addIn(con)) {
					connections.add(con);
					return con;
				}
			} else {
				p = (STRegister)first;
				pp = (STRegister)second;
				DefaultConnection con = new DefaultConnection(p, pp);
				if (p.addOut(con) && pp.addIn(con)) {
					connections.add(con);
					return con;
				}
			}
		} else {
			if (second instanceof STRegister) {
				p = (STRegister)second;
				t = (STLogic)first;
				DefaultConnection con = new DefaultConnection(t, p);
				if (p.addIn(con) && t.addOut(con)) {
					connections.add(con);
					return con;
				}
			} else {
				t = (STLogic)first;
				tt = (STLogic)second;
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

   public List<EditableConnection> getConnections() {
		return (List<EditableConnection>)((List)connections);		
	}

	public void validate() throws ModelValidationException {
		ModelValidationException errors =  new ModelValidationException();
		boolean good = true;
		for (SDFSLogicBase node : logic) {
			if (node.getIn().isEmpty() || node.getOut().isEmpty()) {
				errors.addError(STLogic._displayname+" ["+node.getId()+"]: combinational logic with empty preset or postset is not allowed");
				good = false;
			}
		}
		if (!good)
			throw errors;
	}

	public JPanel getSimulationControls() {
		if (panelSimControls == null) {
			panelSimControls = new DefaultSimControls(_modeluuid.toString());
		}
		return panelSimControls;
	}

	public void bind(WorkCraftServer server) {
		super.bind(server);
		server.python.exec(py_eval_function);
		server.python.exec(py_reset_function);
		server.python.exec(py_marked_function);
		server.python.exec(py_enabled_function);
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

	public List<SDFSRegisterBase> getRegisters() {
		return registers;
	}

	public List<SDFSLogicBase> getLogic() {
		return logic;
	}
	
	
}