package workcraft.stg;

import java.util.List;
import java.util.UUID;

import workcraft.UnsupportedComponentException;
import workcraft.editor.BasicEditable;
import workcraft.petri.EditablePetriTransition;
import workcraft.util.Colorf;

public class EditableSTGTransition extends EditablePetriTransition {
	public static final UUID _modeluuid = UUID.fromString("10418180-D733-11DC-A679-A32656D89593");

	public EditableSTGTransition(BasicEditable parent) throws UnsupportedComponentException {
		super(parent);
	}

	public enum TransitionType {internalTransition, inputTransition, outputTransition, dummyTransition};

	protected static Colorf inputTransitionColor = new Colorf (1.0f, 0.0f, 0.0f, 1.0f);
	protected static Colorf outputTransitionColor = new Colorf (0.0f, 0.0f, 1.0f, 1.0f);
	protected static Colorf dummyTransitionColor = new Colorf(0.8f, 0.0f, 0.8f, 1.0f);
	protected static Colorf internalTransitionColor = new Colorf(0.0f, 0.0f, 0.0f, 1.0f);
	
	public Colorf getTransitionColor(TransitionType trType) {
		switch (trType) {
		case internalTransition:
			return internalTransitionColor;
		case inputTransition:
			return inputTransitionColor;
		case outputTransition:
			return outputTransitionColor;
		case dummyTransition:
			return dummyTransitionColor;
		}
		return internalTransitionColor;
	}
	
	protected TransitionType transitionType = TransitionType.internalTransition; 

	protected Colorf getLabelColor() {
		return getTransitionColor(this.transitionType);
	}
	
	public void setLabel(String label) {
		super.setLabel(label);
		
	}
	
	public List<String>getEditableProperties()  {
		
		List<String> list = super.getEditableProperties(); 
		list.add("enum,Transition type,getTransitionType,setTransitionType,Internal,Input,Output,Dummy");
		
		return list;
	}
	
	public Integer getTransitionType() {
		switch (transitionType) {
		case internalTransition:
			return 0;
		case inputTransition:
			return 1;
		case outputTransition:
			return 2;
		case dummyTransition:
			return 3;
		}
		return 0;
	}
	

	public void setTransitionType(Integer trType) {
		switch (trType) {
		case 0:
			if (this.transitionType != TransitionType.internalTransition) {
				
				this.transitionType = TransitionType.internalTransition;
			}
			break;
		case 1:
			if (this.transitionType != TransitionType.inputTransition) {
				
				this.transitionType = TransitionType.inputTransition;
			}
			break;
		case 2:
			if (this.transitionType != TransitionType.outputTransition) {
				
				this.transitionType = TransitionType.outputTransition;
			}
			break;
		case 3:
			if (this.transitionType != TransitionType.dummyTransition) {
				this.transitionType = TransitionType.dummyTransition;
			}
			break;
		}
	}
}
