package workcraft.stg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import workcraft.InvalidConnectionException;
import workcraft.Tool;
import workcraft.ToolType;
import workcraft.UnsupportedComponentException;
import workcraft.WorkCraftServer;
import workcraft.editor.BasicEditable;
import workcraft.editor.Editor;
import workcraft.petri.EditablePetriPlace;
import workcraft.petri.EditablePetriTransition;
import workcraft.petri.GFileFilter;
import workcraft.util.Vec2;

public class STGDotGLoader implements Tool {
	public static final String _modeluuid = "10418180-D733-11DC-A679-A32656D89593";
	public static final String _displayname = "Import from .g format";


	// create the lists of all of the transition types
	ArrayList<String> internal	= new ArrayList<String>();
	ArrayList<String> inputs	= new ArrayList<String>();
	ArrayList<String> outputs	= new ArrayList<String>();
	ArrayList<String> dummy		= new ArrayList<String>();

	public BasicEditable createBasicEditable(String nameid, STGModel doc, SortedMap<String, BasicEditable> bem) {

		// get the name of the first component
		Pattern p = Pattern.compile("([a-zA-Z\\_][a-zA-Z\\_0-9]*).*");
//		Pattern p2 = Pattern.compile("^p([0-9]+)$");
		Matcher m1 = p.matcher(nameid);
		
		// check whether this element is created already
		BasicEditable be1 = bem.get(nameid);
		if (be1==null) {
			// if not created, try to decide, how to create it
			if (m1.find()) {
				String name = m1.group(1);
				// find out what type of component that is
				try {
					if (inputs.contains(name)) {
						be1 = new EditableSTGTransition(doc.getRoot());
						be1.setLabel(nameid);
					} else if (outputs.contains(name)) {
						be1 = new EditableSTGTransition(doc.getRoot());
						be1.setLabel(nameid);
					} else if (internal.contains(name)) {
						be1 = new EditableSTGTransition(doc.getRoot());
						be1.setLabel(nameid);
					} else if (dummy.contains(name)) {
						be1 = new EditableSTGTransition(doc.getRoot());
						be1.setLabel(nameid);
					} else { 
						// consider it as the place
						be1 = new EditableSTGPlace(doc.getRoot());
						// place label is used inside the program, but
						// can be deleted after the import
						be1.setLabel(nameid);
					}
					bem.put(nameid, be1);
				} catch (UnsupportedComponentException e) {
					e.printStackTrace();
					return null;
				}
			}
		}
		
		return be1;
	}
	
	public boolean readFile (String path, STGModel doc) throws IOException {
		File file = new File(path);
		
//		Scanner scanner = new Scanner(file);
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		String str;
		String s[];
		String inputList="";
		String outputList="";
		
		// read heading
		while ((str=br.readLine())!=null) {
			s = str.trim().split("[ \\t\\v\\f]+");
			if (s.length==0) continue;
			if (s[0].charAt(0)=='#') continue;
			
			if (s[0].equals(".inputs"))
				for (int i=1;i<s.length;i++) {
					if (s[i].charAt(0)=='#') break;
					inputs.add(s[i]);
					if (!s[i].equals("")) inputList+=" ";
					inputList+=s[i];
				}

			if (s[0].equals(".outputs"))
				for (int i=1;i<s.length;i++) {
					if (s[i].charAt(0)=='#') break;
					outputs.add(s[i]);
					if (!s[i].equals("")) outputList+=" ";
					outputList+=s[i];
				}

			if (s[0].equals(".internal"))
				for (int i=1;i<s.length;i++) {
					if (s[i].charAt(0)=='#') break;
					internal.add(s[i]);
				}
			
			if (s[0].equals(".dummy"))
				for (int i=1;i<s.length;i++) {
					if (s[i].charAt(0)=='#') break;
					dummy.add(s[i]);
				}
			
			if (s[0].equals(".graph")) break;
		}
		// if neither type of transition was found, quit with an error
		if (inputs.isEmpty()&&outputs.isEmpty()&&internal.isEmpty()&&dummy.isEmpty()) return false;
		
		
		BasicEditable be1, be2; // first and second connection candidates
		Pattern p;
		Matcher m;
		SortedMap<String, BasicEditable> bem = new TreeMap<String, BasicEditable>();

		// read connections
		while ((str=br.readLine())!=null) { 
			s = str.trim().split("[ \\t\\v\\f]+");
			if (s.length==0) continue;
			if (s[0].charAt(0)=='#') continue;
			
			if (s[0].equals(".marking")) {
				p = Pattern.compile(".marking \\{([^#]*)\\}");
				m = p.matcher(str);
				if (!m.find()) continue;
				str = m.group(1).trim();
				
				s = str.split("[ \\t\\v\\f]+");
				
				// read starting markings
				for (int i=0;i<s.length;i++) {
					
					if (s[i].charAt(0)!='<') {
						// simple case, just find the place and put the tokens
						p = Pattern.compile("([a-zA-Z\\_][a-zA-Z\\_0-9\\/]*)(=([0-9]+))?");
						m = p.matcher(s[i]);
						if (m.find()) {
							str=m.group(1); // name of the signal

							if (m.group(m.groupCount())!=null) {
								((EditableSTGPlace)bem.get(str)).setTokens(Integer.valueOf(m.group(m.groupCount())));
							} else {
								((EditableSTGPlace)bem.get(str)).setTokens(1);
							}
						}
						
					} else {
						
						str = "\\<("+STGModel.signalPattern+"),("+STGModel.signalPattern+")\\>(=([0-9]+))?";
						p = Pattern.compile(str);
						m = p.matcher(s[i]);
						
						if (m.find()) {
//							for (int j=0;j<=m.groupCount();j++)
//								System.out.println(m.group(j));
							// groups 1 and 5 correspond to full transition names 
							EditableSTGTransition et1 = (EditableSTGTransition)bem.get(m.group(1));
							EditableSTGTransition et2 = (EditableSTGTransition)bem.get(m.group(5));
							
							if (et1!=null&&et2!=null) {
								LinkedList<EditablePetriPlace> ep = et1.getOut();
								ep.retainAll(et2.getIn());
								
								if (m.group(m.groupCount())!=null) {
									ep.getFirst().setTokens(Integer.valueOf(m.group(m.groupCount())));
								} else {
									ep.getFirst().setTokens(1);
								}
							}
							
							// TODO: finish this part...
						}
					}
					
				}
				break;
			}
			
			if (s[0].charAt(0)=='.') continue; // ignore other lines beginning with '.' (some unimplemented features?)


			be1 = createBasicEditable(s[0], doc, bem);
						
//			System.out.print(s[0] + " connects to:");
			for (int i=1;i<s.length;i++) {
				
				if (s[i].charAt(0)=='#') break;
				
				be2 = createBasicEditable(s[i], doc, bem);
				
				try {
					doc.createConnection(be1, be2);
				} catch (InvalidConnectionException e) {
					e.printStackTrace();
				}
			}

			// repositioning
			LinkedList<EditablePetriTransition> transitions	= new LinkedList<EditablePetriTransition>();
			float posx = (float) (transitions.size()*0.2);
			float posy = (float) 0.5;
			doc.getTransitions(transitions);
			for (EditablePetriTransition t : transitions) {
				t.transform.translateAbs(new Vec2(posx, -posy));
				posx-=0.2;
			}
			
			LinkedList<EditablePetriPlace> places	= new LinkedList<EditablePetriPlace>();
			doc.getPlaces(places);
			posx=0;
			for (EditablePetriPlace pl : places) {
				pl.transform.translateAbs(new Vec2(posx, posy));
				posx-=0.2;
				// wipe out place names
				pl.setLabel("");
			}
			
			// set input and output lists
			doc.setSTGInputList(inputList);
			doc.setSTGOutputList(outputList);

		}
		br.close();
		return true;
	}
		

	public void deinit(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}

	public ToolType getToolType() {
		// TODO Auto-generated method stub
		return ToolType.IMPORT;
	}

	public void init(WorkCraftServer server) {
		// TODO Auto-generated method stub
		
	}

	public boolean isModelSupported(UUID modelUuid) {
		// TODO Auto-generated method stub
		return false;
	}

	public void run(Editor editor, WorkCraftServer server) {
		STGModel doc = (STGModel) (editor.getDocument());
		String last_directory = editor.getLastDirectory();

		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new GFileFilter());
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		if (fc.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
		{
			String path = fc.getSelectedFile().getPath();
			if (!path.endsWith(".g")) path += ".g";
			{
				// saving in .g format
				try
				{
					readFile(path, doc);
				}
				catch (IOException e)
				{
					JOptionPane.showMessageDialog(null, "File could not be opened for reading.");
					return;
				}					
			}
		}
		
	}

}
