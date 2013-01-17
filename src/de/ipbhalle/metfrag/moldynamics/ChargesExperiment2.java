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

package de.ipbhalle.metfrag.moldynamics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.charges.GasteigerMarsiliPartialCharges;
import org.openscience.cdk.charges.GasteigerPEPEPartialCharges;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import de.ipbhalle.metfrag.bondPrediction.Charges;

public class ChargesExperiment2 {
	
	public static void main(String[] args) {
		List<String> files = new ArrayList<String>();
		//at most 2 cpd's for now
			
		//compound0 D1
		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound0/Charges/CID_20097272.sdf");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound0/Charges/CID_20097272_D1.sdf");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound0/mopac/20097272_mopac.mol2");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound0/mopac/20097272_D1_mopac.mol2");
		
		//compound1 F1
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound1/Charges/CID_3365.sdf");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound1/Charges/CID_3365_F1.sdf");
		//compound1 F2
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound1/Charges/CID_3365.sdf");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound1/Charges/CID_3365_F2.sdf");
		
		//compound2 V1
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054.sdf");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054_V1.sdf");
		//compound2 V2
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054.sdf");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054_V2.sdf");
		//compound2 V3
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054.sdf");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054_V3.sdf");
		//compound2 V4
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054.sdf");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054_V4.sdf");
		//compound2 V5
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054.sdf");
//		files.add("/home/swolf/bioclipse-workspace-21/sybyl/molDFTPaper/compound2/Charges/CID_5231054_V5.sdf");
		
	
		List<IAtomContainer> containersList;
		
		
		try {
			File input = new File(files.get(0));
	        ReaderFactory readerFactory = new ReaderFactory();
	        ISimpleChemObjectReader reader;
			reader = readerFactory.createReader(new FileReader(input));
			//ISimpleChemObjectReader content = reader.read(builder.newChemFile());
			ChemFile chemFile = (ChemFile)reader.read((ChemObject)new ChemFile());
	        containersList = ChemFileManipulator.getAllAtomContainers(chemFile);
	        IAtomContainer cpd = containersList.get(0);
	        	        
	        
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        

	}

}
