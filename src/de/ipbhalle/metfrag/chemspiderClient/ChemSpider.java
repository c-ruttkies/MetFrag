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
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import com.chemspider.www.MassSpecAPIStub;
import com.chemspider.www.MassSpecAPIStub.GetRecordsSdf;
import com.chemspider.www.MassSpecAPIStub.GetRecordsSdfResponse;
import com.chemspider.www.MassSpecAPIStub.SearchByFormulaAsync;
import com.chemspider.www.MassSpecAPIStub.SearchByFormulaAsyncResponse;
import com.chemspider.www.MassSpecAPIStub.SearchByMassAsync;
import com.chemspider.www.MassSpecAPIStub.SearchByMassAsyncResponse;
import com.chemspider.www.SearchStub;
import com.chemspider.www.SearchStub.AsyncSimpleSearch;

public class ChemSpider {
	
	protected String token = "";
	protected Map<String, IAtomContainer> csidToMolecule;
	
	public ChemSpider(String token) {
		this.token = token;
		this.csidToMolecule = new HashMap<String, IAtomContainer>();
	}
	
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
	public Vector<String> getChemspiderByMass(Double mass, Double error) throws RemoteException
	{
		MassSpecAPIStub stub = new MassSpecAPIStub();
		SearchByMassAsync sbma = new SearchByMassAsync();
		sbma.setMass(mass);
		sbma.setRange(error);
		sbma.setToken(this.token);
		SearchByMassAsyncResponse sbmar = stub.searchByMassAsync(sbma);
		GetRecordsSdf getRecordsSdf = new GetRecordsSdf();
		getRecordsSdf.setRid(sbmar.getSearchByMassAsyncResult());
		getRecordsSdf.setToken(this.token);
		GetRecordsSdfResponse grsr = stub.getRecordsSdf(getRecordsSdf);
		Vector<String> csids = new Vector<String>();
		try {
			Vector<IAtomContainer> cons = this.getAtomContainerFromString(grsr.getGetRecordsSdfResult());
			for(int i = 0; i < cons.size(); i++) {
				String csid = (String)cons.get(i).getProperty("CSID");
				csids.add(csid);
				this.csidToMolecule.put(csid, cons.get(i));
			}
		} catch (CDKException e) {
			e.printStackTrace();
		}
		return csids;
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
	public Vector<String> getChemspiderBySumFormula(String molecularFormula) throws RemoteException
	{ 
		MassSpecAPIStub stub = new MassSpecAPIStub();
		SearchByFormulaAsync sbfa = new SearchByFormulaAsync();
	    sbfa.setFormula(molecularFormula);
	    sbfa.setToken(this.token);
	    SearchByFormulaAsyncResponse sbfar = stub.searchByFormulaAsync(sbfa);
	    GetRecordsSdf getRecordsSdf = new GetRecordsSdf();
		getRecordsSdf.setRid(sbfar.getSearchByFormulaAsyncResult());
		getRecordsSdf.setToken(this.token);
		GetRecordsSdfResponse grsr = stub.getRecordsSdf(getRecordsSdf);
		Vector<String> csids = new Vector<String>();
		try {
			Vector<IAtomContainer> cons = this.getAtomContainerFromString(grsr.getGetRecordsSdfResult());
			for(int i = 0; i < cons.size(); i++) {
				String csid = (String)cons.get(i).getProperty("CSID");
				csids.add(csid);
				this.csidToMolecule.put(csid, cons.get(i));
			}
		} catch (CDKException e) {
			e.printStackTrace();
		}
		return csids;
	}
	
	/**
	 * 
	 * @param _csids
	 * @param token
	 * @return
	 * @throws RemoteException
	 */
	public Vector<String> getChemSpiderByCsids(String[] _csids) throws RemoteException
	{
		MassSpecAPIStub stub = new MassSpecAPIStub();
		AsyncSimpleSearch ass = new AsyncSimpleSearch();
		String query = "";
		if(_csids.length != 0) query += _csids[0];
		for(int i = 1; i < _csids.length; i++) 
			query += "," + _csids[i];
        ass.setQuery(query);
        ass.setToken(this.token);
        SearchStub thisSearchStub = new SearchStub();
        
        GetRecordsSdf getRecordsSdf = new GetRecordsSdf();
        getRecordsSdf.setRid(thisSearchStub.asyncSimpleSearch(ass).getAsyncSimpleSearchResult());
        getRecordsSdf.setToken(this.token);
        GetRecordsSdfResponse grsr = stub.getRecordsSdf(getRecordsSdf);
        Vector<String> csids = new Vector<String>();
        try {
			Vector<IAtomContainer> cons = this.getAtomContainerFromString(grsr.getGetRecordsSdfResult());
			for(int i = 0; i < cons.size(); i++) {
				String csid = (String)cons.get(i).getProperty("CSID");
				csids.add(csid);
				this.csidToMolecule.put(csid, cons.get(i));
			}
		} catch (CDKException e) {
			e.printStackTrace();
		}
        return csids;
	}
	
	/**
	 * 
	 * @param csid
	 * @param getAll
	 * @return
	 * @throws RemoteException
	 * @throws CDKException
	 */
	public IAtomContainer getMol(String csid) 
			throws RemoteException, CDKException
	{
		return this.csidToMolecule.get(csid);
	}
	
	/**
	 * 
	 * @param sdfString
	 * @return
	 * @throws CDKException
	 */
	protected Vector<IAtomContainer> getAtomContainerFromString(String sdfString) throws CDKException {
		MDLV2000Reader reader = new MDLV2000Reader(new StringReader(sdfString));
		
		java.util.List<IAtomContainer> containersList;
		java.util.Vector<IAtomContainer> ret = new Vector<IAtomContainer>();
		
		ChemFile chemFile = (ChemFile)reader.read((ChemObject)new ChemFile());
        containersList = ChemFileManipulator.getAllAtomContainers(chemFile);
        for (IAtomContainer container: containersList) {
        	ret.add(container);
		}
        return ret;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getChemSpiderToken() {
		return this.token;
	}
}
