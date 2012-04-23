package workcraft;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class XwdFileFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;
		if (f.getName().endsWith(".xwd"))
			return true;
		return false;
	}

	@Override
	public String getDescription() {
		return "XML WorkCraft Document (*.xwd)";
	}

}