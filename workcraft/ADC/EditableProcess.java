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
import workcraft.WorkCraftServer;
import workcraft.ADC.EditableReader;
import workcraft.ADC.EditableWriter;
import workcraft.common.DefaultConnection;
import workcraft.editor.BasicEditable;
import workcraft.editor.EditableConnection;
import workcraft.sdfs.LogicState;
import workcraft.sdfs.SDFSModelBase;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.JOGLPainter;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import workcraft.visual.TextAlign;
import workcraft.visual.VertexBuffer;
import workcraft.visual.GeometryUtil;
import workcraft.visual.VertexFormat;
import workcraft.visual.PrimitiveType;
import workcraft.visual.VertexFormatException;

public class EditableProcess extends BasicEditable {
	public static final UUID _modeluuid = UUID.fromString("3c60dee8-0545-11dc-8314-0800200c9a66");
	public static final String _displayname = "Process";

	private static Colorf processColor = new Colorf(1.0f, 1.0f, 1.0f, 1.0f);
	private static Colorf selectedProcessColor = new Colorf(1.0f, 0.9f, 0.9f, 1.0f);
	private static Colorf processOutlineColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	private static Colorf selectedProcessOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);
	private static Colorf finishedProcessColor = new Colorf ( 0.6f, 1.0f, 0.6f, 1.0f);
	
	//private static Colorf enabledProcessColor = new Colorf(0.6f, 0.6f, 1.0f, 1.0f);
	//private static Colorf userProcessOutlineColor = new Colorf(0.0f, 0.6f, 0.0f, 1.0f);
	
	private int processingDelay;
	private int cooldownDelay;
	
	private int delay = 0;
	private long start = 0;
	private float progress = 0.0f;
	private ProcessState state = ProcessState.READY;
	private boolean can_process = true;
	private boolean can_cooldown = true;
	
	//public boolean canFire = false;
	//public boolean canWork = false;
	
	private static float width = 0.09f;
	private static float height = 0.06f;

	private LinkedList<EditableReader> in;
	private LinkedList<EditableWriter> out;

	public LinkedList<EditableWriter> getOut()
	{
		return (LinkedList<EditableWriter>)out.clone();
	}

	public LinkedList<EditableReader> getIn()
	{
		return (LinkedList<EditableReader>)in.clone();
	}

	public void removeIn(EditableReader t)
	{
		in.remove(t);
	}

	public void removeOut(EditableWriter t)
	{
		out.remove(t);
	}

	public boolean addIn(DefaultConnection con)
	{
		EditableReader t = (EditableReader)con.getFirst();
		if (in.contains(t))
			return false;
		in.add(t);
		connections.add(con);
		return true;
	}

	public boolean addOut(DefaultConnection con)
	{
		EditableWriter t = (EditableWriter)con.getSecond();
		if (out.contains(t))
			return false;
		out.add(t);
		connections.add(con);
		return true;
	}

	public int getProcessingDelay()
	{
		return processingDelay;
	}
	
	public void setProcessingDelay(Integer t)
	{
		processingDelay = t;
	}
	
	public int getCooldownDelay()
	{
		return cooldownDelay;
	}
	
	public void setCooldownDelay(Integer t)
	{
		cooldownDelay = t;
	}	
	
	
	public EditableProcess(BasicEditable parent) throws UnsupportedComponentException
	{
		super(parent);
		boundingBox.setExtents(new Vec2(-width, -height), new Vec2(width, height));
		
		setProcessingDelay(1000);
		setCooldownDelay(500);
		
		out = new LinkedList<EditableWriter>();
		in = new LinkedList<EditableReader>();
	}

	public void draw(Painter p)
	{	
		p.setTransform(transform.getLocalToViewMatrix());
		p.setShapeMode(ShapeMode.FILL);

		if (selected)
			p.setFillColor(selectedProcessOutlineColor);
		else
			p.setFillColor(processOutlineColor);

		p.drawRect(-width, -height, width, height);	
		
		if (state == ProcessState.READY || state == ProcessState.PROCESSING)
			p.setFillColor(processColor);
		else
			p.setFillColor(finishedProcessColor);
		
		p.drawRect(-width * 0.8f, -height * 0.8f, width * 0.8f, height * 0.8f);	

		if (state == ProcessState.READY || state == ProcessState.PROCESSING)
			p.setFillColor(finishedProcessColor);
		else
			p.setFillColor(processColor);
		
		p.drawRect(-width * 0.8f, -height * 0.8f, width * (-0.8f + 1.6f * progress), height * 0.8f);
			
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
		list.add("int,Proc delay,getProcessingDelay,setProcessingDelay");
		list.add("int,Cool delay,getCooldownDelay,setCooldownDelay");
		return list;
	}


	public void fromXmlDom(Element element) throws DuplicateIdException
	{
		NodeList nl = element.getElementsByTagName("process");
		Element ne = (Element) nl.item(0);
		setProcessingDelay(Integer.parseInt(ne.getAttribute("processingDelay")));
		setCooldownDelay(Integer.parseInt(ne.getAttribute("cooldownDelay")));
		super.fromXmlDom(element);
	}
	
	public Element toXmlDom(Element parent_element)
	{
		Element ee = super.toXmlDom(parent_element);
		Document d = ee.getOwnerDocument();
		Element ppe = d.createElement("process");
		ppe.setAttribute("processingDelay", Integer.toString(getProcessingDelay()));
		ppe.setAttribute("cooldownDelay", Integer.toString(getCooldownDelay()));
		ee.appendChild(ppe);
		return ee;
	}
	
	public void simTick(long time)
	{
		switch (state)
		{
		case READY:
			if (can_process)
			{
				state = ProcessState.PROCESSING;
				progress = 0.0f;
				delay = processingDelay;
				start = time;
				can_process = false;
			}
			break;
		case PROCESSING:
			if (time > start + delay)
			{
				state = ProcessState.FINISHED;
				progress = 0.0f;
			}
			else
			{
				progress = (float)(time - start) / delay;
			}
			break;
		case FINISHED:
			if (can_cooldown)
			{
				state = ProcessState.COOLINGDOWN;
				progress = 0.0f;
				delay = cooldownDelay; 
				start = time;
				can_cooldown = false;
			}
			break;		
		case COOLINGDOWN:
			if (time > start + delay)
			{
				state = ProcessState.READY;
				progress = 0.0f;
			}
			else
			{ 
				progress = (float)(time - start) / delay;
			}
			break;
		}
	}	
}