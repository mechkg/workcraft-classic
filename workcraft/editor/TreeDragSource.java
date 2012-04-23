package workcraft.editor;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.tree.*;


public class TreeDragSource implements DragSourceListener, DragGestureListener {
	DragSource source;
	DragGestureRecognizer recognizer;
	TransferableComponent transferable;
	DefaultMutableTreeNode oldNode;
	JTree sourceTree;
	
	public TreeDragSource(JTree tree, int actions) {
		sourceTree = tree;
		source = new DragSource();
		recognizer = source.createDefaultDragGestureRecognizer(
				sourceTree, actions, this);
	}

	public void dragGestureRecognized(DragGestureEvent dge) {
		if (sourceTree.getSelectionCount() > 1)
			return;
		
		TreePath path = sourceTree.getSelectionPath();
		
		if ((path == null) || (path.getPathCount() <= 1)) {
			return;
		}
		
		DefaultMutableTreeNode n = (DefaultMutableTreeNode)path.getLastPathComponent();
		if (!n.isLeaf())
			return;
		
		transferable = new TransferableComponent( (ComponentWrapper)n.getUserObject());
		
		dge.startDrag(null, transferable, this);
	}

	public void dragEnter(DragSourceDragEvent dsde) { }
	public void dragExit(DragSourceEvent dse) { }
	public void dragOver(DragSourceDragEvent dsde) { }
	public void dropActionChanged(DragSourceDragEvent dsde) { }
	public void dragDropEnd(DragSourceDropEvent dsde) { }
}