package workcraft.petri;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class TAGFileFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;
		if (f.getName().endsWith(".tag"))
			return true;
		return false;
	}

	@Override
	public String getDescription() {
		return "Petri Net labels (*.tag)";
	}

}