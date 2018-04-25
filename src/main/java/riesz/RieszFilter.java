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
package riesz;

import fft.ComplexSignal;
import imageware.Builder;
import imageware.ImageWare;

public class RieszFilter {

	private ComplexSignal A[];
	private ComplexSignal S[];
	private String name[];
	private int channels;
	private int order;
	private boolean cancelDC = false; // false to perfect reconstruction

	public RieszFilter(int nx, int ny, int order, boolean cancelDC) {
		this.order = order;
		this.cancelDC = cancelDC;
		this.channels = order + 1;
		A = new ComplexSignal[channels];
		S = new ComplexSignal[channels];
		name = new String[channels];
		
		ComplexSignal baseX = generateBaseX(nx, ny);
		ComplexSignal baseY = generateBaseY(nx, ny);
	
		double c =  Math.sqrt(Math.pow(2, order));
		for(int k=0; k<channels; k++) {
			name[k] = "F";
		}
		for(int k=0; k<channels; k++) {
			for(int kx=1; kx<channels-k; kx++) {
				if (A[k] == null)
					A[k] = baseX.duplicate();
				else
					A[k].multiply(baseX);
				name[k] += "X";
			}
			for(int ky=channels-k; ky<channels; ky++) {
				if (A[k] == null)
					A[k] = baseY.duplicate();
				else
					A[k].multiply(baseY);
				name[k] += "Y";
			}
			double coef = Math.sqrt(binomial(channels-1, k));
			A[k].multiply(coef);
			A[k].imag[0] /= c;
			A[k].real[0] /= c;
			S[k] = A[k].conjugate();
		}
	}
	public int getChannels() {
		return channels;
	}
	
	public ComplexSignal getAnalysis(int channel) {
		return A[channel];
	}
	
	public ComplexSignal getSynthesis(int channel) {
		return S[channel];
	}

	public int getOrder() {
		return order;
	}

	/**
	* Return the real (if order is odd) or the imaginary (if order is even) 
	* parts of the analysis filter for a specific channel.
	*/
	public ImageWare getAnalysisVisible(int channel) {
		int nx = A[channel].nx;
		int ny = A[channel].ny;
		ImageWare out = Builder.create(nx, ny, 1, ImageWare.FLOAT);
		float[] pout = out.getSliceFloat(0);
		A[channel].shift();
		if (order%2==0) {
			for(int k=0; k<nx*ny; k++)
				pout[k] = (float)A[channel].real[k];
		}
		else {
			for(int k=0; k<nx*ny; k++)
				pout[k] = (float)A[channel].imag[k];
		}
		A[channel].shift();
		return out;
	}

	/**
	*/
	public String getName(int channel) {
		return name[channel];
	}

	/**
	*/
	private ComplexSignal generateBaseX(int nx, int ny) {
		ComplexSignal filter = new ComplexSignal(nx, ny);
		
		for(int x=0; x<nx/2; x++) 
		for(int y=0; y<=ny/2; y++) {
			double px = (double)x / (nx-1);
			double py = (double)y / (ny-1);
			double w = Math.sqrt(px*px + py*py);
			filter.imag[x + y*nx] = -px/w;
			if (y >= 1)
				filter.imag[x + (ny-y)*nx] = -px/w;
			if (x >= 1)
				filter.imag[nx-x + y*nx] = px/w;
			if (y >= 1 && x >= 1)
				filter.imag[nx-x + (ny-y)*nx] = px/w;
		}
		// Nyquist frequency
		int x = nx/2;
		for(int y=0; y<=ny/2; y++) {
			double px = (double)x / (nx-1);
			double py = (double)y / (ny-1);
			double w = Math.sqrt(px*px + py*py);
			filter.real[x+nx*(y)] = px/w;
			filter.imag[x+nx*(y)] = 0;
			if (y >= 1) {
				filter.real[x+nx*(ny-y)] = px/w;
				filter.imag[x+nx*(ny-y)] = 0;
			}
		}
		// DC frequency
		filter.imag[0] = 0.0;
		filter.real[0] = (cancelDC ? 1 : 0);
		return filter;
	}

	/**
	*/
	private ComplexSignal generateBaseY(int nx, int ny) {
		ComplexSignal filter = new ComplexSignal(nx, ny);
		for(int x=0; x<=nx/2; x++) 
		for(int y=0; y<ny/2; y++) {
			double px = (double)x / (nx-1);
			double py = (double)y / (ny-1);
			double w = Math.sqrt(px*px + py*py);
			filter.imag[x + nx*y] = -py/w;
			if (x >= 1)
				filter.imag[nx-x + nx*y] = -py/w;
			if (y >= 1)
				filter.imag[x + nx*(ny-y)] = py/w;
			if (y >= 1 && x >= 1)
				filter.imag[nx-x + nx*(ny-y)] = py/w;
		}
		// Nyquist frequency
		int y = ny/2;
		for(int x=0; x<=nx/2; x++) {
			double px = (double)x / (nx-1);
			double py = (double)y / (ny-1);
			double w = Math.sqrt(px*px + py*py);
			filter.real[x + nx*y] = py/w;
			filter.imag[x + nx*y] = 0;
			if (x >= 1) {
				filter.real[nx-x + nx*y] = py/w;
				filter.imag[nx-x + nx*y] = 0;
			}
		}
		// DC frequency
		filter.imag[0] = 0.0;
		filter.real[0] = (cancelDC ? 1 : 0);
		return filter;
	}

	private double binomial(int n, int k) {
		return factorial(n) / factorial(k) / factorial(n-k);
	}
	
	private double factorial(int n) {
		if (n == 0)
			return 1;
		int fact = 1;
		for(int i=1; i<=n; i++)
			fact *= i;
		return fact;
	}

}
