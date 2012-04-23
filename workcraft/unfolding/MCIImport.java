package workcraft.unfolding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.python.core.Py;
import org.python.core.PyObject;

import workcraft.DuplicateIdException;
import workcraft.InvalidConnectionException;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;


public class MCIImport implements Tool {

	class BadTiming extends Exception {};

	public static final String _modeluuid = "23a72f18-5c90-11dc-8314-0800200c9a66";
	public static final String _displayname = "Import unfolding (.mci)";

	static final float xSpread = 0.4f;
	static final float ySpread = 0.3f;

	Prefix p;
	float minx = 0.0f;

	void putCondition (UnfoldingModel doc, Condition c, float x, float y) throws UnsupportedComponentException, DuplicateIdException, InvalidConnectionException {

		if (c.c == null) {
			c.c = new EditableCondition(doc.getRoot());
			c.c.transform.translateAbs(x, y, 0.0f);

			if (x < minx)
				minx = x;

			c.c.setLabel(c.origPlaceId);
			y+=ySpread;

			for (Event e: c.postset) {
				putEvent(doc, e, x, y);
				x -= xSpread;
				doc.createConnection(c.c, e.e);
			}
		}
	}

	void putEvent (UnfoldingModel doc, Event e, float x, float y) throws UnsupportedComponentException, DuplicateIdException, InvalidConnectionException {
		if (e.e == null) {
			e.e = new EditableEvent(doc.getRoot());
			e.e.setId("e"+Integer.toString(e.number));
			
			if (e.origTransId.startsWith("d."))
				e.e.setLabel(e.origTransId.substring(2));
			else
				e.e.setLabel(e.origTransId.substring(2));
			
			e.e.transform.translateAbs(x, y, 0.0f);

			if (x < minx)
				minx = x;


			for (Condition c: p.conditions) {
				if (c.presetEvent == e) {
					putCondition (doc, c, x, y+ySpread);					
					x -= xSpread;
					doc.createConnection(e.e, c.c);
				}
			}
		}
	}

	public UnfoldingModel readMCIFile (String path, String tagsFilePath) throws IOException {
		UnfoldingModel doc = new UnfoldingModel();

		try
		{
			minx = 0.0f;
			p = new Prefix();
			File f = new File(path);

			p.fromMCI(f);

			LinkedList<Condition> root = new LinkedList<Condition>();

			for (Condition c: p.conditions) {
				if (c.presetEvent == null)
					root.add(c);
			}


			for (Condition c: root) {
				putCondition(doc, c, minx - xSpread, 0.0f);
			}

			for (Event cut : p.cutoffs.keySet()) {
				cut.e.setCutOff(true);
				Event corr =  p.cutoffs.get(cut);
				if (corr != null)
					cut.e.setCorresponding(corr.e);
				else
					cut.e.setCorresponding(null);

			}



			if (tagsFilePath != null) {
				try {
					File f2 = new File(tagsFilePath);
					BufferedReader read = new BufferedReader(new InputStreamReader(new FileInputStream(f2)));

					HashMap <String, String> tags = new HashMap<String, String>();

					String s = read.readLine();
					while (s!=null) {
						String[] s2 = s.split(":");

						if (s2.length==2)
							tags.put(s2[0], s2[1]);

						s = read.readLine();
					}
					read.close();

					if (tags.size() == 0)
						throw new BadTiming();

					for (EditableEvent e : doc.events) {
						String t = tags.get(e.getLabel());
						if (t!=null) {
							String[] timing = t.split(" ");
							if ( (timing.length == 3) && (timing[0].equals("bd"))) {
								e.setDelayMin(Double.parseDouble(timing[1]));
								e.setDelayMax(Double.parseDouble(timing[2]));
							} else {
								throw new BadTiming();
							}
						}
					}

					doc.setTimingAttached(true);
				} 
				catch (BadTiming e) 
				{
					doc.setTimingAttached(false);
				}

			}

			return doc;
		} catch (UnsupportedComponentException e) {
			e.printStackTrace();
		} catch (DuplicateIdException e) {
			e.printStackTrace();
		} catch (InvalidConnectionException e) {
			e.printStackTrace();
		}	

		return null;
	}

	public void run(Editor editor, WorkCraftServer server) {
		String last_directory = editor.getLastDirectory();
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new MCIFileFilter());
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		if (fc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			try {
				String path = fc.getSelectedFile().getPath(), path2;
				if (path.endsWith(".mci"))
					path2 = path.substring(0, path.length()-4)+".tag";
				else
					path2 = path + ".tag";
				
				UnfoldingModel doc = readMCIFile(fc.getSelectedFile().getPath(), new File(path2).exists() ? path2 : null); 
				if (doc != null)
					editor.setDocument(doc);
				else
					JOptionPane.showMessageDialog(null, "File could not be opened.");
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "File could not be opened:\n\n"+e.getMessage());
				return;
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
		return ToolType.GENERAL;
	}
}
