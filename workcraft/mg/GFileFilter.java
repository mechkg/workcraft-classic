package workcraft.mg;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class GFileFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;
		if (f.getName().endsWith(".g"))
			return true;
		return false;
	}

	@Override
	public String getDescription() {
		return "Petri model text description (*.g)";
	}

}