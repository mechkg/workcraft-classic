package workcraft.common;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import workcraft.DuplicateIdException;
import workcraft.InvalidConnectionException;
import workcraft.Model;
import workcraft.ModelBase;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.editor.BasicEditable;
import workcraft.editor.BoundingBox;
import workcraft.editor.Editor;
import workcraft.logic.DNF;
import workcraft.logic.DNFClause;
import workcraft.logic.DNFLiteral;
import workcraft.logic.DNFUtil;
import workcraft.logic.InvalidExpressionException;
import workcraft.petri.EditablePetriPlace;
import workcraft.petri.EditablePetriTransition;
import workcraft.petri.PetriModel;
import workcraft.util.Vec2;

public class PetriNetMapper implements Tool {
	public static final String _modeluuid = "*";
	public static final String _displayname = "Convert to Petri Net";

	HashMap <UUID, Mapping> map_uuid_mapping = new HashMap <UUID, Mapping>();
	HashMap <String, Object> nodes;
	HashSet <Class> ignore = new HashSet<Class>();

	class CycleDef {
		public String suffix, initializer, set, reset, plusTag, minusTag;
	}

	class ExpansionDef {
		public Class sourceClass;
		public LinkedList <CycleDef> cycles = new LinkedList<CycleDef>();
	}

	class Mapping {
		public String preMapMethod = null;
		public HashMap <Class, ExpansionDef> classExpansion = new HashMap <Class, ExpansionDef>();
		public HashMap <String, String> predicates = new HashMap<String, String>(); 
	}

	class Cycle {
		final static float D = 0.2f; // Distance between components inside one cycle
		final static float S = 0.8f; // Default distance between neighbouring cycles
		
		private String baseId;
		private CycleDef def;
		private int plusCount = 1, minusCount = 1;


		public Vec2 center;

		EditablePetriPlace p_true = null, p_false = null;
		LinkedList <EditablePetriTransition> t_plus = new LinkedList<EditablePetriTransition>(), t_minus = new LinkedList<EditablePetriTransition>();

		public Cycle (Vec2 center, String baseId, CycleDef def) {
			this.center = center;
			this.baseId = baseId;
			this.def = def;
		}

		public BoundingBox getBoundingBox() {

			return null;			
		}

		private Vec2[] makeTurnL(Vec2 v1, Vec2 v2)
		{
			Vec2 [] v = new Vec2[2];
			v[0] = new Vec2(-v1.getX(), -(v1.getY()+0.66f*(v2.getY()-v1.getY())));					
			v[1] = new Vec2(-(v2.getX() + 0.66f*(v1.getX()-v2.getX())), -v2.getY());
			return v;
		}

		private Vec2[] makeTurnR(Vec2 v1, Vec2 v2)
		{
			Vec2 [] v = new Vec2[2];
			v[0] = new Vec2(-(v1.getX() + 0.66f*(v2.getX()-v1.getX())), -v1.getY());
			v[1] = new Vec2(-v2.getX(), -(v2.getY()+0.66f*(v1.getY()-v2.getY())));					
			return v;
		}

		private EditablePetriTransition addPlusTransition(Model target, String tag) {
			Vec2 vt = new Vec2();
			DefaultConnection c;
			EditablePetriTransition t = null;
			try {
				t = new EditablePetriTransition(target.getRoot());
				if (def.suffix.length()>0)
					t.setId(baseId + "_" + def.suffix+"_plus"+plusCount);
				else
					t.setId(baseId + "_plus"+plusCount);
				
				if (tag != null)
					t.setCustomProperty("mapping-tag", tag);
	
				nodes.put(t.getId(), t);

				vt.setXY(center.getX(), center.getY()-D*plusCount);
				t.transform.translateAbs(vt);
				t_plus.add(t);

				c = (DefaultConnection) target.createConnection(p_false, t);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnL(p_false.transform.getTranslation2d(), t.transform.getTranslation2d()));

				c = (DefaultConnection) target.createConnection(t, p_true);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnR(t.transform.getTranslation2d(), p_true.transform.getTranslation2d()));		

				plusCount++;

				return t;
			} catch (DuplicateIdException e) {
				e.printStackTrace();
			} catch (UnsupportedComponentException e) {
				e.printStackTrace();
			} catch (InvalidConnectionException e) {
				e.printStackTrace();
			}
			return null;
		}

		private EditablePetriTransition addMinusTransition(Model target, String tag) {
			Vec2 vt = new Vec2();
			DefaultConnection c;
			EditablePetriTransition t = null;
			try {
				t = new EditablePetriTransition(target.getRoot());
				if (def.suffix.length()>0)
					t.setId(baseId + "_" + def.suffix+"_minus"+minusCount);
				else
					t.setId(baseId + "_minus"+minusCount);

				nodes.put(t.getId(), t);
				
				if (tag != null)
					t.setCustomProperty("mapping-tag", tag);


				vt.setXY(center.getX(), center.getY()+D*minusCount);
				t.transform.translateAbs(vt);
				t_plus.add(t);

				c = (DefaultConnection) target.createConnection(p_true, t);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnL(p_true.transform.getTranslation2d(), t.transform.getTranslation2d()));

				c = (DefaultConnection) target.createConnection(t, p_false);
				c.connectionType = DefaultConnection.Type.bezierConnection;
				c.setInternalPoints(makeTurnR(t.transform.getTranslation2d(), p_false.transform.getTranslation2d()));	

				minusCount++;

				return t;
			} catch (DuplicateIdException e) {
				e.printStackTrace();
			} catch (UnsupportedComponentException e) {
				e.printStackTrace();
			} catch (InvalidConnectionException e) {
				e.printStackTrace();
			}
			return null;
		}		

		public void createPlaces(Model target, WorkCraftServer server) {
			Vec2 vt = new Vec2();

			try {
				p_true = new EditablePetriPlace(target.getRoot());
				p_true.setId(baseId + "_" + def.suffix+"1");

				nodes.put(p_true.getId(), p_true);

				vt.setXY(center.getX()-D, center.getY());
				p_true.transform.translateAbs(vt);
			} catch (UnsupportedComponentException e) {
				e.printStackTrace();
			} catch (DuplicateIdException e) {
				e.printStackTrace();
			}

			try {
				p_false = new EditablePetriPlace(target.getRoot());
				p_false.setId(baseId + "_" + def.suffix+"0");

				nodes.put(p_false.getId(), p_false);

				vt.setXY(center.getX()+D, center.getY());
				p_false.transform.translateAbs(vt);
			} catch (UnsupportedComponentException e) {
				e.printStackTrace();
			} catch (DuplicateIdException e) {
				e.printStackTrace();
			}

			Object o = server.getObjectById(baseId);
			if (o==null) {
				System.err.println ("Petri Net Mapper: server could not find object with id \""+baseId+"\"");
				return;
			}
			// plus transition
			boolean enabled = false;
			try {
				Method m = o.getClass().getMethod(def.initializer, (Class[])null);
				enabled = (Boolean)m.invoke(o, (Object[])null);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			if (enabled)
				p_true.setTokens(1);
			else
				p_false.setTokens(1);
		}

		public void createTransitions(Model target, HashMap<String, String> predicates, WorkCraftServer server) {
			Object o = server.getObjectById(baseId);
			if (o==null) {
				System.err.println ("Petri Net Mapper: server could not find object with id \""+baseId+"\"");
				return;
			}
			// plus transition
			String plusFunc = null;
			String plusTag = null;
			try {
				Method m = o.getClass().getMethod(def.set, (Class[])null);
				plusFunc = (String)m.invoke(o, (Object[])null);
				
				if (def.plusTag.length() > 0) {
					Method t = o.getClass().getMethod(def.plusTag, (Class[])null);
					plusTag = (String)t.invoke(o, (Object[])null);
				}
				
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			if (plusFunc == null) {
				System.err.println ("Petri Net Mapper: object with id \""+baseId+"\" does not have method \""+def.set+"\"");
			} else {
				DNF set = new DNF();
				try {

					if (plusFunc.equals("ALWAYS")) {
						addPlusTransition(target, plusTag);
					} else if (!plusFunc.equals("NEVER")) {
						set.parseExpression(plusFunc, predicates);
						DNFUtil.resolveLiterals(set, server, nodes);
						
						
					//	System.err.println("SET: "+plusFunc+"\n-----------------");
					//	System.err.println("BEFORE: " + set);
						set = set.minimise();
				//		System.err.println("AFTER: " + set + "\n\n");
						
						for (DNFClause clause : set.clauses) {
							EditablePetriTransition t = addPlusTransition(target, plusTag);
							
							for (DNFLiteral literal : clause.pos) {
								Object obj = nodes.get(literal.id);

								if (obj == null) {
									System.err.println("Petri Net Mapper: server cannot find required object \""+literal.id+"\"");
									continue;
								}

								if (obj instanceof BasicEditable) {
									BasicEditable p = (BasicEditable)obj;
									DefaultConnection c = (DefaultConnection)target.createConnection(p, t);
									if (c!=null) 
										c.drawArrow = false;
									else
										System.err.println ("Petri Net Mapper: duplicate connection (???) "+p.getId()+" -> "+t.getId());

									c = (DefaultConnection)target.createConnection(t, p);
									if (c!=null)  {
										c.drawArrow = false;
										//c.setColorOverride(colorOverride)
									}
									else
										System.err.println ("Petri Net Mapper: duplicate connection (???) "+t.getId()+" -> "+p.getId());
								}
							}

							for (DNFLiteral literal : clause.neg) {
								int len = literal.id.length();
								String id = literal.id.substring(0, len-1) + ((literal.id.charAt(len-1)=='0')?'1':'0'); 

								Object obj = nodes.get(id);

								if (obj == null) {
									obj = server.getObjectById(id);
									System.err.println("Petri Net Mapper: server cannot find required object \""+id+"\"");

								}							
								if (obj instanceof BasicEditable) {
									BasicEditable p = (BasicEditable)obj;
									DefaultConnection c = (DefaultConnection)target.createConnection(p, t);
									if (c!=null) 
										c.drawArrow = false;
									else
										System.err.println ("Petri Net Mapper: duplicate connection (???) "+p.getId()+" -> "+t.getId());

									c = (DefaultConnection)target.createConnection(t, p);
									if (c!=null) 
										c.drawArrow = false;
									else
										System.err.println ("Petri Net Mapper: duplicate connection (???) "+t.getId()+" -> "+p.getId());
								}
							}

						}
					}
				} catch (InvalidExpressionException e) {
					e.printStackTrace();
				} catch (InvalidConnectionException e) {
					e.printStackTrace();
				}

			}

			String minusFunc = null;
			String minusTag = null;
			
			try {
				Method m = o.getClass().getMethod(def.reset, (Class[])null);
				minusFunc = (String)m.invoke(o, (Object[])null);
				
				if (def.minusTag.length() > 0) {
					Method t = o.getClass().getMethod(def.minusTag, (Class[])null);
					minusTag = (String)t.invoke(o, (Object[])null);
				}
				
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			if (minusFunc == null) {
				System.err.println ("Petri Net Mapper: object with id \""+baseId+"\" does not have method \""+def.reset+"\"");
				return;
			} else {
				try {
					DNF reset = new DNF();

					if (plusFunc.equals("ALWAYS")) {
						addMinusTransition(target, minusTag);
					} else if (!plusFunc.equals("NEVER"))	{
						reset.parseExpression(minusFunc, predicates);
						DNFUtil.resolveLiterals(reset, server, nodes);
						
						
					//	System.err.println("RESET: "+minusFunc+"\n-----------------");
					//	System.err.println("BEFORE: " + reset);
						reset = reset.minimise();
				//		System.err.println("AFTER: " + reset + "\n\n");
						
				
						for (DNFClause clause : reset.clauses) {

							EditablePetriTransition t = addMinusTransition(target, minusTag);

							for (DNFLiteral literal : clause.pos) {
								Object obj = nodes.get(literal.id);

								if (obj == null) {
									System.err.println("Petri Net Mapper: server cannot find required object \""+literal.id+"\"");
									continue;
								}							
								if (obj instanceof BasicEditable) {
									BasicEditable p = (BasicEditable)obj;
									DefaultConnection c = (DefaultConnection)target.createConnection(p, t);
									if (c!=null) 
										c.drawArrow = false;
									else
										System.err.println ("Petri Net Mapper: duplicate connection (???) "+p.getId()+" -> "+t.getId());

									c = (DefaultConnection)target.createConnection(t, p);
									if (c!=null) 
										c.drawArrow = false;
									else
										System.err.println ("Petri Net Mapper: duplicate connection (???) "+t.getId()+" -> "+p.getId());

								}
							}

							for (DNFLiteral literal : clause.neg) {
								int len = literal.id.length();
								String id = literal.id.substring(0, len-1) + ((literal.id.charAt(len-1)=='0')?'1':'0'); 

								Object obj = nodes.get(id);

								if (obj == null) {
									System.err.println("Petri Net Mapper: server cannot find required object \""+id+"\"");
									continue;
								}							
								if (obj instanceof BasicEditable) {
									BasicEditable p = (BasicEditable)obj;
									DefaultConnection c = (DefaultConnection)target.createConnection(p, t);
									if (c!=null) 
										c.drawArrow = false;
									else
										System.err.println ("Petri Net Mapper: duplicate connection (???) "+p.getId()+" -> "+t.getId());

									c = (DefaultConnection)target.createConnection(t, p);
									if (c!=null) 
										c.drawArrow = false;
									else
										System.err.println ("Petri Net Mapper: duplicate connection (???) "+t.getId()+" -> "+p.getId());

								}
							}

						}
					}

				} catch (InvalidExpressionException e) {
					e.printStackTrace();
				} catch (InvalidConnectionException e) {
					e.printStackTrace();
				}
			}

		}
	}



	public boolean isModelSupported(UUID modelUuid) {
		if (map_uuid_mapping.get(modelUuid)!=null)
			return true;
		else
			return false;
	}

	public void init(WorkCraftServer server) {
		File dir = new File("Mappings");

		System.out.println("Petri Net Mapper: loading mappings from "+dir.getPath());

		if (!dir.exists() || !dir.isDirectory()) {
			System.err.println("Petri Net Mapper: mappings directory does not exist");
			return;
		}

		File files[] = dir.listFiles(new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory())
					return false;
				return f.getPath().endsWith("xml");
			}
		});

		boolean first = true;

		for (File f : files) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document doc;
			DocumentBuilder db;

			try {
				db = dbf.newDocumentBuilder();
				doc = db.parse(f);
			} catch (ParserConfigurationException e) {
				System.err.println(e.getMessage());
				continue;
			} catch (IOException e) {
				System.err.println(e.getMessage());
				continue;
			} catch (SAXException e) {
				System.err.println(e.getMessage());
				continue;
			}

			Element root = doc.getDocumentElement();

			if (root.getTagName() != "mapping")
				continue;

			UUID sourceUuid = UUID.fromString(root.getAttribute("sourceUuid"));

			//System.out.println ("PN Mapper: adding model UUID "+sourceUuid);

			Mapping mapping = new Mapping();

			if (root.hasAttribute("preMap")) 
				mapping.preMapMethod = root.getAttribute("preMap");
			else
				mapping.preMapMethod = null;

			NodeList nl = root.getElementsByTagName("expansion");

			for (int i=0; i<nl.getLength(); i++) {
				Element e = (Element)nl.item(i);
				ExpansionDef exp = new ExpansionDef();

				//	System.out.println ("\tPN Mapper: adding expansion for class "+e.getAttribute("sourceClass"));

				NodeList nl2 = e.getElementsByTagName("cycle");
				for (int j=0; j<nl2.getLength(); j++) {
					Element ee = (Element)nl2.item(j);

					// System.out.println ("\t\tPN Mapper: adding cycle "+ee.getAttribute("trueSuffix"));

					CycleDef cd = new CycleDef();
					cd.suffix = ee.getAttribute("suffix");
					cd.initializer = ee.getAttribute("initializer");
					cd.set = ee.getAttribute("set");
					cd.reset = ee.getAttribute("reset");
					cd.plusTag = ee.getAttribute("plusTag");
					cd.minusTag = ee.getAttribute("minusTag");
					exp.cycles.add(cd);
				}

				try {
					Class cls = ClassLoader.getSystemClassLoader().loadClass(e.getAttribute("sourceClass"));
					mapping.classExpansion.put(cls, exp);
				}
				catch (NoClassDefFoundError e1) {
					e1.printStackTrace();
				}
				catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
			}
			
			nl = root.getElementsByTagName("ignore");

			for (int i=0; i<nl.getLength(); i++) {
				Element e = (Element)nl.item(i);

				try {
					Class cls = ClassLoader.getSystemClassLoader().loadClass(e.getAttribute("sourceClass"));
					ignore.add(cls);
				}
				catch (NoClassDefFoundError e1) {
					e1.printStackTrace();
				}
				catch (ClassNotFoundException e1) {
					e1.printStackTrace();
				}
			}			

			nl = root.getElementsByTagName("predicate");

			for (int i=0; i<nl.getLength(); i++) {
				Element e = (Element)nl.item(i);
				mapping.predicates.put(e.getAttribute("token"),e.getAttribute("suffix"));				
			}

			map_uuid_mapping.put(sourceUuid, mapping);
		}
	}

	public PetriModel map(WorkCraftServer server, Model source) {
		UUID uuid = server.mmgr.getModelUUID(source.getClass());
		Mapping mapping = map_uuid_mapping.get(uuid);

		nodes = new HashMap<String, Object>();

		// Call pre-mapping if it's defined

		if (mapping.preMapMethod != null) {
			try {
				Method m = source.getClass().getMethod(mapping.preMapMethod, (Class[])null);
				m.invoke(source, (Object[])null);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.err.println("Pre-mapping method failed: "+e.getTargetException().getMessage());
				e.printStackTrace();
			}	
		}

		// Pull all components from the model	
		List<BasicEditable> components = new LinkedList<BasicEditable>();
		source.getComponents(components);

		// Convert components to elementary cycles
		LinkedList<Cycle> cycles = new LinkedList<Cycle>();

		for (BasicEditable e: components) {
			// System.out.println ("Petri Net Mapper: expanding " + e.getId());
			ExpansionDef exdef = mapping.classExpansion.get(e.getClass());

			if (exdef == null) {
				if (!ignore.contains(e.getClass()))
					System.err.println("Petri Net Mapper: expansion for class "+e.getClass().getName()+" is not defined, skipping component");
				continue;
			}

			Vec2 center = e.transform.getTranslation2d();
			center.mul(8.0f);

			int n = exdef.cycles.size();
			int cols = (n+1)/2;
			int rows = n/cols + (((n%cols)==0)?0:1);

			float w = Cycle.S * (float)cols * 0.5f;
			float h = Cycle.S * (float)rows * 0.5f;

			for (int i =0; i<n; i++) {
				CycleDef cydef = exdef.cycles.get(i);
				Vec2 cycleCenter = new Vec2(center);
				cycleCenter.setXY( cycleCenter.getX() - w + (i%cols) * Cycle.S, cycleCenter.getY() - h + (i/cols) * Cycle.S);
				cycles.add(new Cycle (cycleCenter, e.getId(), cydef));				
			}
		}

		// Create new Petri Net document
		PetriModel petri = new PetriModel();
		// petri.bind(server);

		// Create places
		for (Cycle c: cycles)
			c.createPlaces(petri, server);
		for (Cycle c: cycles)
			c.createTransitions(petri, mapping.predicates, server);

		return petri;
	}

	public void run(Editor editor, WorkCraftServer server) {
		Model doc = editor.getDocument(); 

		PetriModel petri = map(server, doc);

		if (petri == null) {
			JOptionPane.showMessageDialog(null, "No mapping defined for this model", "Cannot perform mapping", JOptionPane.ERROR_MESSAGE);
			return;
		}

		editor.setDocument(petri);
	}

	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub
	}

	public ToolType getToolType() {
		return ToolType.TRANSFORM;
	}
}