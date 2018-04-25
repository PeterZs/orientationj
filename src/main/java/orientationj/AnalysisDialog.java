//==============================================================================
//
// Project: Directional Image Analysis - OrientationJ plugin
// 
// Author: Daniel Sage
// 
// Organization: Biomedical Imaging Group (BIG)
// Ecole Polytechnique Federale de Lausanne (EPFL), Lausanne, Switzerland
//
// Information: 
// OrientationJ: http://bigwww.epfl.ch/demo/orientation/
// MonogenicJ: http://bigwww.epfl.ch/demo/monogenic/
//  
//
// Reference on methods and plugin
// Z. Püspöki, M. Storath, D. Sage, M. Unser
// "Transforms and Operators for Directional Bioimage Analysis: A Survey," 
// Advances in Anatomy, Embryology and Cell Biology, vol. 219, Focus on Bio-Image Informatics, 
// Springer International Publishing, ch. 3, pp. 69-93, May 21, 2016.
//
// Reference on applications
// E. Fonck, G.G. Feigl, J. Fasel, D. Sage, M. Unser, D.A. Ruefenacht, N. Stergiopulos 
// "Effect of Aging on Elastin Functionality in Human Cerebral Arteries,"
// Stroke, vol. 40, no. 7, pp. 2552-2556, July 2009.
//
// R. Rezakhaniha, A. Agianniotis, J.T.C. Schrauwen, A. Griffa, D. Sage, C.V.C. Bouten, F.N. van de Vosse, M. Unser, N. Stergiopulos
// "Experimental Investigation of Collagen Waviness and Orientation in the Arterial Adventitia Using Confocal Laser Scanning Microscopy,"
// Biomechanics and Modeling in Mechanobiology, vol. 11, no. 3-4, pp. 461-473, 2012.

// Conditions of use: You'll be free to use this software for research purposes,
// but you should not redistribute it without our consent. In addition, we 
// expect you to include a citation or acknowledgment whenever you present or 
// publish results that are based on it.
//
// History:
// - Updated (Daniel Sage, 24 January 2011)
// - Added the Vector field (Daniel Sage, 1 March 2017)
//
//==============================================================================

package orientationj;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import additionaluserinterface.GridPanel;
import additionaluserinterface.GridToolbar;
import additionaluserinterface.Settings;
import additionaluserinterface.SpinnerDouble;
import additionaluserinterface.SpinnerInteger;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.ImageWindow;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.plugin.frame.Recorder;
import ij.text.TextPanel;
import imageware.ImageWare;
import orientation.Cluster;
import orientation.Clusters;
import orientation.GroupImage;
import orientation.OrientationKMeans;
import orientation.OrientationParameters;
import orientation.OrientationProcess;
import orientation.OrientationService;

public class AnalysisDialog extends JDialog implements ActionListener, ChangeListener, WindowListener, Runnable {

	private Settings				settings				= new Settings("OrientationJ",
			IJ.getDirectory("plugins") + "OrientationJ.txt");
	private Thread					thread					= null;
	protected int					countRun				= 0;

	private String[]				gradientsOperators		= new String[] { "Cubic Spline Gradient (best)",
			"Finite Difference Gradient", "Fourier Gradient", "Riesz Filters", "Gaussian Gradient",
			"Finite Difference Hessian" };

	protected OrientationParameters	params;
	protected GroupImage			gim;
	private Vector<ImagePlus>		listImage				= new Vector<ImagePlus>();

	protected WalkBarOrientationJ	walk					= new WalkBarOrientationJ();
	protected JButton				bnRun					= new JButton("Run");
	private JButton					bnKMeans				= new JButton("K-means");
	private JButton					bnVectors				= new JButton("Vectors");
	private JButton					bnTableVectorField		= new JButton("Show Table");
	private JButton					bnShowVectorField		= new JButton("Show Overlay");

	private SpinnerDouble			spnST					= new SpinnerDouble(1, 0.01, 100, 1);
	private SpinnerDouble			spnLoG					= new SpinnerDouble(0, 0, 100, 0);

	private SpinnerDouble			spnHarrisK				= new SpinnerDouble(0.1, 0.01, 0.2, 0.01);
	private SpinnerInteger			spnHarrisL				= new SpinnerInteger(3, 1, 201, 1);
	private SpinnerDouble			spnHarrisMin			= new SpinnerDouble(10, -10000.0, 10000.0, 1);

	private SpinnerInteger			spnGridSize				= new SpinnerInteger(10, 4, 10000, 1);
	private SpinnerInteger			spnGridSizeVectorField	= new SpinnerInteger(10, 1, 10000, 1);
	private SpinnerInteger			spnNbClasses			= new SpinnerInteger(3, 1, 10000, 1);
	private SpinnerInteger			spnTransparency			= new SpinnerInteger(50, 0, 100, 5);
	private SpinnerDouble			spnScaleVectors			= new SpinnerDouble(1.0, 0, 10, 0.2);
	private SpinnerDouble			spnOrderCohere			= new SpinnerDouble(2, 1, 100, 1);
	private JComboBox				cmbLengthVectorField	= new JComboBox(
			new String[] { "Maximum", "~ Energy", "~ Coherency", "~ Energy x Coherency" });
	private ComboFeature			cmbHue					= new ComboFeature("Orientation");
	private ComboFeature			cmbSaturation			= new ComboFeature("Coherency");
	private ComboFeature			cmbBrightness			= new ComboFeature("Original-Image");

	private JLabel					lblHue					= new JLabel("Hue");
	private JLabel					lblSaturation			= new JLabel("Saturation");
	private JLabel					lblBrightness			= new JLabel("Brightness");

	private JLabel[]				lblFeature				= new JLabel[OrientationParameters.NB_FEATURES];
	protected ButtonPopup[]			bnMore					= new ButtonPopup[OrientationParameters.NB_FEATURES];
	protected JButton[]				bnShow					= new JButton[OrientationParameters.NB_FEATURES];
	protected JButton[]				bnHide					= new JButton[OrientationParameters.NB_FEATURES];
	private JButton					bnDetect				= new JButton("Detect Corners");

	private SpinnerDouble			spnMinEnergy			= new SpinnerDouble(0, 0, 180, 1);
	private SpinnerDouble			spnMinCoherency			= new SpinnerDouble(0, 0, 100, 1);
	private SpinnerDouble			spnScaleVectorField		= new SpinnerDouble(100, 0, 1000, 1);
	private JCheckBox				chkOrientation			= new JCheckBox("Orientation");
	private JCheckBox				chkEnergy				= new JCheckBox("Energy");
	private JCheckBox				chkCoherency			= new JCheckBox("Coherency");
	private JComboBox				cmbGradient				= new JComboBox(gradientsOperators);
	private VectorCanvas			vectorcanvas;

	/**
	 * Constructor
	 */
	public AnalysisDialog(OrientationService service) {
		super(new JFrame(), "OrientationJ ");
		String title = "OrientationJ ";
		this.params = new OrientationParameters(service);
		setTitle(title + params.getServiceName());
	}

	/**
	*/
	public void showDialog() {

		// Panel Tensor
		GridToolbar pnTensor = new GridToolbar("Structure Tensor");
		pnTensor.place(0, 0, new JLabel("Gaussian window"));
		pnTensor.place(0, 1, new JLabel("\u03C3"));
		pnTensor.place(0, 2, spnST);
		pnTensor.place(0, 3, new JLabel("[pix]"));
		pnTensor.place(1, 0, 4, 1,
				new JLabel("<html><small>&sigma; is roughly the thickness of the structure of interest</html>"));
		pnTensor.place(2, 0, 4, 1, cmbGradient);

		if (params.isServiceHarris()) {
			pnTensor.place(3, 0, new JLabel("Coefficient"));
			pnTensor.place(3, 1, new JLabel("\u03BA"));
			pnTensor.place(3, 2, spnHarrisK);
			pnTensor.place(3, 3, new JLabel("[0.1]"));
		}

		for (int k = 0; k < OrientationParameters.NB_FEATURES; k++) {
			lblFeature[k] = new JLabel(OrientationParameters.name[k]);
			bnHide[k] = new JButton(" Hide ");
			bnShow[k] = new JButton(" Show ");
			bnMore[k] = new ButtonPopup(k, new String[] { "Show after computation" }, this);
			bnShow[k].addActionListener(this);
			bnHide[k].addActionListener(this);
			bnMore[k].addActionListener(this);
		}

		// Panel Features
		GridToolbar pnFeatures = new GridToolbar("Features");
		for (int k = 0; k <= OrientationParameters.SURVEY; k++) {
			if (k == OrientationParameters.TENSOR_COHERENCY)
				bnMore[k] = new ButtonPopup(k, new String[] { "M/Set Epsilon ...", "S/", "Show after computation" },
						this);
			else if (k == OrientationParameters.SURVEY)
				bnMore[k] = new ButtonPopup(k,
						new String[] { "M/Set HSB or RGB mode ...", "S/", "Show after computation" }, this);
			else if (k == OrientationParameters.TENSOR_ORIENTATION)
				bnMore[k] = new ButtonPopup(k, new String[] { "Show in degrees", "Show after computation" }, this);
			boolean display = true;

			if (k < 2 & !params.isServiceAnalysis())
				display = false;
			if (k == OrientationParameters.SURVEY & params.isServiceHarris())
				display = false;
			if (k == OrientationParameters.HARRIS & !params.isServiceHarris())
				display = false;
			if (display) {
				pnFeatures.place(k, 1, lblFeature[k]);
				pnFeatures.place(k, 2, bnHide[k]);
				pnFeatures.place(k, 3, bnShow[k]);
				pnFeatures.place(k, 4, bnMore[k]);
			}
		}

		GridPanel pnMain = new GridPanel(false);
		pnMain.place(0, 0, pnTensor);
		pnMain.place(1, 0, pnFeatures);

		// Panel Distribution
		if (params.isServiceDistribution()) {
			GridToolbar pnDistribution = new GridToolbar("Selection", 0);
			GridToolbar pn1 = new GridToolbar(false);
			pn1.place(3, 1, new JLabel("Min. Coherency"));
			pn1.place(3, 2, spnMinCoherency);
			pn1.place(3, 3, new JLabel("%"));
			pn1.place(4, 1, new JLabel("Min. Energy"));
			pn1.place(4, 2, spnMinEnergy);
			pn1.place(4, 3, new JLabel("%"));

			GridToolbar pn2 = new GridToolbar(false);
			for (int k = OrientationParameters.SURVEY + 1; k < OrientationParameters.NB_FEATURES; k++) {
				pn2.place(k, 1, lblFeature[k]);
				pn2.place(k, 2, bnHide[k]);
				pn2.place(k, 3, bnShow[k]);
				pn2.place(k, 4, bnMore[k]);
			}
			pnDistribution.place(1, 0, pn1);
			pnDistribution.place(2, 0, pn2);
			pnMain.place(2, 0, pnDistribution);
		}

		// Panel VectorField
		if (params.isServiceVectorField()) {
			GridPanel pnVectors = new GridPanel("Vector Field");
			pnVectors.place(0, 0, new JLabel("Grid size"));
			pnVectors.place(0, 1, spnGridSizeVectorField);
			pnVectors.place(1, 0, new JLabel("Length vector"));
			pnVectors.place(1, 1, cmbLengthVectorField);
			pnVectors.place(2, 0, new JLabel("Scale vector (%)"));
			pnVectors.place(2, 1, spnScaleVectorField);
			pnVectors.place(6, 0, bnTableVectorField);
			pnVectors.place(6, 1, bnShowVectorField);
			bnTableVectorField.addActionListener(this);
			bnShowVectorField.addActionListener(this);
			cmbLengthVectorField.addActionListener(this);
			spnGridSizeVectorField.addChangeListener(this);
			pnMain.place(3, 0, pnVectors);
		}

		// Panel Directions
		if (params.isServiceDirections()) {
			GridPanel pnVectors = new GridPanel("Showing Vectors");
			pnVectors.place(0, 0, new JLabel("Grid size"));
			pnVectors.place(0, 1, spnGridSize);
			pnVectors.place(1, 0, new JLabel("Scale"));
			pnVectors.place(1, 1, spnScaleVectors);
			pnVectors.place(3, 0, chkOrientation);
			pnVectors.place(5, 0, chkCoherency);
			pnVectors.place(5, 1, spnOrderCohere);
			pnVectors.place(6, 0, chkEnergy);
			pnVectors.place(6, 1, bnVectors);
			chkOrientation.addActionListener(this);
			chkEnergy.addActionListener(this);
			chkCoherency.addActionListener(this);
			bnVectors.addActionListener(this);
			spnTransparency.addChangeListener(this);
			spnScaleVectors.addChangeListener(this);
			spnGridSize.addChangeListener(this);
			spnOrderCohere.addChangeListener(this);
			GridPanel pnKMeans = new GridPanel("Grouping Orientations");
			pnKMeans.place(2, 0, new JLabel("Classes"));
			pnKMeans.place(2, 1, spnNbClasses);
			pnKMeans.place(2, 2, bnKMeans);
			bnKMeans.addActionListener(this);
			pnMain.place(3, 0, pnVectors);
			pnMain.place(4, 0, pnKMeans);
		}

		// Panel Color
		GridToolbar pnColor = new GridToolbar("Color survey");
		pnColor.place(1, 0, lblHue);
		pnColor.place(1, 1, cmbHue);
		pnColor.place(2, 0, lblSaturation);
		pnColor.place(2, 1, cmbSaturation);
		pnColor.place(3, 0, lblBrightness);
		pnColor.place(3, 1, cmbBrightness);

		// Panel Harris
		if (params.isServiceHarris()) {
			GridToolbar pnHarris = new GridToolbar("Harris Corner Detection");
			pnHarris.place(2, 0, new JLabel("Window size"));
			pnHarris.place(2, 2, spnHarrisL);
			pnHarris.place(3, 0, new JLabel("Min. level"));
			pnHarris.place(3, 2, spnHarrisMin);
			pnHarris.place(4, 0, 2, 1, bnDetect);
			pnMain.place(4, 0, pnHarris);
			bnDetect.addActionListener(this);
		} else {
			pnMain.place(5, 0, pnColor);
		}

		pnMain.place(6, 0, bnRun);
		pnMain.place(7, 0, walk);

		GridPanel pn = new GridPanel(false, 10);
		pn.place(0, 0, pnMain);

		// Listener
		walk.getButtonClose().addActionListener(this);
		bnRun.addActionListener(this);

		// Finalize
		addWindowListener(this);
		getContentPane().add(pn);
		pack();
		setResizable(false);
		GUI.center(this);
		setVisible(true);

		settings.record("spnHarrisK", spnHarrisK, "0.1");
		settings.record("spnHarrisL", spnHarrisL, "3");
		settings.record("spnHarrisMin", spnHarrisMin, "10");
		settings.record("spnLoG", spnLoG, "0");
		settings.record("spnTensor", spnST, "1");
		settings.record("Color_Hue", cmbHue, "Orientation");
		settings.record("Color_Staturation", cmbSaturation, "Coherency");
		settings.record("Color_Brigthness", cmbBrightness, "Original-Image");
		settings.record("spnMinCoherency", spnMinCoherency, "70.0");
		settings.record("spnMinEnergy", spnMinEnergy, "10.0");
		settings.record("spnGridSize", spnGridSize, "10");
		settings.record("spnNbClasses", spnNbClasses, "3");
		settings.record("spnTransparency", spnTransparency, "50");
		settings.record("spnScaleVectors", spnScaleVectors, "50");
		settings.record("spnOrderCohere", spnOrderCohere, "1");
		settings.record("chkOrientation", chkOrientation, true);
		settings.record("chkEnergy", chkEnergy, false);
		settings.record("chkCoherency", chkCoherency, false);
		settings.record("cmbGradient", cmbGradient, gradientsOperators[0]);
		settings.record("spnGridSizeVectorField", spnGridSizeVectorField, "10");
		settings.record("cmbLengthVectorField", cmbLengthVectorField, (String)cmbLengthVectorField.getItemAt(0));
		settings.record("spnScaleVectorField", spnScaleVectorField, "100");
		settings.loadRecordedItems();
		params.load(settings);
		updateInterface();
	}

	/**
	 * Implements the actionPerformed for the ActionListener.
	 */
	public synchronized void actionPerformed(ActionEvent e) {

		getParameters();

		Object source = e.getSource();
		if (e.getSource() == walk.getButtonClose()) {
			settings.storeRecordedItems();
			params.store(settings);
			dispose();
		}
		for (int k = 0; k < OrientationParameters.NB_FEATURES; k++) {
			if (source == bnShow[k])
				actionPerformedButtonPopup(bnMore[k], "Show", k, true);
			if (source == bnHide[k])
				actionPerformedButtonPopup(bnMore[k], "Hide", k, true);
		}

		if (e.getSource() == bnDetect) {
			params.harrisL = spnHarrisL.get();
			params.harrisMin = spnHarrisMin.get();
			detectCorners(gim, params.harrisMin, params.harrisL);
		} else if (e.getSource() == bnVectors) {
			ImageWare c = gim.selectChannel(params.featureBri);
			ImagePlus imp = new ImagePlus("Vectors on " + params.featureBri, c.buildImageStack());
			Clusters[] clusters = computerVectors(spnGridSize.get());
			vectorcanvas = new VectorCanvas(imp, clusters, spnScaleVectors.get(), spnTransparency.get(),
					spnOrderCohere.get());
			vectorcanvas.setFeatures(chkOrientation.isSelected(), chkEnergy.isSelected(), chkCoherency.isSelected());
		} else if (e.getSource() == bnTableVectorField) {
			tableVectorField();
		} else if (e.getSource() == bnShowVectorField) {
			showVectorField();
		}

		else if (e.getSource() == chkOrientation || e.getSource() == chkEnergy || e.getSource() == chkCoherency) {
			if (vectorcanvas != null)
				vectorcanvas.setFeatures(chkOrientation.isSelected(), chkEnergy.isSelected(),
						chkCoherency.isSelected());
		} else if (e.getSource() == bnKMeans) {
			if (gim == null)
				return;
			if (gim.orientation == null)
				return;
			OrientationKMeans kmeans = new OrientationKMeans();
			ImageWare out = kmeans.run(gim.orientation, spnNbClasses.get(), 1000);
			out.show("KMeans " + spnNbClasses.get());
		} else if (e.getSource() == bnRun) {
			if (thread != null)
				return;
			thread = new Thread(this);
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
		updateInterface();
	}

	/**
	*/
	public void actionPerformedButtonPopup(ButtonPopup source, String action, int feature, boolean state) {
		if (action.equals("Show")) {
			if (gim != null) {
				if (feature == OrientationParameters.DIST_MASK || feature == OrientationParameters.DIST_ORIENTATION
						|| feature == OrientationParameters.DIST_COLOR || feature == OrientationParameters.DIST_HISTO) {
					params.minEnergy = spnMinEnergy.get();
					params.minCoherency = spnMinCoherency.get();
					if (gim != null) {
						OrientationProcess process = new OrientationProcess(gim, params);
						float histo[] = process.distribution();
						if (feature == OrientationParameters.DIST_HISTO) {
							ImagePlus imp = process.plotDistribution(countRun, histo);
							if (imp != null)
								listImage.add(imp);
						}
					}
				}
				ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature],
						params);
				if (imp != null)
					listImage.add(imp);
			}
		} else if (action.equals("Hide")) {
			gim.hideFeature(lblFeature[feature].getText(), listImage, countRun);
		} else if (action.equals("Set Epsilon ...")) {
			new CoherencyDialog(params);
		} else if (action.equals("Set HSB or RGB mode ...")) {
			new ColorSurveyDialog(params);
		} else if (action.equals("Set showing options ...")) {
			new ShowingOptionsDialog(params);
		} else if (action.equals("Show after computation")) {
			params.view[source.getFeature()] = state;
		} else if (action.equals("Show in degrees")) {
			params.radian[source.getFeature()] = !state;
		}
		updateInterface();
	}

	/**
	 * 
	 */
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == spnTransparency || e.getSource() == spnScaleVectors || e.getSource() == spnOrderCohere) {
			if (vectorcanvas != null)
				vectorcanvas.setSettings(spnScaleVectors.get(), spnTransparency.get(), spnOrderCohere.get());
		}
		if (e.getSource() == spnGridSize) {
			if (vectorcanvas != null) {
				vectorcanvas.setClusters(computerVectors(spnGridSize.get()));
			}
		}
	}

	public void getParameters() {
		params.featureHue = (String) cmbHue.getSelectedItem();
		params.featureSat = (String) cmbSaturation.getSelectedItem();
		params.featureBri = (String) cmbBrightness.getSelectedItem();
		params.sigmaST = spnST.get();
		params.sigmaLoG = spnLoG.get();
		params.harrisK = spnHarrisK.get();
		params.minCoherency = spnMinCoherency.get();
		params.minEnergy = spnMinEnergy.get();
		params.gradient = cmbGradient.getSelectedIndex();
	}

	/**
	 * Implements the run for the Runnable.
	 */
	public void run() {
		ImageWare source = GroupImage.getCurrentImage();
		if (source == null) {
			thread = null;
			return;
		}
		getParameters();
		walk.reset();
		recordMacroParameters();
		Cursor cursor = getCursor();
		setCursor(new Cursor(Cursor.WAIT_CURSOR));
		OrientationProcess process = new OrientationProcess(walk, source, params);
		process.start();
		while (process.isAlive()) {
		}

		gim = process.getGroupImage();

		Vector<ImagePlus> list = process.show(++countRun);
		listImage.addAll(list);

		if (params.isServiceDistribution()) {
			float[] histo = process.distribution();
			Vector<ImagePlus> listD = process.showDistribution(countRun, histo);
			listImage.addAll(listD);
			process.saveDistribution(params, histo);
		}
		setCursor(cursor);

		if (params.isServiceDirections()) {
			Clusters clusters[] = computerVectors(spnGridSize.get());
			ImagePlus imps = gim.getImagePlus("Vectors");
			imps.show();
			vectorcanvas = new VectorCanvas(imps, clusters, spnScaleVectors.get(), spnTransparency.get(),
					spnOrderCohere.get());
		}

		walk.finish();
		updateInterface();
		thread = null;
	}

	/**
	 * 
	 */
	private Clusters[] computerVectors(int size) {

		int nz = gim.energy.getSizeZ();
		Clusters[] clusters = new Clusters[nz];
		int xstart = (gim.nx - (gim.nx / size) * size) / 2;
		int ystart = (gim.ny - (gim.ny / size) * size) / 2;
		double max = gim.energy.getMaximum();

		if (max <= 0)
			return null;
		int size2 = size * size;
		for (int z = 0; z < nz; z++) {
			clusters[z] = new Clusters();
			for (int y = ystart; y < gim.ny; y += size)
				for (int x = xstart; x < gim.nx; x += size) {
					double dx = 0.0;
					double dy = 0.0;
					double coherencies = 0.0;
					double energies = 0.0;
					for (int k = 0; k < size; k++)
						for (int l = 0; l < size; l++) {
							double angle = gim.orientation.getPixel(x, y, z);
							double coh = gim.coherency.getPixel(x, y, z);
							dx += Math.cos(angle);
							dy += Math.sin(angle);
							coherencies += coh;
							energies += gim.energy.getPixel(x, y, z);
						}
					dx /= size2;
					dy /= size2;
					coherencies /= size2;
					energies /= size2;
					if (energies > 0)
						if (coherencies > 0)
							clusters[z].add(new Cluster(x, y, size, size, dx, dy, coherencies, (energies / max)));
				}
		}
		return clusters;
	}

	/**
	 * Implements the actionPerformed for the ActionListener.
	 */
	public void updateInterface() {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Enabled the hide buttons

				int flagHide[] = new int[OrientationParameters.NB_FEATURES];
				int list[] = WindowManager.getIDList();
				if (list != null) {
					for (int i = 0; i < list.length; i++) {
						ImagePlus imp = WindowManager.getImage(list[i]);
						for (int feature = 0; feature < OrientationParameters.NB_FEATURES; feature++)
							for (int k = 0; k <= countRun; k++) {
								if (imp.getTitle().equals(OrientationParameters.name[feature] + "-" + k)) {
									flagHide[feature]++;
								}
							}
					}
					for (int feature = 0; feature < OrientationParameters.NB_FEATURES; feature++)
						bnHide[feature].setEnabled(flagHide[feature] > 0);
				}

				// Enable the show button
				if (gim != null) {
					bnShow[OrientationParameters.GRADIENT_HORIZONTAL].setEnabled(gim.gx != null || gim.hxx != null);
					bnShow[OrientationParameters.GRADIENT_VERTICAL].setEnabled(gim.gy != null || gim.hyy != null);
					bnShow[OrientationParameters.TENSOR_ORIENTATION].setEnabled(gim.orientation != null);
					bnShow[OrientationParameters.TENSOR_COHERENCY].setEnabled(gim.coherency != null);
					bnShow[OrientationParameters.TENSOR_ENERGY].setEnabled(gim.energy != null);
					bnShow[OrientationParameters.HARRIS].setEnabled(gim.harris != null);
					bnShow[OrientationParameters.SURVEY].setEnabled(gim != null);
					bnShow[OrientationParameters.DIST_HISTO].setEnabled(gim != null);
					bnShow[OrientationParameters.DIST_MASK].setEnabled(gim != null);
					bnShow[OrientationParameters.DIST_ORIENTATION].setEnabled(gim != null);
					bnShow[OrientationParameters.DIST_COLOR].setEnabled(gim != null);
					bnDetect.setEnabled(gim.harris != null);
					bnKMeans.setEnabled(gim.orientation != null);
					bnVectors.setEnabled(gim.orientation != null);
				} else {
					bnDetect.setEnabled(false);
					bnKMeans.setEnabled(false);
					bnVectors.setEnabled(false);
					for (int k = 0; k < OrientationParameters.NB_FEATURES; k++)
						bnShow[k].setEnabled(false);
				}

				// Enabled the color survey channels
				if (!params.hsb) {
					lblHue.setText("Red");
					lblSaturation.setText("Green");
					lblBrightness.setText("Blue");
				} else {
					lblHue.setText("Hue");
					lblSaturation.setText("Saturation");
					lblBrightness.setText("Brightness");
				}
				if (params.gradient == OrientationParameters.HESSIAN) {
					if (lblFeature[0] != null)
						lblFeature[0].setText("Hessian-XX");
					if (lblFeature[1] != null)
						lblFeature[1].setText("Hessian-YY");
				} else {
					if (lblFeature[0] != null)
						lblFeature[0].setText("Gradient-X");
					if (lblFeature[1] != null)
						lblFeature[1].setText("Gradient-Y");
				}
			}
		});

	}

	/**
	*/
	private void recordMacroParameters() {
		if (!Recorder.record)
			return;
		String options = "";
		options += "log=" + spnLoG.get() + " ";
		options += "tensor=" + spnST.get() + " ";
		options += "gradient=" + cmbGradient.getSelectedIndex() + " ";

		String plugin = "OrientationJ " + params.getServiceName();

		if (params.isServiceDistribution()) {
			options += "min-coherency=" + spnMinCoherency.get() + " ";
			options += "min-energy=" + spnMinEnergy.get() + " ";
		}
		for (int i = 0; i < OrientationParameters.NB_FEATURES; i++)
			if (params.view[i])
				options += OrientationParameters.name[i].toLowerCase() + "=on ";

		if (params.isServiceHarris()) {
			options += "harrisk=" + spnHarrisK.get() + " ";
			Recorder.record("run", "OrientationJ Corner Harris", options);
		} else {
			options += "hue=" + cmbHue.getSelectedItem() + " ";
			options += "sat=" + cmbSaturation.getSelectedItem() + " ";
			options += "bri=" + cmbBrightness.getSelectedItem() + " ";
			Recorder.record("run", plugin, options);
		}
	}

	/**
	*/
	public OrientationParameters getSettingParameters() {
		return params;
	}

	/**
	 * Implements the methods for the WindowListener.
	 */
	public void windowActivated(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		dispose();
	}

	/**
	 * 
	 */
	private void tableVectorField() {
		CustomizedTable table = new CustomizedTable(
				new String[] { "X", "Y", "Frame", "DX", "DY", "Orientation", "Coherency", "Energy" }, true);
		int size = spnGridSizeVectorField.get();
		Clusters[] clusters = computerVectors(size);
		for (int t = 0; t < clusters.length; t++) {
			for (Cluster c : clusters[t]) {
				double a = Math.atan2(c.dy, c.dy);
				table.append(new String[] { "" + (c.x + size / 2), "" + (c.y + size / 2), "" + t, "" + (-c.dx),
						"" + c.dy, "" + a, "" + c.coherency, "" + c.energy });
			}
		}
		table.show("Table Vector Field", 400, 400);
	}

	/**
	 * 
	 */
	private void showVectorField() {
		int len = cmbLengthVectorField.getSelectedIndex();
		int size = spnGridSizeVectorField.get();
		double scale = spnScaleVectorField.get();
		ImageWare ch = gim.selectChannel(params.featureBri);
		ImagePlus imp = new ImagePlus("Vectors on " + params.featureBri, ch.buildImageStack());
		Clusters[] clusters = computerVectors(size);
		double r = scale / 100.0 * size * 0.5;
		Overlay overlay = new Overlay();
		for (int t = 0; t < clusters.length; t++) {
			for (Cluster c : clusters[t]) {
				double a = r;
				if (len == 1)
					a = r * c.energy;
				else if (len == 2)
					a = r * c.coherency;
				else if (len == 3)
					a = r * c.energy * c.coherency;

				int x1 = (int) Math.round(c.x + a * c.dx);
				int y1 = (int) Math.round(c.y - a * c.dy);
				int x2 = (int) Math.round(c.x - a * c.dx);
				int y2 = (int) Math.round(c.y + a * c.dy);
				Roi roi = new Line(x1, y1, x2, y2);
				roi.setPosition(t + 1);
				overlay.add(roi);
			}
		}
		imp.setOverlay(overlay);
		imp.show();

	}

	/**
	*/
	private void detectCorners(GroupImage gim, double min, int L) {
		if (L <= 0)
			L = 0;
		int size = 2 * L + 1;
		JFrame frame = new JFrame("Corners");
		TextPanel table = new TextPanel();
		table.setColumnHeadings("X\tY\tframe");

		Vector<PointTime> corners = new Vector<PointTime>();

		double block[][] = new double[size][size];
		boolean flag = true;

		double v;
		for (int t = 0; t < gim.nt; t++)
			for (int y = 0; y < gim.ny; y++)
				for (int x = 0; x < gim.nx; x++) {
					if ((v = gim.harris.getPixel(x, y, t)) > min) {
						flag = true;
						gim.harris.getNeighborhoodXY(x, y, t, block, ImageWare.MIRROR);
						for (int k = 0; k < size; k++)
							for (int l = 0; l < size; l++) {
								if (v < block[k][l]) {
									flag = false;
								}
							}
						if (flag)
							corners.add(new PointTime(x, y, t));
					}
				}

		for (int i = 0; i < corners.size(); i++) {
			PointTime pt = (PointTime) corners.get(i);
			table.appendLine("" + pt.x + "\t" + pt.y + "\t" + pt.t);
		}
		table.setPreferredSize(new Dimension(200, 200));
		frame.add(table);
		frame.pack();
		frame.setVisible(true);

		ImagePlus impc = gim.getImagePlus("Corners");
		impc.show();
		CornerCanvas canvas = new CornerCanvas(impc, corners);
		if (impc.getStackSize() > 1)
			impc.setWindow(new StackWindow(impc, canvas));
		else
			impc.setWindow(new ImageWindow(impc, canvas));
	}
}
