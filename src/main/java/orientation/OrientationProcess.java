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

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import additionaluserinterface.WalkBar;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PlotWindow;
import imageware.FMath;
import imageware.ImageWare;

public class OrientationProcess extends Thread {
	
	private GroupImage gim;
	private OrientationParameters params;
	private WalkBar walk;
	private ImageWare source;
	
	public OrientationProcess(ImageWare source, OrientationParameters params) {
		this.walk = new WalkBar("", false, false, false, 100);
		this.source = source;
		this.params = params;
	}
	
	public OrientationProcess(WalkBar walk, ImageWare source, OrientationParameters params) {
		this.walk = walk;
		this.source = source;
		this.params = params;
	}
	
	public OrientationProcess(GroupImage gim, OrientationParameters params) {
		this.walk = new WalkBar();
		this.source = gim.source;
		this.gim = gim;
		this.params = params;
	}
	
	public GroupImage getGroupImage() {
		return gim;
	}

	public void run() {
		walk.reset();

		gim = new GroupImage(walk, source, params);

		if (params.gradient == OrientationParameters.HESSIAN)
			new Hessian(walk, gim, params).run();
		else
			new Gradient(walk, gim, params).run();
					
		StructureTensor st = new StructureTensor(walk, gim, params);
		ExecutorService executor = Executors.newFixedThreadPool(1);
		executor.execute(st);
		executor.shutdown();
		while (!executor.isTerminated()) {}
			
		walk.finish();
	}

	/**
	*/
	public float[] distribution() {
		double cohmin = params.minCoherency / 100.0;
		double enemin = params.minEnergy / 100.0;
		gim.selectedOrientation = gim.orientation.replicate();
		gim.selectedOrientation.fillConstant(-1.0);
		gim.selectedMask = gim.coherency.replicate();
		gim.selectedEnergy = gim.energy.duplicate();
		gim.selectedEnergy.rescale(0, 1);
		float histo[] = new float[180];
		float angles[] = new float[180];
		for(int a=0; a<180; a++) 
			angles[a] = a-90;
		int nx = gim.nx;
		int ny = gim.ny;
		double r = 180.0/Math.PI;
		for(int x=0; x<nx; x++)
		for(int y=0; y<ny; y++) {
			double coh = gim.coherency.getPixel(x, y, 0);
			if (cohmin <= coh) {
				double ene = gim.selectedEnergy.getPixel(x, y, 0);
				if (enemin <= ene) {
					double ori = 90.0 + gim.orientation.getPixel(x, y, 0) * r;
					gim.selectedOrientation.putPixel(x, y, 0, ori);
					gim.selectedMask.putPixel(x, y, 0, 1.0);
					int a = FMath.floor(ori);
					float da = (float)(ori-a);
					int a1 = a;
					a1 = (a1 < 0 ? 180+a1 : (a1 >= 180 ? a1-180 : a1));
					histo[a1] += (1f - da) * coh;
					int a2 = a+1;
					a2 = (a2+1 < 0 ? 180+a2 : (a2 >= 180 ? a2-180 : a2));
					histo[a2] += da * coh;
				}
			}
		}
		return histo;
	}

	public void saveDistribution(OrientationParameters params, float[] histo) {
		if (params.pathSaveDistribution != "") {
			IJ.log("Saved the distribution in the file: " + params.pathSaveDistribution);
			File file = new File(params.pathSaveDistribution);
			try {
				FileWriter fw = new FileWriter(file);
				for(int a=0; a<180; a++) {
					fw.write("" + (a-90) + "\t" + (histo[a]) + "\n");
				}
				fw.close();
			}
			catch (Exception ex) {
				IJ.log("Error to write into the file: " + params.pathSaveDistribution);
			}
		}
	}
	
	public Vector<ImagePlus> show(int countRun) {
		Vector<ImagePlus> list = new Vector<ImagePlus>();
		int 		feature = OrientationParameters.GRADIENT_VERTICAL;
		
		feature = OrientationParameters.GRADIENT_HORIZONTAL;
		if (params.view[feature]) {
			if (params.isServiceAnalysis()) {
				ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
				if (imp != null)
					list.add(imp);
			}
		}

		feature = OrientationParameters.GRADIENT_VERTICAL;
		if (params.view[feature]) {
			if (params.isServiceAnalysis()) {
				ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
				if (imp != null)
					list.add(imp);
			}
		}

		feature = OrientationParameters.TENSOR_ENERGY;
		if (params.view[feature]) {
			ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
			if (imp != null)
				list.add(imp);
		}

		feature = OrientationParameters.TENSOR_ORIENTATION;
		if (params.view[feature]) {
			ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
			if (imp != null)
				list.add(imp);
		}
		
		feature = OrientationParameters.TENSOR_COHERENCY;
		if (params.view[feature]) {
			ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
			if (imp != null)
				list.add(imp);
		}

		feature = OrientationParameters.HARRIS;
		if (params.view[feature]) {
			if (params.isServiceHarris()) {
				ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
				if (imp != null)
					list.add(imp);
			}
		}
		
		feature = OrientationParameters.SURVEY;
		if (params.view[feature]) {
			if (!params.isServiceHarris()) {
				ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
				if (imp != null)
					list.add(imp);
			}
		}
		return list;
	}
	
	public Vector<ImagePlus> showDistribution(int countRun, float histo[]) {
		Vector<ImagePlus> list = new Vector<ImagePlus>();
		int feature = 0;
		
		feature = OrientationParameters.DIST_MASK;
		if (params.view[feature]) {
			ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
			if (imp != null)
				list.add(imp);
		}
		
		feature = OrientationParameters.DIST_ORIENTATION;
		if (params.view[feature]) {
			ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
			if (imp != null)
				list.add(imp);
		}
		
		feature = OrientationParameters.DIST_COLOR;
		if (params.view[feature]) {
			ImagePlus imp = gim.showFeature(OrientationParameters.name[feature], countRun, !params.radian[feature], params);
			if (imp != null)
				list.add(imp);
		}
		
		feature = OrientationParameters.DIST_HISTO;
		if (params.view[feature]) {
			ImagePlus imp = plotDistribution(countRun, histo);
			if (imp != null)
				list.add(imp);
		}

		return list;
	}
	
	public ImagePlus plotDistribution(int countRun, float histo[]) {
		float max = -Float.MIN_VALUE;
		for(int a=0; a<180; a++) {
			if (histo[a] > max)
				max = histo[a];
		}
		float angles[] = new float[180];
		for(int a=0; a<180; a++) 
			angles[a] = a-90;
		PlotWindow pw = new PlotWindow(OrientationParameters.name[OrientationParameters.DIST_HISTO] + "-" + countRun, "Orientation in Degrees", "Distribution of orientation", angles, histo);
		PlotWindow.noGridLines = false;
		PlotWindow.interpolate = false;
		pw.setColor(Color.red);
		pw.setLineWidth(1);
		pw.setLimits(-90, 90, 0, max);
		pw.draw();
		return pw.getImagePlus();
	}
}
