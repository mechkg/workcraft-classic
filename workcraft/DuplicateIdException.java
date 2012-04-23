package workcraft;

public class DuplicateIdException extends Exception {
	public DuplicateIdException(String id) {
		super(id);		
	}
}
