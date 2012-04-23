package workcraft.spreadtoken;

import java.awt.event.KeyEvent;
import java.util.UUID;

import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.sdfs.SDFSLogic1Way;

public class STLogic extends SDFSLogic1Way {
	public STLogic(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
	}

	public static final UUID _modeluuid = UUID.fromString("a57b3350-73d3-11db-9fe1-0800200c9a66");
	public static final String _displayname = "Combinatorial Logic";
	public static final String _hotkey = "w";
	public static final int _hotkeyvk = KeyEvent.VK_W;	

	@Override
	public void rebuildRuleFunctions() {
		resetFunc = expandRule("preset:l r,preset:r !m");
		evalFunc = expandRule("preset:l e,preset:r m");
	}
}