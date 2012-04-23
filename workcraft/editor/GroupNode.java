package workcraft.editor;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.TreeSet;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.Painter;

public class GroupNode extends BasicEditable {
	
	/* (non-Javadoc)
	 * @see workcraft.editor.EditableNode#getChildAt(workcraft.util.Vec2)
	 */
	
	public GroupNode (Model owner, String id)  {
		this.zOrder = 0;
		this.parent = null;
		this.id = id;
		this.children = new TreeSet<BasicEditable>();
		transform = new TransformNode(this);
		boundingBox = new BoundingBox();
		
		ownerDocument = owner;
	}
	
	public GroupNode ()  {
		super();
	}
	
	@Override
	public BasicEditable getChildAt(Vec2 point) {
		for (BasicEditable n : children ) {
			if (n.hitsBB(point)) {
				return n;				
			}
			else {
				BasicEditable ch = n.getChildAt(point);
				if(ch!=null)
					return ch;
			}
		}
		return null;
	}
	
	public EditableConnection getChildConnectionAt(Vec2 point) {
		float min = Float.MAX_VALUE;
		EditableConnection nearest = null;
		for (BasicEditable n : children ) {
			for(EditableConnection con : n.getAllConnections()) {
				float d = con.hitTest(point);
				if(d>=0 && d<min) {
					min = d;
					nearest = con;
				}
			}
			TreeSet<BasicEditable> ch =	n.getChildren();
			for (BasicEditable nch : ch ) {
				for(EditableConnection con : nch.getAllConnections()) {
					float d = con.hitTest(point);
					if(d>=0 && d<min) {
						min = d;
						nearest = con;
					}
				}
			}
		}
		return nearest;
	}

	public void fromXmlDom(Element element) throws DuplicateIdException {
		super.fromXmlDom(element);
		NodeList nl = element.getElementsByTagName("editable");
		for (int i=0; i<nl.getLength(); i++ ) {
			Element e = (Element)nl.item(i);
			String class_name = e.getAttribute("class");
			try {
				Class cls = ClassLoader.getSystemClassLoader().loadClass(class_name);
				Constructor ctor = cls.getDeclaredConstructor(BasicEditable.class);
				BasicEditable n = (BasicEditable)ctor.newInstance(this);
				n.fromXmlDom(e);
				addChild(n);
			} catch (ClassNotFoundException ex) {
				System.err.println("Failed to load class: "+ex.getMessage());
				ex.printStackTrace();
			} catch (InstantiationException ex) {
				ex.printStackTrace();
			} catch (IllegalAccessException ex) {
				ex.printStackTrace();
			} catch (IllegalArgumentException ex) {
				ex.printStackTrace();
			} catch (InvocationTargetException ex) {
				ex.getTargetException().printStackTrace();
				ex.printStackTrace();
			} catch (SecurityException ex) {
				ex.printStackTrace();
			} catch (NoSuchMethodException ex) {
				ex.printStackTrace();
			}
		}
	}

	public Element toXmlDom(Element parent_element) {
		Element ee = super.toXmlDom(parent_element);
		for (BasicEditable n: children) {
			n.toXmlDom(ee);
		}
		return ee;
	}

	@Override
	public void update(Mat4x4 matView) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public void draw(Painter p) {
		for (BasicEditable n: children) {
			n.draw(p);
		}

	}

}