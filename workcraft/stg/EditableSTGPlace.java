package workcraft.stg;

import java.util.UUID;

import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.petri.EditablePetriPlace;

public class EditableSTGPlace extends EditablePetriPlace {
	public static final UUID _modeluuid = UUID.fromString("10418180-D733-11DC-A679-A32656D89593");
	
	public EditableSTGPlace(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
	}

}
