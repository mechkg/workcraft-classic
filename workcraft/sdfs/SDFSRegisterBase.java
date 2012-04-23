package workcraft.sdfs;


import java.awt.event.MouseEvent;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.ModelBase;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.sdfs.SDFSNode;
import workcraft.sdfs.SDFSRegisterBase;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;

public abstract class SDFSRegisterBase extends SDFSNode  {
	protected static Colorf disabled_color = new Colorf (1.0f, 1.0f, 1.0f, 1.0f);
	protected static Colorf enabled_color = new Colorf (0.6f, 1.0f, 0.6f, 1.0f);
	protected static Colorf active_frame_color = new Colorf (0.0f, 0.8f, 0.0f, 1.0f);
	protected static Colorf inactive_frame_color = new Colorf (0.0f, 0.0f, 0.0f, 1.0f);
	protected static Colorf selected_frame_color = new Colorf (0.5f, 0.0f, 0.0f, 1.0f);

	protected boolean can_work;

	public SDFSRegisterBase(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
		boundingBox.setExtents(new Vec2(-0.05f, -0.05f), new Vec2(0.05f, 0.05f) );
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
		// TODO: static colors editor
		return list;
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		NodeList nl = element.getElementsByTagName("sdfs-register");
		Element ne = (Element) nl.item(0);

		super.fromXmlDom(element);		
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("sdfs-register");
		ee.appendChild(ppe);
		return ee;
	}

	public void drawFrame(Painter p) {
		Colorf frame_color;

		if (selected)
			frame_color = selected_frame_color;
		else
			if (can_work)
				frame_color = active_frame_color;
			else
				if (highlight && ((ModelBase)ownerDocument).isShowHighlight())
					frame_color = (((ModelBase)ownerDocument).getHighlightColor());
				else
					frame_color = inactive_frame_color;

		p.setShapeMode(ShapeMode.FILL);
		p.setFillColor(frame_color);

		p.drawRect(-0.05f, 0.05f, 0.05f, -0.05f);

		p.setFillColor(disabled_color);
		p.drawRect(-0.045f, 0.045f, -0.03f, -0.045f); 
		p.drawRect(0.03f, 0.045f, 0.045f, -0.045f);

	}

	public void simAction(int flag) {
		if (flag == MouseEvent.BUTTON1) {
			can_work = !can_work;
		}
	}

	public boolean canWork() {
		return can_work;
	}

}
