package workcraft.logic;

public class DNFImplicant
{
	String S;
	boolean used;
	
	public DNFImplicant()
	{
		super();
		S = "";
		used = false;
	}
	
	public int countDontCares()
	{
		int res = 0;
		for(int i = 0; i < S.length(); i++) if (S.charAt(i) == '-') res++;
		return res;
	}

	public boolean canCombineWith(DNFImplicant x)
	{
		int diff = 0;
		for(int i = 0; i < S.length() && diff < 2; i++) if (S.charAt(i) != x.S.charAt(i)) diff++;
		return diff < 2;
	}

	public DNFImplicant combineWith(DNFImplicant x)
	{
		DNFImplicant res = new DNFImplicant();
	
		for(int i = 0; i < S.length(); i++)
			if (S.charAt(i) != x.S.charAt(i)) res.S += "-";
			else res.S += S.charAt(i);
		
		return res;
	}
}
