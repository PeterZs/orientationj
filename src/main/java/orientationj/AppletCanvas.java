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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.OvalRoi;
import imageware.FMath;

public class AppletCanvas extends ImageCanvas implements MouseListener {

	private Table table;
	private ImagePlus imp;
	private int nx;
	private int ny;
	private Vector<Measure> measures;
	private double LIMIT_ELLIPSE = 0.99;
	private double ENERGY_MAX = 4000;
	private DemoMeasure applet;
	private Color bgColor = new Color(200, 200, 0, 80);
	private Color areaColor = new Color(100, 200, 0, 25);
	private Color elliColor = new Color(255, 0, 0, 250);
	private boolean start = true;
	int xprev = -1;
	int yprev = -1;
	int imageSelected = 1;
	int imageUsed = 1;
	int featureSelected = 0;
	
	public AppletCanvas(ImagePlus imp, DemoMeasure applet) {
   		super(imp);
		this.imp = imp;
		nx = imp.getWidth();
		ny = imp.getHeight();
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.applet = applet;
		repaint();
	}

	public void setMeasures(Vector<Measure> measures, Table table) {
		this.table = table;
		this.measures = measures;
	}

	public void paint(Graphics g) {
		super.paint(g);
		g.setColor(bgColor);
		g.fillRect(0, ny-20, nx, 20);
		g.fillRect(0, 0, nx, 20);
		drawString(g, "(c) 2008 Biomedical Imaging Group (BIG), EPFL, Switzerland.", 10, ny-6, Color.orange, 12);
		if (imageUsed == 1)
			drawString(g, "Elastin fibers in artery. Courtesy of E. Fonck, LHTC, EPFL.", 380, ny-6, Color.orange, 12);
		if (imageUsed == 2)
			drawString(g, "Chorio-allantoic membrane. Courtesy of F. Piffaretti, LPAS, EPFL.", 380, ny-6, Color.orange, 12);
		if (imageUsed == 3)
			drawString(g, "Synthetic image.", 380, ny-6, Color.orange, 12);
		
		if (start == true) {
			drawStartingText(g);
		}
		if (start == false) {
			if (measures == null)
				drawInvitationText(g);
			else if (measures.size() == 0)
				drawInvitationText(g);
		}
		
		if (table != null) {
			g.setColor(Color.cyan);
			g.drawLine(10 + featureSelected*150, 20, 130 + featureSelected*150, 20);
			g.drawLine(10 + featureSelected*150, 19, 130 + featureSelected*150, 19);
			for(int i=0; i<measures.size(); i++) {
				Measure measure = (Measure)measures.get(i);
				if (measure != null) {
					int xc = measure.px;
					int yc = measure.py;
					
					if (table.isRectangle(i)) {
						g.setColor(areaColor);
						g.fillPolygon(measure.polygon);
						double ore = Math.toDegrees(measure.orientation) ; //(measure.orientation<0.01 ? 0 : Math.toDegrees(measure.orientation));
						double coh = (measure.coherency*100<0.01?0:measure.coherency*100);
						double ene = (measure.energy<0.01?0:measure.energy);
						int radius = (int)Math.sqrt(measure.area / Math.PI)/2;
						if (featureSelected == 0)
							drawString(g, ""+ IJ.d2s(ore, 1), xc+radius, yc, Color.green, 14);
						if (featureSelected == 1)
							drawString(g, ""+ IJ.d2s(coh, 1), xc+radius, yc, Color.green, 14);
						if (featureSelected == 2)
							drawString(g, ""+ IJ.d2s(ene, 1), xc+radius, yc, Color.green, 14);
						if (i==measures.size()-1) {
							drawString(g, "Orientation: "+ IJ.d2s(ore, 1) + " deg.", 10, 14, Color.orange, 14);
							drawString(g, "Coherency: "+ IJ.d2s(coh, 1) + "%", 160, 14, Color.orange, 14);
							drawString(g, "Energy: "+ IJ.d2s(ene, 1), 310, 14, Color.orange, 14);
							drawString(g, "Diameter  [-]", 480, 14, Color.cyan, 14);
							drawString(g, "" + applet.getDiameter(), 572, 14, Color.cyan, 14);
							drawString(g, "[+]", 600, 14, Color.cyan, 14);
							drawString(g, "Restart", 700, 14, Color.cyan, 14);
						}
					}
					if (table.isEllipse(i)) {
						double k1 = (1.0 - LIMIT_ELLIPSE*measure.coherency);
						double k2 = (1.0 + LIMIT_ELLIPSE*measure.coherency);
						double norm = Math.sqrt(Math.sqrt(measure.energy/ENERGY_MAX));
						double a = Math.sqrt(norm * (measure.area/(2.0*Math.PI)) * (k2/k1));
						double b = Math.sqrt(norm * (measure.area/(2.0*Math.PI)) * (k1/k2));
						Polygon ellipse = new Polygon();
						double cosa = Math.cos(measure.orientation);
						double sina = Math.sin(measure.orientation);
						double astep = Math.PI/36.0;
						for(double an=0; an<=2*Math.PI; an+=astep) {
							double xe = a * Math.cos(an);
							double ye = b * Math.sin(an);
							int x = FMath.round((measure.px + cosa * xe + sina * ye));
							int y = FMath.round((measure.py - sina * xe + cosa * ye));
							ellipse.addPoint(x, y);
						}
						g.setColor(elliColor);
						g.drawPolygon(ellipse);
					}
				}
			}
		}
	}

	/**
	*/
	private void drawStartingText(Graphics g) {
		
		g.setColor(bgColor);
		drawString(g, "OrientationJ  [24/01/2008] - Java Version:"+ System.getProperty("java.version") + "          http://bigwww.epfl.ch/demo/orientation/", 10, 14, Color.orange, 14);
		
		int pos;
		pos = 200;
		g.setColor(bgColor);
		g.fillRect(110, pos, nx-420, 18);
		drawString(g, "Choose a source image", 120, pos+14, Color.cyan, 14);
		
		pos = 220;
		g.setColor(bgColor);
		g.fillRect(110, pos, nx-420, 18);
		drawString(g, "1. Elastin fibers in cerebral artery.", 120, pos+14, (imageSelected==1 ? Color.red:Color.orange), 14);

		pos = 240;
		g.setColor(bgColor);
		g.fillRect(110, pos, nx-420, 18);
		drawString(g, "2. Chorio-allantoic membrane.", 120, pos+14, (imageSelected==2 ? Color.red:Color.orange), 14);

		pos = 260;
		g.setColor(bgColor);
		g.fillRect(110, pos, nx-420, 18);
		drawString(g, "3. Synthetic image.", 120, pos+14,(imageSelected==3 ? Color.red:Color.orange), 14);
		
		start = true;
	}

	/**
	*/
	private void drawInvitationText(Graphics g) {
		g.setColor(bgColor);
		drawString(g, "OrientationJ  [24/01/2008] - Java Version:"+ System.getProperty("java.version") + "          http://bigwww.epfl.ch/demo/orientation/", 10, 14, Color.orange, 14);
		drawString(g, "Drag the mouse or click to make measurements.", 220, 250, Color.cyan, 13);
		start = false;
	}
		
	/**
	*/
	private void drawString(Graphics g, String msg, int x, int y, Color c, int size) {
		g.setFont(new Font("Arial", Font.PLAIN, size));
		g.setColor(new Color(33, 33, 33));
		g.drawString(msg, x+1, y+1);
		g.setColor(c);
		g.drawString(msg, x, y);
	}
	
	/**
	*/
	public void mouseClicked(MouseEvent e)  {
		int y = e.getY();
		int x = e.getX();
		if (y == yprev && x == xprev)
			return;
		if (start) 
		if (y > 220) {
			if (y < 240) {
				applet.load("1.jpg");
				imageUsed = 1;
			}
			else if (y < 260) {
				applet.load("2.jpg");
				imageUsed = 2;
			}
			else if (y < 280) {
				applet.load("3.jpg");
				imageUsed = 3;
			}
			start = false;
			xprev = x;
			yprev = y;
			repaint();
			return;
		}
		
		if(!start)
		if (y < 20) {
			if (x > 700) {
				applet.reset();
				start = true;
			}
			else if (x > 620) {
			}
			else if (x > 590) {
				applet.incrementDiameter();
			}
			else if (x > 570) {
			}	
			else if (x > 550) {
				applet.decrementDiameter();
			}
			else if (x > 430) {
			}
			else if (x > 310) {
				featureSelected = 2;
			}
			else if (x > 280) {
			}
			else if (x > 160) {
				featureSelected = 1;
			}
			else if (x > 130) {
			}
			else if (x > 10) {
				featureSelected = 0;
			}
			
			xprev = -1;
			yprev = -1;
			repaint();
			return;
		}
		
		if(!start)
		if (y < ny-20) {
			applet.measure(e.getX(), e.getY(), false);
			repaint();
			xprev = -1;
			yprev = -1;
		}
	}
	
	public void mouseMoved(MouseEvent e) 	{ 
		int y = e.getY();
		if (start) {
			if (y > 220) {
				if (y < 240)
					imageSelected = 1;
				else if (y < 260)
					imageSelected = 2;
				else if (y < 280)
					imageSelected = 3;
			}
			repaint();
			return;
		}
		if (y < 20 || y > ny-20) 
			imp.killRoi();
		else {
			int diam = applet.getDiameter();
			imp.setRoi(new OvalRoi(e.getX()-diam/2, e.getY()-diam/2, diam, diam));
		}
	}
	
	public void mouseDragged(MouseEvent e) 	{ 
		if (start)
			return;
		if (e.getY() < 20 || e.getY() > ny-20) {
			imp.killRoi();
		}
		else {
			int diam = applet.getDiameter();
			imp.setRoi(new OvalRoi(e.getX()-diam/2, e.getY()-diam/2, diam, diam));
			applet.measure(e.getX(), e.getY(), true);
		}
	}
	public void mouseEntered(MouseEvent e)  { super.mouseEntered(e); }
	public void mouseExited(MouseEvent e)  	{ super.mouseExited(e); }
	public void mousePressed(MouseEvent e)  { super.mousePressed(e); }
	public void mouseReleased(MouseEvent e) { super.mouseReleased(e); }
		

}
