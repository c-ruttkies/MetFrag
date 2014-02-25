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

package de.ipbhalle.metfrag.chemspiderClient;

import java.io.StringReader;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;

import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLReader;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import com.chemspider.www.ExtendedCompoundInfo;
import com.chemspider.www.MassSpecAPISoapProxy;

import de.ipbhalle.metfrag.tools.MolecularFormulaTools;

public class ChemSpider {
	
	/**
	 * Gets Chemspider compound ID's by mass.
	 * 
	 * @param mass the mass
	 * @param error the error
	 * 
	 * @return the chemspider by mass
	 * 
	 * @throws RemoteException the remote exception
	 */
	public static Vector<String> getChemspiderByMass(Double mass, Double error) throws RemoteException
	{
		Vector<String> result = new Vector<String>();
		MassSpecAPISoapProxy chemSpiderProxy = new MassSpecAPISoapProxy();
		String[] resultIDs = chemSpiderProxy.searchByMass2(mass, error);
		for (int i = 0; i < resultIDs.length; i++) {
			result.add(resultIDs[i]);
		}
		return result;
	}
	
	/**
	 * Gets the chemspider by sum formula.
	 * 
	 * @param sumFormula the sum formula
	 * 
	 * @return the chemspider by sum formula
	 * 
	 * @throws RemoteException the remote exception
	 */
	public static Vector<String> getChemspiderBySumFormula(String sumFormula) throws RemoteException
	{
		Vector<String> result = new Vector<String>();
		MassSpecAPISoapProxy chemSpiderProxy = new MassSpecAPISoapProxy();
		String[] resultIDs = chemSpiderProxy.searchByFormula2(sumFormula);
		for (int i = 0; i < resultIDs.length; i++) {
			result.add(resultIDs[i]);
		}
		return result;
	}
	
	
	/**
	 * Gets the mol by id.
	 * 
	 * @param ID the iD
	 * 
	 * @return the molby id
	 * 
	 * @throws RemoteException the remote exception
	 */
	public static String getMolByID(String ID, String token) throws RemoteException
	{
		MassSpecAPISoapProxy chemSpiderProxy = new MassSpecAPISoapProxy();
		String mol = chemSpiderProxy.getRecordMol(ID, false, token);
		return mol;
	}
	
	/**
	 * Gets the mol by id.
	 * 
	 * @param ID the iD
	 * 
	 * @return the molby id
	 * 
	 * @throws RemoteException the remote exception
	 * @throws CDKException 
	 * @throws CloneNotSupportedException 
	 */
	public static IAtomContainer getMol(String ID, boolean getAll, String token) throws RemoteException, CDKException, CloneNotSupportedException
	{
		MassSpecAPISoapProxy chemSpiderProxy = new MassSpecAPISoapProxy();
		String mol = chemSpiderProxy.getRecordMol(ID, false, token);
		MDLReader reader;
		List<IAtomContainer> containersList;
		IAtomContainer molecule = null;
		try
		{
	        reader = new MDLReader(new StringReader(mol));
	        ChemFile chemFile = (ChemFile)reader.read((ChemObject)new ChemFile());
	        containersList = ChemFileManipulator.getAllAtomContainers(chemFile);
	        molecule = containersList.get(0);
		}
		catch(Exception e)
		{
			System.err.println("Error retrieving chemspider id " + ID + "!");
			return null;
		}
		
        if(getAll)
        	return molecule;
        
        if(!MolecularFormulaTools.isBiologicalCompound(molecule))
    		molecule = null;
        
		return molecule;
	}
	
	/**
	 * Gets the extended compound information like name, mass, InchI.....
	 * 
	 * @param key the key
	 * 
	 * @return the extended cpd info
	 * 
	 * @throws RemoteException the remote exception
	 */
	public static ExtendedCompoundInfo getExtendedCpdInfo(int key, String token) throws RemoteException
	{
		MassSpecAPISoapProxy chemSpiderProxy = new MassSpecAPISoapProxy();
		ExtendedCompoundInfo cpdInfo = chemSpiderProxy.getExtendedCompoundInfo(key, token);
		return cpdInfo;
	}
	
}
