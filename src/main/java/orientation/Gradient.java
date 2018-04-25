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
import fft.ComplexSignal;
import fft.FFT2D;
import imageware.Builder;
import imageware.ImageWare;
import orientationj.WalkBarOrientationJ;
import riesz.RieszTransform;

public class Gradient implements Runnable {

	private GroupImage gim;
	private OrientationParameters params;
	private WalkBar walk;
	
	public Gradient(WalkBar walk, GroupImage gim, OrientationParameters params) {
		if (walk == null)
			walk = new WalkBarOrientationJ();
		this.walk = walk;
		this.gim = gim;
		this.params = params;
	}
	
	public void run() {
		if (params.gradient == OrientationParameters.GRADIENT_CUBIC_SPLINE)
			gradientSpline(gim, params.sigmaLoG);
		else if (params.gradient == OrientationParameters.GRADIENT_GAUSSIAN)
			gradientGaussian(gim, 1);
		else if (params.gradient == OrientationParameters.GRADIENT_FOURIER_DOMAIN)
			gradientFourier(gim);
		else if (params.gradient == OrientationParameters.GRADIENT_FINITE_DIFF)
			gradientFiniteDifference(gim, params.sigmaLoG);
		else if (params.gradient == OrientationParameters.GRADIENT_RIESZ)
			gradientRiesz(gim);
	}

	private void gradientRiesz(GroupImage gim) {
		RieszTransform riesz = new RieszTransform(gim.nx, gim.ny, 1, false);
		ImageWare slice = Builder.create(gim.nx, gim.ny, 1, ImageWare.FLOAT);
		for (int t=0; t<gim.nt; t++) {
			walk.progress("Riesz", t*100.0/gim.nt);
			gim.source.getXY(0, 0, t, slice);
			ImageWare channels[] = riesz.analysis(slice);
			gim.gx.putXY(0, 0, t, channels[0]);
			gim.gy.putXY(0, 0, t, channels[1]);
		}
	}
	
	private void gradientGaussian(GroupImage gim, double sigma) {
		int size = ((int)Math.ceil(sigma*4.0))*2 + 1;
		int hsize = size / 2;
		double kx[][] = new double[size][size];
		double ky[][] = new double[size][size];
		double kg[] = new double[size];
		double kd[] = new double[size];
		double s2 = sigma*sigma;
		double cst = 1.0 / Math.sqrt(2.0 * Math.PI * s2);
		for(int i=-hsize; i<=hsize; i++)
			kg[i+hsize] = cst * Math.exp(-0.5*i*i/s2);
		for(int i=-hsize; i<=hsize; i++)
			kd[i+hsize] = cst * Math.exp(-0.5*i*i/s2) * (-i/s2);
		for(int j=0; j<size; j++) 
		for(int i=0; i<size; i++) {
			ky[i][j] = kg[i] * kd[j];
			kx[i][j] = kd[i] * kg[j];
		}
		
		double w = 100.0/(gim.nt*gim.nx);
		double block[][] = new double[size][size];
		for(int t=0; t<gim.nt; t++) {
			for (int x=0; x<gim.nx; x++) {
				walk.progress("Gradient", (t*gim.nx+x)*w);
				for (int y=0; y<gim.ny; y++) {
					gim.source.getNeighborhoodXY(x, y, t, block, ImageWare.MIRROR);
					double sx = 0.0;
					double sy = 0.0;
					for(int j=0; j<size; j++) 
					for(int i=0; i<size; i++) {
						sx += block[i][j] * kx[i][j];
						sy += block[i][j] * ky[i][j];
					}
					gim.gx.putPixel(x, y, t, sx);
					gim.gy.putPixel(x, y, t, sy);
				}
			}
		}
	}
	
	private void gradientFourier(GroupImage gim) {
		
		int mx = (gim.nx % 2 == 0 ? gim.nx : gim.nx+1);
		int my = (gim.ny % 2 == 0 ? gim.ny : gim.ny+1);
		for (int t=0; t<gim.nt; t++) {

			// Generate the filters in x and y
			ComplexSignal filterx = new ComplexSignal(mx, my);
			ComplexSignal filtery = new ComplexSignal(mx, my);
			double rx = Math.PI / mx;
			double ry = Math.PI / my;
			double pix[] = new double[mx*my];
		
			for (int wx=0; wx<mx; wx++)
			for (int wy=0; wy<my; wy++) {
				filterx.imag[wx+mx*wy] = rx*(wx-mx/2);
				filtery.imag[wx+my*wy] = ry*(wy-my/2);
			}
			walk.progress("Fourier", t*100.0/gim.nt);

			// generate the source signal
			for (int x=0; x<gim.nx; x++)
			for (int y=0; y<gim.ny; y++)
				pix[x+y*mx] = gim.source.getPixel(x, y, t);
			ComplexSignal sSource = new ComplexSignal(pix, mx, my);
			ComplexSignal fSource = FFT2D.transform(sSource);
			fSource.shift();
		
			// derivate in x
			filterx.multiply(fSource);
			filterx.shift();
			ComplexSignal is_x = FFT2D.inverse(filterx);

			// derivate in y
			filtery.multiply(fSource);
			filtery.shift();
			ComplexSignal is_y = FFT2D.inverse(filtery);
			// create the gradient
			for (int x=0; x<gim.nx; x++)
			for (int y=0; y<gim.ny; y++) {
				gim.gx.putPixel(x, y, t, is_x.real[y*mx+x]);
				gim.gy.putPixel(x, y, t, is_y.real[y*mx+x]);
			}
			
		}
	}

	private void gradientSpline(GroupImage gim, double sigmaLoG) {
		
		int nx = gim.nx;
		int ny = gim.ny;
		int nt = gim.nt;
		double rowin[]  = new double[nx];
		double rowck[]  = new double[nx];
		
		double colin[]  = new double[ny];
		double colck[]  = new double[ny];
		
		double	c0 = 6.0;
		double	a = Math.sqrt(3.0) - 2.0;
		double sp[] = getQuadraticSpline(0.5);
		double neighbor[] = new double[3];
		double v = 0;
		walk.reset();
		double w = 1000.0/(nt*(nx+ny));

		for(int t=0; t<nt; t++) {
			ImageWare log= null;
			if (sigmaLoG > 0) {
				log = LaplacianOfGaussian.run(gim.source, sigmaLoG, sigmaLoG);
			}
			else {
				log = gim.source.convert(ImageWare.FLOAT);
			}
			
			for(int y=0; y<ny; y++) {
				walk.progress("Gradient", (t*(ny+nx)+y)*w);
				log.getX(0, y, t, rowin);
				CubicSpline.doSymmetricalExponentialFilter(rowin, rowck, c0, a);
				int x;
				for(x=2; x<nx-1; x++) {
					neighbor[0] = rowck[x-2] - rowck[x-1];
					neighbor[1] = rowck[x-1] - rowck[x];
					neighbor[2] = rowck[x] - rowck[x+1];
					v = neighbor[0] * sp[0] + neighbor[1] * sp[1] + neighbor[2] * sp[2];
					gim.gx.putPixel(x, y, t, v);
				}
				x = 1;
				neighbor[0] = rowck[1] - rowck[x-1];
				neighbor[1] = rowck[x-1] - rowck[x];
				neighbor[2] = rowck[x] - rowck[x+1];
				v = neighbor[0] * sp[0] + neighbor[1] * sp[1] + neighbor[2] * sp[2];
				gim.gx.putPixel(x, y, t, v);
			}
			
			for(int x=0; x<nx; x++) {
				walk.progress("Gradient", (t*(ny+nx)+x+ny)*w);
				log.getY(x, 0, t, colin);
				CubicSpline.doSymmetricalExponentialFilter(colin, colck, c0, a);
				int y;
				for(y=2; y<ny-1; y++) {
					neighbor[0] = colck[y-2] - colck[y-1];
					neighbor[1] = colck[y-1] - colck[y];
					neighbor[2] = colck[y] - colck[y+1];
					v = neighbor[0] * sp[0] + neighbor[1] * sp[1] + neighbor[2] * sp[2];
					gim.gy.putPixel(x, y, t, v);
				}
				y = 1;
				neighbor[0] = colck[1] - colck[y-1];
				neighbor[1] = colck[y-1] - colck[y];
				neighbor[2] = colck[y] - colck[y+1];
				v = neighbor[0] * sp[0] + neighbor[1] * sp[1] + neighbor[2] * sp[2];
				gim.gy.putPixel(x, y, t, v);
			}
		}
	}

	/**
	*/
	private double[] getQuadraticSpline(double t) {
		double v[] = new double[3];
		v[0] = ( (t - 0.5) * (t - 0.5) ) / 2.0;
		v[2] = ( (t + 0.5) * (t + 0.5) ) / 2.0;
		v[1] = 1.0 - v[0] - v[2];
		return v;
	}
	
	private void gradientFiniteDifference(GroupImage gim, double sigmaLoG) {
		int nx = gim.nx;
		int ny = gim.ny;
		int nt = gim.nt;
		double rowin[] = new double[nx];
		double rowou[] = new double[nx];
		double colin[] = new double[ny];
		double colou[] = new double[ny];
		for(int t=0; t<nt; t++) {
			ImageWare log = null; 
			if (sigmaLoG > 0) {
				log = LaplacianOfGaussian.run(gim.source, sigmaLoG, sigmaLoG);
			}
			else {
				log = gim.source.convert(ImageWare.FLOAT);
			}
			for(int y=0; y<ny; y++) {
				log.getX(0, y, t, rowin);
				for(int x=1; x<nx-1; x++)
					rowou[x] = rowin[x-1] -  rowin[x+1];
				gim.gx.putX(0, y, t, rowou);
			}
			for(int x=0; x<nx; x++) {
				log.getY(x, 0, t, colin);
				for(int y=1; y<ny-1; y++)
					colou[y] = -colin[y+1] +  colin[y-1];
				gim.gy.putY(x, 0, t, colou);
			}
		}
	}

}
