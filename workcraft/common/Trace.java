package workcraft.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import workcraft.sdfs.WTFileFilter;

public class Trace {
	private LinkedList<String> trace = new LinkedList<String>();
	
	public void addEvent (String event) {
		trace.add(event);
	}
	
	
	public List<String> getEvents() {
		return (List<String>)trace.clone();
	}

	public void setTrace (List<String> events) {
		trace.clear();
		for (String e : events)
			trace.add(e);
	};
	
	public void parseTrace (String s) {
		trace.clear();
		String[] events = s.split(";");
		for (String ss: events)
			trace.add(ss);
	}
	
	public String toString() {
		String s = "";
		for (String e : trace) {
			if (s.length() > 0)
				s += ";";
			s += e;
		}	
		return s;
	}

	public void clear() {
		trace.clear();
	}

	
}
