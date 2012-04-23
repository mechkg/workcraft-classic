package workcraft.logic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;

public class DNF
{
	class Token
	{
		String token;
		int precedence;
		boolean inverse;
		
		Token(String token, int precedence)
		{
			this.token = token;
			this.precedence = precedence;
			inverse = false;
		}
	}	
	
	public HashSet<DNFClause> clauses;
	public HashSet<DNFLiteral> domain;
	
	public DNF()
	{
		clauses = new HashSet<DNFClause>();
		domain = new HashSet<DNFLiteral>();
	}
	
	public void clear()
	{
		clauses.clear();
		domain.clear();
	}
	
	public DNF add(DNF dnf)
	{
		DNF res = new DNF();
		
		res.domain.addAll(domain);
		res.domain.addAll(dnf.domain);
		
		res.clauses.addAll(clauses);
		res.clauses.addAll(dnf.clauses);
		
		return res;
	}

	public DNF add(DNFClause clause)
	{
		DNF res = new DNF();
		
		res.domain.addAll(domain);
		res.clauses.addAll(clauses);

		for(DNFLiteral literal: clause.pos) res.domain.add(literal);
		for(DNFLiteral literal: clause.neg) res.domain.add(literal);
		
		if (!clause.inconsistent())	res.clauses.add(clause);
		
		return res;		
	}

	public DNF multiplyBy(DNFClause clause)
	{
		DNF res = new DNF();
		
		for(DNFClause x: clauses) res = res.add(x.multiplyBy(clause));
		
		return res;
	}
	
	public boolean inconsistent()
	{
		for(DNFClause clause: clauses) if (!clause.inconsistent()) return false;
		return true;
	}
	
	public DNF multiplyBy(DNF dnf)
	{
		DNF res = new DNF();
		
		res.domain.addAll(domain);
		res.domain.addAll(dnf.domain);

		for(DNFClause x: clauses) for(DNFClause y: dnf.clauses) res = res.add(x.multiplyBy(y));

		return res;	
	}
	
	public DNF negate()
	{
		DNF res = new DNF();
		boolean first = true;
		for(DNFClause clause: clauses) 
			if (first)
			{
				first = false;
				res = res.add(clause.negate());
			}
			else
			{
				res = res.multiplyBy(clause.negate());
			}
		return res;
	}
	
	// no predicates dummy
	public void parseExpression(String expression) throws InvalidExpressionException
	{
		parseExpression(expression, new HashMap<String, String>());
	}
		
	public void parseExpression(String expression, HashMap<String, String> predicates) throws InvalidExpressionException
	{
		Stack<Token> ops = new Stack<Token>();
		Vector<Token> res = new Vector<Token>();
		
		int pos = 0;
		while(pos < expression.length())
		{
			char ch = expression.charAt(pos);
			
			if (Character.isWhitespace(ch)) { pos++; continue;}
			
			String token = "";
			token += ch;
			
			pos++;
			while(pos < expression.length())
			{
				if ("()&|!*+".contains(""+ch)) break;
				ch = expression.charAt(pos);
				if (Character.isWhitespace(ch)) break;
				if ("()&|!*+".contains(""+ch)) break;
				token += ch;
				pos++;
			}
			
			if (token.equals("("))
			{
				ops.push(new Token("(", 100));
			}
			else
			if (token.equals(")"))
			{
				while(!ops.isEmpty() && !ops.peek().token.equals("(")) res.add(ops.pop());
				if (ops.isEmpty()) throw new InvalidExpressionException("Wrong brackets structure");
				ops.pop();
			}
			else
			if (token.equals("&") || token.equals("*") || token.equals("and"))
			{
				// operator AND
				
				while(!ops.isEmpty() && ops.peek().precedence < 3) res.add(ops.pop());
				ops.push(new Token("*", 3));
			}
			else
			if (token.equals("|") || token.equals("+") || token.equals("or"))
			{
				// operator OR
				
				while(!ops.isEmpty() && ops.peek().precedence < 4) res.add(ops.pop());
				ops.push(new Token("+", 4));
			}
			else
			if (token.equals("!") || token.equals("not"))
			{
				// operator NOT

				ops.push(new Token("!", 2));
			}
			else
			if (predicates.containsKey(token))
			{
				// predicate
				ops.push(new Token(token, 1));
			}
			else
			{
				// literal
				res.add(new Token(token, 0));
			}
		}
		
		while(!ops.isEmpty())
		{
			if (ops.peek().token.equals("(")) throw new InvalidExpressionException("Wrong brackets structure");
			res.add(ops.pop());
		}
		
		Stack<DNF> stack = new Stack<DNF>();
		
		// Negations elimination
		
		for(int i = 0; i<res.size(); i++)
		{
			Token token = res.elementAt(i);
			if (token.token.equals("!"))
			{
				Stack<Integer> todo = new Stack<Integer>();
				
				todo.add(i - 1);
				
				while(!todo.isEmpty())
				{
					int j = todo.pop();
					Token next = null;
					
					while(true)
					{
						next = res.elementAt(j);
						if (next.precedence == 0) break;
						if (next.token.equals("+")) break;
						if (next.token.equals("*")) break;
						j--;
					}
					
					if (next.precedence == 0)
					{
						next.inverse = !next.inverse;
					}
					else
					{
						// De Morgan
						
						if (next.token.equals("+")) next.token = "*";
						else
						if (next.token.equals("*")) next.token = "+";
						
						todo.push(j - 1);
						
						j--;
						int need = 1;
						while(need > 0)
						{
							next = res.elementAt(j);
							if (next.precedence == 0) need--;
							if (next.token.equals("+")) need++;
							if (next.token.equals("*")) need++;
							j--;
						}
						
						todo.push(j);						
					}
					
				}
				
			}
		}
		
		for(int i = 0; i<res.size(); i++)
		{
			Token token = res.elementAt(i);
			if (token.precedence==0) // token is literal (component id)
			{
				String id = token.token;
				if (i+1<res.size() && res.elementAt(i+1).precedence == 1)
				{
					String suffix = predicates.get(res.elementAt(i+1).token);
					if (suffix != null)
						id = id + "_" + suffix;
					else
						throw new InvalidExpressionException("Predicate \""+token.token+"\" mapping is undefined");
				}
				DNFLiteral literal = new DNFLiteral(id);
				DNFClause clause = new DNFClause();
				DNF dnf = new DNF();
				stack.push(dnf.add(clause.multiplyBy(literal, !token.inverse)));				
			}
			else
			if (token.token.equals("!"))
			{
//				if (stack.size()<1) throw new InvalidExpressionException("Operand for NOT operator not found");
//				stack.push(stack.pop().negate());
			}
			else
			if (token.token.equals("+"))
			{
				if (stack.size()<2) throw new InvalidExpressionException("Operand for OR operator not found");
				stack.push(stack.pop().add(stack.pop()));
			}
			else
			if (token.token.equals("*"))
			{
				if (stack.size()<2) throw new InvalidExpressionException("Operand for AND operator not found");
				stack.push(stack.pop().multiplyBy(stack.pop()));
			}
		}
		
		if (stack.size()!=1) throw new InvalidExpressionException("Expression syntax error");
		
		clear();
		
		domain.addAll(stack.peek().domain);		
		clauses.addAll(stack.peek().clauses);		
	}
	
	public String toString()
	{
		String res = "";
		for (DNFClause c : clauses) {
			if (res.length() > 0)
				res += "+";
			res += c;
		}
		return res;
	}
	
	public DNF minimise()
	{
		int n = domain.size();
		
		DNFLiteral [] literals = new DNFLiteral[n];
		
		int k = 0;
		for(DNFLiteral literal : domain) literals[k++] = literal;
		
		Vector<DNFImplicant> current = new Vector<DNFImplicant>();
		Vector<DNFImplicant> next = new Vector<DNFImplicant>();
		Vector<DNFImplicant> result = new Vector<DNFImplicant>();
		
		int m = clauses.size();
		DNFClause [] c = new DNFClause[m];
		int kq = 0;
		
		for(DNFClause clause : clauses)
		{
			c[kq++] = clause;
			
			DNFImplicant x = new DNFImplicant();
			for(k = 0; k < n; k++)
				if (clause.pos.contains(literals[k])) x.S += "1";
				else
				if (clause.neg.contains(literals[k])) x.S += "0";
				else
					x.S += "-";
			
			current.add(x);
		}
		
		// combine implicants with k dont_cares
		for(k = 0; k < n; k++)
		{
			next.clear();

			for(DNFImplicant a : current)
				if (a.countDontCares() == k)
					for(DNFImplicant b : current)
						if (b.countDontCares() == k && a.canCombineWith(b))
						{
							a.used = true;
							b.used = true;
							next.add(a.combineWith(b));
						}
							
			for(DNFImplicant a : current)
				if (a.countDontCares() == k)
				{
					if (!a.used) result.add(a);
				}
				else
				{
					next.add(a);
				}
			
			current.clear();
			current.addAll(next);
		}
		
		result.addAll(current);
		
		boolean [] covered = new boolean[m];
		
		int left = m;

		DNF res = new DNF();		
		
		while(left > 0)
		{
			// find essential implicants
			for(k = 0; k < m; k++)
				if (!covered[k])
				{
					int best = -1;
					for(int i = 0; i < result.size(); i++)
					{
						boolean covers = true;
						DNFImplicant im = result.get(i);
						for(int j = 0; j < n && covers; j++)
						{
							if (im.S.charAt(j) == '0' && !c[k].neg.contains(literals[j])) covers = false;
							if (im.S.charAt(j) == '1' && !c[k].pos.contains(literals[j])) covers = false;
						}
						if (covers)
						{
							if (best == -1) best = i;
							else
							{
								best = -1;
								break;
							}
						}
					}
					if (best != -1)
					{
						DNFClause clause = new DNFClause();
						DNFImplicant im = result.get(best);
						
						for(int i = 0; i < n; i++)
							if (im.S.charAt(i) == '0') clause.neg.add(literals[i]);
							else
							if (im.S.charAt(i) == '1') clause.pos.add(literals[i]);
						
						res = res.add(clause);
						
						for(int cl = 0; cl < m; cl++)
						if (!covered[cl])
						{
							boolean covers = true;
							for(int j = 0; j < n && covers; j++)
							{
								if (im.S.charAt(j) == '0' && !c[cl].neg.contains(literals[j])) covers = false;
								if (im.S.charAt(j) == '1' && !c[cl].pos.contains(literals[j])) covers = false;
							}
							if (covers)
							{
								covered[cl] = true;
								left--;
							}
						}
					}
				}
			
			// select greedy coverage
			
			int best = -1;
			DNFImplicant best_im = null;
			
			for(int i = 0; i < result.size(); i++)
			{
				int coverage = 0;
				DNFImplicant im = result.get(i);
				
				for(k = 0; k < m; k++)
					if (!covered[k])
					{
						boolean covers = true;
						for(int j = 0; j < n && covers; j++)
						{
							if (im.S.charAt(j) == '0' && !c[k].neg.contains(literals[j])) covers = false;
							if (im.S.charAt(j) == '1' && !c[k].pos.contains(literals[j])) covers = false;
						}
						if (covers) coverage++;
					}
				
				if (coverage > best)
				{
					best = coverage;
					best_im = im;
				}
			}			
			
			// apply best coverage

			DNFClause clause = new DNFClause();
			
			for(int i = 0; i < n; i++)
				if (best_im.S.charAt(i) == '0') clause.neg.add(literals[i]);
				else
				if (best_im.S.charAt(i) == '1') clause.pos.add(literals[i]);
			
			res = res.add(clause);
			
			for(k = 0; k < m; k++)
				if (!covered[k])
				{
					boolean covers = true;
					for(int j = 0; j < n && covers; j++)
					{
						if (best_im.S.charAt(j) == '0' && !c[k].neg.contains(literals[j])) covers = false;
						if (best_im.S.charAt(j) == '1' && !c[k].pos.contains(literals[j])) covers = false;
					}
					if (covers)
					{
						covered[k] = true;
						left--;
					}
				}
		}
		
		return res;
	}
	
	public boolean evaluate(HashMap<String, Boolean> values)
	{
		for(DNFClause clause: clauses)
		{
			boolean satisfied = true;
			for(DNFLiteral literal: clause.pos)
				if (values.containsKey(literal.id) && !values.get(literal.id))
				{
					satisfied = false;
					break;
				}
			
			if (satisfied)
				for(DNFLiteral literal: clause.neg)
					if (values.containsKey(literal.id) && values.get(literal.id))
					{
						satisfied = false;
						break;
					}
			
			if (satisfied) return true;
		}
		return false;
	}
}
