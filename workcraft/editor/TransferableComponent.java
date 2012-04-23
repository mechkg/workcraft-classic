package workcraft.editor;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class TransferableComponent implements Transferable {
	public static DataFlavor COMPONENT_WRAPPER_FLAVOR =
		new DataFlavor(ComponentWrapper.class, "Component Wrapper");

	DataFlavor flavors[] = { COMPONENT_WRAPPER_FLAVOR, DataFlavor.stringFlavor };
	ComponentWrapper wrapper;

	public TransferableComponent(ComponentWrapper w) {
		wrapper = w;
	}

	public synchronized DataFlavor[] getTransferDataFlavors() {
		return flavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return (
				flavor.getRepresentationClass() == ComponentWrapper.class ||
				flavor == DataFlavor.stringFlavor				
				);
	}
	public synchronized Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (isDataFlavorSupported(flavor)) {
			if (flavor == DataFlavor.stringFlavor)
			{
				return ((ComponentWrapper)wrapper).toString();
			} else
				return (Object)wrapper;
			
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
	}
}