package workcraft.gate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.python.core.Py;
import org.python.core.PyObject;

import workcraft.DuplicateIdException;
import workcraft.ModelBase;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.common.PetriNetMapper;
import workcraft.editor.BasicEditable;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;
import workcraft.petri.PetriDotGSaver;
import workcraft.petri.PetriModel;

public class ExtractInterface implements Tool {
	public static final String _modeluuid = "6f704a28-e691-11db-8314-0800200c9a66";
	public static final String _displayname = "Environment interface as .g";

	public PetriModel extract (WorkCraftServer server, GateModel doc)  {
		PetriNetMapper mapper = (PetriNetMapper)server.getToolInstance(PetriNetMapper.class);
		if (mapper == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net Mapper tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}

		PetriDotGSaver saver = (PetriDotGSaver)server.getToolInstance(PetriDotGSaver.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return null;
		}

		float xi = 0.0f;
		float xo = 0.0f;

		GateModel inter = new GateModel();
		server.unbind();
		inter.bind(server);

		try
		{
			for (BasicGate sg : doc.gates ) {
				if (sg instanceof Input) {
					Input i = new Input(inter.getRoot());
					i.setId(sg.getId());
					i.transform.translateAbs(xi, 0.0f, 0.0f);
					xi -= 0.1f;
				} else if (sg instanceof Output) {
					Input i = new Input(inter.getRoot());
					i.setId(sg.getId());
					i.transform.translateAbs(xo, 0.2f, 0.0f);
					xo -= 0.1f;
				}
			}
		}
		catch (UnsupportedComponentException e) {
			e.printStackTrace();
		} catch (DuplicateIdException e) {
			e.printStackTrace();
		}

		PetriModel mdl =  mapper.map(server, inter);

		LinkedList<BasicEditable> comp = new LinkedList();
		mdl.getComponents(comp);

		for (BasicEditable p : comp) 
			try {
				p.setId("iface_"+p.getId());
			} catch (DuplicateIdException e) {
				e.printStackTrace();
			}

		return mdl;
	}

	public void run(Editor editor, WorkCraftServer server) {
		editor.setDocument(extract(server, (GateModel) editor.getDocument() ));
	}

	public void init(WorkCraftServer server) {
	}

	public boolean isModelSupported(UUID modelUuid) {
		return false;
	}

	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub

	}

	public ToolType getToolType() {
		return ToolType.EXPORT;
	}
}