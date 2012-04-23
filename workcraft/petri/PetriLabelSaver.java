package workcraft.petri;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.python.core.Py;
import org.python.core.PyObject;

import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;
import workcraft.petri.PetriModel;

public class PetriLabelSaver implements Tool {
	public static final String _modeluuid = "65f89260-641d-11db-bd13-0800200c9a66";
	public static final String _displayname = "Labels as .tag";

	public void writeFile (String path, PetriModel doc) throws IOException {
		PrintWriter out = new PrintWriter(new FileWriter(path));

		for(EditablePetriTransition t: doc.transitions)
			out.println(t.getId()+":"+t.getLabel());			
		out.close();
	}

	public void run(Editor editor, WorkCraftServer server) {
		PetriModel doc = (PetriModel) (editor.getDocument());
		String last_directory = editor.getLastDirectory();

		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new TAGFileFilter());
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		if (fc.showSaveDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			String path = fc.getSelectedFile().getPath();
			if (!path.endsWith(".tag")) path += ".tag";
			{
				try
				{
					writeFile(path, doc);
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
