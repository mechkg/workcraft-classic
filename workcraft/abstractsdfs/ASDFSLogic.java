package workcraft.abstractsdfs;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.UUID;

import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.sdfs.SDFSLogic1Way;
import workcraft.sdfs.SDFSLogic2Way;

public class ASDFSLogic extends SDFSLogic1Way {
	public ASDFSLogic(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
	}

	public static final UUID _modeluuid = UUID.fromString("24a8a176-dafe-11db-8314-0800200c9a66");
	public static final String _displayname = "Combinatorial Logic";
	public static final String _hotkey = "w";
	public static final int _hotkeyvk = KeyEvent.VK_W;	

	@Override
	public void rebuildRuleFunctions() {
	}
}