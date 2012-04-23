package workcraft.editor;

import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import workcraft.XmlSerializable;
import workcraft.util.Vec2;
import workcraft.visual.Drawable;
import workcraft.visual.Painter;

public abstract class EditableConnection implements XmlSerializable, Drawable, PropertyEditable {
	protected BasicEditable first = null;
	protected BasicEditable second = null;
	public LinkedList <EditableAnchor> anchors = new LinkedList <EditableAnchor>();
	
	public boolean selected;
	
	public EditableConnection() {
		this(null, null);
	}
	
	public EditableConnection (BasicEditable first, BasicEditable second) {
		this.first = first;
		this.second = second;
	}
	
	public BasicEditable getFirst() {
		return first;
	}
	
	public BasicEditable getSecond() {
		return second;
	}
	
	public void setFirst(BasicEditable first) {
		this.first = first;
	}
	
	public void setSecond(BasicEditable second) {
		this.second = second;
	}

	public Element createEditableConnectionXmlElement (Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element ee = d.createElement("editable-connection");
		ee.setAttribute("selected", Boolean.toString(selected));
		ee.setAttribute("first", first.getId());
		ee.setAttribute("second", second.getId());
		parent_element.appendChild(ee);
		return ee;
	}
	
	public void draw(Painter p) {
		for(EditableAnchor a : anchors) {
			a.draw(p);
		}
	}
	
	public abstract void updatePointsFromAnchors();
	public abstract boolean acceptDynamicAnchors();
	public abstract void addDynamicAnchor(Vec2 point);
	public abstract void removeAnchor(EditableAnchor anc);
	
	public void select() {
		selected = true;
	}
	
	public void deselect() {
		anchors.clear();
		selected = false;
	}
	
	public float hitTest(Vec2 pointInViewSpace) {
		return -1;
	}
}