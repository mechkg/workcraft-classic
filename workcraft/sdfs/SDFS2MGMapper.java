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
import workcraft.abstractsdfs.ASDFS2MGMapper;
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

public class SDFS2MGMapper implements Tool {
	public static final String _modeluuid = "*";
	public static final String _displayname = "Convert to register connectivity graph";

	public boolean isModelSupported(UUID modelUuid) {
		if (modelUuid.compareTo(STModel._modeluuid)==0)
			return true;
		return false;
	}
	
	public void run(Editor editor, WorkCraftServer server) {
		
		ASDFSMapper asdfsmapper = (ASDFSMapper)server.getToolInstance(ASDFSMapper.class);
		if (asdfsmapper == null) {
			JOptionPane.showMessageDialog(null, "This tool requires ASDFS Mapper tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ASDFS2MGMapper mgmapper = (ASDFS2MGMapper)server.getToolInstance(ASDFS2MGMapper.class);
		if (asdfsmapper == null) {
			JOptionPane.showMessageDialog(null, "This tool requires ASDFS to MG Mapper tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}			
		
		editor.setDocument(mgmapper.map(server, asdfsmapper.map(server, editor.getDocument())));
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