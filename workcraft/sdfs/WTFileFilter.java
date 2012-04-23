package workcraft.sdfs;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class WTFileFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;
		if (f.getName().endsWith(".wt"))
			return true;
		return false;
	}

	@Override
	public String getDescription() {
		return "Workcraft simulation trace (*.wt)";
	}

}