/*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
*/
package de.ipbhalle.metfrag.tools.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.templates.MoleculeFactory;

import de.ipbhalle.metfrag.spectrum.PeakMolPair;

public class StructureRendererTable {
	
	/**
	 * Draw Molecule and the fragments of it
	 * 
	 * @param original the original molecule
	 * @param List of Fragments
	 */
	public static void Draw(IAtomContainer original, List<IAtomContainer> l, String name) {
		
		List<IAtomContainer> containers = new ArrayList<IAtomContainer>();
		containers.addAll(l);
		containers.add(0,original);
        
        MoleculeTableJFrame example = new MoleculeTableJFrame(containers);
        
        example.pack();
        example.setVisible(true);
	}
	
	
	/**
	 * Draw Molecule and the explained fragments of it
	 * 
	 * @param original the original molecule
	 * @param List of Fragments
	 */
	public static void DrawHits(IAtomContainer original, Vector<PeakMolPair> l, String name) {
		
		List<IAtomContainer> containers = new ArrayList<IAtomContainer>();
		containers.add(original);
		
		//add fragments to list
		for (PeakMolPair frag : l) {
			containers.add(frag.getFragment());
		}
		
        
        MoleculeTableJFrame example = new MoleculeTableJFrame(containers);
        
        example.pack();
        example.setVisible(true);
	}


}
