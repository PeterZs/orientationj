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
import java.util.Vector;

import ij.ImagePlus;
import ij.gui.ImageCanvas;

public class CornerCanvas extends ImageCanvas {

	private Vector<PointTime> corners;
	private ImagePlus imp;
	
	public CornerCanvas(ImagePlus imp, Vector<PointTime> corners) {
		super(imp);
		this.imp = imp;
		this.corners = corners;
	}
		
	public void paint(Graphics g) {
		super.paint(g);
		int t = imp.getCurrentSlice()-1;
		int len = (int)Math.round(5*getMagnification());
		g.setColor(Color.red);
		for(int i=0; i<corners.size(); i++) {
			
			PointTime pt = corners.get(i);
			if (pt.t == t) {
				int x = screenX(pt.x);
				int y = screenY(pt.y);
				g.drawLine(x-len, y, x+len, y);
				g.drawLine(x, y-len, x, y+len);
			}
		}
		
	}
}

