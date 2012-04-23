package workcraft.sdfs;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.python.core.PyObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.XmlSerializable;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.spreadtoken.STLogic;
import workcraft.spreadtoken.STRegister;

public abstract class SDFSNode extends BasicEditable implements XmlSerializable{
	protected LinkedList<SDFSNode> out = new LinkedList<SDFSNode>();
	protected LinkedList<SDFSNode> in = new LinkedList<SDFSNode>();

	protected boolean userEdited = false;


	private HashSet<SDFSRegisterBase> _rpreset = null;
	private HashSet<SDFSRegisterBase> _rpostset = null;
	private HashSet<SDFSLogicBase> _traversed = null;	

	private void findRPreset(SDFSNode node) {
		for (SDFSNode n : node.getIn()) {
			if (n instanceof SDFSRegisterBase)
				_rpreset.add( (SDFSRegisterBase) n);
			else
			{
				if (!_traversed.contains((SDFSLogicBase) n))
				{
					_traversed.add((SDFSLogicBase) n);
					findRPreset(n);
				}
			}
		}
	}

	private void findRPostset(SDFSNode node) {
		for (SDFSNode n : node.getOut()) {
			if (n instanceof SDFSRegisterBase)
				_rpostset.add( (SDFSRegisterBase) n);
			else
			{
				if (!_traversed.contains((SDFSLogicBase) n))
				{
					_traversed.add((SDFSLogicBase) n);
					findRPostset(n);			
				}
			}
		}
	}

	public HashSet<SDFSRegisterBase> getRegisterPreset() {
		_rpreset = new HashSet<SDFSRegisterBase>();
		_traversed = new HashSet<SDFSLogicBase>();
		findRPreset(this);
		return _rpreset;
	}

	public HashSet<SDFSRegisterBase> getRegisterPostset() {
		_rpostset = new HashSet<SDFSRegisterBase>();
		_traversed = new HashSet<SDFSLogicBase>();
		findRPostset(this);
		return _rpostset;	
	}	

	public List<SDFSNode> getPreset() {
		return getIn();
	}

	public List<SDFSNode> getPostset() {
		return getOut();
	}

	public LinkedList<SDFSNode> formOperands(String token) {
		LinkedList<SDFSNode> operands = new LinkedList<SDFSNode>();

		if (token.equals("self")) {
			operands.add(this);				
		} else if (token.equals("preset:l")) {
			for (SDFSNode n : getPreset()) {
				if (n instanceof SDFSLogicBase)
					operands.add(n);
			}
		} else if (token.equals("preset:r")) {
			for (SDFSNode n : getPreset()) {
				if (n instanceof SDFSRegisterBase)
					operands.add(n);
			}
		} else if (token.equals("r-preset")) {
			for (SDFSNode n: getRegisterPreset())
				operands.add(n);
		} else if (token.equals("postset:l")) {
			for (SDFSNode n : getPostset()) {
				if (n instanceof SDFSLogicBase)
					operands.add(n);
			}
		} else if (token.equals("postset:r")) {
			for (SDFSNode n : getPostset()) {
				if (n instanceof SDFSRegisterBase)
					operands.add(n);
			}
		} else if (token.equals("r-postset")) {
			for (SDFSNode n: getRegisterPostset())
				operands.add(n);
		} else
			return null;

		return operands;
	}

	protected String expandTerm(String token) {
		String res = "";
		String[] args = token.split(" ");

		if (args.length != 2)
			return null;

		boolean negative = (args[1].charAt(0)=='!');
		if (negative)
			args[1] = args[1].substring(1, args[1].length());

		String predicate = args[1];

		LinkedList<SDFSNode> operands = formOperands(args[0]);
		if (operands == null)
			return null;

		if (operands.size()==0)
			return res;
		else if (operands.size()==1) {
			res = predicate+"("+operands.get(0).getId()+")";
		} else
		{
			res = "(";
			boolean first = true;
			for (SDFSNode n: operands) {
				if (first)
					first = false;
				else
					res+= (negative)?"|":"&";
				res += predicate +"("+n.getId()+")"; 
			}
			res += ")";
		}

		if (negative)
			res = "(not "+res+")";

		return res;
	}

	public String expandRule (String def) {
		String res = "";

		String[] rules = def.split(",");

		boolean firstTerm = true;

		for (String rule: rules) {
			String term = "";
			String[] soju = rule.split("\\|");

			if (soju.length > 1)
				term = "(";
			boolean first = true;
			for (String s: soju) {
				String k = expandTerm(s);

				if (k==null) {
					System.err.println ("SDFSNode.expandRule: invalid syntax ["+s+"]");
					continue;
				}

				if (k.equals(""))
					continue;

				if (first)
					first = false;
				else
					term += "|";

				term += k;
			}
			if (soju.length > 1)
				term += ")";

			if (term.equals("")||term.equals("()"))
				continue;

			if (firstTerm)
				firstTerm = false;
			else
				res += "&";

			res+=term;
		}

		if (res.equals(""))
			res = "ALWAYS";

		return res;
	}

	public SDFSNode(BasicEditable parent) throws UnsupportedComponentException{
		super(parent);
		rebuildRuleFunctions();
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("sdfs-node");
		Element ne = (Element) nl.item(0);
		userEdited = Boolean.parseBoolean(ne.getAttribute("user-edited"));
		super.fromXmlDom(element);		
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("sdfs-node");
		ppe.setAttribute("user-edited", Boolean.toString(userEdited));
		ee.appendChild(ppe);
		return ee;
	}


	public LinkedList<SDFSNode> getIn() {
		return (LinkedList<SDFSNode>)in.clone();
	}

	public LinkedList<SDFSNode> getOut() {
		return (LinkedList<SDFSNode>)out.clone();
	}

	public boolean addIn(DefaultConnection con) {
		if (in.contains(con.getFirst()))
			return false;
		in.add( (SDFSNode)con.getFirst());
		connections.add(con);
		rebuildAllRuleFunctions();
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		if (out.contains(con.getSecond()))
			return false;
		out.add( (SDFSNode)con.getSecond());		
		connections.add(con);
		rebuildAllRuleFunctions();
		return true;
	}	

	public void removeIn(SDFSNode node) {
		in.remove(node);
		rebuildAllRuleFunctions();
	}

	public void removeOut(SDFSNode node) {
		out.remove(node);
		rebuildAllRuleFunctions();
	}

	public  Object saveState() {
		return null;
	}

	public void restoreState(Object object) {

	}

	public abstract boolean simTick(int time_ms);
	public abstract void rebuildRuleFunctions();

//	Kinda quick hack, should be done properly.
	public void rebuildAllRuleFunctions() {
		PyObject po = null;
		if (ownerDocument != null)
			if (ownerDocument.getServer() != null)
			po = ownerDocument.getServer().python.get("_loading");

		if (po == null || !po.__nonzero__()) {
			if (ownerDocument != null) {
				LinkedList<BasicEditable> components = new LinkedList<BasicEditable>();
				ownerDocument.getComponents(components);
				for (BasicEditable e: components)
					if (e instanceof SDFSNode)
						if (!((SDFSNode)e).userEdited)
							((SDFSNode)e).rebuildRuleFunctions();
			}
		}
	}
}