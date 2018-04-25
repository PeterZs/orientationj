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

import java.awt.Dimension;
import java.text.DecimalFormat;

import additionaluserinterface.NumericTable;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import imageware.Builder;
import imageware.ImageWare;

public class OrientationJ_Dominant_Direction implements PlugIn {

	public static void main(String arg[]) {
		new OrientationJ_Test_Image().run("");
		new OrientationJ_Dominant_Direction().run("");
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
		IJ.log("Frame, Orientation [Degrees], Coherency [%]");
		String formats[] = {"#0.00", "#0.0000", "#0.00000"};
		double data[][] = new double[nt][3];
		for (int i=1; i<=nt; i++) {
			imp.setSlice(i);
			ImageProcessor ip = imp.getProcessor().crop();
			ip.crop();
			ImageWare input = Builder.create(new ImagePlus("", ip), ImageWare.DOUBLE);
			double res[] = compute(input);
			data[i-1][0] = i;
			data[i-1][1] = res[0];
			data[i-1][2] = res[1];
			String s1 = (new DecimalFormat(formats[0])).format(i);
			String s2 = (new DecimalFormat(formats[1])).format(res[0]);
			String s3 = (new DecimalFormat(formats[2])).format(res[1]);
			IJ.log(s1 + ", " + s2 + ", " + s3);
		}
		String headings[] = {"Frame", "Orientation [Degrees]", "Coherency [%]"};
		int widths[] = {60, 160, 160};
		
		NumericTable table = new NumericTable("Results Orientation", headings, new Dimension(400, 300));

		table.setColumnSize(widths);
		table.setData(data, formats);
		table.show(0, 0);

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
			vxx += dx * dx;
			vyy += dy * dy;
			vxy += dx * dy;
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
