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

public class STGMPSatChecker implements Tool {
	public static final String _modeluuid = "10418180-D733-11DC-A679-A32656D89593";
	public static final String _displayname = "Report complex gate implementation";

	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub

	}

	public void init(WorkCraftServer server) {
		// TODO Auto-generated method stub

	}

	public boolean isModelSupported(UUID modelUuid) {
		// TODO Auto-generated method stub
		return false;
	}


	public void run(Editor editor, WorkCraftServer server) {
		STGModel model = (STGModel) editor.getDocument();

		STGDotGSaver saver = (STGDotGSaver)server.getToolInstance(STGDotGSaver.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		try {
			File f = new File("tmp/_net_.g");
			f.delete();
			f = new File("tmp/_net_.mci");
			f.delete();
			f = new File ("tmp/_smodch");
			f.delete();

			saver.writeFile("tmp/_net_.g", model);

			JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
			ExternalProcess p = new ExternalProcess(frame);
	
//			String formula = "";
	//		p.run(new String[] {"util/petrify", "-ip", "-o", "tmp/_new_net_.g", "tmp/_net_.g"}, ".", "Petrify", true);

			p.run(new String[] {"util/punf", "-s", "-t", "-p", "tmp/_net_.g"}, ".", "Unfolding report", true);

//			String name1, name2;
	//		String[] strl = null;
		//	int conflicts = 0;
			
			f = new File("tmp/_net_.mci");
			if (f.exists()) {
				
//				p.run(new String[] {"util/mpsat", 	"-Fs", "-f", "-d", "@reach/out-pers.re","tmp/_net_.mci"}, ".", "Model-checking report", true);
				p.run(new String[] {"util/mpsat", 	"-E", "tmp/_net_.mci"}, ".", "Model-checking report", true);
					
//					String badTrace = MPSATOutputParser.parsePetriNetTrace(p.getOutput());
			} else {
				JOptionPane.showMessageDialog(null, "File _net_.mci was not found", "Procedure report", JOptionPane.WARNING_MESSAGE);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public ToolType getToolType() {
		return ToolType.GENERAL;
	}

}
