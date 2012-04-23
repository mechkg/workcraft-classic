package workcraft.logic;
import java.util.HashMap;
import java.util.LinkedList;

import workcraft.WorkCraftServer;

public class DNFUtil {

	public static void resolveLiterals (DNF dnf, WorkCraftServer server, HashMap<String,Object> resolved)
	{
		LinkedList markedClauses = new LinkedList();
		LinkedList markedLiterals = new LinkedList();

		for (DNFClause clause : dnf.clauses)
		{
			boolean clauseMarked = false;

			for (DNFLiteral literal : clause.pos) {
				if (resolved.containsKey(literal.id))
					continue;
				if (server.testObject(literal.id))
					markedLiterals.add(literal);
				else {
					markedClauses.add(clause);
					clauseMarked = true;
					break;
				}
			}

			for (Object o : markedLiterals)
				clause.pos.remove(o);

			markedLiterals.clear();

			if (!clauseMarked) {
				for (DNFLiteral literal : clause.neg) {
					if (resolved.containsKey(literal.id))
						continue;

					if (server.testObject(literal.id)) {
						markedClauses.add(clause);
						clauseMarked = true;
						break;
					}
					else 
						markedLiterals.add(literal);
				}

				for (Object o : markedLiterals)
					clause.neg.remove(o);

				if (!clauseMarked) {
					if (clause.isEmpty())
						markedClauses.add(clause);
				}
			}
		}

		for (Object o : markedClauses)
			dnf.clauses.remove(o);
	}
}