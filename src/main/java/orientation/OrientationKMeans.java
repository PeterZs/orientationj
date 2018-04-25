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

import ij.IJ;
import imageware.Builder;
import imageware.ImageWare;

public class OrientationKMeans {
	private boolean valueChanged;
	private int[] regions;

	private double[] osin;
	private double[] ocos;
	private double[] classSin;
	private double[] classCos;
	
	public ImageWare run(ImageWare orientation, int nbClasses, int maxIterations) {
		int ny = orientation.getHeight();
		int nx = orientation.getWidth();
		int size = nx*ny;
		float angles[] = orientation.getSliceFloat(0);
		osin = new double[size];
		ocos = new double[size];
		for(int i=0; i<size; i++) {
			osin[i] = Math.sin(angles[i]);
			ocos[i] = Math.cos(angles[i]);
		}
		
		float classAngle[] = new float[nbClasses];
        classSin = new double[nbClasses];
        classCos = new double[nbClasses];
        double max = orientation.getMaximum();
        double min = orientation.getMinimum();

        for(int i = 0; i < nbClasses; i++) {
        	classAngle[i] =(float)( i*(max-min)/nbClasses + min);
        	classSin[i] = Math.sin(classAngle[i]);
        	classCos[i] = Math.cos(classAngle[i]);
           	IJ.log(" class:" + i + " " + classAngle[i]);
        }

        regions = new int[size];
        int iterations = 0;
        do {
        	iterate();
        	update(classAngle);
        	iterations++;
         } while(valueChanged && iterations < maxIterations);
         
        ImageWare out = Builder.create(nx, ny, 1, ImageWare.FLOAT);
        float[] pixout = out.getSliceFloat(0);
        for(int i=0; i<size; i++) {
        	pixout[i] = classAngle[regions[i]];
        }

        IJ.log("iterations: "+iterations);
        for(int k=0; k<nbClasses; k++) {
         	IJ.log(" class:" + k + " " + classAngle[k]);
        }
        return out;
	}

	private void iterate(){
		int size = osin.length;
		int nbClasses = classSin.length;
		valueChanged = false;
		double current, distance;
		for(int i=0; i<size; i++) {
			current = cost(i, regions[i]);
			for(int k=0; k<nbClasses; k++) {
				distance = cost(i, k);
				if((distance > current)) {
					current = distance;
					regions[i] = k;
					valueChanged = true;
				}
			}
        }
	}

	private double cost(int p, int m) {
		return Math.abs(ocos[p] * classCos[m] + osin[p]*classSin[m]);
	}

	private void update(float classAngle[]) {
		int nbClasses = classAngle.length;
		for(int k=0; k<nbClasses; k++) {
			double dx = 0.0;
			double dy = 0.0;
			for(int j=0; j<regions.length; j++) {
				if(regions[j] == k) {
					dx += ocos[j];
					dy += osin[j];
				}
			}
			double angle = Math.atan2(dy, dx);
			if (angle > Math.PI/2)
				angle -= Math.PI/2;
			if (angle < -Math.PI/2)
				angle += Math.PI/2;
			classAngle[k] = (float)angle;
	        classSin[k] = Math.sin(angle);
	        classCos[k] = Math.cos(angle);
		}
	}
}