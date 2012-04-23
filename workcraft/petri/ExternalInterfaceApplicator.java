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

import workcraft.DocumentOpenException;
import workcraft.Model;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.WorkCraftServer;
import workcraft.XwdFileFilter;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;
import workcraft.petri.PetriModel;

public class ExternalInterfaceApplicator implements Tool {
	public static final String _modeluuid = "65f89260-641d-11db-bd13-0800200c9a66";
	public static final String _displayname = "Apply environment interface";

	public void run(Editor editor, WorkCraftServer server) {
		PetriModel doc = (PetriModel) (editor.getDocument());
		String last_directory = editor.getLastDirectory();
		
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new XwdFileFilter());
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		if (fc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			try {
				Model env = editor.load(fc.getSelectedFile().getAbsolutePath());
				if (env.getClass() != PetriModel.class)
					JOptionPane.showMessageDialog(null, "Hmmmm... that is not a Petri Net!", "Deception", JOptionPane.WARNING_MESSAGE);
				else
					doc.applyInterface((PetriModel)env);
				
			} catch (DocumentOpenException e) {
				e.printStackTrace();
			}
		}
		
		editor.refresh();
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
		return ToolType.GENERAL;
	}
}
