import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Example_ImageJ_Plugin implements PlugIn {

	@Override
	public void run(String arg) {
		ImagePlus imp = IJ.getImage();
		if (imp == null) {
			IJ.log("NO open image.");
			return;
		}
		
		GenericDialog gd = new GenericDialog("11 Template ImageJ plugin 1");
		gd.addNumericField("Add value", 50, 1);

		gd.showDialog();
		if (gd.wasCanceled())
			return;
		double value = gd.getNextNumber();
		for (int i = 1; i <= imp.getStackSize(); i++)
			add(imp.getStack().getProcessor(i), value);
		imp.updateAndDraw();
	}

	private void add(ImageProcessor ip, double value) {
		for (int x=0; x < ip.getWidth(); x++)
		for (int y=0; y < ip.getHeight(); y++)
			ip.putPixelValue(x, y, ip.getPixelValue(x, y) + value);
	}


	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Example_ImageJ_Plugin.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
