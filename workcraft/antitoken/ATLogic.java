package workcraft.antitoken;

import java.awt.event.KeyEvent;
import java.util.UUID;

import workcraft.Model;
import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.sdfs.SDFSLogic2Way;

public class ATLogic extends SDFSLogic2Way {
	public static final UUID _modeluuid = UUID.fromString("5e947520-7af7-11db-9fe1-0800200c9a66");
	public static final String _displayname = "Combinatorial Logic";
	public static final String _hotkey = "w";
	public static final int _hotkeyvk = KeyEvent.VK_W;

	public ATLogic(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
	}

	@Override
	public void rebuildRuleFunctions() {
		fwdEvalFunc = expandRule("preset:l lfe,preset:r m,preset:r !am");
		fwdResetFunc = expandRule("preset:r !m|preset:r am,preset:l lfr");
		backEvalFunc = expandRule("postset:l lbe,postset:r am,postset:r !m");
		backResetFunc = expandRule("postset:l lbr,postset:r !am|postset:r m");	
	}
}
