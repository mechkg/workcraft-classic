package workcraft.abstractsdfs;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.JOptionPane;
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
import workcraft.sdfs.SDFSModelBase;
import workcraft.sdfs.SDFSNode;

public class ASDFSModel extends SDFSModelBase{
	public static final UUID _modeluuid = UUID.fromString("24a8a176-dafe-11db-8314-0800200c9a66");
	public static final String _displayname = "SDFS (Abstract)";
	
	public static DefaultSimControls panelSimControls = null;

	int t_name_cnt = 0;
	int p_name_cnt = 0;

	private boolean loading = false;
	private LinkedList<ASDFSRegister> registers;
	private LinkedList<ASDFSLogic> logic;
	LinkedList<DefaultConnection> connections;

	public ASDFSModel() {
		registers = new LinkedList<ASDFSRegister>();
		logic = new LinkedList<ASDFSLogic>();
		connections = new LinkedList<DefaultConnection>();
	}

	public String getNextTransitionID() {
		return "l"+t_name_cnt++;
	}

	public String getNextPlaceID() {
		return "r"+p_name_cnt++;
	}

	public void addComponent(BasicEditable c, boolean auto_name) throws UnsupportedComponentException {
		if (c instanceof ASDFSRegister) {
			ASDFSRegister p = (ASDFSRegister)c;
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
		else if (c instanceof ASDFSLogic) {
			ASDFSLogic t = (ASDFSLogic)c;
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
		if (c instanceof ASDFSRegister) {
			ASDFSRegister p = (ASDFSRegister)c;
			registers.remove(p);
		}
		else if (c instanceof ASDFSLogic) {
			ASDFSLogic t = (ASDFSLogic)c;
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
		ASDFSRegister p,pp;
		ASDFSLogic t, tt;
		if (first instanceof ASDFSRegister) {
			if (second instanceof ASDFSLogic) {
				p = (ASDFSRegister)first;
				t = (ASDFSLogic)second;
				DefaultConnection con = new DefaultConnection(p, t);
				if (p.addOut(con) && t.addIn(con)) {
					connections.add(con);
					return con;
				}
			} else {
				p = (ASDFSRegister)first;
				pp = (ASDFSRegister)second;
				DefaultConnection con = new DefaultConnection(p, pp);
				if (p.addOut(con) && pp.addIn(con)) {
					connections.add(con);
					return con;
				}
			}
		} else {
			if (second instanceof ASDFSRegister) {
				p = (ASDFSRegister)second;
				t = (ASDFSLogic)first;
				DefaultConnection con = new DefaultConnection(t, p);
				if (p.addIn(con) && t.addOut(con)) {
					connections.add(con);
					return con;
				}
			} else {
				t = (ASDFSLogic)first;
				tt = (ASDFSLogic)second;
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
		JOptionPane.showMessageDialog(null, "Abstract SDFS cannot be simulated");
	}

	public void simStep() {
		// TODO Auto-generated method stub
	}

	public boolean simIsRunning() {
		return false;

	}

	public void simFinish() {

	}

	public List<EditableConnection> getConnections() {
		return (List<EditableConnection>)((List)connections);		
	}



	public void validate() throws ModelValidationException {
		ModelValidationException errors =  new ModelValidationException();
		boolean good = true;
		for (ASDFSLogic node : logic) {
			if (node.getIn().isEmpty() || node.getOut().isEmpty()) {
				errors.addError(ASDFSLogic._displayname+" ["+node.getId()+"]: combinational logic with empty preset or postset is not allowed");
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
		for (BasicEditable n: logic)
			out.add(n);
		for (BasicEditable n: registers)
			out.add(n);
		
	}

	public void setRegisters(LinkedList<ASDFSRegister> registers) {
		this.registers = registers;
	}

	public List<ASDFSRegister> getRegisters() {
		return registers;
	}

	public List<ASDFSLogic> getLogic() {
		return logic;
	}
	
	
}