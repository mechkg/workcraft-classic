package workcraft.unfolding;

import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import javax.media.opengl.GL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.JOGLPainter;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.TextAlign;
import workcraft.visual.VertexBuffer;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public class EditableEvent extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("23a72f18-5c90-11dc-8314-0800200c9a66");
	public static final String _displayname = "Event";
	private static VertexBuffer geometry = null;

	private LinkedList<EditableCondition> out;
	private LinkedList<EditableCondition> in;

	public boolean canFire = false;
	public boolean canWork = false;

	private boolean isCutOff = false;
	private EditableEvent corresponding = null;
	
	private static final Vec2 delayLabelLocation1 = new Vec2(-0.055f, 0.02f);
	private static final Vec2 delayLabelLocation2 = new Vec2(-0.055f, -0.03f);
	private Vec2 v = new Vec2();
	
	protected double delayMin = 0.0f;
	protected double delayMax = 0.0f;
	protected double cumulativeDelayMin = 0.0f;
	protected double cumulativeDelayMax = 0.0f;
		

	private static Colorf corrColor = new Colorf(0.0f, 0.7f, 0.5f, 1.0f);
	private static Colorf transitionColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedTransitionColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf enabledTransitionColor = new Colorf(0.6f, 0.6f, 1.0f, 1.0f);
	private static Colorf transitionOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedTransitionOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf userTransitionOutlineColor = new Colorf(0.0f, 0.6f, 0.0f, 1.0f);


	public LinkedList<EditableCondition> getOut() {
		return (LinkedList<EditableCondition>)out.clone();
	}

	public LinkedList<EditableCondition> getIn() {
		return (LinkedList<EditableCondition>)in.clone();
	}
	public void removeIn(EditableCondition t) {
		in.remove(t);
	}

	public void removeOut(EditableCondition t) {
		out.remove(t);
	}

	public boolean addIn(DefaultConnection con) {
		EditableCondition t = (EditableCondition)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con) {
		EditableCondition t = (EditableCondition)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}

	public EditableEvent(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f));
		in = new LinkedList<EditableCondition>();
		out = new LinkedList<EditableCondition>();
	}

	public void doDraw(Painter p) {
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);
		
		if (corresponding != null) {
			p.setLineMode(LineMode.HAIRLINE);
			p.setLineColor(corrColor);
			
			Vec2 v0 = new Vec2();
			Vec2 v1 = new Vec2();
			
			transform.getFinalInvMatrix().transform(v0);
			corresponding.transform.getFinalInvMatrix().transform(v1);
			
			v1.sub(v0);
		
			
			p.drawLine(new Vec2(), v1);
		}

		if (selected)
			p.setFillColor(selectedTransitionOutlineColor);
		else
			if (canWork)
				p.setFillColor(userTransitionOutlineColor);
			else
				p.setFillColor(transitionOutlineColor);

		p.drawRect(-0.05f, 0.05f, 0.05f, -0.05f);		

		if (selected)
			p.setFillColor(selectedTransitionColor);
		else
			if (canFire)
				p.setFillColor(enabledTransitionColor);
			else
				p.setFillColor(transitionColor);

		p.drawRect(-0.04f, 0.04f, 0.04f, -0.04f);

		if (isCutOff) {
			p.setShapeMode(ShapeMode.OUTLINE);
			p.setLineMode(LineMode.SOLID);
			p.setLineWidth(0.005f);
			if (selected)
				p.setLineColor(selectedTransitionOutlineColor);
			else
				if (canWork)
					p.setLineColor(userTransitionOutlineColor);
				else
					p.setLineColor(transitionOutlineColor);
			
			p.drawRect(-0.03f, 0.03f, 0.03f, -0.03f);
		}
		
		
		if ( ((UnfoldingModel)ownerDocument).isTimingAttached() ) {
			v.copy(delayLabelLocation1);
			transform.getLocalToViewMatrix().transform(v);
			p.drawString("["+delayMin+","+delayMax+"]", v, 0.04f, TextAlign.RIGHT);
			
			v.copy(delayLabelLocation2);
			transform.getLocalToViewMatrix().transform(v);
			p.drawString("["+cumulativeDelayMin+","+cumulativeDelayMax+"]", v, 0.04f, TextAlign.RIGHT);
			
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
	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("event");
		Element te = (Element) nl.item(0);
		super.fromXmlDom(element);		
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element); 
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("event");
		ee.appendChild(ppe);
		return ee;
	}

	public void simAction(int flag) {
		if (flag == MouseEvent.BUTTON1) {
			canWork = !canWork;
		}
	}

	public EditableEvent getCorresponding() {
		return corresponding;
	}

	public void setCorresponding(EditableEvent corresponding) {
		this.corresponding = corresponding;
	}

	public boolean isCutOff() {
		return isCutOff;
	}

	public void setCutOff(boolean isCutOff) {
		this.isCutOff = isCutOff;
	}

	public double getDelayMin() {
		return delayMin;
	}

	public void setDelayMin(double delayMin) {
		this.delayMin = delayMin;
	}

	public double getDelayMax() {
		return delayMax;
	}

	public void setDelayMax(double delayMax) {
		this.delayMax = delayMax;
	}	
}