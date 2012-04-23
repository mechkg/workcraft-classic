package workcraft.abstractsdfs;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
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
import workcraft.sdfs.SDFSNode;
import workcraft.sdfs.SDFSRegisterBase;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.BlendMode;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;

public class ASDFSRegister extends SDFSRegisterBase {
	public static final UUID _modeluuid = UUID.fromString("24a8a176-dafe-11db-8314-0800200c9a66");
	public static final String _displayname = "Register";
	public static final String _hotkey = "q";
	public static final int  _hotkeyvk = KeyEvent.VK_Q;

	private static Colorf token_color = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	private boolean marked;
	private RegisterState state = RegisterState.DISABLED;
	private boolean can_work = false;
	private boolean waiting_for_user = false;

	private LinkedList<SDFSNode> out = new LinkedList<SDFSNode>();
	private LinkedList<SDFSNode> in = new LinkedList<SDFSNode>();

	private String markFunc;// = "";
	private String unmarkFunc;// = "";
	private String enableFunc; //= "";
	private String disableFunc; //= "";

	
	public ASDFSRegister(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f) );
		marked = false;
	}

	@Override
	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());
		
		drawFrame(p);
				
		if (state == RegisterState.ENABLED)
			p.setFillColor(enabled_color);
		else
			p.setFillColor(disabled_color);

		p.drawRect(-0.025f, 0.045f, 0.025f, -0.045f);

		if (waiting_for_user) {
			p.blendEnable();
			p.setBlendConstantAlpha(0.5f);
			p.setBlendMode(BlendMode.CONSTANT_ALPHA);

			if (state == RegisterState.DISABLED) {
				p.setFillColor(token_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawCircle(0.015f, 0.0f, 0.025f);
			} else {
				p.setFillColor(enabled_color);
				p.setLineColor(token_color);
				p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
				p.setLineMode(LineMode.SOLID);
				p.setLineWidth(0.0025f);
				p.drawCircle(0.015f, 0.0f, 0.025f);
			}
			p.blendDisable();
		} else {
			if (marked) {
				p.setFillColor(token_color);
				p.setShapeMode(ShapeMode.FILL);
				p.drawCircle(0.015f, 0.0f, 0.025f);

			} 	
		}
		super.doDraw(p);
	}

	@Override
	public BasicEditable getChildAt(Vec2 point) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(Mat4x4 matView) {
		// TODO Auto-generated method stub

	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("bool,Marked,isMarked,setMarked");
		list.add("str,Enabling,getEnableFunc,setEnableFunc");
		list.add("str,Disabling,getDisableFunc,setDisableFunc");
		list.add("str,Marking,getMarkFunc,setMarkFunc");
		list.add("str,Unmarking,getUnmarkFunc,setUnmarkFunc");
		return list;
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("st-register");
		Element ne = (Element) nl.item(0);
		setMarked(Boolean.parseBoolean(ne.getAttribute("marked")));
		markFunc = ne.getAttribute("mark-func");
		unmarkFunc = ne.getAttribute("unmark-func");
		enableFunc = ne.getAttribute("enable-func");
		disableFunc = ne.getAttribute("disable-func");
		super.fromXmlDom(element);		
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("st-register");
		ppe.setAttribute("marked", Boolean.toString(isMarked()));
		ppe.setAttribute("mark-func", markFunc);
		ppe.setAttribute("unmark-func", unmarkFunc);		
		ppe.setAttribute("enable-func", enableFunc);
		ppe.setAttribute("disable-func", disableFunc);
		ee.appendChild(ppe);
		return ee;
	}

	public boolean simTick(int time_ms) {
		WorkCraftServer server = ownerDocument.getServer();
		switch (state) {
		case ENABLED:
			if (!isMarked()) {
				if (server.python.eval(markFunc).__nonzero__()) {
					if ( ((ASDFSModel)ownerDocument) .isUserInteractionEnabled())
					{
						if (can_work) {
							setMarked(true);
							waiting_for_user = false;
							can_work = false;
						} else {
							waiting_for_user = true;
						}
					}
					else
					{
						setMarked(true);
					}
				}

			} else {
				if (server.python.eval(disableFunc).__nonzero__()) {
					state = RegisterState.DISABLED;
				}
			}
			break;
		case DISABLED:
			if (isMarked()) {
				boolean can_unmark = server.python.eval(unmarkFunc).__nonzero__();
				if (can_unmark) {
					if ( ((ASDFSModel)ownerDocument).isUserInteractionEnabled() ) {
						if (can_work) {
							setMarked(false);
							can_work = false;
							waiting_for_user = false;
						} else {
							waiting_for_user = true;
						}
					}
					else
						setMarked(false);
				}
			} else {
				if ( server.python.eval(enableFunc).__nonzero__()) {
					state = RegisterState.ENABLED;
				}
			}
			break;				
		}
		
		return true;
	}

	@Override
	public void rebuildRuleFunctions() {
	/*	markFunc = expandRule("self re,r-preset m,r-postset !m");
		unmarkFunc = expandRule("self !re,r-preset !m,r-postset m");
		enableFunc = expandRule("self !m,preset:l e,preset:r m");
		disableFunc = expandRule("self m,preset:l r,preset:r !m");*/
	}

	public void simAction(int flag) {
		if (flag == MouseEvent.BUTTON1) {
			can_work = !can_work;
		}
	}

	public Boolean isMarked() {
		return marked;
	}
	
	public void setMarked(Boolean marked) {
		this.marked = marked;
	}

	public boolean isEnabled() {
		return state == RegisterState.ENABLED;
	}

	public boolean canWork() {
		return can_work;
	}

	public void setState(RegisterState state) {
		this.state = state;
	}

	public RegisterState getState() {
		return state;
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

	public String getEnableFunc() {
		return enableFunc;
	}

	public void setEnableFunc(String enableFunc) {
		this.enableFunc = enableFunc;
	}

	public String getDisableFunc() {
		return disableFunc;
	}

	public void setDisableFunc(String disableFunc) {
		this.disableFunc = disableFunc;
	}
}
