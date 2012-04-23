package workcraft.ADC;

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
import workcraft.visual.GeometryUtil;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public class EditableACM extends BasicEditable
{
	public static final UUID _modeluuid = UUID.fromString("3c60dee8-0545-11dc-8314-0800200c9a66");
	public static final String _displayname = "ACM";
	
	private static Colorf ACMColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedACMColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf ACMOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedACMOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf validTokenColor = new Colorf(0.0f, 0.3f, 0.0f, 1.0f);
	private static Colorf invalidTokenColor = new Colorf(0.3f, 0.0f, 0.0f, 1.0f);
	
	private static float radius = 0.08f;
	private static float innerRadius = 0.07f;

	private boolean blockReading = false;
	private boolean blockWriting = false;
	
	private int capacity;
	private ADCToken [] tokens;	
	
	private LinkedList<EditableWriter> in;
	private LinkedList<EditableReader> out;

	public LinkedList<EditableReader> getOut()
	{
		return (LinkedList<EditableReader>)out.clone();
	}

	public LinkedList<EditableWriter> getIn()
	{
		return (LinkedList<EditableWriter>)in.clone();
	}

	public void removeIn(EditableWriter t)
	{
		in.remove(t);
	}

	public void removeOut(EditableReader t)
	{
		out.remove(t);
	}

	public boolean addIn(DefaultConnection con)
	{
		EditableWriter t = (EditableWriter)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con)
	{
		EditableReader t = (EditableReader)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}
	
	public void setCapacity(Integer t)
	{
		capacity = t;
		ADCToken [] old = tokens;
		tokens = new ADCToken[capacity];
		if (old == null)
		{
			for(int i=0; i < capacity; i++) tokens[i] = new ADCToken();
		}
		else
		{
			for(int i = 0; i < capacity; i++)
				if (i < old.length) tokens[i] = old[i];
				else tokens[i] = new ADCToken();
		}
	}

	public int getCapacity()
	{
		return capacity;
	}
	
	public void setBlockReading(Boolean b)
	{
		blockReading = b;
	}
	
	public boolean getBlockReading()
	{
		return blockReading;
	}
	
	public void setBlockWriting(Boolean b)
	{
		blockWriting = b;
	}
	
	public boolean getBlockWriting()
	{
		return blockWriting;
	}
	
	public int countValidTokens()
	{
		int res = 0;
		for(int i = 0; i < capacity; i++) if (tokens[i].valid) res++;
		return res;
	}
	
	public boolean canWrite()
	{
		// non blocking
		if (!blockWriting) return true;
		return (countValidTokens() < capacity);
	}

	public boolean canRead()
	{
		// non blocking
		if (!blockReading) return true;
		return (countValidTokens() > 0);
	}
	
	public boolean write(ADCToken token)
	{
		// try to find invalid token
		
		for(int i = 0; i < capacity; i++)
			if (!tokens[i].valid)
			{
				tokens[i] = new ADCToken(token);
				tokens[i].valid = true;
				return true;
			}
		
		// overwrite the oldest token
		
		int oldest = 0;
		for(int i = 1; i < capacity; i++)
			if (tokens[i].time < tokens[oldest].time) oldest = i;
		
		tokens[oldest] = new ADCToken(token);
		tokens[oldest].valid = true;
		
		return false;
	}
	
	public ADCToken read()
	{
		// find the oldest valid token
		
		int to_read = -1;
		for(int i = 0; i < capacity; i++)
			if (tokens[i].valid)
			if (to_read == -1 || tokens[i].time < tokens[to_read].time) to_read = i;
		
		if (to_read == -1)
		{
			// all the tokens are invalid. find the newest of them
			
			for(int i = 0; i < capacity; i++)
				if (to_read == -1 || tokens[i].time > tokens[to_read].time) to_read = i;
		}
		
		ADCToken res = new ADCToken(tokens[to_read]);
		tokens[to_read].valid = false;
		
		return res;
	}

	public EditableACM(BasicEditable parent) throws UnsupportedComponentException
	{
		super(parent);
		boundingBox.setExtents(new Vec2(-radius, -radius), new Vec2(radius, radius));
		setCapacity(1);
		out = new LinkedList<EditableReader>();
		in = new LinkedList<EditableWriter>();
	}
	
	@Override
	public boolean hits(Vec2 pointInViewSpace) {
		Vec2 v = new Vec2(pointInViewSpace);
		transform.getViewToLocalMatrix().transform(v);
		return v.length() < radius;
	}
	
	public void draw(Painter p)
	{		
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);		
		
		if (selected)
			p.setFillColor(selectedACMOutlineColor);
		else
			p.setFillColor(ACMOutlineColor);

		p.drawCircle(radius, null);
		
		if (selected)
			p.setFillColor(selectedACMColor);
		else
			p.setFillColor(ACMColor);

		p.drawCircle(innerRadius, null);
		
		if (selected)
			p.setLineColor(selectedACMOutlineColor);
		else
			p.setLineColor(ACMOutlineColor);

		float L = -radius, R = radius;
		if (blockWriting) L *= 0.6f;
		if (blockReading) R *= 0.6f;
		
		p.setLineMode(LineMode.SOLID);
		p.setLineWidth(0.01f);
		p.drawLine(L, 0.0f, R, 0.0f);
		
		p.setLineWidth(0.004f);
		if (blockWriting) p.drawLine(L, -radius * 0.75f, L, radius * 0.75f);
		if (blockReading) p.drawLine(R, -radius * 0.75f, R, radius * 0.75f);	
		
		String n = Integer.toString(countValidTokens());
		float h = radius * 0.8f;
		Vec2 v = new Vec2(0, radius * 0.2f);
		transform.getLocalToViewMatrix().transform(v);
		p.setTextColor(validTokenColor);
		p.drawString(n, v, h, TextAlign.CENTER);		
		
		n = Integer.toString(capacity - countValidTokens());
		h = radius * 0.8f;
		v = new Vec2(0, -radius * 0.7f);
		transform.getLocalToViewMatrix().transform(v);
		p.setTextColor(invalidTokenColor);
		p.drawString(n, v, h, TextAlign.CENTER);
		
		super.draw(p);
	}

	@Override
	public BasicEditable getChildAt(Vec2 point)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(Mat4x4 matView)
	{
		// TODO Auto-generated method stub

	}

	public List<String> getEditableProperties()
	{
		List<String> list = super.getEditableProperties();
		list.add("int,Capacity,getCapacity,setCapacity");
		list.add("bool,BlockReading,getBlockReading,setBlockReading");
		list.add("bool,BlockWriting,getBlockWriting,setBlockWriting");
		return list;
	}


	public void fromXmlDom(Element element) throws DuplicateIdException
	{
		NodeList nl = element.getElementsByTagName("ACM");
		Element ne = (Element) nl.item(0);
		setCapacity(Integer.parseInt(ne.getAttribute("capacity")));
		setBlockReading(Boolean.parseBoolean(ne.getAttribute("blockReading")));
		setBlockWriting(Boolean.parseBoolean(ne.getAttribute("blockWriting")));
		super.fromXmlDom(element);
	}
	
	public Element toXmlDom(Element parent_element)
	{
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("ACM");
		ppe.setAttribute("capacity", Integer.toString(getCapacity()));
		ppe.setAttribute("blockReading", Boolean.toString(getBlockReading()));
		ppe.setAttribute("blockWriting", Boolean.toString(getBlockWriting()));
		ee.appendChild(ppe);
		return ee;
	}

}