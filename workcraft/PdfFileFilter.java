package workcraft;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class PdfFileFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if (f.isDirectory())
			return true;
		if (f.getName().endsWith(".pdf"))
			return true;
		return false;
	}

	@Override
	public String getDescription() {
		return "Portable Document Format (*.pdf)";
	}

}