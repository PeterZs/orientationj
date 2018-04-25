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

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import imageware.ImageWare;

public class ColorMapping {

	public static ImagePlus colorHSB(int n, String name, ImageWare hue, ImageWare sat, ImageWare bri) {
		int nx = hue.getSizeX();
		int ny = hue.getSizeY();
		ImageStack stack = new ImageStack(nx, ny);
		int size = nx*ny;
		for(int k=0; k<n; k++) {
			int[] pixels = new int[size];
			float[] h = hue.getSliceFloat((k < hue.getSizeZ() ? k : 0));
			float[] s = sat.getSliceFloat((k < sat.getSizeZ() ? k : 0));
			float[] b = bri.getSliceFloat((k < bri.getSizeZ() ? k : 0));
			for (int index=0; index<size; index++) {
				pixels[index] = Color.HSBtoRGB(h[index], s[index], b[index]) + (0xFF << 24);
					
			}
			stack.addSlice("", new ColorProcessor(nx, ny, pixels));
		}
		ImagePlus imp = new ImagePlus(name, stack);
		imp.show();
		return imp;
	}

	public static ImagePlus colorRGB(int n, String name, ImageWare red, ImageWare green, ImageWare blue) {
		int nx = red.getSizeX();
		int ny = red.getSizeY();
		ImageStack stack = new ImageStack(nx, ny);
		int size = nx*ny;
		for(int k=0; k<n; k++) {
			int[] pixels = new int[size];
			float[] r = red.getSliceFloat((k < red.getSizeZ() ? k : 0));
			float[] g = green.getSliceFloat((k < green.getSizeZ() ? k : 0));
			float[] b = blue.getSliceFloat((k < blue.getSizeZ() ? k : 0));
			for (int index=0; index<size; index++) {
				int ri = (int)(r[index]*255);
				int gi = (int)(g[index]*255);
				int bi = (int)(b[index]*255);
				pixels[index] = (bi + (gi<<8) + (ri<<16) + (0xFF << 24));
			}
			stack.addSlice("", new ColorProcessor(nx, ny, pixels));
		}
		ImagePlus imp = new ImagePlus(name, stack);
		imp.show();
		return imp;
	}
	
	/*
	private static void addBar(int bar, float[] r, float[] g, float[] b, int nx, int ny) {
		
		float ratio = 1.0f/(ny/2);
		int index;
		int len = ny/4;		
		for(int j=len; j<3*len; j++) {
			if (bar == 1) {
				for(int i=0; i<5; i++) {
					index = i+j*nx; r[index] = (j-len)*ratio; g[index] = 0; b[index] = 0;
				}
				for(int i=5; i<10; i++) {
					index = i+j*nx; r[index] = 0; g[index] = (j-len)*ratio; b[index] = 0;
				}
				for(int i=10; i<15; i++) {
					index = i+j*nx; r[index] = 0; g[index] = 0; b[index] = (j-len)*ratio;
				}
			}
			else {
				for(int i=nx-15; i<nx-10; i++) {
					index = i+j*nx; r[index] = (j-len)*ratio; g[index] = 0; b[index] = 0;
				}
				for(int i=nx-10; i<nx-5; i++) {
					index = i+j*nx; r[index] = 0; g[index] = (j-len)*ratio; b[index] = 0;
				}
				for(int i=nx-5; i<nx; i++) {
					index = i+j*nx; r[index] = 0; g[index] = 0; b[index] = (j-len)*ratio;
				}
			}
		}
	}
	*/

	/*
	private static void addRing(int bar, float[] h, float[] s, float[] b, int nx, int ny) {
		if (nx < 16)
			return;
		if (ny < 16)
			return;
			
		int index;
		int size = 16;
		float norm = (float)Math.sqrt(2)*size;	
		for(int i=-size; i<=size; i++)
		for(int j=-size; j<=size; j++) {
			float rad = (float)Math.sqrt(i*i+j*j);

			if (rad < size) {
				rad /= norm;
				float theta = (float)((Math.atan2(i, j)+Math.PI/2) / Math.PI);
				if (bar == 1) {
					index = (size+i+1) + nx*(j+ny/2-size); 
					h[index] = theta;
					s[index] = rad;
					b[index] = 1.0f;
					index = (size+i+1) + nx*(j+ny/2+size); 
					h[index] = theta;
					s[index] = 1.0f;
					b[index] = rad;
				}
				else {
					index = (nx-size-1+i) + nx*(j+ny/2-size); 
					h[index] = theta;
					s[index] = rad;
					b[index] = 1.0f;
					index = (nx-size-1+i) + nx*(j+ny/2+size); 
					h[index] = theta;
					s[index] = 1.0f;
					b[index] = rad;
				}
			}
		}
	}
	*/
	
}