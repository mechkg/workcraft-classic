package workcraft.logic;

public class DNFLiteral
{
	public String id;
		
	DNFLiteral(String id) {
		this.id = id;
	}
	
	@Override
	public boolean equals(Object obj) 	{
		if(this == obj) return true;
		if((obj == null) || (obj.getClass() != this.getClass())) return false;
		return id.equals(((DNFLiteral)obj).id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public String toString() {
		return id;
	}
}