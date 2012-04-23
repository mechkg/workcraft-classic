package workcraft.sdfs;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.media.opengl.GL;

import org.python.core.PyObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.ModelBase;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.sdfs.SDFSLogicBase;
import workcraft.sdfs.SDFSNode;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public abstract class SDFSLogic1Way extends SDFSLogicBase {
	protected LogicState state = LogicState.RESET;
	private boolean can_evaluate = false;

	private int eval_delay = 300;
	private int reset_delay = 300;
	protected int event_start;
	private int event_delay;
	private float event_progress = 0.0f;

	private static final Colorf reset = new Colorf (1.0f, 1.0f, 1.0f, 1.0f);
	private static final Colorf frameColor = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	private static final Colorf selectedColor = new Colorf (0.5f, 0.0f, 0.0f, 1.0f);
	private static final Colorf evaluated = new Colorf ( 0.6f, 0.6f, 1.0f, 1.0f);

	protected String evalFunc;
	protected String resetFunc;

	public SDFSLogic1Way(BasicEditable parent)  throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
	}

	@Override
	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());

		p.setShapeMode(ShapeMode.FILL_AND_OUTLINE);
		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.005f);
		p.setLineColor(selectedColor);

		if (selected)
			p.setLineColor(selectedColor);
		else
			if (highlight && ((ModelBase)ownerDocument).isShowHighlight())
				p.setLineColor(((ModelBase)ownerDocument).getHighlightColor());
			else
				p.setLineColor(frameColor);

		Colorf c;

		switch (state) {
		case EVALUATED:
			p.setFillColor(evaluated);
			break;
		case EVALUATING:
			c = Colorf.lerp( evaluated, reset, event_progress);
			p.setFillColor(c);
			break;
		case RESETTING:
			c = Colorf.lerp( reset, evaluated, event_progress);
			p.setFillColor(c);
			break;
		case RESET:
			p.setFillColor(reset);
			break;
		}

		p.drawRect(-0.0475f, 0.0475f, 0.0475f, -0.0475f);

		super.doDraw(p);
	}

	public List<String> getEditableProperties() {
		List<String> list = super.getEditableProperties();
		list.add("int,Eval delay,getEvalDelay,setEvalDelay");
		list.add("int,Reset delay,getResetDelay,setResetDelay");		
		list.add("str,Evaluation,getEvalFunc,setEvalFunc");
		list.add("str,Reset,getResetFunc,setResetFunc");
		return list;
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
	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("st-logic");
		Element te = (Element) nl.item(0);
		evalFunc = te.getAttribute("eval-func");
		resetFunc = te.getAttribute("reset-func");
		super.fromXmlDom(element);
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("st-logic");
		ppe.setAttribute("eval-func", evalFunc);
		ppe.setAttribute("reset-func", resetFunc);
		ee.appendChild(ppe);
		return ee;
	}
	
	public abstract void rebuildRuleFunctions();
	
	public boolean simTick(int time_ms) {
		WorkCraftServer server = ownerDocument.getServer();
		SDFSModelBase doc = (SDFSModelBase)ownerDocument;

		switch (state) {
		case RESET:
			if (evalFunc.equals(""))
				break;
			boolean can_evaluate = server.python.eval(evalFunc).__nonzero__();
			if (can_evaluate) {
				state = LogicState.EVALUATING;
				event_progress = 0.0f;
				event_delay = eval_delay; // (int)(rnd.nextGaussian()*avg_delay*2);
				event_start = time_ms;
				can_evaluate = false;
			}
			break;
		case RESETTING:
			if (time_ms > event_start+event_delay) {
				state = LogicState.RESET;
				doc.addTraceEvent(this.getId()+"/reset");
				return true;
			}
			else {
				event_progress = (float)(time_ms - event_start) / (float)(event_delay);
				break;
			}
		case EVALUATING:
			if (time_ms > event_start+event_delay) {
				state = LogicState.EVALUATED;
				doc.addTraceEvent(this.getId()+"/eval");
				return true;
			}
			else { 
				event_progress = (float)(time_ms - event_start) / (float)(event_delay);
				break;
			}
		case EVALUATED:
			if (resetFunc.equals(""))
				break;
			boolean can_reset = server.python.eval(resetFunc).__nonzero__();
			if (can_reset) {
				state = LogicState.RESETTING;
				event_progress = 0.0f;
				event_delay = reset_delay; 
				event_start = time_ms;
			}
			break;
		}
		
		return false;
	}

	public void simAction(int flag) {
		if (flag == MouseEvent.BUTTON1) {
			can_evaluate = true;
		}
	}

	@Override
	public void restoreState(Object object) {
		state = (LogicState)object;
	}

	@Override
	public Object saveState() {
		return state;
	}

	public boolean canEvaluate() {
		return can_evaluate;
	}

	public void setEvalDelay(Integer eval_delay) {
		this.eval_delay = eval_delay;
		if (this.eval_delay<0)
			this.eval_delay = 0;
		if (this.eval_delay>2000)
			this.eval_delay = 2000;
	}

	public int getEvalDelay() {
		return eval_delay;
	}

	public void setResetDelay(Integer reset_delay) {
		this.reset_delay = reset_delay;
		if (this.reset_delay<0)
			this.reset_delay = 0;
		if (this.reset_delay>2000)
			this.reset_delay = 2000;
	}

	public int getResetDelay() {
		return reset_delay;
	}

	public void setState(LogicState state) {
		this.state = state;
	}

	public LogicState getState() {
		return state;
	}

	public void setEvalFunc(String evalFunc) {
		this.evalFunc = evalFunc;
	}

	public String getEvalFunc() {
		return evalFunc;
	}

	public void setResetFunc(String resetFunc) {
		this.resetFunc = resetFunc;
	}

	public String getResetFunc() {
		return resetFunc;
	}
	
	public boolean isEvaluated() {
		return (state == LogicState.EVALUATED);
	}

	public boolean isReset() {
		return (state == LogicState.RESET);
	}

}