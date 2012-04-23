package workcraft.abstractsdfs;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
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
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.abstractsdfs.ASDFSLogic;
import workcraft.abstractsdfs.ASDFSModel;
import workcraft.abstractsdfs.ASDFSRegister;
import workcraft.antitoken.ATModel;
import workcraft.counterflow.CFModel;
import workcraft.editor.BasicEditable;
import workcraft.editor.BoundingBox;
import workcraft.editor.Editor;
import workcraft.logic.DNF;
import workcraft.logic.DNFClause;
import workcraft.logic.DNFLiteral;
import workcraft.logic.InvalidExpressionException;
import workcraft.mg.MGModel;
import workcraft.mg.MGPlace;
import workcraft.mg.MGTransition;
import workcraft.petri.EditablePetriPlace;
import workcraft.petri.EditablePetriTransition;
import workcraft.petri.PetriModel;
import workcraft.sdfs.SDFSLogic1Way;
import workcraft.sdfs.SDFSLogic2Way;
import workcraft.sdfs.SDFSLogicBase;
import workcraft.sdfs.SDFSNode;
import workcraft.sdfs.SDFSRegisterBase;
import workcraft.spreadtoken.STModel;
import workcraft.util.Vec2;

public class ASDFS2MGMapper implements Tool {
	public static final String _modeluuid = "24a8a176-dafe-11db-8314-0800200c9a66";
	public static final String _displayname = "Convert to Marked Graph";

	public boolean isModelSupported(UUID modelUuid) {
		return true;
	}

	static HashMap<ASDFSRegister, MGTransition> r2t = new HashMap<ASDFSRegister, MGTransition>();

	static ASDFSRegister source;
	static MGModel g;
	static final float delta = 0.05f;

	public static void connect(ASDFSRegister dest, List<ASDFSLogic> path) {
		MGTransition t1 = r2t.get(source);
		MGTransition t2 = r2t.get(dest);

		int evalDelay = 0;
		int resetDelay = 0;
		String logic = "";

		if (path != null)
			for (ASDFSLogic l : path) {
				if (logic.length() > 0)
					logic +=",";
				logic += l.getId();
				evalDelay += l.getEvalDelay();
				resetDelay += l.getResetDelay();
			}


		try {
			MGPlace p1 = new MGPlace(g.getRoot());
			MGPlace p2 = new MGPlace(g.getRoot());

			p1.setLabel(source.getId()+"-f/"+logic);
			p2.setLabel(source.getId()+"-e/"+logic);


			Vec2 v1 = t1.transform.getTranslation2d();
			Vec2 v2 = t2.transform.getTranslation2d();



			Vec2 v3 = new Vec2(v2);
			v3.sub(v1);
			v3.normalize();

			Vec2 v4 = new Vec2(v1);
			v4.mul(0.5f);
			Vec2 v5 = new Vec2(v2);
			v5.mul(0.5f);
			v4.add(v5);


			p1.transform.translateAbs(v4.getX() - v3.getY() * delta, v4.getY() + v3.getX() * delta, 0.0f);
			p2.transform.translateAbs(v4.getX() + v3.getY() * delta, v4.getY() - v3.getX() * delta, 0.0f);

			p1.setDelayDev(1.0);
			p1.setDelayMean((double)evalDelay);

			p2.setDelayDev(1.0);
			p2.setDelayMean((double)resetDelay);

			if (source.isMarked()) {
				p1.setTokens(1);
				p2.setTokens(0);

			}
			else {

				p1.setTokens(0);
				p2.setTokens(1);
			}

			g.createConnection(t1, p1);
			g.createConnection(p1, t2);
			g.createConnection(t2, p2);
			g.createConnection(p2, t1);

		} catch (UnsupportedComponentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}

	public static void search(ASDFSLogic n, LinkedList<ASDFSLogic> path) {
		path.add(n);
		LinkedList<SDFSNode> out = n.getOut();
		for (SDFSNode on : out) {
			if (on instanceof ASDFSRegister)
				connect ((ASDFSRegister)on, path);
			else
				search ((ASDFSLogic)on, (LinkedList)path.clone());
		}
	}

	public MGModel map (WorkCraftServer server, ASDFSModel src) {
		g = new MGModel();

		List<ASDFSRegister> regs = src.getRegisters();

		r2t.clear();

		try {
			for (ASDFSRegister r : regs) {
				MGTransition t = new MGTransition(g.getRoot());
				t.setLabel(r.getId());
				t.transform.copy(r.transform);
				r2t.put(r, t);
			}

			for (ASDFSRegister r : regs) {
				source = r;
				LinkedList<SDFSNode> out = r.getOut();
				for (SDFSNode n : out) {
					if (n instanceof ASDFSRegister)
						connect ((ASDFSRegister)n, null);
					else
						search ((ASDFSLogic)n, new LinkedList<ASDFSLogic>());
				}

			}
		}

		catch (UnsupportedComponentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return g;
	}

	public void run(Editor editor, WorkCraftServer server) {
		editor.setDocument(map(server, (ASDFSModel)editor.getDocument()));
		g = null;
		source = null;
	}

	public void init(WorkCraftServer server) {

	}

	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub

	}

	public ToolType getToolType() {
		return ToolType.TRANSFORM;
	}
}