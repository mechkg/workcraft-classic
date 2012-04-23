package workcraft.mg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;
import workcraft.mg.MGModel;

public class MGDotGSaver implements Tool {
	public static final String _modeluuid = "4fbdb830-dba3-11db-8314-0800200c9a66";
	public static final String _displayname = ".g";

	public void run(Editor editor, WorkCraftServer server) {
		MGModel doc = (MGModel) (editor.getDocument());
		
		String last_directory = editor.getLastDirectory();
		
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new GFileFilter());
			if (last_directory != null)
				fc.setCurrentDirectory(new File(last_directory));
			if (fc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
			{
				String path = fc.getSelectedFile().getPath();
				if (!path.endsWith(".g")) path += ".g";
				{
					// saving in .g format
					
					try
					{
						PrintWriter out = new PrintWriter(new FileWriter(path));
						
						out.println("# File generated by Workcraft.");
						out.print(".dummy");
						
						for(MGTransition t: doc.transitions) out.print(" "+t.getId());
						
						out.println();
						out.println(".graph");
						
						for(MGTransition t: doc.transitions)
						{
							for(MGPlace prev: t.getIn()) out.println(prev.getId()+" "+t.getId());
							for(MGPlace next: t.getOut()) out.println(t.getId()+" "+next.getId());
						}					
						
						out.print(".marking {");
						
						for(MGPlace p: doc.places) if (p.getTokens()>0) out.print(" "+p.getId());
						out.println(" }");
						out.println(".end");
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
