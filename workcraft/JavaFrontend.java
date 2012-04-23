package workcraft;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import workcraft.common.ExternalProcess;
import workcraft.editor.BasicEditable;
import workcraft.editor.BoundingBox;
import workcraft.editor.ComponentHotkey;
import workcraft.editor.ComponentWrapper;
import workcraft.editor.EditableConnection;
import workcraft.editor.Editor;
import workcraft.editor.EditorPane;
import workcraft.editor.GroupNode;
import workcraft.editor.ModelWrapper;
import workcraft.editor.PropertyEditable;
import workcraft.editor.PropertyEditor;
import workcraft.editor.PropertyEditorTable;
import workcraft.editor.PropertyEditorTableModel;
import workcraft.editor.TransferableDocumentFragment;
import workcraft.editor.TreeDragSource;
import workcraft.editor.colorcell.ColorCellRenderer;
import workcraft.propertyeditor.EnumWrapper;
import workcraft.util.Colorf;
import workcraft.util.Mat4x4;
import workcraft.util.Vec2;
import workcraft.visual.PSPainter;
import workcraft.visual.SVGPainter;

public class JavaFrontend extends JFrame implements Editor, PropertyEditor, TableModelListener, ClipboardOwner {

	class ConsoleOutputStream extends FilterOutputStream {
		JTextArea target;

		public ConsoleOutputStream(OutputStream aStream, JTextArea target) {
			super(aStream);
			this.target = target;
		}

		public void write(byte b[]) throws IOException {
			String s = new String(b);
			target.append(s);
		}

		public void write(byte b[], int off, int len) throws IOException {
			String s = new String(b , off , len);
			target.append(s);
		}
	}

	class ErrorOutputStream extends FilterOutputStream {
		JTextArea target;

		public ErrorOutputStream(OutputStream aStream, JTextArea target) {
			super(aStream);
			this.target = target;
		}

		public void puts(String s) {
			target.append(s);
			if (getPanelBottomTabs().getSelectedIndex()!=1) {
				getPanelBottomTabs().setForegroundAt(1, Color.RED);
			}
		}

		public void write(byte b[]) throws IOException {
			String s = new String(b);
			puts(s);
		}

		public void write(byte b[], int off, int len) throws IOException {
			String s = new String(b , off , len);
			puts(s);
		}
	}

	private static final long serialVersionUID = 1L;	

	
	WeakHashMap<JMenuItem, Tool> mnu_tool_map = new WeakHashMap<JMenuItem, Tool>();

	private JSplitPane splitMain = null;

	private JSplitPane splitLeft = null;

	private JSplitPane splitRight = null;

	private JSplitPane splitCentral = null;

	private Object preSimState = null;

	private JMenuBar menubarMain = null;

	private JMenu menuFile = null;
	private JMenuItem menuitemNew = null;

	private JScrollPane scrollComponentTree = null;

	private JTree componentTree = null;

	private DefaultMutableTreeNode component_root;  //  @jve:decl-index=0:

	private JTabbedPane panelBottomTabs = null;

	private JPanel panelConsole = null;

	private JTextField textCommandLine = null;

	private JButton btnExecute = null;
	private JScrollPane scrollConsoleOutput = null;
	private JTextArea textConsoleOutput = null;
	private JPanel panelEditor = null;
	private JScrollPane scrollEditorToolbar = null;
	private JPanel panelEditorView = null;
	private JPanel panelEditorToolbar = null;
	private JToggleButton btnToggleSnap = null;
	private PrintStream outPrintStream;
	private PrintStream errPrintStream;
	private EditorPane editorView;
	private WorkCraftServer server;  //  @jve:decl-index=0:
	private JScrollPane scrollErrorOutput = null;
	private JTextArea textErrorOutput = null;
	private JToggleButton btnToggleGrid = null;
	private JMenuItem menuitemExit = null;
	private JPanel panelEditorCommands = null;
	private JButton btnConnect = null;
	private JButton btnDelete = null;
	private JPanel panelSimulation = null;
	private JPanel panelSimControls = null;


	private JButton btnSimStep = null;
	private JPanel panelModelSpecificControls = null;
	private JButton btnSimStart = null;
	private JButton btnSimStop = null;
	private JButton btnSimReset = null;

	private JMenuItem menuitemSave = null;
	private JMenuItem menuitemSaveAs = null;
	private String file_name = null;  //  @jve:decl-index=0:
	private String last_directory = null;
	private JMenuItem menuitemOpen = null;
	private JScrollPane scrollPropertyTable = null;
	private PropertyEditorTable tableProperties = null;
	private JToggleButton btnToggleIds = null;
	private JToggleButton btnToggleLabels = null;
	private JMenuItem jMenuItem = null;

	private JMenuItem mnuExportGraphics = null;
	private JMenuItem mnuExportPSGraphics = null;
	private JMenuItem mnuExportPDFGraphics = null;
	

	private JMenu mnuTools = null;

	private JMenu mnuEdit = null;

	private JMenuItem mnuCopy = null;

	private JMenuItem mnuPaste = null;

	private JMenu mnuExport = null;

	private JMenu mnuImport = null;

	private JMenu mnuTransform = null;

	/**
	 * This is the default constructor
	 */
	public JavaFrontend() {
		super();
		initialize();
	}
	
	/**
	 * This method initializes mnuEdit	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getMnuEdit() {
		if (mnuEdit == null) {
			mnuEdit = new JMenu();
			mnuEdit.setText("Edit");
			mnuEdit.setMnemonic(KeyEvent.VK_E);
			mnuEdit.add(getMnuCopy());
			mnuEdit.add(getMnuPaste());
		}
		return mnuEdit;
	}

	/**
	 * This method initializes mnuCopy	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getMnuCopy() {
		if (mnuCopy == null) {
			mnuCopy = new JMenuItem();
			mnuCopy.setText("Copy");
			mnuCopy.setMnemonic(KeyEvent.VK_C);
			mnuCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));			

			mnuCopy.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					copy();
				}
			});
		}
		return mnuCopy;
	}

	/**
	 * This method initializes mnuPaste	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getMnuPaste() {
		if (mnuPaste == null) {
			mnuPaste = new JMenuItem();
			mnuPaste.setText("Paste");
			mnuPaste.setMnemonic(KeyEvent.VK_P);
			mnuPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));			

			mnuPaste.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					paste();
				}
			});
		}
		return mnuPaste;
	}

	private JMenuItem getMnuExportPSGraphics() {
		if (mnuExportPSGraphics == null) {
			mnuExportPSGraphics = new JMenuItem();
			mnuExportPSGraphics.setText("Encapsulated PostScript...");
			mnuExportPSGraphics.setActionCommand("Encapsulated PostScript...");
			mnuExportPSGraphics.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					exportPSGraphics();
				}
			});
		}
		return mnuExportPSGraphics;
	}

	/**
	 * This method initializes mnuExportPDFGraphics	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getMnuExportPDFGraphics() {
		if (mnuExportPDFGraphics == null) {
			mnuExportPDFGraphics = new JMenuItem();
			mnuExportPDFGraphics.setActionCommand("PDF (using ps2pdf)...");
			mnuExportPDFGraphics.setText("PDF (using ps2pdf)...");
			mnuExportPDFGraphics.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					exportPDFGraphics();
				}
			});
		}
		return mnuExportPDFGraphics;
	}
	
	/**
	 * This method initializes mnuExport	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getMnuExport() {
		if (mnuExport == null) {
			mnuExport = new JMenu();
			mnuExport.setText("Export");
			mnuExport.add(getMnuExportGraphics());
			mnuExport.add(getMnuExportPSGraphics());
			mnuExport.add(getMnuExportPDFGraphics());
		}
		return mnuExport;
	}
	


	/**
	 * This method initializes mnuImport	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getMnuImport() {
		if (mnuImport == null) {
			mnuImport = new JMenu();
			mnuImport.setText("Import");
			mnuImport.setEnabled(false);
		}
		return mnuImport;
	}

	/**
	 * This method initializes mnuConversion	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getMnuConversion() {
		if (mnuTransform == null) {
			mnuTransform = new JMenu();
			mnuTransform.setText("Transform");
			mnuTransform.setEnabled(false);
		}
		return mnuTransform;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JDialog.setDefaultLookAndFeelDecorated(true);
				JavaFrontend thisClass = new JavaFrontend();
				thisClass.setVisible(true);				
			}
		});
	}


	public void beginSimulation() {
		Model document = editorView.getDocument();

		if ( document == null)
			return;

		try {
			document.validate();

		} catch (ModelValidationException e) {
			String errlist = "";
			for (String err : e.getErrors()) {
				errlist += err + "\n";
			}
			JOptionPane.showMessageDialog(null, errlist, "Cannot begin simulation: model validation failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		btnSimStart.setEnabled(false);
		btnSimStop.setEnabled(true);
		btnSimStep.setEnabled(true);
		btnSimReset.setEnabled(false);
		editorView.deselect();

		preSimState = document.saveState();
		document.simBegin();
	}
	public boolean checkChanges() {
		if (editorView.hasChanged()) {
			switch (JOptionPane.showConfirmDialog(this, "There are unsaved changes. Do you want to save the document before closing?", "Warning", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)) {
			case JOptionPane.YES_OPTION:
				save();
				return true;
			case JOptionPane.NO_OPTION:
				return true;
			case JOptionPane.CANCEL_OPTION:
				return false;
			}
		}
		return true;
	}

	/*
	public int getBaseAnimationLengthMs() {
		return slAnimSpeed.getValue();
	}

	public boolean areAnimationsEnabled() {
		return chkEnableAnimation.isSelected();
	}
	 */
	public void clearObject() {
		PropertyEditorTableModel m = (PropertyEditorTableModel)tableProperties.getModel();
		m.clearObject();
	}
	private void newDocument() {
		if (!checkChanges())
			return;

		JFENewDialog dialog = new JFENewDialog(this);
		JList l = dialog.getModelList();

		LinkedList<Class> models = server.mmgr.getModelList();
		DefaultListModel list = (DefaultListModel)l.getModel(); 

		l.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		LinkedList<String> modelNames = new LinkedList<String>();
		Hashtable<String, Class> modelClasses = new Hashtable<String, Class>();

		for (Class cls : models) {
			modelNames.add(server.mmgr.getModelDisplayName(cls));
			modelClasses.put(server.mmgr.getModelDisplayName(cls), cls);
		}

		Collections.sort(modelNames);

		for (String m: modelNames) {
			list.addElement(new ModelWrapper(modelClasses.get(m)));
		}
		l.setModel(list);
		dialog.setVisible(true);

		if (dialog.modalResult==1) {
			try {
				setDocumentUI((Model)dialog.choice.getModelClass().newInstance());
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			file_name = null;
		}
		editorView.requestFocus();

		dialog.dispose();
		updateTitle();
	}


	public void open() 
	{
		if (!checkChanges())
			return;

		JFileChooser fc = new JFileChooser();
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		fc.setFileFilter(new XwdFileFilter());
		if (fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
			return;

		Model doc;
		try {
			doc = load(fc.getSelectedFile().getAbsolutePath());
			setDocumentUI(doc);

			file_name = fc.getSelectedFile().getPath();
			updateTitle();


		} catch (DocumentOpenException e) {
			e.printStackTrace();
		}

		editorView.resetChanged();
		editorView.repaint();
		editorView.requestFocus();

		btnToggleSnap.setSelected(editorView.snap_to_grid);
		btnToggleGrid.setSelected(editorView.draw_grid);
		btnToggleIds.setSelected(editorView.show_ids);
		btnToggleLabels.setSelected(editorView.show_labels);

		//TODO: PACHINIT' load editor properties nah

		last_directory = fc.getSelectedFile().getParent();
	}

	public void save() {
		if (file_name == null) {
			saveAs();			
		} else
			doDocumentSave(file_name);
	}
	public void saveAs() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new XwdFileFilter());
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		if (fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getPath();
			if (!path.endsWith(".xwd"))
				path += ".xwd";
			doDocumentSave(path);	 
		}
	}
	private void doDocumentSave(String file_name) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc; DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			System.err.println(e.getMessage());
			return;
		}

		Element root = doc.createElement("workcraft");
		doc.appendChild(root);
		root = doc.getDocumentElement();

		Element options = doc.createElement("options");
		options.setAttribute("tab", Integer.toString(panelBottomTabs.getSelectedIndex()));

		root.appendChild(options);

		editorView.toXmlDom(root);
		editorView.documentToXml(root);

		try
		{
			TransformerFactory tFactory = TransformerFactory.newInstance();
			tFactory.setAttribute("indent-number", new Integer(2));
			Transformer transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			FileOutputStream fos = new FileOutputStream(file_name); 

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(new OutputStreamWriter(fos));

			//			t.transform(new DOMSource(doc),
			//				new StreamResult(new OutputStreamWriter(out, "utf-8"));

			transformer.transform(source, result);
			fos.close();

			this.file_name = file_name;
			editorView.resetChanged();
			updateTitle();
		} catch (TransformerException e) {
			System.err.println(e.getMessage());			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

		File f = new File(file_name);
		last_directory = f.getParent(); 
	}
	public void exit() {
		if (!checkChanges())
			return;
		savePreferences();
		System.exit(0);
	}
	protected void exportGraphics() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new SvgFileFilter());
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		if (fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getPath();
			if (!path.endsWith(".svg"))
				path += ".svg";

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			Document doc; DocumentBuilder db;
			try {
				db = dbf.newDocumentBuilder();
				doc = db.newDocument();
			} catch (ParserConfigurationException e) {
				System.err.println(e.getMessage());
				return;
			}

			Element root = doc.createElement("svg");
			doc.appendChild(root);

			BoundingBox bb = editorView.getBoundingBox();
			Vec2 ll = bb.getLowerLeft();
			Vec2 ur = bb.getUpperRight();

			float width = (ur.getX()-ll.getX());
			float height = (ur.getY()-ll.getY());

			root.setAttribute("width", ""+width*100.0f+"mm");
			root.setAttribute("height", ""+height*100.0f+"mm");
			root.setAttribute("viewBox", String.format("%f %f %f %f", ll.getX(), -ll.getY()-height, width, height));

			SVGPainter svgp = new SVGPainter(root);
			editorView.overridePainter(svgp);

			svgp.scale(1.0f, -1.0f);
			svgp.setRootTransform();

			editorView.setGridRange(ll, ur);
			editorView.draw();
			editorView.restorePainter();

			try
			{
				TransformerFactory tFactory = TransformerFactory.newInstance();
				Transformer transformer = tFactory.newTransformer();

				FileOutputStream fos = new FileOutputStream(path); 

				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(fos);

				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.transform(source, result);
				fos.close();

			} catch (TransformerException e) {
				System.err.println(e.getMessage());			
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}

			File f = new File(path);
			last_directory = f.getParent(); 			
		}
	}

	protected void exportPSGraphics() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new PsFileFilter());
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		if (fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getPath();
			if (!path.endsWith(".eps"))
				path += ".eps";

			BoundingBox bb = editorView.getBoundingBox();
			Vec2 ll = bb.getLowerLeft();
			Vec2 ur = bb.getUpperRight();

			ll.setXY(ll.getX()-0.3f, ll.getY()-0.3f);
			ur.setXY(ur.getX()+0.3f, ur.getY()+0.3f);

			PSPainter psp;
			PrintWriter out;
			try {
				out = new PrintWriter(new File(path));

				out.println ("%!PS-Adobe-3.0 EPSF-3.0");
				out.println ("%%Creator: Workcraft rev.1");
				out.println ("%%DocumentMedia: Plain "+ String.format("%d %d", (int)( (ur.getX() - ll.getX() )*283), (int)((ur.getY() - ll.getY())*283)) + " 0 ( ) ( )");
				out.println ("%%BoundingBox: " + String.format("%d %d %d %d", 0, 0, (int)((ur.getX() - ll.getX())*283), (int)( (ur.getY() - ll.getY())*283)));
				out.println ("%%HiResBoundingBox: " + String.format("%f %f %f %f", 0.0f, 0.0f, ((ur.getX() - ll.getX())*283.0f), ((ur.getY() - ll.getY())*283.0f)));
				out.println ("%%LanguageLevel: 2\n");


				psp = new PSPainter(out, 283.0f, -ll.getX(), -ll.getY());

				editorView.overridePainter(psp);

				// psp.scale(1.0f, -1.0f);
				//svgp.setRootTransform();

				boolean grid_restore = 	editorView.draw_grid;
				editorView.draw_grid = false;
				// editorView.setGridRange(ll, ur);
				editorView.draw();
				editorView.draw_grid = grid_restore;
				editorView.restorePainter();

				out.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	protected void exportPDFGraphics() {
		JFileChooser fc = new JFileChooser();
		fc.setFileFilter(new PdfFileFilter());
		if (last_directory != null)
			fc.setCurrentDirectory(new File(last_directory));
		if (fc.showSaveDialog(this)==JFileChooser.APPROVE_OPTION) {
			String path = fc.getSelectedFile().getPath();
			if (!path.endsWith(".pdf"))
				path += ".pdf";

			String tmppspath = path+".eps";

			BoundingBox bb = editorView.getBoundingBox();
			Vec2 ll = bb.getLowerLeft();
			Vec2 ur = bb.getUpperRight();

			ll.setXY(ll.getX()-0.3f, ll.getY()-0.3f);
			ur.setXY(ur.getX()+0.3f, ur.getY()+0.3f);

			PSPainter psp;
			PrintWriter out;
			try {
				out = new PrintWriter(new File(tmppspath));

				out.println ("%!PS-Adobe-3.0 EPSF-3.0");
				out.println ("%%Creator: Workcraft rev.1");
				out.println ("%%DocumentMedia: Plain "+ String.format("%d %d", (int)( (ur.getX() - ll.getX() )*283), (int)((ur.getY() - ll.getY())*283)) + " 0 ( ) ( )");
				out.println ("%%BoundingBox: " + String.format("%d %d %d %d", 0, 0, (int)((ur.getX() - ll.getX())*283), (int)( (ur.getY() - ll.getY())*283)));
				out.println ("%%HiResBoundingBox: " + String.format("%f %f %f %f", 0.0f, 0.0f, ((ur.getX() - ll.getX())*283.0f), ((ur.getY() - ll.getY())*283.0f)));
				out.println ("%%LanguageLevel: 2\n");


				psp = new PSPainter(out, 283.0f, -ll.getX(), -ll.getY());

				editorView.overridePainter(psp);

				// psp.scale(1.0f, -1.0f);
				//svgp.setRootTransform();

				boolean grid_restore = 	editorView.draw_grid;
				editorView.draw_grid = false;
				// editorView.setGridRange(ll, ur);
				editorView.draw();
				editorView.draw_grid = grid_restore;
				editorView.restorePainter();

				out.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				return;
			}

			ExternalProcess ps2pdf = new ExternalProcess(this);
			try {
				ps2pdf.run(new String[] { "ps2pdf", "-dEPSCrop", "-dEmbedAllFonts", tmppspath, path}, "./tmp", "ps2pdf", false);
				new File(tmppspath).delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This method initializes btnConnect	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnConnect() {
		if (btnConnect == null) {
			btnConnect = new JButton();
			btnConnect.setText("Connect");
			btnConnect.setPreferredSize(new Dimension(100, 20));
			btnConnect.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					editorView.beginConnection();
				}
			});
		}
		return btnConnect;
	}

	/**
	 * This method initializes btnDelete	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnDelete() {
		if (btnDelete == null) {
			btnDelete = new JButton();
			btnDelete.setPreferredSize(new Dimension(100, 20));
			btnDelete.setText("Delete");
			btnDelete.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					editorView.delete();
				}
			});
		}
		return btnDelete;
	}

	/**
	 * This method initializes btnExecute	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnExecute() {
		if (btnExecute == null) {
			btnExecute = new JButton();
			btnExecute.setText("Execute");
			btnExecute.setPreferredSize(new Dimension(80, 20));
			btnExecute.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (textCommandLine.getText() != ""){
						server.execPython(textCommandLine.getText());
						getEditorView().repaint();
					}
					textCommandLine.setText("");
				}
			});
		}
		return btnExecute;
	}

	/**
	 * This method initializes btnSimReset	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnSimReset() {
		if (btnSimReset == null) {
			btnSimReset = new JButton();
			btnSimReset.setText("Reset");
			btnSimReset.setEnabled(false);
			btnSimReset.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					editorView.getDocument().restoreState(preSimState);
					btnSimReset.setEnabled(false);
					editorView.repaint();
				}
			});
		}
		return btnSimReset;
	}

	/**
	 * This method initializes btnSimStart	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnSimStart() {
		if (btnSimStart == null) {
			btnSimStart = new JButton();
			btnSimStart.setText("Start");
			btnSimStart.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					beginSimulation();
				}
			});
		}
		return btnSimStart;
	}

	/**
	 * This method initializes btnSimStep	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnSimStep() {

		if (btnSimStep == null) {
			btnSimStep = new JButton();
			btnSimStep.setText("Step");
			btnSimStep.setEnabled(false);
			btnSimStep.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					stepSimulation();
				}
			});
		}
		return btnSimStep;
	}

	/**
	 * This method initializes btnSimStop	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnSimStop() {
		if (btnSimStop == null) {
			btnSimStop = new JButton();
			btnSimStop.setText("Stop");
			btnSimStop.setEnabled(false);
			btnSimStop.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					stopSimulation();
				}
			});
		}
		return btnSimStop;
	}

	/**
	 * This method initializes btnToggleGrid	
	 * 	
	 * @return javax.swing.JToggleButton	
	 */
	private JToggleButton getBtnToggleGrid() {
		if (btnToggleGrid == null) {
			btnToggleGrid = new JToggleButton();
			btnToggleGrid.setText("Grid");
			btnToggleGrid.setSelected(true);
			btnToggleGrid.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					getEditorView().draw_grid = !getEditorView().draw_grid;
					getEditorView().repaint();
				}
			});
		}
		return btnToggleGrid;
	}

	/**
	 * This method initializes btnToggleIds	
	 * 	
	 * @return javax.swing.JToggleButton	
	 */
	private JToggleButton getBtnToggleIds() {
		if (btnToggleIds == null) {
			btnToggleIds = new JToggleButton();
			btnToggleIds.setText("IDs");
			btnToggleIds.setSelected(true);
			btnToggleIds.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					getEditorView().show_ids = !getEditorView().show_ids;
					server.python.set("_draw_ids", getEditorView().show_ids);
					getEditorView().repaint();
				}
			});
		}
		return btnToggleIds;
	}

	/**
	 * This method initializes btnToggleLabels	
	 * 	
	 * @return javax.swing.JToggleButton	
	 */
	private JToggleButton getBtnToggleLabels() {
		if (btnToggleLabels == null) {
			btnToggleLabels = new JToggleButton();
			btnToggleLabels.setText("Labels");
			btnToggleLabels.setSelected(true);
			btnToggleLabels.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					getEditorView().show_labels = !getEditorView().show_labels;
					server.python.set("_draw_labels", getEditorView().show_labels);
					getEditorView().repaint();

				}
			});
		}
		return btnToggleLabels;
	}

	/**
	 * This method initializes btnToggleSnap	
	 * 	
	 * @return javax.swing.JToggleButton	
	 */
	private JToggleButton getBtnToggleSnap() {
		if (btnToggleSnap == null) {
			btnToggleSnap = new JToggleButton();
			btnToggleSnap.setHorizontalTextPosition(SwingConstants.TRAILING);
			btnToggleSnap.setMnemonic(KeyEvent.VK_UNDEFINED);
			btnToggleSnap.setHorizontalAlignment(SwingConstants.CENTER);
			btnToggleSnap.setText("Snap");
			btnToggleSnap.setSelected(true);
			btnToggleSnap.setPreferredSize(new Dimension(20, 20));
			btnToggleSnap.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					getEditorView().snap_to_grid = !getEditorView().snap_to_grid;
					getEditorView().repaint();
				}
			});
		}
		return btnToggleSnap;
	}

	/**
	 * This method initializes componentTree	
	 * 	
	 * @return javax.swing.JTree	
	 */
	private JTree getComponentTree() {
		if (componentTree == null) {
			componentTree = new JTree();
		}
		return componentTree;
	}

	private EditorPane getEditorView() {
		if (editorView == null ) {
			//			GLCapabilities caps = new GLCapabilities();
			//		caps.setSampleBuffers(true);
			//	caps.setNumSamples(2);

			editorView = new EditorPane();
			editorView.setMinimumSize(new Dimension(0, 0));
			editorView.setBackground(Color.white);
		}
		return editorView;
	}

	public String getFileName() {
		return file_name;
	}


	private JFrame getFrame() {
		return this;
	}


	public String getLastDirectory() {
		return last_directory;
	}

	/**
	 * This method initializes menubarMain	
	 * 	
	 * @return javax.swing.JMenuBar	
	 */
	private JMenuBar getMenubarMain() {
		if (menubarMain == null) {
			menubarMain = new JMenuBar();
			menubarMain.add(getMenuFile());
			menubarMain.add(getMnuEdit());
			menubarMain.add(getMnuTools());
			menubarMain.add(getMnuConversion());
		}
		return menubarMain;
	}

	/**
	 * This method initializes menuFile	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getMenuFile() {
		if (menuFile == null) {
			menuFile = new JMenu();
			menuFile.setText("File");
			menuFile.setMnemonic(KeyEvent.VK_F);
			menuFile.add(getMenuitemNew());
			menuFile.add(getMenuitemOpen());
			menuFile.add(getMenuitemSave());
			menuFile.add(getMenuitemSaveAs());
			menuFile.addSeparator();
			menuFile.add(getMnuExport());
			menuFile.add(getMnuImport());
			menuFile.addSeparator();
			menuFile.add(getMenuitemExit());
		}
		return menuFile;
	}	

	/**
	 * This method initializes menuitemExit	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getMenuitemExit() {
		if (menuitemExit == null) {
			menuitemExit = new JMenuItem();
			menuitemExit.setText("Exit");
			menuitemExit.setMnemonic(KeyEvent.VK_X);
			menuitemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
			menuitemExit.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					exit();
				}
			});
		}
		return menuitemExit;
	}


	/**
	 * This method initializes menuitemNew	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getMenuitemNew() {
		if (menuitemNew == null) {
			menuitemNew = new JMenuItem();
			menuitemNew.setText("New...");
			menuitemNew.setMnemonic(KeyEvent.VK_N);
			menuitemNew.setActionCommand("New ...");
			menuitemNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
			menuitemNew.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					newDocument();
				}
			});
		}
		return menuitemNew;
	}


	/**
	 * This method initializes menuitemOpen	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getMenuitemOpen() {
		if (menuitemOpen == null) {
			menuitemOpen = new JMenuItem();
			menuitemOpen.setText("Open...");
			menuitemOpen.setMnemonic(KeyEvent.VK_O);
			menuitemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
			menuitemOpen.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					open();
				}
			});
		}
		return menuitemOpen;
	}

	/**
	 * This method initializes menuitemSave	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getMenuitemSave() {
		if (menuitemSave == null) {
			menuitemSave = new JMenuItem();
			menuitemSave.setText("Save");
			menuitemSave.setMnemonic(KeyEvent.VK_S);			
			menuitemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));			
			menuitemSave.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					save();
				}
			});
		}
		return menuitemSave;
	}

	/**
	 * This method initializes menuitemSaveAs	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getMenuitemSaveAs() {
		if (menuitemSaveAs == null) {
			menuitemSaveAs = new JMenuItem();
			menuitemSaveAs.setText("Save as...");
			menuitemSaveAs.setMnemonic(KeyEvent.VK_A);
			menuitemSaveAs.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					saveAs();
				}
			});
		}
		return menuitemSaveAs;
	}

	/**
	 * This method initializes mnuExportGraphics	
	 * 	
	 * @return javax.swing.JMenuItem	
	 */
	private JMenuItem getMnuExportGraphics() {
		if (mnuExportGraphics == null) {
			mnuExportGraphics = new JMenuItem();
			mnuExportGraphics.setText("Scalable Vector Graphics (.svg)...");
			mnuExportGraphics.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					exportGraphics();
				}
			});
		}
		return mnuExportGraphics;
	}

	/**
	 * This method initializes mnuTools	
	 * 	
	 * @return javax.swing.JMenu	
	 */
	private JMenu getMnuTools() {
		if (mnuTools == null) {
			mnuTools = new JMenu();
			mnuTools.setText("Tools");
			// mnuTools.setAccelerator(buba);
			mnuTools.setEnabled(false);
		}
		return mnuTools;
	}

	/**
	 * This method initializes panelBottomTabs	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
	private JTabbedPane getPanelBottomTabs() {
		if (panelBottomTabs == null) {
			panelBottomTabs = new JTabbedPane();
			panelBottomTabs.addTab("Console", null, getPanelConsole(), null);
			panelBottomTabs.addTab("Problems", null, getScrollErrorOutput(), null);
			panelBottomTabs.addTab("Simulation", null, getPanelSimulation(), null);
			panelBottomTabs.addChangeListener(new javax.swing.event.ChangeListener() {
				public void stateChanged(javax.swing.event.ChangeEvent e) {
					if (panelBottomTabs.getSelectedIndex()==1) {
						panelBottomTabs.setForegroundAt(1, Color.BLACK);
					}
				}
			});
		}
		return panelBottomTabs;
	}

	/**
	 * This method initializes panelConsole	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelConsole() {
		if (panelConsole == null) {
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.fill = GridBagConstraints.BOTH;
			gridBagConstraints2.gridy = 0;
			gridBagConstraints2.weightx = 1.0;
			gridBagConstraints2.weighty = 1.0;
			gridBagConstraints2.gridwidth = 2;
			gridBagConstraints2.gridheight = 1;
			gridBagConstraints2.gridx = 0;
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 1;
			gridBagConstraints11.fill = GridBagConstraints.BOTH;
			gridBagConstraints11.anchor = GridBagConstraints.EAST;
			gridBagConstraints11.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints11.gridy = 1;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = GridBagConstraints.BOTH;
			gridBagConstraints1.gridy = 1;
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.anchor = GridBagConstraints.WEST;
			gridBagConstraints1.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints1.gridx = 0;
			panelConsole = new JPanel();
			panelConsole.setLayout(new GridBagLayout());
			panelConsole.add(getTextCommandLine(), gridBagConstraints1);
			panelConsole.add(getBtnExecute(), gridBagConstraints11);
			panelConsole.add(getScrollConsoleOutput(), gridBagConstraints2);
		}
		return panelConsole;
	}

	/**
	 * This method initializes panelEditor	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelEditor() {
		if (panelEditor == null) {
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.anchor = GridBagConstraints.SOUTH;
			gridBagConstraints3.weighty = 1.0;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.gridheight = 2;
			gridBagConstraints3.gridwidth = 1;
			gridBagConstraints3.fill = GridBagConstraints.BOTH;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 0.0;
			gridBagConstraints.gridwidth = 0;
			gridBagConstraints.gridx = 0;
			panelEditor = new JPanel();
			panelEditor.setLayout(new GridBagLayout());
			panelEditor.add(getScrollEditorToolbar(), gridBagConstraints);
			panelEditor.add(getPanelEditorView(), gridBagConstraints3);
		}
		return panelEditor;
	}

	/**
	 * This method initializes panelEditorCommands	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelEditorCommands() {
		if (panelEditorCommands == null) {
			FlowLayout flowLayout1 = new FlowLayout();
			flowLayout1.setAlignment(java.awt.FlowLayout.LEFT);
			panelEditorCommands = new JPanel();
			panelEditorCommands.setPreferredSize(new Dimension(120, 300));
			panelEditorCommands.setLayout(flowLayout1);
			panelEditorCommands.add(getBtnConnect(), null);
			panelEditorCommands.add(getBtnDelete(), null);
		}
		return panelEditorCommands;
	}

	/**
	 * This method initializes panelEditorToolbar	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelEditorToolbar() {
		if (panelEditorToolbar == null) {
			panelEditorToolbar = new JPanel();
			panelEditorToolbar.setLayout(new BoxLayout(getPanelEditorToolbar(), BoxLayout.X_AXIS));
			panelEditorToolbar.setPreferredSize(new Dimension(20, 20));
			panelEditorToolbar.setSize(new Dimension(188, 20));
			panelEditorToolbar.add(getBtnToggleSnap(), null);
			panelEditorToolbar.add(getBtnToggleGrid(), null);
			panelEditorToolbar.add(getBtnToggleIds(), null);
			panelEditorToolbar.add(getBtnToggleLabels(), null);
		}
		return panelEditorToolbar;
	}

	/**
	 * This method initializes panelEditorView	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelEditorView() {
		if (panelEditorView == null) {
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.fill = GridBagConstraints.BOTH;
			gridBagConstraints4.gridx = 0;
			gridBagConstraints4.gridy = 1;
			gridBagConstraints4.weightx = 1.0;
			gridBagConstraints4.weighty = 1.0;
			gridBagConstraints4.gridheight = -1;
			panelEditorView = new JPanel();
			panelEditorView.setLayout(new GridBagLayout());
			panelEditorView.setBackground(new Color(106, 143, 9));
			panelEditorView.setPreferredSize(new Dimension(0, 0));
			panelEditorView.add(getEditorView(), gridBagConstraints4);
		}
		return panelEditorView;
	}

	/**
	 * This method initializes panelModelSpecificControls	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelModelSpecificControls() {
		if (panelModelSpecificControls == null) {
			TitledBorder titledBorder = BorderFactory.createTitledBorder(null, "Animation controls", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51));
			titledBorder.setTitle("Simulation controls");
			panelModelSpecificControls = new JPanel();
			panelModelSpecificControls.setLayout(new BorderLayout());
			panelModelSpecificControls.setBorder(titledBorder);
		}
		return panelModelSpecificControls;
	}

	/**
	 * This method initializes panelSimControls	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelSimControls() {
		if (panelSimControls == null) {
			FlowLayout flowLayout = new FlowLayout();
			flowLayout.setAlignment(java.awt.FlowLayout.LEFT);
			panelSimControls = new JPanel();
			panelSimControls.setLayout(flowLayout);
			panelSimControls.add(getBtnSimStep(), null);
			panelSimControls.add(getBtnSimStart(), null);
			panelSimControls.add(getBtnSimStop(), null);
			panelSimControls.add(getBtnSimReset(), null);
		}
		return panelSimControls;
	}

	/**
	 * This method initializes panelSimulation	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getPanelSimulation() {
		if (panelSimulation == null) {
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 0;
			gridBagConstraints6.fill = GridBagConstraints.BOTH;
			gridBagConstraints6.weightx = 1.0;
			gridBagConstraints6.weighty = 1.0;
			gridBagConstraints6.anchor = GridBagConstraints.NORTHWEST;
			gridBagConstraints6.gridy = 1;
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 0;
			gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints5.weightx = 1.0;
			gridBagConstraints5.weighty = 0.0;
			gridBagConstraints5.anchor = GridBagConstraints.NORTHWEST;
			gridBagConstraints5.gridy = 0;
			panelSimulation = new JPanel();
			panelSimulation.setLayout(new GridBagLayout());
			panelSimulation.add(getPanelSimControls(), gridBagConstraints5);
			panelSimulation.add(getPanelModelSpecificControls(), gridBagConstraints6);
		}
		return panelSimulation;
	}

	/**
	 * This method initializes scrollComponentTree	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getScrollComponentTree() {
		if (scrollComponentTree == null) {
			scrollComponentTree = new JScrollPane();
			scrollComponentTree.setViewportView(getComponentTree());
		}
		return scrollComponentTree;
	}

	/**
	 * This method initializes scrollConsoleOutput	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getScrollConsoleOutput() {
		if (scrollConsoleOutput == null) {
			scrollConsoleOutput = new JScrollPane();
			scrollConsoleOutput.setViewportView(getTextConsoleOutput());
		}
		return scrollConsoleOutput;
	}

	/**
	 * This method initializes scrollEditorToolbar	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getScrollEditorToolbar() {
		if (scrollEditorToolbar == null) {
			scrollEditorToolbar = new JScrollPane();
			scrollEditorToolbar.setPreferredSize(new Dimension(3, 30));
			scrollEditorToolbar.setViewportView(getPanelEditorToolbar());
		}
		return scrollEditorToolbar;
	}

	/**
	 * This method initializes scrollErrorOutput	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getScrollErrorOutput() {
		if (scrollErrorOutput == null) {
			scrollErrorOutput = new JScrollPane();
			scrollErrorOutput.setViewportView(getTextErrorOutput());
		}
		return scrollErrorOutput;
	}

	/**
	 * This method initializes scrollPropertyTable	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getScrollPropertyTable() {
		if (scrollPropertyTable == null) {
			scrollPropertyTable = new JScrollPane();
			scrollPropertyTable.setViewportView(getTableProperties());
		}
		return scrollPropertyTable;
	}	


	/**
	 * This method initializes splitCentral	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getSplitCentral() {
		if (splitCentral == null) {
			splitCentral = new JSplitPane();
			splitCentral.setOrientation(JSplitPane.VERTICAL_SPLIT);
			splitCentral.setResizeWeight(1.0D);
			splitCentral.setPreferredSize(new Dimension(0, 0));
			splitCentral.setDividerLocation(-1);
			splitCentral.setOneTouchExpandable(false);
			splitCentral.setContinuousLayout(true);
			splitCentral.setBottomComponent(getPanelBottomTabs());
			splitCentral.setTopComponent(getPanelEditor());
			splitCentral.setDividerSize(4);
		}
		return splitCentral;
	}

	/**
	 * This method initializes splitLeft	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getSplitLeft() {
		if (splitLeft == null) {
			splitLeft = new JSplitPane();
			splitLeft.setOrientation(JSplitPane.VERTICAL_SPLIT);
			splitLeft.setPreferredSize(new Dimension(150, 0));
			splitLeft.setResizeWeight(1.0D);
			splitLeft.setDividerLocation(150);
			splitLeft.setBottomComponent(getScrollComponentTree());
			splitLeft.setTopComponent(getPanelEditorCommands());
			splitLeft.setDividerSize(2);
		}
		return splitLeft;
	}

	/**
	 * This method initializes splitMain	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getSplitMain() {
		if (splitMain == null) {
			splitMain = new JSplitPane();
			splitMain.setDividerSize(4);
			splitMain.setLeftComponent(getSplitLeft());
			splitMain.setRightComponent(getSplitRight());
		}
		return splitMain;
	}

	/**
	 * This method initializes splitRight	
	 * 	
	 * @return javax.swing.JSplitPane	
	 */
	private JSplitPane getSplitRight() {
		if (splitRight == null) {
			splitRight = new JSplitPane();
			splitRight.setDividerSize(2);
			splitRight.setResizeWeight(1.0D);
			splitRight.setPreferredSize(new Dimension(0, 0));
			splitRight.setDividerLocation(-1);
			splitRight.setRightComponent(getScrollPropertyTable());
			splitRight.setLeftComponent(getSplitCentral());
		}
		return splitRight;
	}

	/**
	 * This method initializes tableProperties	
	 * 	
	 * @return javax.swing.JTable	
	 */
	private PropertyEditorTable getTableProperties() {
		if (tableProperties == null) {
			PropertyEditorTableModel petm = new PropertyEditorTableModel();
			petm.addTableModelListener(this);
			tableProperties = new PropertyEditorTable(petm);
			tableProperties.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			tableProperties.setRowSelectionAllowed(false);
			tableProperties.setDefaultRenderer(Color.class, new ColorCellRenderer(true));
		}
		return tableProperties;
	}

	/**
	 * This method initializes textCommandLine	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getTextCommandLine() {
		if (textCommandLine == null) {
			textCommandLine = new JTextField();
			textCommandLine.addKeyListener(new java.awt.event.KeyAdapter() {
				public void keyPressed(java.awt.event.KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_ENTER)
					{
						if (textCommandLine.getText() != ""){
							server.execPython(textCommandLine.getText());
							getEditorView().repaint();
						}
						textCommandLine.setText("");
					}
				}
			});
		}
		return textCommandLine;
	}

	/**
	 * This method initializes textConsoleOutput	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getTextConsoleOutput() {
		if (textConsoleOutput == null) {
			textConsoleOutput = new JTextArea();
		}
		return textConsoleOutput;
	}

	/**
	 * This method initializes textErrorOutput	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getTextErrorOutput() {
		if (textErrorOutput == null) {
			textErrorOutput = new JTextArea();
			textErrorOutput.setForeground(Color.red);
		}
		return textErrorOutput;
	}

	private void copy() {
		Clipboard cb = getToolkit().getSystemClipboard();

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc; DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			return;
		}

		Element root = doc.createElement("workcraft-document-fragment");
		doc.appendChild(root);
		root = doc.getDocumentElement();
		editorView.selectionToXml(root);

		cb.setContents(new TransferableDocumentFragment(doc), this);
	}

	private void paste() {
		Document doc; 

		try {
			doc = (Document) getToolkit().getSystemClipboard().getData(TransferableDocumentFragment.DOCUMENT_FRAGMENT_FLAVOR);
		} catch (HeadlessException e) {
			e.printStackTrace();
			return;
		} catch (UnsupportedFlavorException e) {
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		editorView.pasteFromXml(doc);		
	}

	private void setDocumentUI(Model document) {
		stopSimulation();

		Class model_class = document.getClass();
		UUID model_uuid = ModelManager.getModelUUID(model_class);

		try {
			editorView.setDocument(document);
			LinkedList<Class> components =  server.mmgr.getComponentsByModelUUID(model_uuid);
			LinkedList<Class> tools = server.mmgr.getToolsByModelUUID(model_uuid);
			component_root.removeAllChildren();
			component_root.setUserObject(ModelManager.getModelDisplayName(model_class));

			editorView.deselect();
			editorView.hotKeyClear();

			int cnt = 1;

			if (components!=null)
				for (Class cls : components) {
					component_root.add(new DefaultMutableTreeNode(new ComponentWrapper(cls)));
					editorView.hotKeySetNum(cnt++, cls);

					int vk = ComponentHotkey.getComponentHotkeyVk(cls);
					if (vk>0)
						editorView.hotKeySetVk(vk, cls);
				}

			LinkedList<String> generalNames = new LinkedList<String>();
			Hashtable<String, JMenuItem> generalItems = new Hashtable<String, JMenuItem>();

			LinkedList<String> exportNames = new LinkedList<String>();
			Hashtable<String, JMenuItem> exportItems = new Hashtable<String, JMenuItem>();

			LinkedList<String> importNames = new LinkedList<String>();
			Hashtable<String, JMenuItem> importItems = new Hashtable<String, JMenuItem>();

			LinkedList<String> conversionNames = new LinkedList<String>();
			Hashtable<String, JMenuItem> conversionItems = new Hashtable<String, JMenuItem>();


			if (tools!=null) {
				for (Class cls : tools) {
					String displayName = ModelManager.getToolDisplayName(cls); 

					JMenuItem toolItem = new JMenuItem();
					toolItem.setText(displayName+"...");

					Tool tool = server.getToolInstance(cls);
					if (tool == null)
						tool =(Tool) cls.newInstance();
					tool.init(null);
					server.registerToolInstance(tool);

					mnu_tool_map.put(toolItem, tool);
					toolItem.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {
							mnu_tool_map.get((JMenuItem)e.getSource()).run(JavaFrontend.this, server);
						}
					});

					switch (tool.getToolType()) {
					case GENERAL:
						generalNames.add(displayName);
						generalItems.put(displayName, toolItem);
						break;
					case IMPORT:
						importNames.add(displayName);
						importItems.put(displayName, toolItem);
						break;
					case EXPORT:
						exportNames.add(displayName);
						exportItems.put(displayName, toolItem);
						break;
					case TRANSFORM:
						conversionNames.add(displayName);
						conversionItems.put(displayName, toolItem);
						break;
					}
				}
			}

			for (Tool tool : server.mmgr.getMultiModelTools()) {
				if (tool.isModelSupported(model_uuid)) {
					String displayName = ModelManager.getToolDisplayName(tool.getClass());

					JMenuItem toolItem = new JMenuItem();
					toolItem.setText(displayName+"...");
					mnu_tool_map.put(toolItem, tool);
					toolItem.addActionListener(new java.awt.event.ActionListener() {
						public void actionPerformed(java.awt.event.ActionEvent e) {
							mnu_tool_map.get((JMenuItem)e.getSource()).run(JavaFrontend.this, server);
						}
					});

					switch (tool.getToolType()) {
					case GENERAL:
						generalNames.add(displayName);
						generalItems.put(displayName, toolItem);
						break;
					case IMPORT:
						importNames.add(displayName);
						importItems.put(displayName, toolItem);
						break;
					case EXPORT:
						exportNames.add(displayName);
						exportItems.put(displayName, toolItem);
						break;
					case TRANSFORM:
						conversionNames.add(displayName);
						conversionItems.put(displayName, toolItem);
						break;
					}
				}
			}

			Collections.sort(generalNames);
			Collections.sort(importNames);
			Collections.sort(exportNames);
			Collections.sort(conversionNames);

			mnuTools.removeAll();
			mnuTools.setEnabled(!generalNames.isEmpty());
			for (String n : generalNames)
				mnuTools.add(generalItems.get(n));

			mnuTransform.removeAll();
			mnuTransform.setEnabled(!conversionNames.isEmpty());
			for (String n : conversionNames)
				mnuTransform.add(conversionItems.get(n));

			mnuImport.removeAll();
			mnuImport.setEnabled(!importNames.isEmpty());
			for (String n : importNames)
				mnuImport.add(importItems.get(n));

			mnuExport.removeAll();
			mnuExport.add(getMnuExportGraphics());
			mnuExport.add(getMnuExportPSGraphics());
			mnuExport.add(getMnuExportPDFGraphics());
			
			if (!exportNames.isEmpty())
				mnuExport.addSeparator();
			for (String n : exportNames)
				mnuExport.add(exportItems.get(n));


			panelModelSpecificControls.removeAll();
			JPanel simctl = document.getSimulationControls(); 
			if (simctl!=null)
				panelModelSpecificControls.add(simctl);
			panelModelSpecificControls.repaint();

			componentTree.setModel(new DefaultTreeModel(component_root));
			componentTree.expandRow(0);
		}
		catch (IllegalAccessException e)
		{
			System.err.println(e);
			return;
		}
		catch (InstantiationException e) {
			System.err.println(e);
			return;
		}

	}
	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(555, 331);
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		this.setJMenuBar(getMenubarMain());
		this.setContentPane(getSplitMain());
		this.setTitle("Workcraft");
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e) {
				exit();
			}
		});

		component_root = new DefaultMutableTreeNode("Components");
		this.getComponentTree().setModel(new DefaultTreeModel(component_root));
		new TreeDragSource(this.getComponentTree(), DnDConstants.ACTION_COPY_OR_MOVE);

		RedirectStreams();

		server = new WorkCraftServer();
		server.initialize();

		getEditorView().setPropertyEditor(this);
		getEditorView().setServer(server);

		setSize(Toolkit.getDefaultToolkit().getScreenSize());

		this.setExtendedState(MAXIMIZED_VERT);
		this.setExtendedState(MAXIMIZED_HORIZ);
		this.setExtendedState(MAXIMIZED_BOTH);

		server.python.set("_draw_labels", true);
		server.python.set("_draw_ids", true);
		server.python.set("_loading", false);
		server.python.set("_pasting", false);
		server.python.set("_main_frame", this);


		loadPreferences();

		updateTitle();

		Mat4x4 m = new Mat4x4();
		m.randomize();
		m.invert();
	}

	public void loadPreferences() {
		File f = new File("preferences.xml");
		if (!f.exists())
			return;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc; 
		DocumentBuilder db;

		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(f);
		} catch (ParserConfigurationException e) {
			System.err.println(e.getMessage());
			return;
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return;
		} catch (SAXException e) {
			System.err.println(e.getMessage());
			return;
		}

		Element root = doc.getDocumentElement();

		NodeList nl = root.getElementsByTagName("last-directory");
		if (nl.getLength()>0) {
			last_directory = ((Element) nl.item(0)).getAttribute("path");
		}

		nl =  root.getElementsByTagName("window-layout");
		if (nl.getLength()>0) {
			Element layout = (Element)nl.item(0);

			splitMain.setDividerLocation(Integer.parseInt(layout.getAttribute("div1")));
			splitLeft.setDividerLocation(Integer.parseInt(layout.getAttribute("div2")));
			splitRight.setDividerLocation(Integer.parseInt(layout.getAttribute("div3")));
			splitCentral.setDividerLocation(Integer.parseInt(layout.getAttribute("div4")));

			this.setLocation(Integer.parseInt(layout.getAttribute("window-x")), Integer.parseInt(layout.getAttribute("window-y")));
			this.setSize(Integer.parseInt(layout.getAttribute("window-w")), Integer.parseInt(layout.getAttribute("window-h")));
			this.setExtendedState(Integer.parseInt(layout.getAttribute("window-state")));

		}
	}

	public void RedirectStreams() {

		outPrintStream = new PrintStream(
				new ConsoleOutputStream(
						new ByteArrayOutputStream(), getTextConsoleOutput()));
		errPrintStream = new PrintStream(
				new ErrorOutputStream(
						new ByteArrayOutputStream(), getTextErrorOutput()));

		System.setOut(outPrintStream);
		System.setErr(errPrintStream);
	}

	public boolean redraw() {
		editorView.repaint();
		return false;
	}


	public void savePreferences() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc; DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.newDocument();
		} catch (ParserConfigurationException e) {
			System.err.println(e.getMessage());
			return;
		}

		Element root = doc.createElement("workcraft-preferences");
		doc.appendChild(root);

		Element lsd = doc.createElement("last-directory");
		lsd.setAttribute("path", last_directory);
		root.appendChild(lsd);

		Element layout = doc.createElement("window-layout");
		layout.setAttribute("div1", Integer.toString(splitMain.getDividerLocation()));
		layout.setAttribute("div2", Integer.toString(splitLeft.getDividerLocation()));
		layout.setAttribute("div3", Integer.toString(splitRight.getDividerLocation()));
		layout.setAttribute("div4", Integer.toString(splitCentral.getDividerLocation()));
		layout.setAttribute("window-state", Integer.toString(this.getExtendedState()));
		layout.setAttribute("window-x", Integer.toString(this.getX()));
		layout.setAttribute("window-y", Integer.toString(this.getY()));
		layout.setAttribute("window-w", Integer.toString(this.getWidth()));
		layout.setAttribute("window-h", Integer.toString(this.getHeight()));

		root.appendChild(layout);

		try
		{
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();

			FileOutputStream fos = new FileOutputStream("preferences.xml"); 

			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(fos);

			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(source, result);
			fos.close();
		} catch (TransformerException e) {
			System.err.println(e.getMessage());			
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}

	public void setDocument(Model document) {
		checkChanges();
		setDocumentUI(document);
		file_name = null;
		editorView.changed = true;
		updateTitle();
	}

	public void setObject(PropertyEditable o) {
		PropertyEditorTableModel m = (PropertyEditorTableModel)tableProperties.getModel();

		int cnt = 0;
		List<String> list = o.getEditableProperties();

		String propertyNames[] = new String[list.size()];
		String propertySetters[] = new String[list.size()];
		String propertyGetters[] = new String[list.size()];
		Class propertyClasses[] = new Class[list.size()];
		Object propertyData[] = new Object[list.size()];

		for (String s : list) {
			String def[] = s.split(",");
			if (def.length < 4)
				continue;
			if (def[0].equals("str")) {
				propertyClasses[cnt] = String.class;
				propertyNames[cnt] = def[1];
				propertyGetters[cnt] = def[2];
				if (def[3].equals("-"))
					propertySetters[cnt] = null;
				else
					propertySetters[cnt] = def[3];

				tableProperties.setRowClass(cnt, String.class, null);				
			}
			else if (def[0].equals("double")) {
				propertyClasses[cnt] = Double.class;
				propertyNames[cnt] = def[1];
				propertyGetters[cnt] = def[2];
				if (def[3].equals("-"))
					propertySetters[cnt] = null;
				else
					propertySetters[cnt] = def[3];
				tableProperties.setRowClass(cnt, Double.class, null);
			}
			else if (def[0].equals("int")) {
				propertyClasses[cnt] = Integer.class;
				propertyNames[cnt] = def[1];
				propertyGetters[cnt] = def[2];
				if (def[3].equals("-"))
					propertySetters[cnt] = null;
				else
					propertySetters[cnt] = def[3];
				tableProperties.setRowClass(cnt, Integer.class, null);
			} else if (def[0].equals("bool")) {
				propertyClasses[cnt] = Boolean.class;
				propertyNames[cnt] = def[1];
				propertyGetters[cnt] = def[2];
				if (def[3].equals("-"))
					propertySetters[cnt] = null;
				else
					propertySetters[cnt] = def[3];
				tableProperties.setRowClass(cnt, Boolean.class, null);
			}else if (def[0].equals("color")) {
				propertyClasses[cnt] = Colorf.class;
				propertyNames[cnt] = def[1];
				propertyGetters[cnt] = def[2];
				if (def[3].equals("-"))
					propertySetters[cnt] = null;
				else
					propertySetters[cnt] = def[3];
				tableProperties.setRowClass(cnt, Colorf.class, null);
			} else if (def[0].equals("enum")) {
				propertyClasses[cnt] = EnumWrapper.class;
				propertyNames[cnt] = def[1];
				propertyGetters[cnt] = def[2];
				if (def[3].equals("-"))
					propertySetters[cnt] = null;
				else
					propertySetters[cnt] = def[3];
				if (def.length < 5)
					System.err.println ("Property "+def[1]+" declared as enum, but lacks values enumeration - likely will crash now");					
				String[] names = new String[def.length-4];
				for (int j=4; j<def.length; j++)
					names[j-4] = def[j];
				tableProperties.setRowClass(cnt, EnumWrapper.class, names);
				propertyData[cnt] = names;
			} else if (def[0].equals("xwdfile")) {
				propertyClasses[cnt] = File.class;
				propertyNames[cnt] = def[1];
				propertyGetters[cnt] = def[2];
				if (def[3].equals("-"))
					propertySetters[cnt] = null;
				else
					propertySetters[cnt] = def[3];
				tableProperties.setRowClass(cnt, File.class, null);
			}
			else {
				continue;
			}
			cnt++;
		}

		m.setObject(o, propertyNames, propertyGetters, propertySetters, propertyClasses, propertyData);
	}

	public void stepSimulation() {
		Model document = editorView.getDocument();

		if (document == null)
			return;
		document.simStep();
		editorView.repaint();
	}

	public void stopSimulation() {
		Model document = editorView.getDocument();

		if (document==null)
			return;

		document.simFinish();

		btnSimStart.setEnabled(true);
		btnSimStep.setEnabled(false);

		if (preSimState != null)
			btnSimReset.setEnabled(true);
	}

	public void tableChanged(TableModelEvent e) {
		editorView.repaint();
	}

	public void updateTitle() {
		Model document = editorView.getDocument();

		String title = "";
		if (file_name!=null)
			title += file_name + " ";
		else
			if (document != null)
				title += "[unsaved]";

		if (document != null)
			title += " (" + ModelManager.getModelDisplayName(document.getClass()) + ")";
		else
			title += "No document";

		title += " - WorkCraft";

		setTitle(title);
	}

	public Model getDocument() {
		Model document = editorView.getDocument();
		return document;
	}

	public void lostOwnership(Clipboard arg0, Transferable arg1) {
		// TODO Auto-generated method stub

	}

	public void refresh() {
		editorView.repaint();


	}

	public Model load(String path) throws DocumentOpenException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmldoc;
		Model doc;
		DocumentBuilder db;

		try {
			db = dbf.newDocumentBuilder();
			xmldoc = db.parse(new File(path));
		} catch (ParserConfigurationException e) {
			throw new DocumentOpenException(e.getMessage());
		} catch (IOException e) {
			throw new DocumentOpenException(e.getMessage());
		} catch (SAXException e) {
			throw new DocumentOpenException(e.getMessage());
		}

		Element xmlroot = xmldoc.getDocumentElement();

		try {
			// TODO: proper validation
			if (xmlroot.getNodeName()!="workcraft")
				throw new DocumentOpenException("Invalid root element");
			NodeList nl;

			nl =  xmlroot.getElementsByTagName("options");
			Element options = (Element)nl.item(0);
			panelBottomTabs.setSelectedIndex(Integer.parseInt(options.getAttribute("tab")));

			nl = xmlroot.getElementsByTagName("editor");
			Element edit = (Element)nl.item(0);

			editorView.fromXmlDom(edit);

			nl = xmlroot.getElementsByTagName("document");
			Element d = (Element)nl.item(0);

			UUID model_uuid = UUID.fromString(d.getAttribute("model-uuid"));

			Class model_class = server.mmgr.getModelByUUID(model_uuid);
			if (model_class == null)
				throw new DocumentOpenException("Unrecognized model id - "+d.getAttribute("model-uuid"));


			doc = (Model)model_class.newInstance();

			server.python.set("_loading", true);
			doc.loadStart();

			try {
				// load model related parameters
				doc.fromXmlDom(d);
			} catch (DuplicateIdException e1) {
				e1.printStackTrace();
			}

			nl = xmldoc.getElementsByTagName("editable");
			Element re = (Element)nl.item(0);

			GroupNode root = new GroupNode(doc, "_root");
			doc.setRoot(root);


			if (re.getAttribute("class").equals(GroupNode.class.getName()))
				try {
					root.fromXmlDom(re);
				} catch (DuplicateIdException e1) {
					e1.printStackTrace();
				}
				else
					System.err.println("Invalid file format: invalid root group element (id="+re.getAttribute("id")+"; class="+ GroupNode.class.getName() +")");

			nl = xmldoc.getElementsByTagName("editable-connection");
			for (int i=0; i<nl.getLength(); i++ ) {
				Element e = (Element)nl.item(i);

				BasicEditable first = doc.getComponentById(e.getAttribute("first"));
				BasicEditable second = doc.getComponentById(e.getAttribute("second"));

				if (first == null) {
					System.err.println ("Component \""+e.getAttribute("first")+"\" not found while creating connections!");
				} else if (second == null) {
					System.err.println ("Component \""+e.getAttribute("second")+"\" not found while creating connections!");
				} else
					try {
						EditableConnection con = doc.createConnection(first, second);
						con.fromXmlDom(e);
					} catch (InvalidConnectionException ex) {
						ex.printStackTrace();
					} catch (DuplicateIdException ex) {
						ex.printStackTrace();
					}
			}

			doc.loadEnd();
			server.python.set("_loading", false);

			return doc;
		} 	 catch (InstantiationException e2) {
			e2.printStackTrace();
			throw new DocumentOpenException (e2.getMessage());
		} catch (IllegalAccessException e2) {
			e2.printStackTrace();			
			throw new DocumentOpenException (e2.getMessage());
		}/* catch (UnsupportedComponentException e) {
			e.printStackTrace();
			throw new DocumentOpenException (e.getMessage());
		}*/

	}

	public List<BasicEditable> getSelection() {
		return editorView.getSelection();
	}

	public Component getSimControls() {
		return panelModelSpecificControls.getComponent(0);
	}




}  //  @jve:decl-index=0:visual-constraint="10,10"