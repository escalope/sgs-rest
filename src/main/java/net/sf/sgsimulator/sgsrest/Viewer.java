package net.sf.sgsimulator.sgsrest;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.HashSet;
import java.util.Vector;

import javax.swing.JFrame;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import mired.ucm.viewer.PowerGridNode;
import mired.ucm.viewer.PowerGridViewer;
import mired.ucm.viewer.SelectedCellsAction;

public class Viewer {
	public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, TransformerException, InterruptedException {
		JFrame f=new JFrame();

		// The powergrid definition must be in the resources folder
		PowerGridViewer t = new PowerGridViewer("files/griddef/grid.xml");
		f.getContentPane().add(BorderLayout.CENTER, t);
		f.setSize(1024,700);
		f.setVisible(true);		
		t.setScale(0.6f);

	}
}
