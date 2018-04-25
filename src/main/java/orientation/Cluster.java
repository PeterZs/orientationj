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

import java.util.Vector;

public class Cluster implements Comparable<Cluster> {
	
	public Vector<Cluster> list = null;
	private double costBest;
	public int indexBest;
	public int x;
	public int y;
	public int mx;
	public int my;
	public double dx;
	public double dy;
	public double coherency;
	public double energy;
	private double intensity;
	public int count;

	/**
	* Constructor
	*/
	public Cluster(int x, int y, int mx, int my, double dx, double dy, double coherency, double energy) {
		this.x = x;
		this.y = y;
		this.mx = mx;
		this.my = my;
		this.dx = dx;
		this.dy = dy;
		this.coherency = coherency;
		this.energy = energy;
		list = new Vector<Cluster>();
	}

	public void connect(Vector<Cluster> clusters, int index, int nx, int ny) {
		int k;
		int nc = clusters.size();		
		for(int i=-1; i<=1; i++)
		for(int j=-1; j<=1; j++) {
			if (i!=0 || j!=0) {
				int xi = (int)x+i;
				int yi = (int)y+j;
				if (xi >= 0 && yi >= 0)
				if (xi < nx && yi <ny) {
					k = xi + yi*nx;
					if (k >= 0)
					if (k < nc) 
						list.add(clusters.get(k));
				}
			}
		}
	}
	
	public void measure() {
		int n = list.size();
		costBest = -Double.MAX_VALUE;
		indexBest = -1;
		if (n == 0)
			return;
		double cost = 0.0;
		for(int i=0; i<n; i++) {
			cost = 500-Math.abs(intensity - list.get(i).intensity);
			if (costBest < cost) {
				costBest = cost;
				indexBest = i;
			}
		}
	}
	
	public int compareTo(Cluster test) {
		return (test.costBest > costBest ? 1 : 0);
	}
	
	public String toString() {
		if (indexBest >= 0) {
			String s = "(" + x + "," + y + ") " + " -- " + costBest + " sizeliste:" + list.size() + " ";
			for(int i=0; i<list.size(); i++) {
				if (i==indexBest)
					s += "**";
				s += "(" + list.get(i).x + "," + list.get(i).y + ") ";
			}
			return s;
		}
		return "(" + x + "," + y + ") " +  " -- " + costBest + " -> nothing";
	}
}

