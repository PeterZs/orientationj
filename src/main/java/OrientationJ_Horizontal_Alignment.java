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

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imageware.Builder;
import imageware.ImageWare;

public class OrientationJ_Horizontal_Alignment implements PlugIn {

	public static void main(String arg[]) {
		new OrientationJ_Test_Image().run("");
		new OrientationJ_Horizontal_Alignment().run("");
	}

	public void run(String arg) {

		if (IJ.versionLessThan("1.21a")) {
			return;
		}
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.error("No open image.");
			return;
		}
		if (imp.getType() != ImagePlus.GRAY8 && imp.getType() != ImagePlus.GRAY16 && imp.getType() != ImagePlus.GRAY32) {
			IJ.error("Only processed 8-bits, 16-bits, or 32 bits images.");
			return;
		}
		ImageStack stack = imp.getStack();
		int nt = stack.getSize();
		double data[][] = new double[nt][3];
		for (int i=0; i<nt; i++) {
			imp.setSlice(i+1);

			ImageWare input = Builder.create(new ImagePlus("", imp.getProcessor()), ImageWare.DOUBLE);
			double res[] = compute(input);
			data[i][0] = i;
			data[i][1] = res[0];
			data[i][2] = res[1];
		}
		int nx = imp.getWidth();
		int ny = imp.getHeight();
		
		ImageStack rotate = new ImageStack(2*nx, 2*ny);
		
		for (int i=0; i<nt; i++) {
			imp.setSlice(i+1);
			ImageProcessor ip1 = imp.getProcessor();
			ImageProcessor ip2 = ip1.createProcessor(2*nx, 2*ny);
			ip2.insert(ip1, nx/2, ny/2);
			ip2.setInterpolate(true);
		
			ip2.rotate((data[i][1]));
			rotate.addSlice("" + data[i][1], ip2);
		}
		(new ImagePlus("", rotate)).show();
	}

	/**
	*/
	public double[] compute(ImageWare image) {

		int nx = image.getWidth();
		int ny = image.getHeight();	
		
		double[] in = image.getSliceDouble(0);
		double dy = 0;
		double dx = 0;
		double vxy = 0;
		double vxx = 0;
		double vyy = 0;
		int k;
		int area = (nx-2)*(ny-2);
		for(int y=1; y<ny-1; y++)
		for(int x=1; x<nx-1; x++) {
			k = x+y*nx;
			dx = in[k-1] - in[k+1];
			dy = in[k-nx] - in[k+nx];
			vxy += dx * dy;
			vxx += dx * dx;
			vyy += dy * dy;
		}	
		vxy /= area;
		vxx /= area;
		vyy /= area;
		double orientation = Math.toDegrees(0.5*Math.atan2(2.0*vxy, vyy-vxx));
		double d = vyy - vxx;
		double coherency = 0.0;
		if (vxx+vyy > 1) {
			coherency = Math.sqrt(d*d + vxy*vxy*4.0)/(vxx+vyy);
		}
		return new double[] {orientation, coherency};
	}

}
