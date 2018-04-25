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

package orientation;

import additionaluserinterface.Settings;
import ij.Macro;

public class OrientationParameters {

	final public static int		MODE_ANALYSIS			= 0;
	final public static int		MODE_HARRIS				= 1;
	final public static int		MODE_DISTRIBUTION		= 2;
	final public static int		MODE_DIRECTIONS			= 3;

	private OrientationService	service					= OrientationService.ANALYSIS;

	final public static int		GRADIENT_CUBIC_SPLINE	= 0;
	final public static int		GRADIENT_FINITE_DIFF	= 1;
	final public static int		GRADIENT_FOURIER_DOMAIN	= 2;
	final public static int		GRADIENT_RIESZ			= 3;
	final public static int		GRADIENT_GAUSSIAN		= 4;
	final public static int		HESSIAN					= 5;

	final public static int		GRADIENT_HORIZONTAL		= 0;
	final public static int		GRADIENT_VERTICAL		= 1;
	final public static int		TENSOR_ENERGY			= 2;
	final public static int		TENSOR_ORIENTATION		= 3;
	final public static int		TENSOR_COHERENCY			= 4;
	final public static int		HARRIS					= 5;
	final public static int		SURVEY					= 6;
	final public static int		DIST_MASK				= 7;
	final public static int		DIST_ORIENTATION			= 8;
	final public static int		DIST_COLOR				= 9;
	final public static int		DIST_HISTO				= 10;
	final public static int		NB_FEATURES				= 11;

	public int					gradient					= GRADIENT_CUBIC_SPLINE;

	public double				sigmaLoG					= 0;
	public double				sigmaST					= 2;
	public double				epsilon					= 0.01;

	public double				minCoherency				= 0;
	public double				minEnergy				= 0;

	public double				harrisK					= 0.05;
	public int					harrisL					= 2;
	public double				harrisMin				= 10.0;

	public boolean				hsb						= true;
	public boolean				view[]					= new boolean[NB_FEATURES];
	public boolean				radian[]					= new boolean[NB_FEATURES];

	public String				pathSaveDistribution	= "";
	final static public String	name[]					= { "Gradient-X", "Gradient-Y", "Energy", "Orientation",
			"Coherency", "Harris-index", "Color-survey", "S-Mask", "S-Orientation", "S-Color-survey",
			"S-Distribution" };

	// Parameters for the measurement tools
	public int					colorEllipseR			= 255;
	public int					colorEllipseG			= 0;
	public int					colorEllipseB			= 0;
	public int					colorEllipseOpacity		= 100;
	public int					colorAreaR				= 128;
	public int					colorAreaG				= 128;
	public int					colorAreaB				= 0;
	public int					colorAreaOpacity			= 50;
	public double				colorEllipseThickness	= 0.5;

	public String				featureHue				= "Orientation";
	public String				featureSat				= "Coherency";
	public String				featureBri				= "Image Original";

	public OrientationParameters(OrientationService service) {
		this.service = service;
	}

	public String getServiceName() {
		if (isServiceHarris())
			return "Corner Harris";
		else if (isServiceAnalysis())
			return "Analysis";
		else if (isServiceDistribution())
			return "Distribution";
		else if (isServiceDirections())
			return "Direction";
		else if (isServiceVectorField())
			return "Vector Field";
		return "Untitled Service";
	}

	public boolean isServiceAnalysis() {
		return service == OrientationService.ANALYSIS;
	}

	public boolean isServiceDistribution() {
		return service == OrientationService.DISTRIBUTION;
	}

	public boolean isServiceDirections() {
		return service == OrientationService.DIRECTIONS;
	}

	public boolean isServiceVectorField() {
		return service == OrientationService.VECTORFIELD;
	}

	public boolean isServiceHarris() {
		return service == OrientationService.HARRIS;
	}

	public void load(Settings settings) {
		epsilon = settings.loadValue("epsilon", epsilon);
		hsb = settings.loadValue("hsb", hsb);
		for (int k = 0; k < OrientationParameters.NB_FEATURES; k++) {
			view[k] = settings.loadValue("view_" + name[k],
					((k == SURVEY || k == HARRIS || k == DIST_HISTO) ? true : false));
			radian[k] = settings.loadValue("radian_" + name[k], true);
		}
		colorEllipseR = settings.loadValue("Measure_colorEllipseR", colorEllipseR);
		colorEllipseG = settings.loadValue("Measure_colorEllipseG", colorEllipseG);
		colorEllipseB = settings.loadValue("Measure_colorEllipseB", colorEllipseB);
		colorEllipseOpacity = settings.loadValue("Measure_colorEllipseOpacity", colorEllipseOpacity);
		colorEllipseThickness = settings.loadValue("Measure_colorEllipseThickness", colorEllipseThickness);
		colorAreaR = settings.loadValue("Measure_colorAreaR", colorAreaR);
		colorAreaG = settings.loadValue("Measure_colorAreaG", colorAreaG);
		colorAreaB = settings.loadValue("Measure_colorAreaB", colorAreaB);
		colorAreaOpacity = settings.loadValue("Measure_colorAreaOpacity", colorAreaOpacity);
	}

	public void store(Settings settings) {
		settings.storeValue("epsilon", epsilon);
		settings.storeValue("hsb", hsb);
		for (int k = 0; k < OrientationParameters.NB_FEATURES; k++) {
			settings.storeValue("view_" + name[k], view[k]);
			settings.storeValue("radian_" + name[k], radian[k]);
		}
		settings.storeValue("Measure_colorEllipseR", colorEllipseR);
		settings.storeValue("Measure_colorEllipseG", colorEllipseG);
		settings.storeValue("Measure_colorEllipseB", colorEllipseB);
		settings.storeValue("Measure_colorEllipseOpacity", colorEllipseOpacity);
		settings.storeValue("Measure_colorEllipseThickness", colorEllipseThickness);
		settings.storeValue("Measure_colorAreaR", colorAreaR);
		settings.storeValue("Measure_colorAreaG", colorAreaG);
		settings.storeValue("Measure_colorAreaB", colorAreaB);
		settings.storeValue("Measure_colorAreaOpacity", colorAreaOpacity);
	}

	public void getMacroParameters(String options) {
		sigmaLoG = Double.parseDouble(Macro.getValue(options, "log", "1"));
		sigmaST = Double.parseDouble(Macro.getValue(options, "tensor", "1"));
		minCoherency = Double.parseDouble(Macro.getValue(options, "min-coherency", "0"));
		minEnergy = Double.parseDouble(Macro.getValue(options, "min-energy", "0"));
		gradient = Integer.parseInt(Macro.getValue(options, "gradient", "0"));
		for (int i = 0; i < OrientationParameters.NB_FEATURES; i++) {
			view[i] = Macro.getValue(options, OrientationParameters.name[i], "off").equals("on");
		}
		if (isServiceHarris()) {
			harrisK = Double.parseDouble(Macro.getValue(options, "harrisk", "0.1"));
		} else {
			featureHue = Macro.getValue(options, "hue", "Orientation");
			featureSat = Macro.getValue(options, "sat", "Coherency");
			featureBri = Macro.getValue(options, "bri", "Constant");
		}
		pathSaveDistribution = Macro.getValue(options, "filename", "");
	}

}