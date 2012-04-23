package workcraft.common;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import workcraft.DocumentOpenException;
import workcraft.Model;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.counterflow.CFModel;
import workcraft.editor.Editor;
import workcraft.gate.GateModel;
import workcraft.petri.PetriDotGSaver;
import workcraft.petri.PetriDotGSaver2;
import workcraft.petri.PetriModel;
import workcraft.petri.ReadArcsComplexityReduction;
import workcraft.spreadtoken.STModel;

public class SchematicNetSemimodularityCheck implements Tool {
	public static final String _modeluuid = "*";
	public static final String _displayname = "Check for non-semimodularity (PUNF/MPSAT)";

	public boolean isModelSupported(UUID modelUuid) {
		if (modelUuid.compareTo(STModel._modeluuid)==0)
			return true;
		if (modelUuid.compareTo(CFModel._modeluuid)==0)
			return true;
		if (modelUuid.compareTo(GateModel._modeluuid)==0)
			return true;
		return false;
	}

	public void init(WorkCraftServer server) {
		// TODO Auto-generated method stub
	}

	public void run(Editor editor, WorkCraftServer server) {
		Model doc = editor.getDocument();
		PetriNetMapper mapper = (PetriNetMapper)server.getToolInstance(PetriNetMapper.class);
		if (mapper == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net Mapper tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		PetriDotGSaver2 saver = (PetriDotGSaver2)server.getToolInstance(PetriDotGSaver2.class);
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net .g export tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		ReadArcsComplexityReduction rd =(ReadArcsComplexityReduction)server.getToolInstance(ReadArcsComplexityReduction.class);  
		if (saver == null) {
			JOptionPane.showMessageDialog(null, "This tool requires Petri Net read arcs complexity reduction tool, which was not loaded", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		JFrame frame = (JFrame)server.python.get("_main_frame", JFrame.class);
		ExternalProcess p = new ExternalProcess(frame);

		try {
			PetriModel schematicNet =mapper.map(server, doc);

			if (doc instanceof GateModel) {
				GateModel gatedoc = (GateModel)doc;
				File bojo = new File(gatedoc.getActiveInterfacePath());
				if (bojo.exists()) {
					PetriModel iface;
					try {
						iface = (PetriModel)editor.load(bojo.getAbsolutePath());
						schematicNet.applyInterface(iface);
					} catch (DocumentOpenException e) {
						e.printStackTrace();
					}
				}
			}

			schematicNet = rd.reduce(schematicNet);


			saver.writeFile("tmp/_net_.g", schematicNet );

			File mci = new File("tmp/_net_.mci");
			if (mci.exists()) {
				if (JOptionPane.showConfirmDialog(null, "Do you wish to reuse existing mci?", "Confirm", JOptionPane.YES_NO_OPTION)==JOptionPane.NO_OPTION) {
					p.run(new String[] {"util/punf", "-s", "-t", "-p", "tmp/_net_.g"}, ".", "Unfolding report", true);
				}
			} else 
				p.run(new String[] {"util/punf", "-s", "-t", "-p", "tmp/_net_.g"}, ".", "Unfolding report", true);

//			String formula = "";

/*			for (String clause: schematicNet.buildSemimodularityCheckClauses()) {
				if (formula.length()>0)
					formula+="|";
				formula+=clause;
			}
*/
//			PrintWriter out;
			String badTrace;
			
//			out = new PrintWriter(new FileWriter("tmp/_smodch"));
//			out.print(formula);
//			out.close();

/*			if (JOptionPane.showConfirmDialog(null, "Enable shortest trace search option (may take a considerably longer time)?", "Confitm", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
				p.run(new String[] {"util/mpsat", "-F", "-f", "-d", "@tmp/_smodch","tmp/_net_.mci"}, ".", "Model-checking report", false);
			else
				p.run(new String[] {"util/mpsat", "-F", "-d", "@tmp/_smodch","tmp/_net_.mci"}, ".", "Model-checking report", false);*/
			
			p.run(new String[] {"util/mpsat", 	"-Fs", "-f", "-d", "@reach/out-pers.re","tmp/_net_.mci"}, ".", "Model-checking report", true);

			badTrace = MPSATOutputParser.parseSchematicNetTrace(p.getOutput());

			if (badTrace != null) {
				//JOptionPane.showMessageDialog(null, formula+"\n\n"+ p.getOutput(), "Non-semimodularity found!", JOptionPane.WARNING_MESSAGE);
				if (JOptionPane.showConfirmDialog(null, "There is a reachable non-semimodular state. Do you wish to load the event trace that leads to the non-semimodular state?", "Confirm", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
					((DefaultSimControls)editor.getSimControls()).setTrace(badTrace);
			} else {
				JOptionPane.showMessageDialog(null,"No reachable non-semimodular states found");
			}

/*
			for (String clause: schematicNet.buildSemimodularityCheckClauses()) {
				out = new PrintWriter(new FileWriter("tmp/_smodch"));
				out.print(clause);
				out.close();

				p.run(new String[] {"util/mpsat", "-F", "-f", "-d", "@tmp/_smodch","tmp/_net_.mci"}, ".", "Model-checking report", false);
				badTrace = MPSATOutputParser.parseSchematicNetTrace(p.getOutput());

				if (badTrace != null) {
					JOptionPane.showMessageDialog(null, clause+"\n\n"+ p.getOutput(), "Non-semimodularity found!", JOptionPane.WARNING_MESSAGE);
					if (JOptionPane.showConfirmDialog(null, "Do you wish to load the event trace that leads to the non-semimodular state?", "Confirm", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {
						((DefaultSimControls)editor.getSimControls()).setTrace(badTrace);
						break;
					}

				}
			}
*/
			File f = new File("tmp/_net_.g");
			f.delete();
			if (JOptionPane.showConfirmDialog(null, "Do you wish to delete the unfolding file (mci)?", "Confirm", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) { 
				f = new File("tmp/_net_.mci");
				f.delete();
			}
			f = new File ("tmp/_smodch");
			f.delete();

			editor.refresh();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void deinit(WorkCraftServer server) {
			// TODO Auto-generated method stub

		}

		public ToolType getToolType() {
			return ToolType.GENERAL;
		}
	}
