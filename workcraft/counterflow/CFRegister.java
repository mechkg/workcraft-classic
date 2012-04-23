package workcraft.counterflow;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.editor.BasicEditable;
import workcraft.sdfs.RegisterState;
import workcraft.sdfs.SDFSRegisterBase;
import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.visual.BlendMode;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;

public class CFRegister extends SDFSRegisterBase {
	public static final UUID _modeluuid = UUID.fromString("9df82f00-7aec-11db-9fe1-0800200c9a66");
	public static final String _displayname = "Register";
	public static final String _hotkey = "q";
	public static final int  _hotkeyvk = KeyEvent.VK_Q;

	private static Colorf or_token_color = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf and_token_color = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);

	private boolean orMarked;
	private boolean andMarked;

	private RegisterState forward_state = RegisterState.DISABLED;
	private RegisterState backward_state = RegisterState.DISABLED;

	private boolean or_waiting = false;
	private boolean and_waiting = false;

	private String fwdEnableFunc;
	private String backEnableFunc;
	private String fwdDisableFunc;
	private String backDisableFunc;
	private String orMarkFunc;
	private String orUnmarkFunc;
	private String andMarkFunc;
	private String andUnmarkFunc;

	public CFRegister(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f) );
		orMarked = false;
		andMarked = false;
	}

	@Override
	public void draw(Painter p) {
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

		if (or_waiting) {
			p.blendEnable();
			p.setBlendConstantAlpha(0.5f);
			p.setBlendMode(BlendMode.CONSTANT_ALPHA);


			if (orMarked) { // shadow
				p.setFillColor(or_token_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawCircle(0.015f, 0.0f, 0.025f);
			} 
			else // excitement
			{
				p.setFillColor(enabled_color);
				p.setLineColor(or_token_color);
				p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
				p.setLineMode(LineMode.SOLID);
				p.setLineWidth(0.0025f);
				p.drawCircle(0.015f, 0.0f, 0.025f);
			}
			p.blendDisable();
		} else {
			if (orMarked) { // solid
				p.setFillColor(or_token_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawCircle(0.015f, 0.0f, 0.025f);
			} 
		}

		if (and_waiting) {
			p.blendEnable();
			p.setBlendConstantAlpha(0.5f);
			p.setBlendMode(BlendMode.CONSTANT_ALPHA);

			if (andMarked) { // shadow
				p.setFillColor(or_token_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawRect(-0.015f, -0.01f, 0.015f, -0.04f);
			} 
			else // excitement
			{
				p.setFillColor(enabled_color);
				p.setLineColor(or_token_color);
				p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
				p.setLineMode(LineMode.SOLID);
				p.setLineWidth(0.0025f);
				p.drawRect(-0.015f, -0.01f, 0.015f, -0.04f);
			}
			p.blendDisable();
		} else {
			if (andMarked) { // solid
				p.setFillColor(or_token_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawRect(-0.015f, -0.01f, 0.015f, -0.04f);
			} 
		}		

		super.draw(p);		
	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("bool,OR marked,isOrMarked,setOrMarked");
		list.add("bool,AND marked,isAndMarked,setAndMarked");
		list.add("str,Fwd enabling,getFwdEnableFunc,setFwdEnableFunc");
		list.add("str,Back enabling,getBackEnableFunc,setBackEnableFunc");
		list.add("str,Fwd disabling,getFwdDisableFunc,setFwdDisableFunc");
		list.add("str,Back disabling,getBackDisableFunc,setBackDisableFunc");
		list.add("str,OR marking,getOrMarkFunc,setOrMarkFunc");
		list.add("str,OR unmarking,getOrUnmarkFunc,setOrUnmarkFunc");
		list.add("str,AND marking,getAndMarkFunc,setAndMarkFunc");
		list.add("str,AND unmarking,getAndUnmarkFunc,setAndUnmarkFunc");
		return list;
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("cf-register");
		Element ne = (Element) nl.item(0);
		setOrMarked(Boolean.parseBoolean(ne.getAttribute("or-token")));
		setAndMarked(Boolean.parseBoolean(ne.getAttribute("and-token")));

		fwdEnableFunc = ne.getAttribute("fwd-enable-func");
		fwdDisableFunc = ne.getAttribute("fwd-disable-func");
		backEnableFunc = ne.getAttribute("back-enable-func");
		backDisableFunc = ne.getAttribute("back-disable-func");
		orMarkFunc = ne.getAttribute("or-mark-func");
		orUnmarkFunc = ne.getAttribute("or-unmark-func");
		andMarkFunc = ne.getAttribute("and-mark-func");
		andUnmarkFunc = ne.getAttribute("and-unmark-func");

		super.fromXmlDom(element);		
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("cf-register");
		ppe.setAttribute("or-token", Boolean.toString(isOrMarked()));
		ppe.setAttribute("and-token", Boolean.toString(isAndMarked()));
		ppe.setAttribute("fwd-enable-func", fwdEnableFunc);
		ppe.setAttribute("fwd-disable-func", fwdDisableFunc);
		ppe.setAttribute("back-enable-func", backEnableFunc);
		ppe.setAttribute("back-disable-func", backDisableFunc);
		ppe.setAttribute("or-mark-func", orMarkFunc);
		ppe.setAttribute("or-unmark-func", orUnmarkFunc);
		ppe.setAttribute("and-mark-func", andMarkFunc);
		ppe.setAttribute("and-unmark-func", andUnmarkFunc);
		ee.appendChild(ppe);
		return ee;
	}

	public boolean simTick(int time_ms) {
		WorkCraftServer server = ownerDocument.getServer();		
		CFModel doc = ((CFModel)ownerDocument);
		boolean user = doc.isUserInteractionEnabled();

		switch (forward_state) {
		case ENABLED:
			if (server.python.eval(fwdDisableFunc).__nonzero__()) {
				forward_state = RegisterState.DISABLED;
				doc.addTraceEvent(this.id + "/fwd_dis");
				return true;
			}
			break;
		case DISABLED:
			if (server.python.eval(fwdEnableFunc).__nonzero__()) {
				forward_state = RegisterState.ENABLED;
				doc.addTraceEvent(this.id + "/fwd_enb");
				return true;
			}
			break;				
		}

		switch (backward_state) {
		case ENABLED:
			if (server.python.eval(backDisableFunc).__nonzero__()) {
				backward_state = RegisterState.DISABLED;
				doc.addTraceEvent(this.id + "/back_dis");
				return true;
			}
			break;
		case DISABLED:
			if (server.python.eval(backEnableFunc).__nonzero__()) {
				backward_state = RegisterState.ENABLED;
				doc.addTraceEvent(this.id + "/back_enb");
				return true;
			}
			break;				
		}

		if (!isOrMarked()) {
			if (server.python.eval(orMarkFunc).__nonzero__()) {
				if (user) {
					if (can_work) {
						orMarked = true;
						doc.addTraceEvent(this.id + "/or_mrk");
						can_work = false;
						or_waiting = false;
						return true;
					} else {
						or_waiting = true;												
					}
				}
				else {
					or_waiting = false;
					orMarked = true;
					doc.addTraceEvent(this.id + "/or_mrk");
					return true;
				}
			}
		} else {


			if (server.python.eval(orUnmarkFunc).__nonzero__()) {
				if (user) {
					if (can_work) {
						orMarked = false;
						doc.addTraceEvent(this.id + "/or_unmrk");
						can_work = false;
						or_waiting = false;
						return true;
					} else {
						or_waiting = true;												
					}
				}
				else {
					or_waiting = false;
					doc.addTraceEvent(this.id + "/or_unmrk");
					orMarked = false;
					return true;
				}
			}
		}

		if (!isAndMarked()) {

			if (server.python.eval(andMarkFunc).__nonzero__())
				if (user) {
					if (can_work) {
						andMarked = true;
						doc.addTraceEvent(this.id + "/and_mrk");
						can_work = false;
						and_waiting = false;
						return true;
					} else {
						and_waiting = true;												
					}
				}
				else {
					and_waiting = false;
					andMarked = true;
					doc.addTraceEvent(this.id + "/and_mrk");
					return true;
				}
		} else {

			if (server.python.eval(andUnmarkFunc).__nonzero__())
				if (user) {
					if (can_work) {
						andMarked = false;
						doc.addTraceEvent(this.id + "/and_unmrk");
						can_work = false;
						and_waiting = false;
						return true;
					} else {
						and_waiting = true;												
					}
				}
				else {
					and_waiting = false;
					andMarked = false;
					doc.addTraceEvent(this.id + "/and_unmrk");
					return true;
				}
		}

		return false;
	}

	@Override
	public void rebuildRuleFunctions() {
		fwdEnableFunc = expandRule("self !am,preset:l lfe,preset:r om");
		fwdDisableFunc = expandRule("self am,preset:l lfr,preset:r !om");
		backEnableFunc = expandRule("self !am,postset:l lbe,postset:r om");
		backDisableFunc = expandRule("self am,postset:l lbr,postset:r !om");
		orMarkFunc = expandRule("self !am,self rfe|self rbe,r-preset !am,r-postset !am");
		orUnmarkFunc = expandRule("self am,self !rfe|self !rbe,r-preset am,r-postset am");
		andMarkFunc = expandRule("self om,self rfe,self rbe");
		andUnmarkFunc = expandRule("self !om,self !rfe,self !rbe");
	}

	public Boolean isForwardEnabled() {
		return forward_state == RegisterState.ENABLED;
	}

	public Boolean isBackwardEnabled() {
		return backward_state == RegisterState.ENABLED;
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

	public Boolean isOrMarked() {
		return orMarked;		
	}

	public void setOrMarked(Boolean marked) {
		orMarked = marked;
	}

	public Boolean isAndMarked() {
		return andMarked;		
	}

	public void setAndMarked(Boolean marked) {
		andMarked = marked;
	}	

	public String getFwdEnableFunc() {
		return fwdEnableFunc;
	}

	public void setFwdEnableFunc(String fwdEnableFunc) {
		this.fwdEnableFunc = fwdEnableFunc;
	}

	public String getBackEnableFunc() {
		return backEnableFunc;
	}

	public void setBackEnableFunc(String backEnableFunc) {
		this.backEnableFunc = backEnableFunc;
	}

	public String getAndMarkFunc() {
		return andMarkFunc;
	}

	public void setAndMarkFunc(String andMarkFunc) {
		this.andMarkFunc = andMarkFunc;
	}

	public String getAndUnmarkFunc() {
		return andUnmarkFunc;
	}

	public void setAndUnmarkFunc(String andUnmarkFunc) {
		this.andUnmarkFunc = andUnmarkFunc;
	}

	public String getBackDisableFunc() {
		return backDisableFunc;
	}

	public void setBackDisableFunc(String backDisableFunc) {
		this.backDisableFunc = backDisableFunc;
	}

	public String getFwdDisableFunc() {
		return fwdDisableFunc;
	}

	public void setFwdDisableFunc(String fwdDisableFunc) {
		this.fwdDisableFunc = fwdDisableFunc;
	}

	public String getOrMarkFunc() {
		return orMarkFunc;
	}

	public void setOrMarkFunc(String orMarkFunc) {
		this.orMarkFunc = orMarkFunc;
	}

	public String getOrUnmarkFunc() {
		return orUnmarkFunc;
	}

	public void setOrUnmarkFunc(String orUnmarkFunc) {
		this.orUnmarkFunc = orUnmarkFunc;
	}

	@Override
	public void restoreState(Object object) {
		orMarked = (Boolean)((Object[])object)[0];
		andMarked = (Boolean)((Object[])object)[1];
		forward_state = (RegisterState)((Object[])object)[2];
		backward_state = (RegisterState)((Object[])object)[3];
		or_waiting = false;
		and_waiting = false;
		can_work = false;
	}

	@Override
	public Object saveState() {
		Object[] s = new Object[4];
		s[0] = orMarked;
		s[1] = andMarked;
		s[2] = forward_state;
		s[3] = backward_state;
		return s;
	}
}
