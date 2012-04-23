package workcraft.common;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.JButton;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import javax.swing.JDialog;
import javax.swing.JScrollPane;

import workcraft.util.Point;

public class ExternalProcess extends JDialog{

	public class WaitThread extends Thread {
		public void run() {
			try {
				process.waitFor();
			} catch (InterruptedException e) {
				canceled = true;
				process.destroy();
			}
			finish();
		}
	}

	public class StreamReadThread extends Thread {
		private InputStream input;
		private JTextArea output;
		public StreamReadThread(InputStream input, JTextArea output) {
			super();
			this.input = input;
			this.output = output;
		}

		byte[] buf = new byte[1024];
		public void run() {
			boolean eof = false;
			try {
				while(! (canceled || eof)) {
					
					int i = input.read(buf, 0, 1024);
					if (i<0) {
						eof = true;
						break;
					}
						output.append(new String(buf, 0, i));
					}
					
					sleep(50);
 
			}catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
			}
		}
	}


	private static final long serialVersionUID = 1L;
	private JPanel jContentPane = null;
	private JTabbedPane jTabbedPane = null;
	private JButton btnCancel = null;

	private boolean canceled = false;
	private boolean keepWindow;
	private int returnCode = -1;
	private Process process = null;
	private WaitThread waitThread = null;
	private StreamReadThread stdoutThread = null;
	private StreamReadThread stderrThread = null;
	private JScrollPane jScrollPane = null;
	private JTextArea txtStdout = null;
	private JScrollPane jScrollPane1 = null;
	private JTextArea txtStderr = null;


	public boolean wasCanceled() {
		return canceled;		
	}

	public int getReturnCode() {
		return returnCode;
	}

	public String getOutput() {
		return txtStdout.getText();
	}

	public synchronized void finish() {
		if (!canceled)
			returnCode = process.exitValue();
		stdoutThread.interrupt();
		stderrThread.interrupt();

		if (keepWindow)
			btnCancel.setText("OK");
		else
			setVisible(false);
	}

	/**
	 * This is the default constructor
	 */
	public ExternalProcess(JFrame parent) {
		super(parent);

		initialize();

		int x;
		int y;


		Container myParent = getParent();
		java.awt.Point topLeft = myParent.getLocationOnScreen();
		Dimension parentSize = myParent.getSize();

		Dimension mySize = getSize();

		if (parentSize.width > mySize.width) 
			x = ((parentSize.width - mySize.width)/2) + topLeft.x;
		else 
			x = topLeft.x;

		if (parentSize.height > mySize.height) 
			y = ((parentSize.height - mySize.height)/2) + topLeft.y;
		else 
			y = topLeft.y;

		setLocation (x, y);

	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(515, 326);
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.setContentPane(getJContentPane());
		this.setTitle("JFrame");
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints2.gridy = 1;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.fill = GridBagConstraints.BOTH;
			gridBagConstraints.gridy = 0;
			gridBagConstraints.weightx = 1.0;
			gridBagConstraints.weighty = 1.0;
			gridBagConstraints.insets = new Insets(2, 2, 2, 2);
			gridBagConstraints.gridx = 0;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.ipadx = 0;
			gridBagConstraints1.ipady = 0;
			gridBagConstraints1.gridy = 1;
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(getJTabbedPane(), gridBagConstraints);
			jContentPane.add(getBtnCancel(), gridBagConstraints2);
		}
		return jContentPane;
	}

	public void run(String[] command, String directory, String caption, boolean keepWindow) throws IOException {
		this.setTitle(caption);

		this.keepWindow = keepWindow;
		btnCancel.setText("Cancel");

		returnCode = -1;
		canceled = false;
		setModal(true);

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(new File(directory));
		
		

		process = pb.start();
		
		waitThread = new WaitThread();
		waitThread.start();

		txtStdout.setText("");
		txtStderr.setText("");

		//BufferedReader kozo = new BufferedReader ( new InputStreamReader( process.getInputStream()), 1);
		//stdoutThread = new StreamReadThread(kozo, txtStdout);
		//	kozo = new BufferedReader ( new InputStreamReader( process.getErrorStream()), 1);
		//	stderrThread = new StreamReadThread(kozo, txtStderr);

		stdoutThread = new StreamReadThread(process.getInputStream(), txtStdout);
		stderrThread = new StreamReadThread(process.getErrorStream(), txtStderr);

		stdoutThread.start();
		stderrThread.start();

		setVisible(true);
	}

	/**
	 * This method initializes jTabbedPane	
	 * 	
	 * @return javax.swing.JTabbedPane	
	 */
	private JTabbedPane getJTabbedPane() {
		if (jTabbedPane == null) {
			jTabbedPane = new JTabbedPane();
			jTabbedPane.addTab("Output", null, getJScrollPane(), "External process' stdout output");
			jTabbedPane.addTab("Errors/Debug information", null, getJScrollPane1(), "External process\' stderr output");
		}
		return jTabbedPane;
	}

	/**
	 * This method initializes btnCancel	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getBtnCancel() {
		if (btnCancel == null) {
			btnCancel = new JButton();
			btnCancel.setText("Cancel");
			btnCancel.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (btnCancel.getText().equals("Cancel"))
						waitThread.interrupt();
					else
						setVisible(false);
				}
			});

		}
		return btnCancel;
	}



	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getTxtStdout());
		}
		return jScrollPane;
	}



	/**
	 * This method initializes txtStdout	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getTxtStdout() {
		if (txtStdout == null) {
			txtStdout = new JTextArea();
			txtStdout.setEditable(false);
		}
		return txtStdout;
	}



	/**
	 * This method initializes jScrollPane1	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane1() {
		if (jScrollPane1 == null) {
			jScrollPane1 = new JScrollPane();
			jScrollPane1.setViewportView(getTxtStderr());
		}
		return jScrollPane1;
	}



	/**
	 * This method initializes txtStderr	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getTxtStderr() {
		if (txtStderr == null) {
			txtStderr = new JTextArea();
			txtStderr.setEditable(false);
		}
		return txtStderr;
	}



}  //  @jve:decl-index=0:visual-constraint="10,10"
