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
import java.awt.Graphics;
import java.awt.Polygon;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import orientation.Cluster;
import orientation.Clusters;


public class VectorCanvas extends ImageCanvas {
	
	private Clusters[] clustersFrame;
	private double scale = 1.0;
	private double orderCoh = 1.0;
	private int transparency = 255;
	private boolean orientation = true;
	private boolean energy = false;
	private boolean coherency = false;
	
	public VectorCanvas(ImagePlus imp, Clusters[] clustersFrame, double scale, int transparency, double orderCoh) {
		super(imp);
		this.imp = imp;
		this.clustersFrame = clustersFrame;
		this.scale = scale;
		this.orderCoh = orderCoh;
		this.transparency = (int)(transparency*2.55);
		
		if (imp.getStackSize() == 1) {
			imp.setWindow(new ImageWindow(imp, this));
		}
		else {
			imp.setWindow(new StackWindow(imp, this));
		}
	}

	public void setSettings(double scale, int transparency, double orderCoh) {
		this.scale = scale;
		this.transparency = (int)(transparency*2.55);
		this.orderCoh = orderCoh;
		repaint();
	}

	public void setFeatures(boolean orientation, boolean energy, boolean coherency) {
		this.orientation = orientation;
		this.energy = energy;
		this.coherency = coherency;
		repaint();
	}

	public void setClusters(Clusters[] clustersFrame) {
		this.clustersFrame = clustersFrame;
		repaint();
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		if (clustersFrame == null)
			return;
		int z = imp.getSlice();
		if (z>=clustersFrame.length) 
			return;
		Clusters clusters = clustersFrame[z]; 
		for(int i=0; i<clusters.size(); i++) {
			Cluster c = clusters.get(i);
			double ux = c.dx*c.mx*scale*0.5;
			double uy = c.dy*c.my*scale*0.5;
			double vx = ux*c.energy;
			double vy = uy*c.energy;
			double x1 = c.x+vx;
			double y1 = c.y-vy;
			double x2 = c.x-vx;
			double y2 = c.y+vy;
			
			if (coherency) {
				g.setColor(new Color(255, 10, 10, transparency/2));
				double u = Math.pow(1-c.coherency, orderCoh);
				double a = Math.sqrt( (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)) * 0.5;
				double b = a * (u+0.01);
				Polygon ellipse = new Polygon();
				double cosa = c.dx;
				double sina = c.dy;
				double astep = Math.PI/90.0;
				for(double an=0; an<=2*Math.PI; an+=astep) {
					double xe = a * Math.cos(an);
					double ye = b * Math.sin(an);
					int x = screenXD((c.x + cosa * xe + sina * ye));
					int y = screenYD((c.y - sina * xe + cosa * ye));
					ellipse.addPoint(x, y);
				}
				g.fillPolygon(ellipse);
			}
			
			if (orientation) {
				g.setColor(new Color(255, 255, 255, transparency));
				g.drawLine(screenXD(c.x+ux), screenYD(c.y-uy), screenXD(c.x-ux), screenYD(c.y+uy));
			}
			
			if (energy) {
				g.setColor(new Color(5, 255, 55, transparency));
				g.drawLine(screenXD(x1), screenYD(y1), screenXD(x2), screenYD(y2));
			}	
		}
		
		/*for(int i=0; i<clusters.size(); i++) {
			int n = clusters.get(i).aggregate.size();
			if (n >= 2) {
				g.setColor(new Color(0, 128+(int)(128*Math.random()), 128+(int)(128*Math.random())));
			
				for(int k=0; k<clusters.get(i).aggregate.size(); k++ ) {
					Cluster c = clusters.get(i).aggregate.get(k);
					int r = (int)Math.round(c.count*mag);
					int x = screenXD(c.x);
					int y = screenYD(c.y);
					g.fillRect(x, y, r*2, r*2);
				}
			}
		}
		*/
		
	}
}

