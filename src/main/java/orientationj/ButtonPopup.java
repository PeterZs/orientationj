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

import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import orientation.OrientationParameters;

public class ButtonPopup extends JButton implements ActionListener {
	
	private int feature;
	private String[] list;
	public AnalysisDialog parent;
	
	public class Popup extends JPanel implements ActionListener {
		private JPopupMenu popup;
		private Font font = new Font("Arial", Font.PLAIN, 11);
		private ButtonPopup	button;
		
		public Popup(String list[], ButtonPopup button, int feature) {
			popup = new JPopupMenu();
			this.button	= button;
			for (int k=0; k<list.length; k++)
			if (list[k].charAt(1) == '/') {
				if (list[k].charAt(0) == 'M') {
					JMenuItem item = new JMenuItem(list[k].substring(2, list[k].length()));
					item.setFont(font);
					popup.add(item);
					item.addActionListener(this);
				}
				if (list[k].charAt(0) == 'C') {
					JMenuItem item = new JCheckBoxMenuItem(list[k].substring(2, list[k].length()));
					item.setFont(font);
					popup.add(item);
					item.addActionListener(this);
				}
				if (list[k].charAt(0) == 'S') {
					popup.addSeparator();
				}
			}
			else if (list[k].equals("Show after computation")) {
				OrientationParameters params = button.parent.getSettingParameters();
				JMenuItem item = new JCheckBoxMenuItem(list[k], params.view[feature]);
				item.setFont(font);
				popup.add(item);
				item.addActionListener(this);
			}
			else if (list[k].equals("Show in degrees")) {
				OrientationParameters params = button.parent.getSettingParameters();
				JMenuItem item = new JCheckBoxMenuItem(list[k], !params.radian[feature]);
				item.setFont(font);
				popup.add(item);
				item.addActionListener(this);
			}
			popup.setInvoker(this);		
			//addMouseListener(this);
		}

		public void show(Point p) {
			popup.setLocation(p);
			popup.setVisible(true);
		}

		public void actionPerformed(ActionEvent e) {
			JMenuItem item = (JMenuItem)e.getSource();
			button.parent.actionPerformedButtonPopup(button, item.getText(), button.getFeature(), item.isSelected());
		}
	}
	
	
	/**
	* Constructor.
	*/
	public ButtonPopup(int feature, String[] list, AnalysisDialog parent) {
		super("<html>&raquo;</html>");
		this.feature	= feature;
		this.parent	= parent;
		this.list	= list;
		addActionListener(this);
	}
	
	public int getFeature() {
		return feature;
	}
	
	public void actionPerformed(ActionEvent e) {
		Popup popup = new Popup(list, this, feature);
		popup.show(getLocationOnScreen());
	}
	
}