/*
	This file is part of SGSim-REST framework, a game to learn coordination protocols with microgrids
	
    Copyright (C) 2017 Rafael Pax, Jorge J. Gomez-Sanz

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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
