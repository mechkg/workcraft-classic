package workcraft.logic;

import java.util.HashSet;

public class DNFClause
{
	public HashSet<DNFLiteral> pos;	
	public HashSet<DNFLiteral> neg;

	DNFClause()
	{
		pos = new HashSet<DNFLiteral>();
		neg = new HashSet<DNFLiteral>();
	}
	
	public String toString() {
		String res = "";
		for (DNFLiteral l : pos) {
			res += "("+l+")";			
		}
		for (DNFLiteral l : neg) {
			res += "(!"+l+")";			
		}
		return res;
	}

	public DNFClause multiplyBy(DNFClause clause)
	{
		DNFClause res = new DNFClause();

		res.pos.addAll(pos);
		res.neg.addAll(neg);

		res.pos.addAll(clause.pos);
		res.neg.addAll(clause.neg);

		return res;
	}

	public DNFClause multiplyBy(DNFLiteral literal, boolean positive)
	{
		DNFClause res = new DNFClause();

		res.pos.addAll(pos);
		res.neg.addAll(neg);

		if (positive) res.pos.add(literal); else res.neg.add(literal);

		return res;
	}

	public boolean inconsistent()
	{
		for(DNFLiteral literal: pos) if (neg.contains(literal)) return true;
		return false;
	}

	public boolean isEmpty() {
		return pos.isEmpty() && neg.isEmpty();
	}

	public DNF negate()
	{
		DNF res = new DNF();

		for(DNFLiteral literal: neg)
		{
			DNFClause clause = new DNFClause();
			res = res.add(clause.multiplyBy(literal, true));
		}

		for(DNFLiteral literal: pos)
		{
			DNFClause clause = new DNFClause();
			res = res.add(clause.multiplyBy(literal, false));
		}

		return res;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj) return true;
		if((obj == null) || (obj.getClass() != this.getClass())) return false;
		return pos.equals(((DNFClause)obj).pos) && neg.equals(((DNFClause)obj).neg);
	}

	@Override
	public int hashCode()
	{
		return pos.hashCode() ^ neg.hashCode();
	}
}
