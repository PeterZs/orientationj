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

import additionaluserinterface.WalkBar;

public class Hessian implements Runnable {

	private GroupImage gim;
	private WalkBar walk;
	
	public Hessian(WalkBar walk, GroupImage gim, OrientationParameters params) {
		this.walk = walk;
		this.gim = gim;
	}
	
	public void run() {
		walk.reset();
		for(int t=0; t<gim.nt; t++) {
			walk.progress("Hessian", (t+1)*100.0/gim.nt);
			hessianXX(gim, t);
			hessianYY(gim, t);
			hessianXY(gim, t);
		}
	}
	
	private void hessianXX(GroupImage gim, int t) {
		int nx = gim.nx;
		int ny = gim.ny;
		double kernelX[] = {1.0/6.0, -2.0/6.0, 1.0/6.0};
		double kernelY[] = {1.0, 4, 1.0};

		double colin[] = new double[ny];
		double colou[] = new double[ny];
		for(int x=0; x<nx; x++) {
			gim.source.getY(x, 0, t, colin);
			convolve3taps(colin, colou, kernelX);
			gim.hxx.putY(x, 0, t, colou);
		}
		
		double rowin[] = new double[nx];
		double rowou[] = new double[nx];
		for(int y=0; y<ny; y++) {
			gim.hxx.getX(0, y, t, rowin);
			convolve3taps(rowin, rowou, kernelY);
			gim.hxx.putX(0, y, t, rowou);
		}
	}
	
	private void hessianXY(GroupImage gim, int t) {
		int nx = gim.nx;
		int ny = gim.ny;
		double kernelX[] = {1.0/2.0, 0, -1.0/2.0};
		double kernelY[] = {1.0/2.0, 0, -1.0/2.0};

		double colin[] = new double[ny];
		double colou[] = new double[ny];
		for(int x=0; x<nx; x++) {
			gim.source.getY(x, 0, 0, colin);
			convolve3taps(colin, colou, kernelX);
			gim.hxy.putY(x, 0, 0, colou);
		}
		
		double rowin[] = new double[nx];
		double rowou[] = new double[nx];
		for(int y=0; y<ny; y++) {
			gim.hxy.getX(0, y, t, rowin);
			convolve3taps(rowin, rowou, kernelY);
			gim.hxy.putX(0, y, t, rowou);
		}
	}
	
	private void hessianYY(GroupImage gim, int t) {
		int nx = gim.nx;
		int ny = gim.ny;
		double kernelX[] = {1.0, 4.0, 1.0};
		double kernelY[] = {1.0/6.0, -2.0/6.0, 1.0/6.0};

		double colin[] = new double[ny];
		double colou[] = new double[ny];
		for(int x=0; x<nx; x++) {
			gim.source.getY(x, 0, t, colin);
			convolve3taps(colin, colou, kernelX);
			gim.hyy.putY(x, 0, t, colou);
		}
		
		double rowin[] = new double[nx];
		double rowou[] = new double[nx];
		for(int y=0; y<ny; y++) {
			gim.hyy.getX(0, y, t, rowin);
			convolve3taps(rowin, rowou, kernelY);
			gim.hyy.putX(0, y, t, rowou);
		}
	}
	
	/**
	 * Convolves a 1D signal to a kernel with mirror boundary conditions. 
	 * 
	 * Be careful: 
	 * 1) the kernel should be a 3-taps array.
	 * 2) the in and the out should be allocated with the same size.
	 */
	private void convolve3taps(double[] in, double out[], double[] kernel) {
		int n = in.length;
		out[0] = in[1] * kernel[0] + in[0] * kernel[1] + in[1] * kernel[2];
		for(int k=1; k<n-1; k++)
			out[k] = in[k-1] * kernel[0] + in[k] * kernel[1] + in[k+1] * kernel[2];
		out[n-1] = in[n-2] * kernel[0] + in[n-1] * kernel[1] + in[n-2] * kernel[2];
	}
	
}
