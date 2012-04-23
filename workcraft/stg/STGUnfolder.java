package workcraft.stg;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.common.ExternalProcess;
import workcraft.editor.Editor;
import workcraft.unfolding.MCIImport;

public class STGUnfolder implements Tool {
	public static final String _modeluuid = "10418180-D733-11DC-A679-A32656D89593";
	public static final String _displayname = "Petrify, Unfold (PUNF)";

	public void run(Editor editor, WorkCraftServer server) {
		STGModel doc = (STGModel) (editor.getDocument());

		STGDotGSaver saver = (STGDotGSaver)server.getToolInstance(STGDotGSaver.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		
/*			PetriLabelSaver tagsaver = (PetriLabelSaver)server.getToolInstance(PetriLabelSaver.class);
			if (saver == null) {
				JOptionPane.showMessageDialog(null, "This tool requires Petri Net label export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}*/
		
/*		ReadArcsComplexityReduction rd =(ReadArcsComplexityReduction)server.getToolInstance(ReadArcsComplexityReduction.class);  
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net read arcs complexity reduction tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}*/
		
		MCIImport mci =(MCIImport)server.getToolInstance(MCIImport.class);  
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires MCIImport tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}		
		
		JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
		ExternalProcess p = new ExternalProcess(frame);
		

		try {
			saver.writeFile("tmp/_net_.g", doc);
//			tagsaver.writeFile("tmp/_net_.tag", doc);
			p.run(new String[] {"util/petrify", "-o", "tmp/_new_net_.g", "tmp/_net_.g"}, ".", "Petrify", true);
			
			p.run(new String[] {"util/punf", "-s", "-t", "-p", "tmp/_new_net_.g"}, ".", "Unfolding report", true);
			editor.setDocument(mci.readMCIFile("tmp/_new_net_.mci", null));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		File f =new File("tmp/_net_.g");
		f.delete();
		f = new File("tmp/_net_.mci");
		f.delete();
//		f = new File("tmp/_net_.tag");
//		f.delete();
		

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

