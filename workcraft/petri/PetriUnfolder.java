package workcraft.petri;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.python.core.Py;
import org.python.core.PyObject;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.common.ExternalProcess;
import workcraft.common.PetriNetMapper;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;
import workcraft.petri.PetriModel;
import workcraft.unfolding.MCIImport;
import workcraft.unfolding.UnfoldingModel;

public class PetriUnfolder implements Tool {
	public static final String _modeluuid = "65f89260-641d-11db-bd13-0800200c9a66";
	public static final String _displayname = "Unfold (PUNF)";

	public void run(Editor editor, WorkCraftServer server) {
		PetriModel doc = (PetriModel) (editor.getDocument());

		PetriDotGSaver saver = (PetriDotGSaver)server.getToolInstance(PetriDotGSaver.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		
		PetriLabelSaver tagsaver = (PetriLabelSaver)server.getToolInstance(PetriLabelSaver.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net label export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		ReadArcsComplexityReduction rd =(ReadArcsComplexityReduction)server.getToolInstance(ReadArcsComplexityReduction.class);  
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net read arcs complexity reduction tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		MCIImport mci =(MCIImport)server.getToolInstance(MCIImport.class);  
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires MCIImport tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}		
		
		JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
		ExternalProcess p = new ExternalProcess(frame);
		

		try {
			saver.writeFile("tmp/_net_.g", rd.reduce(doc));
			tagsaver.writeFile("tmp/_net_.tag", doc);
			p.run(new String[] {"util/punf", "-s", "-t", "-p", "tmp/_net_.g"}, ".", "Unfolding report", true);
			editor.setDocument(mci.readMCIFile("tmp/_net_.mci", "tmp/_net_.tag"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		File f =new File("tmp/_net_.g");
		f.delete();
		f = new File("tmp/_net_.mci");
		f.delete();
		f = new File("tmp/_net_.tag");
		f.delete();
		

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
		return ToolType.TRANSFORM;
	}
}
