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

import java.applet.Applet;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.net.URL;
import java.util.Vector;

import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import orientation.Gradient;
import orientation.GroupImage;
import orientation.OrientationParameters;
import orientation.OrientationService;
import orientation.StructureTensor;

public class DemoMeasure extends Applet {

	private int countMeasure	= 1;
	private ImagePlus imp;
	//private Vector<Measure> measures = new Vector<Measure>();
	private AppletCanvas canvas;
	private Table table;
	private int diameter = 30;

	/**
	*/
	public void init() {
		
		ByteProcessor bp = (ByteProcessor)((new ImagePlus("", loadImageFile("1.jpg"))).getProcessor());
		imp = new ImagePlus("OrientationJ Measure", bp);
		imp.show();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension window = imp.getWindow().getSize();
		if (window.width==0)
			return;
		int left = screen.width/4-window.width/2;
		int top = (screen.height-window.height)/4;
		if (top<0) top = 0;
		imp.getWindow().setLocation(left, top);

		table = new Table();
	 	canvas  = new AppletCanvas(imp, this);	 	
		imp.setWindow(new ImageWindow(imp, canvas));
		canvas.repaint();

	}

	/**
	*/
	public void measure(int x, int y, boolean update) {
	
		imp.setRoi(new OvalRoi(x-diameter/2, y-diameter/2, diameter, diameter));
		Roi roi = imp.getRoi();
		
		Rectangle rect = roi.getBounds();
		Polygon polygon = roi.getPolygon();
		ByteProcessor mask = (ByteProcessor)imp.getMask();
		ByteProcessor bpmask = null;
		int area = (int)Math.round(diameter*diameter*Math.PI/4.0);
		bpmask = new ByteProcessor(mask.getWidth(), mask.getHeight());
		for(int i=0; i<mask.getWidth(); i++)
		for(int j=0; j<mask.getHeight(); j++) {
			if (mask.getPixel(i, j) == 0)
				bpmask.putPixel(i, j, 255);
			else {
				bpmask.putPixel(i, j, countMeasure);
			}
		}
		
		ImageProcessor ip = imp.getProcessor().crop();
		OrientationParameters params = new OrientationParameters(OrientationService.ANALYSIS);
		GroupImage gim = new GroupImage(null, ip, params);
		(new Gradient(null, gim, params)).run();
		Measure measure = (new StructureTensor(null, gim, params)).measure(0, countMeasure, imp, area, rect, polygon, bpmask);
		Vector<Measure> measures = table.getMeasures();
		if (update)
		if (measures.size() > 0)
			measures.removeElementAt(measures.size()-1);
		measures.add(measure);
		table.add(measure);
		canvas.setMeasures(measures, table);
		canvas.repaint();
	}

	/**
	*/
	public int getDiameter() {
		return diameter;
	}
	
	/**
	*/
	public void incrementDiameter() {
 		 diameter += 10;
 		 if (diameter >= 90)
 		 	diameter = 90; 
	}
	
	/**
	*/
	public void decrementDiameter() {
 		 diameter -= 10;
 		 if (diameter < 10)
 		 	diameter = 10; 
	}
	
	/**
	*/
	public void reset() {
		Vector<Measure> measures = table.getMeasures();
		measures.removeAllElements();
		table.remove();
	}
	/**
	*/
	public void load(String filename) {
		reset();
		ByteProcessor bp = (ByteProcessor)((new ImagePlus("", loadImageFile(filename))).getProcessor());
		imp.setProcessor("", bp);
		imp.updateAndDraw();
	}
	
	/**
	*/
	public Image loadImageURL(String urltext) {
		Image image = null;
		try {
			URL url = new URL(urltext);
			MediaTracker mtracker = new MediaTracker(this);
			image = getImage(url);
			mtracker.addImage(image, 0);
			mtracker.waitForAll();
		}
		catch  (Exception e) {
			System.out.println("Exeception" + e);
		}
	 	return image;
	}
	
	/**
	*/
	public Image loadImageFile(String filename) {
		Image image=null;
		MediaTracker mtracker = new MediaTracker(this);
		image = getImage(this.getDocumentBase(), filename);
		mtracker.addImage(image, 0);
	   	try {
			mtracker.waitForAll();
	   	} 
	   	catch (InterruptedException ie) {
			System.out.println("Bad loading of an image.");
	   	}
	 	return image;
	}
	
}
