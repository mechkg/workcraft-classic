package workcraft.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MPSATOutputParser {
	public static String parsePetriNetTrace (String mpsatOutput) {
		Pattern pat = Pattern.compile("SOLUTION.*\n(.*?)\n", Pattern.UNIX_LINES);
		Matcher m = pat.matcher(mpsatOutput);

		if (m.find()) {
			String mpsat_trace = m.group(1);
			String[] ss = mpsat_trace.split(",");
			String trace = "";

			for (String k: ss) {
				if (trace.length()>0)
					trace+=";";
				if (k.startsWith("d."))
					trace += k.substring(2).trim();
				else
					trace += k.trim();
			}

			return trace;
		} else
			return null;
	}


	public static String parseSchematicNetTrace (String mpsatOutput) {
		System.err.println(mpsatOutput);
		System.err.println("");
		System.err.println("");
		System.err.println("");
		System.err.println("");
		Pattern pat = Pattern.compile("SOLUTION.*\n(.*?)\n", Pattern.UNIX_LINES);
		Matcher m = pat.matcher(mpsatOutput);

		if (m.find()) {
			System.err.print(m.group(1));
			
			String mpsat_trace = m.group(1);
			String[] ss = mpsat_trace.split(",");
			String trace = "";

			Pattern p2 = Pattern.compile("d\\.(.+?)_");

			for (String k: ss) {
				if (k.contains("iface"))
					continue;
				Matcher m2 = p2.matcher(k);
				m2.find();
				if (trace.length()>0)
					trace+=";";

				trace+=m2.group(1);
			}
			return trace;
		} else
			return null;
	}	
}
