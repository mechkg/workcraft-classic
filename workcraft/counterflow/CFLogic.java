package workcraft.counterflow;

import java.awt.event.KeyEvent;
import java.util.UUID;

import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.sdfs.SDFSLogic2Way;

public class CFLogic extends SDFSLogic2Way  {
	public static final UUID _modeluuid = UUID.fromString("9df82f00-7aec-11db-9fe1-0800200c9a66");
	public static final String _displayname = "Combinatorial Logic";
	public static final String _hotkey = "w";
	public static final int _hotkeyvk = KeyEvent.VK_W;	

	public CFLogic(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
	}

	@Override
	public void rebuildRuleFunctions() {
		fwdEvalFunc = expandRule("preset:l lfe,preset:r om");
		fwdResetFunc = expandRule("preset:l lfr,preset:r !om");
		backEvalFunc = expandRule("postset:l lbe,postset:r om");
		backResetFunc = expandRule("postset:l lbr,postset:r !om");
	}
}
