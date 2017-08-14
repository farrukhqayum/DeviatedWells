import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Label;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import org.eclipse.wb.swing.FocusTraversalOnArray;
import java.awt.Component;

public class DeviationConversion extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JPanel contentPanel = new JPanel();
	private JTextField inputFilePath;
	private JTextField surfaceX;
	private JTextField surfaceY;
	private static float _X, _Y;
	private static float _dx, _dy, _TVD, _TVDSS, _KB;
	private static int _nrHeaderLines = 0;
	private static ArrayList<String> _inputFileString;
	private static ArrayList<String> _outFileString;
	private static int _mdCol, _inclCol, _aziCol;

	File[] files;
	public static String USER_HOME = System.getProperty("user.home");
	public static String LAST_VISITED_DIRECTORY = USER_HOME;
	TextArea _filePreview;
	private JTextField nrHeaderLines;
	JSpinner mdCol, inclCol, aziCol;
	private JTextField KB;
	private JButton btnConvert;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		try {
			DeviationConversion dialog = new DeviationConversion();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create the dialog.
	 */
	public DeviationConversion() {
		setResizable(false);
		setTitle("Convert Deviation Surveys");
		setBounds(100, 100, 694, 447);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);

		JLabel label = new JLabel("");
		label.setBounds(10, 83, 141, 0);
		contentPanel.add(label);

		JLabel lblInputFile = new JLabel("Input Deviation File");
		lblInputFile.setBounds(25, 18, 119, 20);
		contentPanel.add(lblInputFile);

		inputFilePath = new JTextField();
		inputFilePath.setToolTipText("Provide an input file.");
		inputFilePath.setText("M:\\surveys\\Teapot_Dome\\RMOTC_TEAPOT_DOME\\Wells\\CD_files\\1-Tp-3.txt");
		inputFilePath.setBounds(144, 18, 413, 20);
		contentPanel.add(inputFilePath);
		inputFilePath.setColumns(10);

		JButton btnSelectInputPath = new JButton("Select");
		btnSelectInputPath.setToolTipText("Select the input file. Post selection, the file is read and its contents are displayed in the Preview panel.");
		btnSelectInputPath.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JFileChooser fc = new JFileChooser();
				fc.setAcceptAllFileFilterUsed(true);
				// fc.setMultiSelectionEnabled(true);
				fc.setCurrentDirectory(new File(LAST_VISITED_DIRECTORY));
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int returnVal = fc.showDialog(fc, null);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					inputFilePath.setText(fc.getSelectedFile().getAbsolutePath());
					LAST_VISITED_DIRECTORY = getDirOnly(inputFilePath.getText());
					// File[] files = fc.getSelectedFiles();
					try {
						readAscii();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					setPreview();
				}
			}
		});
		btnSelectInputPath.setBounds(567, 17, 80, 20);
		contentPanel.add(btnSelectInputPath);

		JLabel lblWellXY = new JLabel("Well (X, Y, KB)");
		lblWellXY.setBounds(25, 51, 87, 20);
		contentPanel.add(lblWellXY);

		surfaceX = new JTextField();
		surfaceX.setToolTipText("X (Easting) in any units.");
		surfaceX.setText("799858");
		surfaceX.setBounds(144, 51, 110, 20);
		contentPanel.add(surfaceX);
		surfaceX.setColumns(10);

		surfaceY = new JTextField();
		surfaceY.setToolTipText("Y (Northing) in any units.");
		surfaceY.setText("958806.2");
		surfaceY.setColumns(10);
		surfaceY.setBounds(294, 51, 110, 20);
		contentPanel.add(surfaceY);
		{
			btnConvert = new JButton("Convert");
			btnConvert.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					setParameters();
					if (!_inputFileString.isEmpty()) {
						estimateDeviation();
						try {
							writeAscii();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						updatePreview();
					}
				}
			});
			btnConvert.setBounds(567, 51, 80, 20);
			contentPanel.add(btnConvert);
			btnConvert.setActionCommand("OK");
			getRootPane().setDefaultButton(btnConvert);
		}

		JLabel lblFileFormat = new JLabel("File Format (Columns):");
		lblFileFormat.setBounds(25, 120, 140, 20);
		contentPanel.add(lblFileFormat);

		JLabel lblMd = new JLabel("MD");
		lblMd.setBounds(177, 120, 34, 20);
		contentPanel.add(lblMd);

		JLabel lblIncl = new JLabel("Inclination");
		lblIncl.setBounds(293, 120, 67, 20);
		contentPanel.add(lblIncl);

		JLabel lblAzi = new JLabel("Azimuth");
		lblAzi.setBounds(463, 120, 50, 20);
		contentPanel.add(lblAzi);

		mdCol = new JSpinner();
		mdCol.setBounds(209, 120, 45, 20);
		contentPanel.add(mdCol);
		mdCol.setValue(2);

		inclCol = new JSpinner();
		inclCol.setBounds(359, 120, 45, 20);
		contentPanel.add(inclCol);
		inclCol.setValue(3);

		aziCol = new JSpinner();
		aziCol.setBounds(512, 120, 45, 20);
		contentPanel.add(aziCol);
		aziCol.setValue(4);

		_filePreview = new TextArea();
		_filePreview.setBounds(10, 171, 668, 196);
		contentPanel.add(_filePreview);

		Label lblPreview = new Label("Preview");
		lblPreview.setFont(new Font("Dialog", Font.BOLD, 12));
		lblPreview.setBounds(10, 146, 62, 22);
		contentPanel.add(lblPreview);

		Label label_1 = new Label("Lines to skip");
		label_1.setBounds(25, 89, 92, 22);
		contentPanel.add(label_1);

		nrHeaderLines = new JTextField();
		nrHeaderLines.setToolTipText("The lines to skip (0-nothing will be skipped).");
		nrHeaderLines.setText("2");
		nrHeaderLines.setColumns(10);
		nrHeaderLines.setBounds(144, 89, 86, 20);
		contentPanel.add(nrHeaderLines);

		KB = new JTextField();
		KB.setToolTipText("Kelly Bushing (in any units).");
		KB.setText("5226");
		KB.setColumns(10);
		KB.setBounds(447, 51, 110, 20);
		contentPanel.add(KB);
		contentPanel.setFocusTraversalPolicy(new FocusTraversalOnArray(new Component[]{inputFilePath, btnSelectInputPath, surfaceX, surfaceY, KB, btnConvert, nrHeaderLines, mdCol, inclCol, aziCol, _filePreview}));

		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setToolTipText("Exits this dialog.");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
	}

	private void setParameters() {
		_X = Float.parseFloat(surfaceX.getText());
		_Y = Float.parseFloat(surfaceY.getText());
		_nrHeaderLines = Integer.parseInt(nrHeaderLines.getText());
		_mdCol = (Integer) mdCol.getValue();
		_inclCol = (Integer) inclCol.getValue();
		_aziCol = (Integer) aziCol.getValue();
		_KB = Float.parseFloat(KB.getText());
	}

	private void readAscii() throws IOException {
		_inputFileString = new ArrayList<String>();
		FileInputStream fstream = new FileInputStream(inputFilePath.getText());
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line;
		while ((line = br.readLine()) != null) {
			_inputFileString.add(line); // line.split(" ");
		}
		br.close();
	}

	private void writeAscii() throws IOException {
		File f = new File(inputFilePath.getText());
		String path = f.getParent();
		String fname = f.getName();
		fname = fname.substring(0, fname.lastIndexOf('.'));
		FileWriter writer = new FileWriter(path + File.separator + fname + "_ed.txt");
		for (String str : _outFileString) {
			writer.write(str + "\n");
		}
		writer.close();
	}

	private void setPreview() {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < _inputFileString.size(); i++) {
			s.append(_inputFileString.get(i) + "\n");
			//_filePreview.append(_inputFileString.get(i) + "\n");
		}
		_filePreview.append(s.toString());
	}

	private void updatePreview() {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < _outFileString.size(); i++) {
			s.append(_outFileString.get(i) + "\n");
			
		}
		_filePreview.append(s.toString());
		_filePreview.append("File conversion finished\n\n\n");
		_filePreview.append("Stored output at:    \n");
		_filePreview.append(new File(inputFilePath.getText()).getAbsolutePath() + "_ed.txt");
	}

	private void estimateDeviation() {
		_outFileString = new ArrayList<String>();
		_dx = 0.f;
		_dy = 0.f;
		_TVD = 0.f;
		float x = 0.f;
		float y = 0.f;

		addHeaders();
		for (int i = _nrHeaderLines; i < _inputFileString.size() - 1; i++) {
			String[] thisline = _inputFileString.get(i).split("\\s+");
			String[] nextline = _inputFileString.get(i + 1).split("\\s+");

			float md1 = Float.parseFloat(thisline[_mdCol - 1]);
			float md2 = Float.parseFloat(nextline[_mdCol - 1]);

			float incl1 = Float.parseFloat(thisline[_inclCol - 1]);
			float incl2 = Float.parseFloat(nextline[_inclCol - 1]);

			float azi1 = Float.parseFloat(thisline[_aziCol - 1]);
			float azi2 = Float.parseFloat(nextline[_aziCol - 1]);
			calcDxDy(md1, md2, incl1, incl2, azi1, azi2);
			x = _X + _dx;
			y = _Y + _dy;
			_outFileString.add(String.format("%.3f", md2) + "\t" + String.format("%.3f", _TVD) + "\t"
					+ String.format("%.3f", _TVDSS) + "\t" + String.format("%.3f", incl2) + "\t"
					+ String.format("%.3f", azi2) + "\t" + String.format("%.3f", _dx) + "\t"
					+ String.format("%.3f", _dy) + "\t" + String.format("%.3f", x) + "\t" + String.format("%.3f", y));
		}
	}

	private void calcDxDy(float md1, float md2, float incl1, float incl2, float azi1, float azi2) {
		float dMD = (md2 - md1) / 2;
		float sinI1 = (float) Math.sin(Math.toRadians(incl1));
		float sinI2 = (float) Math.sin(Math.toRadians(incl2));
		float sinA1 = (float) Math.sin(Math.toRadians(azi1));
		float sinA2 = (float) Math.sin(Math.toRadians(azi2));

		float cosI1 = (float) Math.cos(Math.toRadians(incl1));
		float cosI2 = (float) Math.cos(Math.toRadians(incl2));
		float cosA1 = (float) Math.cos(Math.toRadians(azi1));
		float cosA2 = (float) Math.cos(Math.toRadians(azi2));

		float cosI2mI1 = (float) Math.cos(Math.toRadians(incl2 - incl1));
		float cosA2mA1 = (float) Math.cos(Math.toRadians(azi2 - azi1));

		float beta = (float) Math.acos(cosI2mI1 - (sinI1 * sinI2 * (1 - cosA2mA1)));
		float RF = (float) ((2.f / beta) * Math.tan(beta / 2.f));
		RF = (Float.isNaN(RF)) ? 1.f : RF;
		_dx += dMD * (sinI1 * sinA1 + sinI2 * sinA2) * RF;
		_dy += dMD * (sinI1 * cosA1 + sinI2 * cosA2) * RF;
		_TVD += dMD * (cosI1 + cosI2) * RF;
		_TVDSS = _TVD - _KB;
		System.out.println(beta + "\t" + RF + "\t" + _dx + "\t" + _dy + "\t" + _TVD + "\t" + _TVDSS);
	}

	private void addHeaders() {
		for (int i = 0; i < _nrHeaderLines; i++) {
			_outFileString.add(_inputFileString.get(i));
		}
		String s = "0,000";
		_outFileString.add("MD\t" + "TVD\t" + "TVDSS\t" + "Incl.\t" + "Azi\t" + "DX\t" + "DY\t" + "X\t" + "Y");
		_outFileString.add(s + "\t" + s + "\t" + String.format("%.3f", -_KB) + "\t" + s + "\t" + s + "\t" + s + "\t" + s
				+ "\t" + String.format("%.3f", _X) + "\t" + String.format("%.3f", _Y));
	}

	public static String getDirOnly(String absPath) {
		File f = new File(absPath);
		return f.getParent();
	}

}
