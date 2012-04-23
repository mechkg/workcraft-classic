package workcraft.editor;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLJPanel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import workcraft.DuplicateIdException;
import workcraft.InvalidConnectionException;
import workcraft.Model;
import workcraft.ModelBase;
import workcraft.ModelManager;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.XmlSerializable;
import workcraft.util.Colorf;
import workcraft.util.Vec2;
import workcraft.util.Vec3;
import workcraft.util.ViewState;
import workcraft.visual.BlendMode;
import workcraft.visual.JOGLPainter;
import workcraft.visual.Drawable;
import workcraft.visual.Grid;
import workcraft.visual.LineMode;
import workcraft.visual.Painter;
import workcraft.visual.ShapeMode;
import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;


public class EditorPane extends GLJPanel implements GLEventListener, DropTargetListener, XmlSerializable {
	private DropTarget dropTarget;
	public Grid grid;
	private ViewState view;
	private Point prev_cursor;
	private Painter painter;
	private JOGLPainter joglpainter;

	private Model document = null;
//	private Tester tester = new Tester();

	private boolean pan_drag = false;
	private boolean selection_drag = false;
	private boolean move_drag = false;
	private boolean connecting = false;

	private Vec2 sel_anchor = new Vec2();
	private Vec2 sel_ll = new Vec2();
	private Vec2 sel_ur = new Vec2();
	private Vec2 connect_from = new Vec2();
	private Vec2 connect_to = new Vec2();
	private Vec2 mouse_pos = new Vec2();

	private int mouse_x = 0;
	private int mouse_y = 0;

	private EditableConnection selected_connection = null;
	private LinkedList <BasicEditable> selection = new LinkedList<BasicEditable>();
	private boolean selection_anchors = false;
	private LinkedList <Drawable> overlays = new LinkedList<Drawable>();

	private LinkedList <BasicEditable> guideNodes = new LinkedList<BasicEditable>();

	private BasicEditable move_target = null;
	private BasicEditable connect_first = null;
	private BasicEditable connect_second = null;

	private Colorf clear_color;
	private Colorf connectionColor = new Colorf(1.0f, 0.1f, 0.1f, 1.0f);

	private Colorf selectionFillColor = new Colorf(0.9f, 0.0f, 0.0f, 1.0f);
	private Colorf selectionOutlineColor = new Colorf(0.5f, 0.0f, 0.0f, 1.0f);

	private Colorf selectionBoxFillColor = new Colorf(0.7f, 0.7f, 1.0f, 1.0f);
	private Colorf selectionBoxOutlineColor = new Colorf(0.0f, 0.0f, 1.0f, 1.0f);
	private Colorf selectionAnchorBoxFillColor = new Colorf(0.7f, 1.0f, 0.7f, 1.0f);
	private Colorf selectionAnchorBoxOutlineColor = new Colorf(0.0f, 1.0f, 0.0f, 1.0f);

	private GroupNode root = null;
	private BasicEditable new_node;
	private WorkCraftServer server;
	private PropertyEditor property_editor = null;
	private ComponentHotkey hotkey = new ComponentHotkey();

	public boolean snap_to_grid = true;
	public boolean draw_grid = true;
	public boolean show_ids = true;
	public boolean show_labels = true;

	public boolean changed = false;

	public boolean hasChanged() {
		return changed;
	}

	public void resetChanged() {
		changed = false;
	}

	private void changed() {
		changed = true;
	}

	public void resetView() {
		view.reset();
	}

	public void loadFonts(String directory) {
		joglpainter.loadFonts(directory);		
	}

	public void delete() {
		if (!selection.isEmpty() && !selection_anchors) {
			for (BasicEditable n : selection) {
				try {
					for (EditableConnection con : n.getAllConnections()) {
						if (con.getFirst()==n)
							con.getSecond().removeFromConnections(con);
						else
							con.getFirst().removeFromConnections(con);
						document.removeConnection(con);					
					}
					document.removeComponent(n);
				} catch (UnsupportedComponentException e) {
					e.printStackTrace();
				}
			}
			selection.clear();
			repaint();
		}
		else if(!selection.isEmpty() && selected_connection!=null && selected_connection.acceptDynamicAnchors()){
			for (EditableAnchor an : selected_connection.anchors) {
				if(an.selected)
					selected_connection.removeAnchor(an);
			}
			selection.clear();
			EditableConnection con_save = selected_connection;
			deselectConnection();
			con_save.select();
			selected_connection = con_save;
			repaint();
		}
		else if(selected_connection!=null) {
			try {
				deselect(true);
				selected_connection.getSecond().removeFromConnections(selected_connection);
				selected_connection.getFirst().removeFromConnections(selected_connection);
				document.removeConnection(selected_connection);
				selected_connection = null;
			} catch (UnsupportedComponentException e) {
				e.printStackTrace();
			}
			repaint();
		}

		grid.setMinorInterval(0.1f);

		updateGuidelines();
		changed();
	}

	private BasicEditable createComponent(Class cls) {
		try {
			Constructor ctor = cls.getDeclaredConstructor(BasicEditable.class);
			BasicEditable n = (BasicEditable)ctor.newInstance(document.getRoot());
			Vec2 v = grid.getClosestGridPoint(mouse_pos);
			v.negate();
			n.transform.translateAbs(v);

			updateGuidelines();
			changed();
			repaint();
			return n;
		}
		catch (InstantiationException e) {
			e.printStackTrace();
		} 
		catch (IllegalAccessException e) {
			e.printStackTrace();
		} 
		catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	public EditorPane() {
		super();

		dropTarget = new DropTarget(this, DnDConstants.ACTION_MOVE, this, true);
		grid = new Grid();
		grid.setMajorColor(new Colorf(0.5f, 0.5f, 0.5f, 1.0f));
		grid.setMinorColor(new Colorf(0.925f, 0.925f, 0.925f, 1.0f));
		view = new ViewState();

		prev_cursor = new Point();
		clear_color = new Colorf();

		addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyPressed(java.awt.event.KeyEvent e) {
				switch (e.getKeyCode()) {
				case KeyEvent.VK_MINUS:
					view.scale(1, mouse_x, mouse_y);
					repaint();
					break;
				case KeyEvent.VK_EQUALS:
					view.scale(-1, mouse_x, mouse_y);
					repaint();
					break;				
				case KeyEvent.VK_C:
					if (!e.isControlDown())
						beginConnection();												
					break;
				case KeyEvent.VK_ESCAPE:
					cancelConnection();
					break;
				case KeyEvent.VK_DELETE:
					delete();
					break;
				default:
					Class cls = hotKeyGet(e.getKeyCode());
				if (cls!=null) {
					createComponent(cls);
				}
				}
			}
		});

		addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {   
			public void mouseDragged(java.awt.event.MouseEvent e) {
				mouseMoved(e);
			}   
			public void mouseMoved(java.awt.event.MouseEvent e) {
				Point p = e.getPoint();
				mouse_x = p.x;
				mouse_y = p.y;
				Vec2 v = view.WindowToView(p.x, p.y);

				//System.out.println("GOOD_V:" + v.toString());
				mouse_pos = new Vec2(v);
				boolean do_repaint = false;

				if (connecting) {
					connect_to.copy(v);
					do_repaint = true;
				}

				if (pan_drag)
				{
					view.translate(new workcraft.util.Point(prev_cursor.x, prev_cursor.y), new workcraft.util.Point(p.x, p.y));
					changed();
					prev_cursor.x = p.x;
					prev_cursor.y = p.y;
					do_repaint = true;
				}

				if (selection_drag) {
					sel_ll.setX(Math.min(sel_anchor.getX(), v.getX()));
					sel_ll.setY(Math.min(sel_anchor.getY(), v.getY()));
					sel_ur.setX(Math.max(sel_anchor.getX(), v.getX()));
					sel_ur.setY(Math.max(sel_anchor.getY(), v.getY()));

					do_repaint = true;
				}

				if (move_drag) {
					if (snap_to_grid) 
					{
						v = grid.getClosestGridPoint(v);
						grid.highlightEnable(true);
					}

					v.negate();

					//System.out.println("BAD_V:" + v.toString());
					Vec3 t = move_target.transform.getTranslation();
					Vec3 vv = new Vec3();
					vv.setXYZ(v.getX(), v.getY(), 0.0f);
					vv.sub(t);

					move_target.transform.translateAbs(v);


					for (BasicEditable n : selection) {
						if (n!=move_target) {
							n.transform.translateRel(vv);
						}
						n.acceptTransform();
					}

					if(selection_anchors && selected_connection!=null) {
						selected_connection.updatePointsFromAnchors();
					}

					do_repaint = true;
					changed();

				}

				if (do_repaint)
					repaint();
			}
		});

		addMouseListener(new java.awt.event.MouseAdapter() {  

			public void mouseClicked(java.awt.event.MouseEvent e) {

				if (e.getClickCount()==2 && e.getButton()==MouseEvent.BUTTON1) {
					Vec2 v = view.WindowToView(e.getX(), e.getY());
					BasicEditable n = null;
					if (root!=null)
						n = root.getChildAt(v); 

					if (document!=null && !document.simIsRunning()) {
						if (n!=null)
							n.dblClick();
					}
				}
				
				if (!pan_drag)
				{
					if(selected_connection!=null)
						return;

					Vec2 v = view.WindowToView(e.getX(), e.getY());

					BasicEditable n = null;
					if (root!=null)
						n = root.getChildAt(v);

					if (n!=null)
					{
						if (!connecting && !document.simIsRunning()) {
							deselect();
							select(n);
							selection_anchors = false;
							grid.setMinorInterval(0.1f);
							changed();
						}
					}
					else {
						
						EditableConnection con = null;
						if (root!=null)
							con = 	root.getChildConnectionAt(v);
						if(con!=null && !connecting && !document.simIsRunning()) {
							con.select();
							property_editor.setObject(con);
							selected_connection = con;
						}
					}
				}

			}

			public void mousePressed(java.awt.event.MouseEvent e) {
				if (e.getButton()==MouseEvent.BUTTON3) {
					prev_cursor.x = e.getX();
					prev_cursor.y = e.getY();
					pan_drag = true;
					setCursor(new Cursor(Cursor.MOVE_CURSOR));
				}
				if (e.getButton()==MouseEvent.BUTTON1) {
					Vec2 v = view.WindowToView(e.getX(), e.getY());
					BasicEditable n = null;
					if (root!=null)
						n = root.getChildAt(v); 

					if (document!=null && document.simIsRunning()) {
						if (n!=null)
							n.simAction(e.getButton());														
					} else {
						if (connecting && n!=null ) {
							if (connect_first == null)
							{
								connect_first = n;
								Vec2 k = new Vec2(0.0f, 0.0f);
								n.transform.getLocalToViewMatrix().transform(k);
								connect_from.copy(k);
								connect_to.copy(k);
								select(n); 
							} else
							{
								connect_second = n;
								finishConnection();
							}
						}

						if (!(pan_drag || connecting))
						{
							if (n==null)
							{
								if(selected_connection!=null) {
									for(BasicEditable an : selected_connection.anchors) {
										if(an.hitsBB(v)) {
											if (!an.selected)
											{
												deselect(true);
												select(an);
												changed();
												selection_anchors = true;
												grid.setMinorInterval(0.025f);
											}
											move_target = an;
											setCursor(new Cursor(Cursor.MOVE_CURSOR));
											move_drag = true;
											if (snap_to_grid) {
												grid.getClosestGridPoint(v);
												grid.highlightEnable(true);
												repaint();
											}
											return;
										}
									}

									EditableConnection con = root.getChildConnectionAt(v);
									if(con==selected_connection && con.acceptDynamicAnchors()) {
										deselect(true);
										con.addDynamicAnchor(v);
										deselectConnection();
										con.select();
										selected_connection = con;
										return;
									}
								}

								deselect(true);
								selection_drag = true;
								setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
								sel_anchor.setXY(v.getX(), v.getY());
								sel_ll.setXY(v.getX(), v.getY());
								sel_ur.setXY(v.getX(), v.getY());
							} else
							{
								deselectConnection();
								if (!n.selected)
								{
									deselect();
									select(n);
									changed();
									selection_anchors = false;
									grid.setMinorInterval(0.1f);
								}
								move_target = n;
								setCursor(new Cursor(Cursor.MOVE_CURSOR));
								move_drag = true;
								if (snap_to_grid) {
									grid.getClosestGridPoint(v);
									grid.highlightEnable(true);
									repaint();
								}
							}
						}
					}
				}
				requestFocus();
			}
			public void mouseReleased(java.awt.event.MouseEvent e) {
				if (e.getButton()==MouseEvent.BUTTON1) {

					if (connecting) {
						Vec2 v = view.WindowToView(e.getX(), e.getY());
						BasicEditable n = root.getChildAt(v);

						if (n!=null && n!=connect_first)
						{
							connect_second = n;
							finishConnection();
						}
					}

					if (selection_drag)	{
						selection_drag = false;
						doBoxSelection();
					}

					if (move_drag) {
						move_drag = false;
						move_target = null;

						updateGuidelines();
						grid.highlightEnable(false);
					}

					if (pan_drag)
						setCursor(new Cursor(Cursor.MOVE_CURSOR));
					else if (connecting)
						setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
					else
						setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
					repaint();
				}

				if (e.getButton()==MouseEvent.BUTTON3) {
					pan_drag = false;
					if (selection_drag || connecting)
						setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
					else if (move_drag)
						setCursor(new Cursor(Cursor.MOVE_CURSOR));
					else
						setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});
		addMouseWheelListener(new java.awt.event.MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				view.scale(e.getWheelRotation(), e.getX(), e.getY());
				repaint();
				changed();
			}
		});
		addGLEventListener(this);
		setPreferredSize(new
				Dimension(0, 0));
	}
	public BoundingBox getBoundingBox() {
		BoundingBox bb = new BoundingBox(new Vec2(), new Vec2());
		for (BasicEditable n : root.getChildren()) {
			BoundingBox nbb = n.getBoundingBox();
			Vec2 ll = nbb.getLowerLeft();
			Vec2 ur = nbb.getUpperRight();
			n.transform.getLocalToViewMatrix().transform(ll);
			n.transform.getLocalToViewMatrix().transform(ur);
			bb.addPoint(ll); bb.addPoint(ur);			
		}
		return bb;
	}

	public void addToSelection(BasicEditable o) {
		selection.add(o);		
	}

	public void updateGuidelines() {
		if (document != null) {
			guideNodes.clear();
			document.getGuideNodes(guideNodes);
			if (!guideNodes.isEmpty())
				grid.updateGuidelines(guideNodes);
		}
	}



	public void documentToXml(Element doc) {
		Document d = doc.getOwnerDocument();
		Element de = d.createElement("document");
		de.setAttribute("model-uuid", ModelManager.getModelUUID(document.getClass()).toString());

		document.toXmlDom(de); // save document personal data
		
		root.toXmlDom(de);
		doc.appendChild(de);
		for (EditableConnection con : document.getConnections()) {
			con.toXmlDom(de);
		}
	}

	public void selectionToXml(Element fragment) {
		if(!selection_anchors)
			for (BasicEditable n: selection) {
				n.toXmlDom(fragment);
			}

		for (EditableConnection con : document.getConnections()) {
			if (selection.contains(con.getFirst().getTopParent(root)) && selection.contains(con.getSecond().getTopParent(root)))
				con.toXmlDom(fragment);
		}
	}

	public void pasteFromXml(Document doc) {
		NodeList nl = doc.getElementsByTagName("workcraft-document-fragment");

		deselect();

		if (nl.getLength() == 0)
			return;

		Element re = (Element)nl.item(0);


		HashMap<String, String> renamed = new HashMap<String, String>();

		server.python.set("_pasting", true);
		server.python.set("_renamed", renamed);

		nl = re.getElementsByTagName("editable");
		for (int i=0; i<nl.getLength(); i++ ) {
			Element e = (Element)nl.item(i);
			String class_name = e.getAttribute("class");
			try {
				Class cls = ClassLoader.getSystemClassLoader().loadClass(class_name);
				Constructor ctor = cls.getDeclaredConstructor(BasicEditable.class);
				BasicEditable n = (BasicEditable)ctor.newInstance(this.document.getRoot());

				try {
					n.fromXmlDom(e);
				} catch (DuplicateIdException e1) {
					String old_id = e.getAttribute("id");
					String new_id = new String(old_id);
					for (;;)
						try {
							new_id += "_copy";							
							n.setId(new_id);
							break;
						} catch (DuplicateIdException e2) 
						{
						}
						// System.out.println("Renamed "+old_id+" to "+new_id);
						renamed.put(old_id, new_id);					
				}

				n.transform.translateRel(0.5f, 0.5f, 0.0f);

				select(n);
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
				ex.printStackTrace();
			} catch (SecurityException ex) {
				ex.printStackTrace();
			} catch (NoSuchMethodException ex) {
				ex.printStackTrace();
			}
		}		
		nl = doc.getElementsByTagName("editable-connection");
		for (int i=0; i<nl.getLength(); i++ ) {
			Element e = (Element)nl.item(i);

			String first_id = e.getAttribute("first");
			if (renamed.containsKey(first_id))
				first_id = renamed.get(first_id);

			String second_id = e.getAttribute("second");
			if (renamed.containsKey(second_id))
				second_id = renamed.get(second_id);

			BasicEditable first = (BasicEditable)server.getObjectById(first_id);
			BasicEditable second = (BasicEditable)server.getObjectById(second_id);

			// System.out.println("Connecting "+first_id+" and "+second_id+" "+first+";"+second);

			try {
				EditableConnection con = document.createConnection(first, second);
				con.fromXmlDom(e);
			} catch (InvalidConnectionException ex) {
				ex.printStackTrace();
			} catch (DuplicateIdException ex) {
				ex.printStackTrace();
			}
		}

		server.python.set("_pasting", false);
		server.python.getLocals().__delitem__("_renamed".intern());

		updateGuidelines();
		repaint();

	}

	public void beginConnection() {
		deselect();
		connecting = true;

		pan_drag = false;
		move_drag = false;
		selection_drag = false;

		setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
	}

	public void cancelConnection() {
		if (connecting) {
			deselect();
			connecting = false;
			connect_first = null;
			connect_second = null;
			setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
	}

	public void deselectConnection() {
		if(selected_connection!=null) {
			selected_connection.deselect();
		}
		selected_connection = null;
	}

	public void deselect(boolean keepConnection) {
		for (BasicEditable n : selection) {
			n.selected = false;
			n.setZOrder(0);
		}
		if(!keepConnection)
			deselectConnection();
		selection.clear();
		if (document != null)
			property_editor.setObject(document);
		else
			property_editor.clearObject();
		repaint();
	}

	public void deselect() {
		deselect(false);
	}

	private void deselect(BasicEditable n) {
		if (n.selected) {
			selection.remove(n);
			n.selected = false;
			if (selection.size()==1 && !selection_anchors)
				property_editor.setObject(selection.get(0));
			else if (document != null)
				property_editor.setObject(document);
			else
				property_editor.clearObject();
			repaint();
		}
	}

	public void draw() {
		painter.setIdentityTransform();
		if (document != null)
			painter.setClearColor( ((ModelBase)document).getBackgroundColor() );
		else
			painter.setClearColor(clear_color);
		painter.clear();

		updateGrid();

		if (draw_grid) {
			grid.draw(painter);
		}

		if (connecting) {
			if (connect_first != null) {
				painter.setLineColor(connectionColor);
				painter.drawLine(connect_from, connect_to);

			}
		}

		if (document != null)
			for (EditableConnection con : document.getConnections()) {
				con.draw(painter);
			}

		if (root != null)
			root.draw(painter);

		if (new_node!=null) {
			painter.blendEnable();
			painter.setBlendConstantAlpha(0.3f);
			painter.setBlendMode(BlendMode.CONSTANT_ALPHA);
			new_node.draw(painter);
			painter.blendDisable();
		}

		// TODO

		painter.setIdentityTransform();

		painter.setBlendConstantAlpha(0.3f);
		painter.setBlendMode(BlendMode.CONSTANT_ALPHA);
		painter.setLineMode(LineMode.HAIRLINE);

		if (selection_drag) {
			painter.blendEnable();
			painter.setFillColor(selectionBoxFillColor);
			painter.setLineColor(selectionBoxOutlineColor);
			painter.setShapeMode(ShapeMode.FILL);
			painter.drawRect(sel_ll, sel_ur);
			painter.blendDisable();
			painter.setShapeMode(ShapeMode.OUTLINE);
			painter.setLineMode(LineMode.HAIRLINE);
			painter.setLineWidth(0.1f);
			painter.drawRect(sel_ll, sel_ur);
		}

		if (!selection.isEmpty()) {
			BoundingBox q = new BoundingBox();
			if (!connecting) {
				for (BasicEditable n: selection) {
					BoundingBox bb = n.getBoundingBox();
					Vec2 ll = bb.getLowerLeft();
					Vec2 ur = bb.getUpperRight();
					Vec2 ul = new Vec2(ll.getX(), ur.getY());
					Vec2 lr = new Vec2(ur.getX(), ll.getY());

					n.transform.getLocalToViewMatrix().transform(ll);
					n.transform.getLocalToViewMatrix().transform(ur);
					n.transform.getLocalToViewMatrix().transform(ul);
					n.transform.getLocalToViewMatrix().transform(lr);

					q.addPoint(ll); q.addPoint(ur); q.addPoint(ul); q.addPoint(lr);
				}
				painter.setLineColor((selection_anchors)?selectionAnchorBoxOutlineColor:selectionOutlineColor);
				painter.setFillColor((selection_anchors)?selectionAnchorBoxFillColor:selectionFillColor);
				painter.blendEnable();
				painter.setShapeMode(ShapeMode.FILL);
				painter.drawRect(q.getLowerLeft(), q.getUpperRight());
				painter.blendDisable();
				painter.setShapeMode(ShapeMode.OUTLINE);
				painter.drawRect(q.getLowerLeft(), q.getUpperRight());
			}
		}

//		tester.draw(painter);

		for (Drawable d : overlays) {
			d.draw(painter);
		}
	}


	public void display (GLAutoDrawable gld) {
		GL gl = gld.getGL();
		//gl.glEnable(GL.GL_MULTISAMPLE);

		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadTransposeMatrixf(view.getFinalMatrix().getArray(), 0);

		draw();
	}

	public void displayChanged(GLAutoDrawable gld, boolean a, boolean b) {
	}

	private void doBoxSelection() {
		BoundingBox bb = new BoundingBox(sel_ll, sel_ur);
		if(selected_connection!=null) {
			deselect(true);
			for(EditableAnchor an : selected_connection.anchors) {
				if(an.intersectsBB(bb))
					select(an);
			}
			selection_anchors = true;
			grid.setMinorInterval(0.025f);
			if(!selection.isEmpty())
				return;
		}
		deselectConnection();
		if (root != null)
			for (BasicEditable n : root.getChildren())
				if (n.intersectsBB(bb))
					select(n);
		selection_anchors = false;
		grid.setMinorInterval(0.1f);
	}

	public void dragEnter(DropTargetDragEvent e) {
		if (isDragOk(e))
		{
			new_node = null;
			try{
				ComponentWrapper wrapper = (ComponentWrapper)e.getTransferable().getTransferData(TransferableComponent.COMPONENT_WRAPPER_FLAVOR);
				Class cls = wrapper.getComponentClass();
				new_node = createComponent(cls);
			}
			catch (IOException ex) 
			{
				ex.printStackTrace();				
			}
			catch (UnsupportedFlavorException ex) 
			{ 
				ex.printStackTrace();
			}			

			e.acceptDrag(e.getDropAction());

			Point p = e.getLocation();
			Vec2 v = view.WindowToView(p.x, p.y);

			if (snap_to_grid)
			{
				grid.highlightEnable(true);
				v = grid.getClosestGridPoint(v);
			}

			if (new_node!=null)
				new_node.transform.translateAbs(-v.getX(), -v.getY(), 0);

			repaint();
		}
		else
		{
			grid.highlightEnable(false);
			e.rejectDrag();
		}
	}

	public void dragExit(DropTargetEvent e) {
		grid.highlightEnable(false);

		if (new_node != null) {
			try {
				document.removeComponent(new_node);

			} catch (UnsupportedComponentException ex) {
				ex.printStackTrace();
			}
			new_node = null;
		}
		repaint();		
	}

	public void dragOver(DropTargetDragEvent e) {
		if (isDragOk(e))
		{
			e.acceptDrag(e.getDropAction());

			Point p = e.getLocation();
			Vec2 v = view.WindowToView(p.x, p.y);

			if (snap_to_grid) {
				grid.highlightEnable(true);
				v = grid.getClosestGridPoint(v);
			}

			if (new_node!=null)
				new_node.transform.translateAbs(-v.getX(), -v.getY(), 0);

			repaint();			
		}
		else
		{
			grid.highlightEnable(false);
			e.rejectDrag();
		}
	}

	public void drop(DropTargetDropEvent e) {
		if (new_node == null) {
			JOptionPane.showMessageDialog(null, "The new component could not be instantiated, check 'Problems' tab for details", "Instantiation failed", JOptionPane.ERROR_MESSAGE);
			return;
		} else
		{
			requestFocus();
			changed();

			new_node = null;
		}
		grid.highlightEnable(false);
		repaint();
	}

	public void dropActionChanged(DropTargetDragEvent e) {
		dragOver(e);
	}

	private void finishConnection() {

		try
		{
			document.createConnection(connect_first, connect_second);
		} catch (InvalidConnectionException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Cannot make connection", JOptionPane.ERROR_MESSAGE);
		}

		cancelConnection();
	}

	public void fromXmlDom(Element element) {
		Element ee = element;
		NodeList nl = ee.getElementsByTagName("options");
		Element eo =(Element)nl.item(0);
		nl = ee.getElementsByTagName("viewstate");
		Element ev =(Element)nl.item(0);

		snap_to_grid = Boolean.parseBoolean(eo.getAttribute("snap"));
		draw_grid = Boolean.parseBoolean(eo.getAttribute("grid"));
		show_labels = Boolean.parseBoolean(eo.getAttribute("labels"));
		show_ids = Boolean.parseBoolean(eo.getAttribute("ids"));

		view.fromXmlDom(ev);
	}

	public Model getDocument() {
		return document;
	}

	public WorkCraftServer getServer() {
		return server;
	}

	public void init(GLAutoDrawable d) {
		GL gl = d.getGL();

		System.out.println("Initializing visualisation...");

		System.out.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
		System.out.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
		System.out.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));		

		gl.glEnable(GL.GL_MULTISAMPLE);

		gl.setSwapInterval(1);
		joglpainter = new JOGLPainter(gl, view);
		painter = joglpainter;
		loadFonts("Fonts");

		System.out.println("Visualisation subsystem initialization complete.\n\n");
	}

	public void addOverlay(Drawable d) {
		overlays.add(d);

	}

	public void removeOverlay(Drawable d) {
		overlays.remove(d);		
	}

	private boolean isDragOk(DropTargetDragEvent e) {
		for (DataFlavor k : e.getTransferable().getTransferDataFlavors()) {
			if (k==TransferableComponent.COMPONENT_WRAPPER_FLAVOR)
				return true;
		}
		return false;
	}

	public void reshape (GLAutoDrawable gld, int x, int y, int w, int h) {
		view.viewport(x, y, w, h);
		view.aspect((float)w/h);
		repaint();
	}

	private void select(BasicEditable n) {
		if (!n.selected) {
			selection.add(n);
			n.selected = true;
			if (selection.size()==1 && !selection_anchors)
				property_editor.setObject(n);
			else
				property_editor.clearObject();
			repaint();
		}
	}

	public void setDocument(Model document) {
		if (document == this.document)
			return;
		this.document = document;
		document.setEditor(this);

		server.unbind();
		document.bind(server);


		root = (GroupNode)document.getRoot();

		LinkedList<BasicEditable> components = new LinkedList<BasicEditable>();
		document.getComponents(components);


		view.reset();

		updateGuidelines();
		repaint();
	}

	public void setServer(WorkCraftServer server) {
		this.server = server;
		server.python.set("_editor", this);
		server.execPython("def _redraw():\n\t_editor.repaint()");
	}

	public void setPropertyEditor(PropertyEditor editor) {
		property_editor = editor;		
	}

	public Element toXmlDom(Element parent_element) {
		Document d = parent_element.getOwnerDocument();
		Element ee = d.createElement("editor");
		Element eo = d.createElement("options");
		eo.setAttribute("snap", Boolean.toString(snap_to_grid));
		eo.setAttribute("grid", Boolean.toString(draw_grid));
		eo.setAttribute("labels", Boolean.toString(show_labels));
		eo.setAttribute("ids", Boolean.toString(show_ids));
		ee.appendChild(eo);
		view.toXmlDom(ee);
		parent_element.appendChild(ee);
		return ee;
	}

	private void updateGrid() {
		grid.setVisibleRange(view.getVisibleLL(),view.getVisibleUR());
	}

	public void hotKeyClear() {
		hotkey.hotKeyClear();
	}

	public Class hotKeyGet(int vk) {
		return hotkey.hotKeyGet(vk);
	}

	public void hotKeySetNum(int idx, Class cls) {
		hotkey.hotKeySetNum(idx, cls);
	}

	public void hotKeySetVk(int vk, Class cls) {
		hotkey.hotKeySetVk(vk, cls);
	}

	public void overridePainter(Painter p) {
		painter = p;
	}

	public void restorePainter() {
		painter = joglpainter;
	}

	public void setGridRange(Vec2 lower_left, Vec2 upper_right) {
		grid.setVisibleRange(lower_left, upper_right);
	}

	public List<BasicEditable> getSelection() {
		return selection;
	}

}
