package workcraft.sdfs;

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
import workcraft.editor.EditableConnection;
import workcraft.editor.Editor;
import workcraft.logic.DNF;
import workcraft.logic.DNFClause;
import workcraft.logic.DNFLiteral;
import workcraft.logic.InvalidExpressionException;
import workcraft.petri.EditablePetriPlace;
import workcraft.petri.EditablePetriTransition;
import workcraft.petri.PetriModel;
import workcraft.spreadtoken.STModel;
import workcraft.spreadtoken.STRegister;
import workcraft.util.Vec2;

public class ASDFSMapper implements Tool {
	public static final String _modeluuid = "*";
	public static final String _displayname = "Convert to abstract SDFS";

	static HashMap<String, BasicEditable> nodes = new HashMap<String, BasicEditable>();

	public boolean isModelSupported(UUID modelUuid) {
		if ( modelUuid.equals(CFModel._modeluuid) ||
				modelUuid.equals(STModel._modeluuid)||
				modelUuid.equals(ATModel._modeluuid)
		)
			return true;
		else
			return false;
	}

	public ASDFSModel map (WorkCraftServer server, Model source) {
		ASDFSModel asdfs = new ASDFSModel();
//		asdfs.bind(server);

		LinkedList<BasicEditable> components = new LinkedList<BasicEditable>();
		source.getComponents(components);

		try {
			for (BasicEditable e: components) {
				if (e instanceof SDFSLogicBase) {
					SDFSLogicBase l = (SDFSLogicBase)e;
					ASDFSLogic al = new ASDFSLogic(asdfs.getRoot());
					al.transform.copy(l.transform);
					al.setLabel(l.getLabel());
					al.setId(l.getId());

					if (e instanceof SDFSLogic2Way) {
						al.setEvalDelay(((SDFSLogic2Way)e).getFwdEvalDelay());						
						al.setResetDelay(((SDFSLogic2Way)e).getFwdResetDelay());
					} else
						if (e instanceof SDFSLogic1Way) {
							al.setEvalDelay(((SDFSLogic1Way)e).getEvalDelay());						
							al.setResetDelay(((SDFSLogic1Way)e).getResetDelay());
						}

					nodes.put(al.getId(), al);
				} else
					if (e instanceof SDFSRegisterBase) {
						SDFSRegisterBase r = (SDFSRegisterBase)e;
						ASDFSRegister ar = new ASDFSRegister(asdfs.getRoot());
						ar.transform.copy(r.transform);
						ar.setLabel(r.getLabel());
						ar.setId(r.getId());

						if (e instanceof STRegister) {
							STRegister str = (STRegister) e;

							if (str.isMarked()) {
								boolean front = true; 

								for (SDFSRegisterBase s: str.getRegisterPostset()) {
									if (((STRegister)s).isMarked()) {
										front = false;
										break;
									}
								}

								if (front)
									ar.setMarked(true);
							}

						}

						nodes.put(ar.getId(), ar);
					}
			}
		}
		catch (DuplicateIdException e) {
			e.printStackTrace();
		} catch (UnsupportedComponentException e) {
			e.printStackTrace();
		}

		List<EditableConnection> cons = source.getConnections();

		try {
			for (EditableConnection c : cons) {
				String id1 = c.getFirst().getId();
				String id2 = c.getSecond().getId();

				BasicEditable n1 = nodes.get(id1);
				BasicEditable n2 = nodes.get(id2);

				if ( (n1==null) || (n2==null))
					continue;

				asdfs.createConnection(n1, n2);
			}
		} catch (InvalidConnectionException e) {
			e.printStackTrace();
		}

		nodes.clear();
		return asdfs;
	}

	public void run(Editor editor, WorkCraftServer server) {
		editor.setDocument(map(server, editor.getDocument()));
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