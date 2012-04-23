package workcraft.petri;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.common.DefaultSimControls;
import workcraft.common.ExternalProcess;
import workcraft.common.MPSATOutputParser;
import workcraft.editor.Editor;

public class PetriSemimodularityChecker implements Tool {
	public static final String _modeluuid = "65f89260-641d-11db-bd13-0800200c9a66";
	public static final String _displayname = "Check for non-semi-modularity (PUNF/MPSAT)";

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
		PetriModel model = (PetriModel) editor.getDocument();

		PetriDotGSaver saver = (PetriDotGSaver)server.getToolInstance(PetriDotGSaver.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		ReadArcsComplexityReduction rd =(ReadArcsComplexityReduction)server.getToolInstance(ReadArcsComplexityReduction.class);  
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net read arcs complexity reduction tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			saver.writeFile("tmp/_net_.g", model);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
		ExternalProcess p = new ExternalProcess(frame);

		String formula = "";

		for (String clause: model.buildSemimodularityCheckClauses()) {
			if (formula.length()>0)
				formula+="|";
			formula+=clause;
		}

		try {
			p.run(new String[] {"util/punf", "-s", "-t", "-p", "tmp/_net_.g"}, ".", "Unfolding report", true);


			PrintWriter out = new PrintWriter(new FileWriter("tmp/_smodch"));
			out.print(formula);
			out.close();
			p.run(new String[] {"util/mpsat", "-F","-f" ,"-d", "@tmp/_smodch","tmp/_net_.mci"}, ".", "Model-checking report", false);

			String badTrace = MPSATOutputParser.parsePetriNetTrace(p.getOutput());

			if (badTrace != null) {
				//JOptionPane.showMessageDialog(null, clause+"\n\n"+ p.getOutput(), "Non-semimodularity found!", JOptionPane.WARNING_MESSAGE);
				if (JOptionPane.showConfirmDialog(null, "Do you wish to load the event trace that leads to the non-semimodular state?", "Confirm", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
					((DefaultSimControls)editor.getSimControls()).setTrace(badTrace);

			}



		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File f = new File("tmp/_net_.g");
		f.delete();
		f = new File("tmp/_net_.mci");
		f.delete();
		f = new File ("tmp/_smodch");
		f.delete();
	}

	public ToolType getToolType() {
		return ToolType.GENERAL;
	}

}
