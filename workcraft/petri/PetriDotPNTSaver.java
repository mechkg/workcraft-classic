package workcraft.petri;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;
import workcraft.petri.PetriModel;

public class PetriDotPNTSaver implements Tool {
	public static final String _modeluuid = "65f89260-641d-11db-bd13-0800200c9a66";
	public static final String _displayname = "Net as .pnt";

	public void run(Editor editor, WorkCraftServer server) {
		PetriModel doc = (PetriModel) (editor.getDocument());
		
		String last_directory = editor.getLastDirectory();
		
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new PNTFileFilter());
			if (last_directory != null)
				fc.setCurrentDirectory(new File(last_directory));
			if (fc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			{
				String path = fc.getSelectedFile().getPath();
				if (!path.endsWith(".pnt")) path += ".pnt";
				{
					// saving in .pnt format
					
					try
					{
						PrintWriter out = new PrintWriter(new FileWriter(path));
						
						out.println("P   M   PRE,POST  NETZ 1");
						
						HashMap<EditablePetriPlace, Integer> pid = new HashMap<EditablePetriPlace, Integer>();
						HashMap<EditablePetriTransition, Integer> tid = new HashMap<EditablePetriTransition, Integer>();
						
						int k;
						
						k = 0;
						for(EditablePetriPlace p : doc.places) pid.put(p, k++);
						k = 0;
						for(EditablePetriTransition t : doc.transitions) tid.put(t, k++);
						
						for(EditablePetriPlace p : doc.places)
						{
							out.print(pid.get(p)+" "+p.getTokens()+" ");
							int i = 0;
							for(EditablePetriTransition t: p.getIn())
							{
								if (i == 1) out.print(": "+(p.getIn().size()-1));
								if (i > 0) out.print(" ");
								out.print(tid.get(t));
								i++;
							}
							if (p.getOut().size() > 0)
							{
								out.print(", "); i = 0;
								for(EditablePetriTransition t: p.getOut())
								{
									if (i == 1) out.print(": "+(p.getOut().size()-1));
									if (i > 0) out.print(" ");
									out.print(tid.get(t));
									i++;
								}
							}
							out.println();
						}				
						
						out.println("@");
						out.println("place nr.             name capacity time");
						
						for(EditablePetriPlace p: doc.places) out.println(pid.get(p)+": "+p.getId()+" oo 0");
						
						out.println("@");
						out.println("trans nr.             name priority time");
						for(EditablePetriTransition t: doc.transitions) out.println(tid.get(t)+": "+t.getId()+" oo 0");
						
						out.println("@");
						out.close();
					}
					catch (IOException e)
					{
						JOptionPane.showMessageDialog(null, "File could not be opened for writing.");
						return;
					}					
				}
			}
		
		
	}

	public boolean isModelSupported(UUID modelUuid) {
		// TODO Auto-generated method stub
		return false;
	}

	public void init(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}

	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}

	public ToolType getToolType() {
		return ToolType.EXPORT;
	}
}
