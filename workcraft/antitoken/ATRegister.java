package workcraft.antitoken;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.media.opengl.GL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.editor.BasicEditable;
import workcraft.sdfs.RegisterState;
import workcraft.sdfs.SDFSNode;
import workcraft.sdfs.SDFSRegisterBase;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.BlendMode;
import workcraft.visual.GeometryUtil;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public class ATRegister extends SDFSRegisterBase {
	public static final UUID _modeluuid = UUID.fromString("5e947520-7af7-11db-9fe1-0800200c9a66");
	public static final String _displayname = "Register";
	public static final String _hotkey = "q";
	public static final int  _hotkeyvk = KeyEvent.VK_Q;

	private static Colorf token_color = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf antitoken_color = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);

	private boolean token;
	private boolean antitoken;

	private RegisterState forward_state = RegisterState.DISABLED;
	private RegisterState backward_state = RegisterState.DISABLED;

	private boolean token_waiting = false;
	private boolean antitoken_waiting = false;

	private String fwdEnableFunc;
	private String backEnableFunc;
	private String fwdDisableFunc;
	private String backDisableFunc;
	private String markFunc;
	private String antiMarkFunc;
	private String unmarkFunc;
	private String antiUnmarkFunc;	

	public ATRegister(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f) );
		token = false;
		antitoken = false;
	}

	@Override
	protected String expandTerm(String token) {
		if (!token.startsWith("@"))
			return super.expandTerm(token);
		else
		{
			String res = "";
			String[] first_args = token.substring(1).split(":");

			if (first_args.length != 2)
				return null;

			String[] args1 = first_args[0].split(" ");
			String[] args2 = first_args[1].split(" ");

			if ( (args1.length != 2)||(args2.length !=2))
				return null;

			// @r-preset am:r-preset bm;

			boolean negative1 = (args1[1].charAt(0)=='!');
			if (negative1)
				args1[1] = args1[1].substring(1, args1[1].length());
			negative1 = !negative1;

			boolean negative2 = (args2[1].charAt(0)=='!');
			if (negative2)
				args2[1] = args2[1].substring(1, args2[1].length());

			String predicate1 = args1[1];
			String predicate2 = args2[1];

			LinkedList<SDFSNode> operands = formOperands(args1[0]);
			if (operands == null)
				return null;

			if (operands.size()==0)
				return res;
			else
			{
				res = "(";
				boolean first = true;
				for (SDFSNode n: operands) {
					LinkedList<SDFSNode> op2 = n.formOperands(args2[0]);
					if (op2.isEmpty())
						continue;
					
					if (first)
						first = false;
					else
						res+="&";

					String res2 = "(";
					boolean first2 = true;
					for (SDFSNode nn: op2) {
						if (first2)
							first2 = false;
						else
							res2 += (negative2)?"|":"&";
						res2 += predicate2 +"("+nn.getId()+")"; 
					}
					res2 += ")";

					if (negative2)
						res2 = "(not "+res2+")";

					if (negative1)
						res+= "((not "+predicate1+"("+n.getId()+"))";
					else
						res+= "("+predicate1+"("+n.getId()+")";

					res += "|"+res2+")";				
				}
				if (res.equals("("))
					res = "";
				else
					res += ")";
			}

			return res;
		}
	}

	@Override
	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());

		drawFrame(p);

		if (forward_state == RegisterState.ENABLED)
			p.setFillColor(enabled_color);
		else
			p.setFillColor(disabled_color);
		p.drawRect(-0.025f, 0.045f, 0.025f, 0.0f);

		if (backward_state == RegisterState.ENABLED)
			p.setFillColor(enabled_color);
		else
			p.setFillColor(disabled_color);
		p.drawRect(-0.025f, 0.0f, 0.025f, -0.045f);


		p.setLineColor(inactive_frame_color);
		p.drawLine(-0.03f, 0.0f, 0.03f, 0.0f);

		if (token_waiting) {
			p.blendEnable();
			p.setBlendConstantAlpha(0.5f);
			p.setBlendMode(BlendMode.CONSTANT_ALPHA);


			if (token_waiting) { // shadow
				p.setFillColor(token_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawCircle(0.015f, 0.0f, 0.025f);
			} 
			else // excitement
			{
				p.setFillColor(enabled_color);
				p.setLineColor(token_color);
				p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
				p.setLineMode(LineMode.SOLID);
				p.setLineWidth(0.0025f);
				p.drawCircle(0.015f, 0.0f, 0.025f);
			}
			p.blendDisable();
		} else {
			if (token) { // solid
				p.setFillColor(token_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawCircle(0.015f, 0.0f, 0.025f);
			} 
		}

		if (antitoken_waiting) {
			p.blendEnable();
			p.setBlendConstantAlpha(0.5f);
			p.setBlendMode(BlendMode.CONSTANT_ALPHA);

			if (antitoken) { // shadow
				p.setFillColor(antitoken_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawRect(-0.015f, -0.01f, 0.015f, -0.04f);
			} 
			else // excitement
			{
				p.setFillColor(enabled_color);
				p.setLineColor(antitoken_color);
				p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
				p.setLineMode(LineMode.SOLID);
				p.setLineWidth(0.0025f);
				p.drawRect(-0.015f, -0.01f, 0.015f, -0.04f);
			}
			p.blendDisable();
		} else {
			if (antitoken) { // solid
				p.setFillColor(antitoken_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawRect(-0.015f, -0.01f, 0.015f, -0.04f);
			} 
		}		

		super.doDraw(p);		
	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("bool,Token,isMarked,setMarked");
		list.add("bool,Antitoken,isAntiMarked,setAntiMarked");
		list.add("str,Fwd enabling,getFwdEnableFunc,setFwdEnableFunc");
		list.add("str,Back enabling,getBackEnableFunc,setBackEnableFunc");
		list.add("str,Fwd disabling,getFwdDisableFunc,setFwdDisableFunc");
		list.add("str,Back disabling,getBackDisableFunc,setBackDisableFunc");
		list.add("str,Marking,getMarkFunc,setMarkFunc");
		list.add("str,Unmarking,getUnmarkFunc,setUnmarkFunc");
		list.add("str,Anti marking,getAntiMarkFunc,setAntiMarkFunc");
		list.add("str,Anti unmarking,getAntiUnmarkFunc,setAntiUnmarkFunc");		
		return list;
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		super.fromXmlDom(element);
		NodeList nl = element.getElementsByTagName("at-register");
		Element ne = (Element) nl.item(0);
		setMarked(Boolean.parseBoolean(ne.getAttribute("token")));
		setAntiMarked(Boolean.parseBoolean(ne.getAttribute("antitoken")));

		fwdEnableFunc = ne.getAttribute("fwd-enable-func");
		fwdDisableFunc = ne.getAttribute("fwd-disable-func");
		backEnableFunc = ne.getAttribute("back-enable-func");
		backDisableFunc = ne.getAttribute("back-disable-func");
		markFunc = ne.getAttribute("mark-func");
		unmarkFunc = ne.getAttribute("unmark-func");
		antiMarkFunc = ne.getAttribute("anti-mark-func");
		antiUnmarkFunc = ne.getAttribute("anti-unmark-func");		
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("at-register");
		ppe.setAttribute("token", Boolean.toString(isMarked()));
		ppe.setAttribute("antitoken", Boolean.toString(isAntiMarked()));

		ppe.setAttribute("or-token", Boolean.toString(isMarked()));
		ppe.setAttribute("and-token", Boolean.toString(isAntiMarked()));
		ppe.setAttribute("fwd-enable-func", fwdEnableFunc);
		ppe.setAttribute("fwd-disable-func", fwdDisableFunc);
		ppe.setAttribute("back-enable-func", backEnableFunc);
		ppe.setAttribute("back-disable-func", backDisableFunc);
		ppe.setAttribute("mark-func", markFunc);
		ppe.setAttribute("unmark-func", unmarkFunc);
		ppe.setAttribute("anti-mark-func", markFunc);
		ppe.setAttribute("anti-unmark-func", unmarkFunc);	

		ee.appendChild(ppe);
		return ee;
	}

	public boolean simTick(int time_ms) {
		WorkCraftServer server = ownerDocument.getServer();		
		ATModel doc = ((ATModel)ownerDocument);
		boolean user = doc.isUserInteractionEnabled();

		switch (forward_state) {
		case ENABLED:
			if (server.python.eval(fwdDisableFunc).__nonzero__())
				forward_state = RegisterState.DISABLED;
			break;
		case DISABLED:
			if (server.python.eval(fwdEnableFunc).__nonzero__())
				forward_state = RegisterState.ENABLED;
			break;				
		}

		switch (backward_state) {
		case ENABLED:
			if (server.python.eval(backDisableFunc).__nonzero__())
				backward_state = RegisterState.DISABLED;
			break;
		case DISABLED:
			if (server.python.eval(backEnableFunc).__nonzero__())
				backward_state = RegisterState.ENABLED;
			break;				
		}

		if (!isMarked()) {
			if (server.python.eval(markFunc).__nonzero__()) {
				if (user) {
					if (can_work) {
						token = true;
						can_work = false;
						token_waiting = false;
					} else {
						token_waiting = true;												
					}
				}
				else {
					token_waiting = false;
					token = true;
				}
			}
		} else {
			if (server.python.eval(unmarkFunc).__nonzero__()) {
				if (user) {
					if (can_work) {
						token = false;
						can_work = false;
						token_waiting = false;
					} else {
						token_waiting = true;												
					}
				}
				else {
					token_waiting = false;
					token = false;
				}
			}
		}

		if (!isAntiMarked()) {
			if (server.python.eval(antiMarkFunc).__nonzero__())
				if (user) {
					if (can_work) {
						antitoken = true;
						can_work = false;
						antitoken_waiting = false;
					} else {
						antitoken_waiting = true;												
					}
				}
				else {
					antitoken_waiting = false;
					antitoken = true;
				}
		} else {
			if (server.python.eval(antiUnmarkFunc).__nonzero__())
				if (user) {
					if (can_work) {
						antitoken = false;
						can_work = false;
						antitoken_waiting = false;
					} else {
						antitoken_waiting = true;												
					}
				}
				else {
					antitoken_waiting = false;					
					antitoken = false;
				}
		}
		
		return true;
	}


	public void setMarked(Boolean token) {
		this.token = token;
	}

	public Boolean isMarked() {
		return token;
	}

	public void setAntiMarked(Boolean antitoken) {
		this.antitoken = antitoken;
	}

	public boolean isAntiMarked() {
		return antitoken;
	}

	public void setForwardState(RegisterState forward_state) {
		this.forward_state = forward_state;
	}

	public RegisterState getForwardState() {
		return forward_state;
	}

	public void setBackwardState(RegisterState backward_state) {
		this.backward_state = backward_state;
	}

	public RegisterState getBackwardState() {
		return backward_state;
	}

	@Override
	public void rebuildRuleFunctions() {
		fwdEnableFunc = expandRule("self !m,preset:l lfe,preset:r m,preset:r !am");
		fwdDisableFunc = expandRule("self m,preset:l lfr,preset:r !m|preset:r am");
		backEnableFunc = expandRule("self !am,postset:l lbe,postset:r am,postset:r !m");
		backDisableFunc = expandRule("self am,postset:l lbr,postset:r !am|postset:r m");
		markFunc = expandRule("self rfe,self !rbe,r-preset !am,r-postset !m,@r-preset !m:r-preset !am");
		unmarkFunc = expandRule("self !am,self !rfe,self !rbe,r-postset m");
		antiMarkFunc = expandRule("self !rfe,self rbe,r-preset !am,r-postset !m,@r-postset !am:r-postset !m");
		antiUnmarkFunc = expandRule("self !m,self !rfe,self !rbe,r-preset am");
	}

	public Boolean isForwardEnabled() {
		return forward_state == RegisterState.ENABLED;
	}

	public Boolean isBackwardEnabled() {
		return backward_state == RegisterState.ENABLED;

	}

	public String getAntiMarkFunc() {
		return antiMarkFunc;
	}

	public void setAntiMarkFunc(String antiMarkFunc) {
		this.antiMarkFunc = antiMarkFunc;
	}

	public String getAntiUnmarkFunc() {
		return antiUnmarkFunc;
	}

	public void setAntiUnmarkFunc(String antiUnmarkFunc) {
		this.antiUnmarkFunc = antiUnmarkFunc;
	}

	public String getBackDisableFunc() {
		return backDisableFunc;
	}

	public void setBackDisableFunc(String backDisableFunc) {
		this.backDisableFunc = backDisableFunc;
	}

	public String getBackEnableFunc() {
		return backEnableFunc;
	}

	public void setBackEnableFunc(String backEnableFunc) {
		this.backEnableFunc = backEnableFunc;
	}

	public String getFwdDisableFunc() {
		return fwdDisableFunc;
	}

	public void setFwdDisableFunc(String fwdDisableFunc) {
		this.fwdDisableFunc = fwdDisableFunc;
	}

	public String getFwdEnableFunc() {
		return fwdEnableFunc;
	}

	public void setFwdEnableFunc(String fwdEnableFunc) {
		this.fwdEnableFunc = fwdEnableFunc;
	}

	public String getMarkFunc() {
		return markFunc;
	}

	public void setMarkFunc(String markFunc) {
		this.markFunc = markFunc;
	}

	public String getUnmarkFunc() {
		return unmarkFunc;
	}

	public void setUnmarkFunc(String unmarkFunc) {
		this.unmarkFunc = unmarkFunc;
	}

	@Override
	public void restoreState(Object object) {
		token = (Boolean)((Object[])object)[0];
		antitoken = (Boolean)((Object[])object)[1];
		forward_state = (RegisterState)((Object[])object)[2];
		backward_state = (RegisterState)((Object[])object)[3];
		token_waiting = false;
		antitoken_waiting = false;
		can_work = false;
	}

	@Override
	public Object saveState() {
		Object[] s = new Object[4];
		s[0] = token;
		s[1] = antitoken;
		s[2] = forward_state;
		s[3] = backward_state;
		return s;
	}	
}
