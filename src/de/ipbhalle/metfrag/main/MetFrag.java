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


package de.ipbhalle.metfrag.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.rpc.ServiceException;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.MoleculeSet;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.interfaces.IBond.Stereo;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import de.ipbhalle.metfrag.chemspiderClient.ChemSpider;
import de.ipbhalle.metfrag.databaseMetChem.CandidateMetChem;
import de.ipbhalle.metfrag.databaseMetChem.Query;
import de.ipbhalle.metfrag.fragmenter.Candidates;
import de.ipbhalle.metfrag.fragmenter.Fragmenter;
import de.ipbhalle.metfrag.fragmenter.FragmenterResult;
import de.ipbhalle.metfrag.fragmenter.FragmenterThread;
import de.ipbhalle.metfrag.massbankParser.Peak;
import de.ipbhalle.metfrag.pubchem.PubChemWebService;
import de.ipbhalle.metfrag.read.SDFFile;
import de.ipbhalle.metfrag.scoring.OptimizationMatrixEntry;
import de.ipbhalle.metfrag.scoring.Scoring;
import de.ipbhalle.metfrag.similarity.Similarity;
import de.ipbhalle.metfrag.similarity.SimilarityGroup;
import de.ipbhalle.metfrag.similarity.TanimotoClusterer;
import de.ipbhalle.metfrag.spectrum.AssignFragmentPeak;
import de.ipbhalle.metfrag.spectrum.CleanUpPeakList;
import de.ipbhalle.metfrag.spectrum.PeakMolPair;
import de.ipbhalle.metfrag.spectrum.WrapperSpectrum;
import de.ipbhalle.metfrag.tools.MolecularFormulaTools;
import de.ipbhalle.metfrag.tools.PPMTool;
import de.ipbhalle.metfrag.tools.Writer;

public class MetFrag {
	
	public static FragmenterResult results = new FragmenterResult();
	private String file = "";
	private String date = "";
	private long timeStart;
	private int candidateCount = 0;	
	private static Query query = null;
		
	/**
	 * Instantiates a new metFrag object.
	 * 
	 * @param file the file
	 * @param date the date
	 * @param folder the folder
	 */
	public MetFrag(String file, String date)
	{
		this.file = file;
		this.date = date;
		this.timeStart = System.currentTimeMillis();
	}
	
	
	/**
	 * MetFrag. Start the fragmenter thread. Afterwards score the results and write out a SDF file with 
	 * all the candidates and an SDF file containing all fragments.
	 * 
	 * @param database the database
	 * @param searchPPM the search ppm
	 * @param databaseID the database id
	 * @param molecularFormula the molecular formula
	 * @param exactMass the exact mass
	 * @param spectrum the spectrum
	 * 
	 * @return the string
	 * 
	 * @throws Exception the exception
	 */
	public static String start(String database, String databaseID, String molecularFormula, Double exactMass, WrapperSpectrum spectrum, boolean useProxy, String outputFile) throws Exception
	{
		results = new FragmenterResult();
		//get configuration
		Config config = new Config();
		PubChemWebService pubchem = new PubChemWebService();
		ChemSpider chemSpider = new ChemSpider(config.getChemspiderToken());
		Vector<String> candidates = Candidates.getOnline(database, databaseID, molecularFormula, exactMass, config.getSearchPPM(), useProxy, pubchem, chemSpider);

		//now fill executor!!!
		//number of threads depending on the available processors
	    int threads = Runtime.getRuntime().availableProcessors();
	    //thread executor
	    ExecutorService threadExecutor = null;
	    System.out.println("Used Threads: " + threads);
	    threadExecutor = Executors.newFixedThreadPool(threads);
			
		for (int c = 0; c < candidates.size(); c++) {				
			threadExecutor.execute(new FragmenterThread(candidates.get(c), database, pubchem, spectrum, config.getMzabs(), config.getMzppm(), 
					config.isSumFormulaRedundancyCheck(), config.isBreakAromaticRings(), config.getTreeDepth(), false, config.isHydrogenTest(), config.isNeutralLossAdd(), 
					config.isBondEnergyScoring(), config.isOnlyBreakSelectedBonds(), config, false, chemSpider));		
		}
		
		threadExecutor.shutdown();
		
		//wait until all threads are finished
		while(!threadExecutor.isTerminated())
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}//sleep for 1000 ms
		}
		
		String ret = "";

		Map<Double, Vector<String>> scoresNormalized = Scoring.getCombinedScore(results.getRealScoreMap(), results.getMapCandidateToEnergy(), results.getMapCandidateToHydrogenPenalty());
		Double[] scores = new Double[scoresNormalized.size()];
		scores = scoresNormalized.keySet().toArray(scores);
		Arrays.sort(scores);
		
		
		
		//now collect the result
		Map<String, IAtomContainer> candidateToStructure = results.getMapCandidateToStructure();
		Map<String, Vector<PeakMolPair>> candidateToFragments = results.getMapCandidateToFragments();
		MoleculeSet setOfMolecules = new MoleculeSet();
		for (int i = scores.length -1; i >=0 ; i--) {
			Vector<String> list = scoresNormalized.get(scores[i]);
			for (String string : list) {
				ret += string + "\t" + scores[i] + "\n";
				//get corresponding structure
				IAtomContainer tmp = candidateToStructure.get(string);
				tmp = AtomContainerManipulator.removeHydrogens(tmp);
				tmp.setProperty("DatabaseID", string);
				tmp.setProperty("Score", scores[i]);
				tmp.setProperty("PeaksExplained", candidateToFragments.get(string).size());
				
				//fix for bug in mdl reader setting where it happens that bond.stereo is null when the bond was read in as UP/DOWN (4)
				for (IBond bond : tmp.bonds()) {
					if(bond.getStereo() == null)
						bond.setStereo(Stereo.UP_OR_DOWN);		
				} 
				setOfMolecules.addAtomContainer(tmp);
			}
		}
		
		MoleculeSet setOfFragments = null;
		if(!databaseID.equals(""))
		{
			setOfFragments = new MoleculeSet();
			
			
			for (int i = scores.length -1; i >=0 ; i--) {
				Vector<String> list = scoresNormalized.get(scores[i]);
				for (String string : list) {
					
					//original molecule
					setOfFragments.addAtomContainer(new Molecule(candidateToStructure.get(string)));
					Vector<PeakMolPair> fragments = candidateToFragments.get(string);
					for (PeakMolPair frag : fragments) {
						
						//fix for bug in mdl reader setting where it happens that bond.stereo is null when the bond was read in as UP/DOWN (4)
						for (IBond bond : frag.getFragment().bonds()) {
							if(bond.getStereo() == null)
								bond.setStereo(Stereo.UP_OR_DOWN);		
						} 
						IMolecule mol = new Molecule(AtomContainerManipulator.removeHydrogens(frag.getFragment()));
						setOfFragments.addAtomContainer(mol);
					}
					
					//write results file
					try {
						SDFWriter writer = new SDFWriter(new FileWriter(new File(outputFile + databaseID + "_" + "fragments.sdf")));
						writer.write(setOfFragments);
						writer.close();
					} catch (CDKException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		}
		

		//write results file
		try {
			SDFWriter writer = new SDFWriter(new FileWriter(new File(outputFile + "metfrag" + "_" + database + "_" + spectrum.getFilename() +".sdf")));
			writer.write(setOfMolecules);
			writer.close();
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}
	
	
	
	/**
	 * MetFrag. Start the fragmenter thread. Afterwards score the results.
	 * 
	 * @param database the database
	 * @param searchPPM the search ppm
	 * @param databaseID the database id
	 * @param molecularFormula the molecular formula
	 * @param exactMass the exact mass
	 * @param spectrum the spectrum
	 * @param useProxy the use proxy
	 * @param mzabs the mzabs
	 * @param mzppm the mzppm
	 * @param molecularFormulaRedundancyCheck the molecular formula redundancy check
	 * @param breakAromaticRings the break aromatic rings
	 * @param treeDepth the tree depth
	 * @param hydrogenTest the hydrogen test
	 * @param neutralLossInEveryLayer the neutral loss in every layer
	 * @param bondEnergyScoring the bond energy scoring
	 * @param breakOnlySelectedBonds the break only selected bonds
	 * @param limit the limit
	 * @param isStoreFragments the is store fragments
	 * 
	 * @return the string
	 * 
	 * @throws Exception the exception
	 */
	public static List<MetFragResult> startConvenience(String database, String databaseID, String molecularFormula, Double exactMass, WrapperSpectrum spectrum, boolean useProxy, 
			double mzabs, double mzppm, double searchPPM, boolean molecularFormulaRedundancyCheck, boolean breakAromaticRings, int treeDepth,
			boolean hydrogenTest, boolean neutralLossInEveryLayer, boolean bondEnergyScoring, boolean breakOnlySelectedBonds, int limit, boolean isStoreFragments) throws Exception
	{
		results = new FragmenterResult();
		PubChemWebService pubchem = new PubChemWebService();
		ChemSpider chemSpider = new ChemSpider("");
		Vector<String> candidates = Candidates.getOnline(database, databaseID, molecularFormula, exactMass, searchPPM, useProxy, pubchem, chemSpider);


		//now fill executor!!!
		//number of threads depending on the available processors
	    int threads = Runtime.getRuntime().availableProcessors();
	    //thread executor
	    ExecutorService threadExecutor = null;
	    System.out.println("Used Threads: " + threads);
	    threadExecutor = Executors.newFixedThreadPool(threads);
			
		for (int c = 0; c < candidates.size(); c++) {
			
			if(c > limit)
				break;
			
			threadExecutor.execute(new FragmenterThread(candidates.get(c), database, pubchem, spectrum, mzabs, mzppm, 
					molecularFormulaRedundancyCheck, breakAromaticRings, treeDepth, false, hydrogenTest, neutralLossInEveryLayer, 
					bondEnergyScoring, breakOnlySelectedBonds, null, false, chemSpider));		
		}
		
		threadExecutor.shutdown();
		
		//wait until all threads are finished
		while(!threadExecutor.isTerminated())
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}//sleep for 1000 ms
		}

		Map<Double, Vector<String>> scoresNormalized = Scoring.getCombinedScore(results.getRealScoreMap(), results.getMapCandidateToEnergy(), results.getMapCandidateToHydrogenPenalty());
		Double[] scores = new Double[scoresNormalized.size()];
		scores = scoresNormalized.keySet().toArray(scores);
		Arrays.sort(scores);

		//now collect the result
		Map<String, IAtomContainer> candidateToStructure = results.getMapCandidateToStructure();
		Map<String, Vector<PeakMolPair>> candidateToFragments = results.getMapCandidateToFragments();

		List<MetFragResult> results = new ArrayList<MetFragResult>();
		for (int i = scores.length -1; i >=0 ; i--) {
			Vector<String> list = scoresNormalized.get(scores[i]);
			for (String string : list) {
				//get corresponding structure
				IAtomContainer tmp = candidateToStructure.get(string);
				tmp = AtomContainerManipulator.removeHydrogens(tmp);
				
				if(isStoreFragments)
					results.add(new MetFragResult(string, tmp, scores[i], candidateToFragments.get(string).size(), candidateToFragments.get(string)));
				else
					results.add(new MetFragResult(string, tmp, scores[i], candidateToFragments.get(string).size()));
			}
		}		
		
		return results;
	}
	
	
	/**
	 * Start convenience with a given structure. This structure is fragmented and mathed with the given peaks.
	 *
	 * @param database the database
	 * @param candidateStructure the candidate structure
	 * @param candidate the candidate
	 * @param molecularFormula the molecular formula
	 * @param exactMass the exact mass
	 * @param spectrum the spectrum
	 * @param useProxy the use proxy
	 * @param mzabs the mzabs
	 * @param mzppm the mzppm
	 * @param searchPPM the search ppm
	 * @param molecularFormulaRedundancyCheck the molecular formula redundancy check
	 * @param breakAromaticRings the break aromatic rings
	 * @param treeDepth the tree depth
	 * @param hydrogenTest the hydrogen test
	 * @param neutralLossInEveryLayer the neutral loss in every layer
	 * @param bondEnergyScoring the bond energy scoring
	 * @param breakOnlySelectedBonds the break only selected bonds
	 * @param limit the limit
	 * @param isStoreFragments the is store fragments
	 * @return the list
	 * @throws Exception the exception
	 */
	public static List<MetFragResult> startConvenienceWithStructure(String database, IAtomContainer candidateStructure, String candidate, String molecularFormula, Double exactMass, WrapperSpectrum spectrum, boolean useProxy, 
			double mzabs, double mzppm, double searchPPM, boolean molecularFormulaRedundancyCheck, boolean breakAromaticRings, int treeDepth,
			boolean hydrogenTest, boolean neutralLossInEveryLayer, boolean bondEnergyScoring, boolean breakOnlySelectedBonds, int limit, boolean isStoreFragments) throws Exception
	{
		
		results = new FragmenterResult();
		PubChemWebService pubchem = new PubChemWebService();

	    //thread executor
	    ExecutorService threadExecutor = null;
	    System.out.println("Used Threads: 1");
	    threadExecutor = Executors.newFixedThreadPool(1);
			
	
			
		threadExecutor.execute(new FragmenterThread(candidateStructure, candidate, database, pubchem, spectrum, mzabs, mzppm, 
				molecularFormulaRedundancyCheck, breakAromaticRings, treeDepth, false, hydrogenTest, neutralLossInEveryLayer, 
				bondEnergyScoring, breakOnlySelectedBonds, null, false, null));		
		
		threadExecutor.shutdown();
		
		//wait until all threads are finished
		while(!threadExecutor.isTerminated())
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}//sleep for 1000 ms
		}

		Map<Double, Vector<String>> scoresNormalized = Scoring.getCombinedScore(results.getRealScoreMap(), results.getMapCandidateToEnergy(), results.getMapCandidateToHydrogenPenalty());
		Double[] scores = new Double[scoresNormalized.size()];
		scores = scoresNormalized.keySet().toArray(scores);
		Arrays.sort(scores);

		//now collect the result
		Map<String, IAtomContainer> candidateToStructure = results.getMapCandidateToStructure();
		Map<String, Vector<PeakMolPair>> candidateToFragments = results.getMapCandidateToFragments();

		List<MetFragResult> results = new ArrayList<MetFragResult>();
		for (int i = scores.length -1; i >=0 ; i--) {
			Vector<String> list = scoresNormalized.get(scores[i]);
			for (String string : list) {
				//get corresponding structure
				IAtomContainer tmp = candidateToStructure.get(string);
				tmp = AtomContainerManipulator.removeHydrogens(tmp);
				
				if(isStoreFragments)
					results.add(new MetFragResult(string, tmp, scores[i], candidateToFragments.get(string).size(), candidateToFragments.get(string)));
				else
					results.add(new MetFragResult(string, tmp, scores[i], candidateToFragments.get(string).size()));
			}
		}		
		
		return results;
	}
	
	
	/**
	 * MetFrag. Start the fragmenter thread. Afterwards score the results.
	 * 
	 * @param database the database
	 * @param searchPPM the search ppm
	 * @param databaseID the database id
	 * @param molecularFormula the molecular formula
	 * @param exactMass the exact mass
	 * @param spectrum the spectrum
	 * @param useProxy the use proxy
	 * @param mzabs the mzabs
	 * @param mzppm the mzppm
	 * @param molecularFormulaRedundancyCheck the molecular formula redundancy check
	 * @param breakAromaticRings the break aromatic rings
	 * @param treeDepth the tree depth
	 * @param hydrogenTest the hydrogen test
	 * @param neutralLossInEveryLayer the neutral loss in every layer
	 * @param bondEnergyScoring the bond energy scoring
	 * @param breakOnlySelectedBonds the break only selected bonds
	 * @param limit the limit
	 * @param jdbc the jdbc
	 * @param username the username
	 * @param password the password
	 * 
	 * @return the string
	 * 
	 * @throws Exception the exception
	 */
	public static List<MetFragResult> startConvenienceMetFusion(String database, String databaseID, String molecularFormula, Double exactMass, WrapperSpectrum spectrum, boolean useProxy, 
			double mzabs, double mzppm, double searchPPM, boolean molecularFormulaRedundancyCheck, boolean breakAromaticRings, int treeDepth,
			boolean hydrogenTest, boolean neutralLossInEveryLayer, boolean bondEnergyScoring, boolean breakOnlySelectedBonds, int limit, String jdbc, String username, String password) throws Exception
	{
		
		PubChemWebService pw = null;
		ChemSpider chemSpider = null;
		results = new FragmenterResult();
		List<String> candidates = null;
		if(molecularFormula != null && !molecularFormula.equals("") || (databaseID != null && !databaseID.equals("")))
		{
			pw = new PubChemWebService();
			chemSpider = new ChemSpider("");
			candidates = Candidates.getOnline(database, databaseID, molecularFormula, exactMass, searchPPM, false, pw, chemSpider);
		}
		else
			candidates = Candidates.getLocally(database, exactMass, searchPPM, jdbc, username, password);

		System.out.println("Hits in database: " + candidates.size());
		
		//now fill executor!!!
		//number of threads depending on the available processors
	    int threads = Runtime.getRuntime().availableProcessors();
	    //thread executor
	    ExecutorService threadExecutor = null;
	    System.out.println("Used Threads: " + threads);
	    threadExecutor = Executors.newFixedThreadPool(threads);
			
		for (int c = 0; c < candidates.size(); c++) {
			
			if(c > limit)
				break;
			
			threadExecutor.execute(new FragmenterThread(candidates.get(c), database, null, spectrum, mzabs, mzppm, 
					molecularFormulaRedundancyCheck, breakAromaticRings, treeDepth, false, hydrogenTest, neutralLossInEveryLayer, 
					bondEnergyScoring, breakOnlySelectedBonds, null, true, jdbc, username, password, chemSpider));		
		}
		
		threadExecutor.shutdown();
		
		//wait until all threads are finished
		while(!threadExecutor.isTerminated())
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}//sleep for 1000 ms
		}

		Map<Double, Vector<String>> scoresNormalized = Scoring.getCombinedScore(results.getRealScoreMap(), results.getMapCandidateToEnergy(), results.getMapCandidateToHydrogenPenalty());
		Double[] scores = new Double[scoresNormalized.size()];
		scores = scoresNormalized.keySet().toArray(scores);
		Arrays.sort(scores);

		//now collect the result
		Map<String, IAtomContainer> candidateToStructure = results.getMapCandidateToStructure();
		Map<String, Vector<PeakMolPair>> candidateToFragments = results.getMapCandidateToFragments();

		List<MetFragResult> results = new ArrayList<MetFragResult>();
		for (int i = scores.length -1; i >=0 ; i--) {
			Vector<String> list = scoresNormalized.get(scores[i]);
			for (String string : list) {
				//get corresponding structure
				IAtomContainer tmp = candidateToStructure.get(string);
				tmp = AtomContainerManipulator.removeHydrogens(tmp);
				
				results.add(new MetFragResult(string, tmp, scores[i], candidateToFragments.get(string).size()));
			}
		}		
		
		return results;
	}
	
	
	/**
	 * MetFrag. Start the fragmenter thread. Afterwards score the results. This method is used in the
	 * webinterface to generate all the fragments for one structure.
	 *
	 * @param peakList the peak list
	 * @param smiles the smiles
	 * @param mode the mode
	 * @param molFormulaRedundancyCheck the mol formula redundancy check
	 * @param mzabs the mzabs
	 * @param mzppm the mzppm
	 * @param treeDepth the tree depth
	 * @param isPositive the is positive
	 * @return the string
	 * @throws Exception the exception
	 */
	public static Vector<PeakMolPair> startConvenienceWeb(String peakList, String smiles, int mode, boolean molFormulaRedundancyCheck, double mzabs, double mzppm, int treeDepth, boolean isPositive) throws Exception
	{
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		//parse smiles
		IAtomContainer molecule = sp.parseSmiles(smiles);
		//configure atoms
		AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
		//add all hydrogens explicitly
		CDKHydrogenAdder adder1 = CDKHydrogenAdder.getInstance(molecule.getBuilder());
        adder1.addImplicitHydrogens(molecule);
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule); 
        
        Double molMass = MolecularFormulaTools.getMonoisotopicMass(MolecularFormulaManipulator.getMolecularFormula(molecule));
		molMass = (double)Math.round((molMass)*10000)/10000;
        
		WrapperSpectrum spectrum = new WrapperSpectrum(peakList, mode, molMass, isPositive);		
		
		//constructor for fragmenter
		Fragmenter fragmenter = new Fragmenter((Vector<Peak>)spectrum.getPeakList().clone(), mzabs, mzppm, spectrum.getMode(), true, molFormulaRedundancyCheck, false, false);
		List<IAtomContainer> listOfFrags = fragmenter.generateFragmentsInMemory(molecule, false, treeDepth);
			
		//clean up peak list
		CleanUpPeakList cList = new CleanUpPeakList(spectrum.getPeakList());
		Vector<Peak> cleanedPeakList = cList.getCleanedPeakList(spectrum.getExactMass());
		
		
		//now find corresponding fragments to the mass
		AssignFragmentPeak afp = new AssignFragmentPeak();
		afp.setHydrogenTest(true);
		afp.assignFragmentPeak(listOfFrags, cleanedPeakList, mzabs, mzppm, spectrum.getMode(), false, isPositive);
		Vector<PeakMolPair> hits = afp.getAllHits();

		return sortBackwards(hits);
	}
	
	
	private static Vector<PeakMolPair> sortBackwards(Vector<PeakMolPair> original)
	{
		Vector<PeakMolPair> ret = new Vector<PeakMolPair>();
		for (int i = original.size() - 1; i >= 0 ; i--) {
			ret.add(original.get(i));
		}
		return ret;
	}
	
	private String getCorrectCandidateID(WrapperSpectrum spectrum, Config config)
	{
		String candidate = "";
		if(config.isPubChem())
			candidate = Integer.toString(spectrum.getCID());
		else if(config.isKEGG())
			candidate = spectrum.getKEGG();
		return candidate;
	}
	
	
	/**
	 * Write sdf file with all processed structures.
	 * 
	 * @param keysScore the keys score
	 * @param folder the folder
	 */
	private void writeSDF(Double[] keysScore, String folder)
	{
		Map<String, Vector<PeakMolPair>> candidateToFragments = results.getMapCandidateToFragments();
		Map<Double, Vector<String>> realScoreMap = results.getRealScoreMap();
		Map<String, IAtomContainer> candidateToStructure = results.getMapCandidateToStructure();
		
		MoleculeSet setOfMolecules = new MoleculeSet();
		for (int i = keysScore.length -1; i >=0 ; i--) {
			Vector<String> list = realScoreMap.get(keysScore[i]);
			for (String string : list) {
				//get corresponding structure
				IAtomContainer tmp = candidateToStructure.get(string);
				tmp = AtomContainerManipulator.removeHydrogens(tmp);
				tmp.setProperty("DatabaseID", string);
				tmp.setProperty("Score", keysScore[i]);
				tmp.setProperty("PeaksExplained", candidateToFragments.get(string).size());
				
				//fix for bug in mdl reader setting where it happens that bond.stereo is null when the bond was read in as UP/DOWN (4)
				for (IBond bond : tmp.bonds()) {
					if(bond.getStereo() == null)
						bond.setStereo(Stereo.UP_OR_DOWN);		
				} 
				setOfMolecules.addAtomContainer(tmp);
			}
		}
		//write results file
		try {
			SDFWriter writer = new SDFWriter(new FileWriter(new File(folder + "logs/" + date + "_Structures.sdf")));
			writer.write(setOfMolecules);
			writer.close();
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Evaluate results and write them to the log files
	 * @throws InterruptedException 
	 */
	private void evaluateResults(String correctCandidateID, WrapperSpectrum spectrum, boolean generateOptimizationMatrix, String folder, boolean writeSDF) throws InterruptedException
	{
		//now collect the result
		Map<String, IAtomContainer> candidateToStructure = results.getMapCandidateToStructure();
		Map<String, Double> candidateToEnergy = results.getMapCandidateToEnergy();
		Map<Double, Vector<String>> realScoreMap = results.getRealScoreMap();
		StringBuilder completeLog = results.getCompleteLog();
		
		//this is the real candidate count after filtering not connected compounds
		this.candidateCount = results.getMapCandidateToStructure().size();
		
		
		//generate the parameter optimization matrix
		String parameterOptimization = "";
		if(generateOptimizationMatrix)
		{
			String header = prepareParameterOptimizationMatrix(correctCandidateID, spectrum.getExactMass());
			parameterOptimization = generateOptimizationMatrix(results.getCandidateToOptimizationMatrixEntries(), header);
		}
		
					
//		realScoreMap = Scoring.getCombinedScore(results.getRealScoreMap(), results.getMapCandidateToEnergy(), results.getMapCandidateToHydrogenPenalty());
//		Double[] keysScore = new Double[realScoreMap.keySet().size()];
//		keysScore = realScoreMap.keySet().toArray(keysScore);
//		Arrays.sort(keysScore);
//		TODO: new scoring function
		Double[] keysScore = new Double[realScoreMap.keySet().size()];
		keysScore = realScoreMap.keySet().toArray(keysScore);
		Arrays.sort(keysScore);
		
		//write out SDF with all the structures
		if(writeSDF)
		{
			writeSDF(keysScore, folder);
		}
		
		
		
		StringBuilder scoreListReal = new StringBuilder();
		int rankWorstCase = 0;
		int rankBestCase = 0;
		int rankBestCaseGrouped = 0;		
		
		//now create the tanimoto distance matrix
		//to be able to group results with the same score
		//search molecules with the same connectivity
		StringBuilder similarity = new StringBuilder();
		int rankTanimotoGroup = 0;
		int rankIsomorphism = 0;
		boolean stop = false;
		try {
			for (int i = keysScore.length-1; i >= 0; i--) {
				similarity.append("\nScore: " + keysScore[i] + "\n");
				List<String> candidateGroup = new ArrayList<String>();
				
				Map<String, IAtomContainer> candidateToStructureTemp = new HashMap<String, IAtomContainer>();
				for (int j = 0; j < realScoreMap.get(keysScore[i]).size(); j++) {
					candidateGroup.add(realScoreMap.get(keysScore[i]).get(j));
					candidateToStructureTemp.put(realScoreMap.get(keysScore[i]).get(j), candidateToStructure.get(realScoreMap.get(keysScore[i]).get(j)));
				}
				
				Similarity sim = null;
				try {
					sim = new Similarity(candidateToStructureTemp, true, false);
				} catch (CDKException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				//now cluster 
				TanimotoClusterer tanimoto = new TanimotoClusterer(sim.getSimilarityMatrix(), sim.getCandidateToPosition());
				List<SimilarityGroup> clusteredCpds = tanimoto.clusterCandididates(candidateGroup, 0.95f);
				List<SimilarityGroup> groupedCandidates = tanimoto.getCleanedClusters(clusteredCpds);
				
				for (SimilarityGroup similarityGroup : groupedCandidates) {			
										
					List<String> tempSimilar = similarityGroup.getSimilarCompoundsWithBaseAsArray();				
					
					for (int k = 0; k < tempSimilar.size(); k++) {

						if(correctCandidateID.equals(tempSimilar.get(k)))
							stop = true;
						
						similarity.append(tempSimilar.get(k));
					
						boolean isIsomorph = sim.isIsomorph(tempSimilar.get(k), similarityGroup.getCandidateTocompare());
						if(!isIsomorph)
							rankIsomorphism++;
						
						similarity.append(" (" + isIsomorph + ") ");
					}
					similarity.append("\n");						
					rankTanimotoGroup++;
					rankIsomorphism++;
				}
				if(stop)
					break;
			}
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String resultsTable = "";
		//timing
		long timeEnd = System.currentTimeMillis() - timeStart;
		
		if(correctCandidateID.equals("none"))
		{
			resultsTable += "\n" + file + "\t" + correctCandidateID + "\t\t\t" + spectrum.getExactMass();
		}
		else
		{
			for (int i = keysScore.length-1; i >= 0; i--) {
				boolean check = false;
				int temp = 0;
				for (int j = 0; j < realScoreMap.get(keysScore[i]).size(); j++) {
					scoreListReal.append("\n" + keysScore[i] + " - " + realScoreMap.get(keysScore[i]).get(j) + "[" + candidateToEnergy.get(realScoreMap.get(keysScore[i]).get(j)) + "]");
					if(correctCandidateID.compareTo(realScoreMap.get(keysScore[i]).get(j)) == 0)
					{
						check = true;
					}
					//worst case: count all which are better or have a equal position
					rankWorstCase++;
					temp++;
				}
				rankBestCaseGrouped++;
				if(!check)
				{
					rankBestCase += temp;
				}
				//add it to rank best case
				else
				{
					resultsTable = "\n" + file + "\t" + correctCandidateID + "\t" + this.candidateCount + "\t" + rankWorstCase + "\t" + rankTanimotoGroup + "\t" + rankIsomorphism + "\t" + spectrum.getExactMass() + "\t" + timeEnd;
				}
			}
		}
		
		//the correct candidate was not in the pubchem results
		if(resultsTable.equals(""))
			resultsTable = "\n" + file + "\t" + correctCandidateID + "\t" + this.candidateCount + "\tERROR\tCORRECT\tNOT FOUND\t" + spectrum.getExactMass() + "\t" + timeEnd;
		
		
		completeLog.append("\n\n*****************Scoring(Real)*****************************");
		completeLog.append("Correct candidate ID: " + correctCandidateID);
		completeLog.append("\nTime: " + timeEnd);
		completeLog.append(scoreListReal);
		
		//write all tanimoto distances in one file
		//similarityValues += sim.getAllSimilarityValues();
		completeLog.append("\n********************Similarity***********************\n\n");	
		completeLog.append(similarity);			
		completeLog.append("\n*****************************************************\n\n");	

		System.out.println("Finished LOG!!! " + this.file);
		
		//write string to disk
		try
		{
			new File(folder + "logs/").mkdir();

			//complete log
			Writer.writeToFile(folder + "logs/" + date + "_log.txt", completeLog.toString());
			//write peak data of the correct compounds to file
			Writer.writeToFile(folder + "logs/" + date + "_results.txt", resultsTable);
			new File(folder + "logs/" + date + "/").mkdirs();
			Writer.writeToFile(folder + "logs/" + date + "/" + this.file, parameterOptimization);
		}
		catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Prepare parameter optimization matrix.
	 * 
	 * @param realScoreMap the real score map
	 * 
	 * @return the string
	 */
	private String prepareParameterOptimizationMatrix(String pubChemIdentifier, Double exactMass)
	{
		String ret = "";
		
		ret += pubChemIdentifier + "\n";
		ret += exactMass.toString() + "\n\n";
		ret += "candidate\tpeakMass\tpeakInt\tbondEnergy\thydrogenPenalty\tpCharges\n";
		
		return ret;
	}
			
	
	/**
	 * Generate optimization matrix.
	 * 
	 * @param candidateToOptimizationMatrixEntries the candidate to optimization matrix entries
	 */
	private String generateOptimizationMatrix(Map<String, List<OptimizationMatrixEntry>> candidateToOptimizationMatrixEntries, String header)
	{
		StringBuilder parameterOptimizationMatrix = new StringBuilder();
		parameterOptimizationMatrix.append(header);
		for (String candidate : candidateToOptimizationMatrixEntries.keySet()) {
			for (OptimizationMatrixEntry entry : candidateToOptimizationMatrixEntries.get(candidate)) {
				parameterOptimizationMatrix.append(candidate + "\t" + entry.getPeakMass() + "\t" + entry.getPeakInt() + "\t" + entry.getBondEnergyString() + "\t" + entry.getHydrogenPenalty() + "\t" + entry.getChargesDiffString() + "\n");
			}
		}
		
		return parameterOptimizationMatrix.toString();
	}

	/**
	 * used for database search of command line tool
	 * 
	 * @author c-ruttkies
	 * 
	 * @param database
	 * @param databaseIDs
	 * @param formula
	 * @param exactMass
	 * @param spec
	 * @param useProxy
	 * @param mzabs
	 * @param mzppm
	 * @param searchppm
	 * @param molredundancycheck
	 * @param breakRings
	 * @param treeDepth
	 * @param hydrogenTest
	 * @param neutralLossInEveryLayer
	 * @param bondEnergyScoring
	 * @param breakOnlySelectedBonds
	 * @param limit
	 * @param isStoreFragments
	 * @param pathToStoreFrags
	 * @param numberThreads
	 * @param chemSpiderToken
	 * @param verbose
	 * @param sampleName
	 * @param localdb
	 * @param dblink
	 * @param dbuser
	 * @param dbpass
	 * @return
	 */
	public static List<MetFragResult> startConvenience(String database,
			String[] databaseIDs, String formula, double exactMass,
			WrapperSpectrum spec, boolean useProxy, double mzabs,
			double mzppm, double searchppm, boolean molredundancycheck,
			boolean breakRings, int treeDepth, boolean hydrogenTest, boolean neutralLossInEveryLayer, 
			boolean bondEnergyScoring, boolean breakOnlySelectedBonds, int startindex, int endindex, boolean isStoreFragments, 
			String pathToStoreFrags, int numberThreads, String chemSpiderToken, boolean verbose,
			String sampleName, boolean localdb, boolean onlyChnopsCompounds, String dblink, String dbuser,
			String dbpass, boolean uniqueInchi) {
		
		results = new FragmenterResult();
		Vector<String> candidates = new Vector<String>();
		PubChemWebService pubchem = null;
		ChemSpider chemSpider = null;
		
		if(!localdb) {
			try {
				pubchem = new PubChemWebService();
				chemSpider = new ChemSpider(chemSpiderToken);
				pubchem.setVerbose(verbose);
			} catch (ServiceException e1) {
				e1.printStackTrace();
			}
			try {
				candidates = Candidates.getOnline(database, databaseIDs, formula, exactMass, searchppm, useProxy, pubchem, verbose, chemSpider);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			if(query == null) query = new Query(dbuser, dbpass, dblink);
			double lowerBound = exactMass - PPMTool.getPPMDeviation(exactMass, searchppm); 
			double upperBound = exactMass + PPMTool.getPPMDeviation(exactMass, searchppm);
			List<CandidateMetChem> temp = query.queryByMass(lowerBound, upperBound, database, uniqueInchi);
			for (CandidateMetChem candidateMetChem : temp) {
				candidates.add(candidateMetChem.getAccession());
			}
			//query.closeConnection();
		}
		
		if(verbose) System.out.println(candidates.size()+" hit(s) in database!");

		//now fill executor!!!
		//number of threads depending on the available processors
	    int threads = Runtime.getRuntime().availableProcessors();
	    if(numberThreads != -1) threads = numberThreads;
	    //thread executor
	    ExecutorService threadExecutor = null;
	    threadExecutor = Executors.newFixedThreadPool(threads);
	    
	    FragmenterThread.setVerbose(verbose);
	    FragmenterThread.setSizeCandidates(candidates.size());
	    
	    endindex = Math.min(endindex, candidates.size());

	    FragmenterThread.setCandidateNumber(startindex);
	    
		for (int c = startindex - 1; c < endindex; c++) {
			FragmenterThread ft = new FragmenterThread(candidates.get(c), database, pubchem, spec, mzabs, mzppm, 
					molredundancycheck, molredundancycheck, treeDepth, hydrogenTest, neutralLossInEveryLayer, 
					bondEnergyScoring, breakOnlySelectedBonds, chemSpider, isStoreFragments, pathToStoreFrags, 
					sampleName, localdb, onlyChnopsCompounds, dblink, dbuser, dbpass);
			threadExecutor.execute(ft);		
		}
		
		
		threadExecutor.shutdown();
		
		//wait until all threads are finished
		while(!threadExecutor.isTerminated())
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}//sleep for 1000 ms
		}


		if(query != null) query.closeConnection();
		
	//	Map<Double, Vector<String>> scoresNormalized = Scoring.getCombinedScore(results.getRealScoreMap(), results.getMapCandidateToEnergy(), results.getMapCandidateToHydrogenPenalty());
		Map<Double, Vector>[] scoreInfo = Scoring.getCombinedScoreMoreInfo(results.getRealScoreMap(), results.getMapCandidateToEnergy(), results.getMapCandidateToHydrogenPenalty());
		Map<Double, Vector> scoresNormalized = scoreInfo[0];
		
		Double[] scores = new Double[scoresNormalized.size()];
		scores = scoresNormalized.keySet().toArray(scores);
		Arrays.sort(scores);

		//now collect the result
		Map<String, IAtomContainer> candidateToStructure = results.getMapCandidateToStructure();
		Map<String, Vector<PeakMolPair>> candidateToFragments = results.getMapCandidateToFragments();
		
		results.getRealScoreMap();
		
		List<MetFragResult> results = new ArrayList<MetFragResult>();
		for (int i = scores.length -1; i >=0 ; i--) {
			Vector<String> list = scoresNormalized.get(scores[i]);
			Vector<Double> retPeakCount = scoreInfo[1].get(scores[i]);
			Vector<Double> retBondEnergyCount = scoreInfo[2].get(scores[i]);
			for (int l = 0; l < list.size(); l++) {
				//get corresponding structure
				String string = list.get(l);
				IAtomContainer tmp = candidateToStructure.get(string);
				tmp = AtomContainerManipulator.removeHydrogens(tmp);
				
				MetFragResult curResult = null;
				
				if(isStoreFragments)
					curResult = new MetFragResult(string, tmp, scores[i], candidateToFragments.get(string).size(), candidateToFragments.get(string));
				else
					curResult = new MetFragResult(string, tmp, scores[i], candidateToFragments.get(string).size());
				
				curResult.setRawPeakMatchScore(retPeakCount.get(0));
				curResult.setRawBondEnergyScore(retBondEnergyCount.get(l));
				
				results.add(curResult);
			}
		}		
		
		return results;
		
	}


	/**
	 * used for sdf database of command line tool
	 * 
	 * @author c-ruttkies
	 * 
	 * @param spec
	 * @param mzabs
	 * @param mzppm
	 * @param searchppm
	 * @param molredundancycheck
	 * @param breakRings
	 * @param treeDepth
	 * @param hydrogenTest
	 * @param neutralLossInEveryLayer
	 * @param bondEnergyScoring
	 * @param breakOnlySelectedBonds
	 * @param limit
	 * @param isStoreFragments
	 * @param sdfFile
	 * @param filenameprefix
	 * @param ids
	 * @param massFilter
	 * @param pathToStoreFrags
	 * @param numberThreads
	 * @param verbose
	 * @param sampleName
	 * @param onlyBiologicalCompounds
	 * @return
	 */
	public static List<MetFragResult> startConvenienceSDF(WrapperSpectrum spec,
			double mzabs, double mzppm, double searchppm,
			boolean molredundancycheck, boolean breakRings, int treeDepth,
			boolean hydrogenTest, boolean neutralLossInEveryLayer, boolean bondEnergyScoring, boolean breakOnlySelectedBonds, 
			int limit, boolean isStoreFragments, String sdfFile, String filenameprefix, String[] ids,
			boolean massFilter, String pathToStoreFrags, int numberThreads,
			boolean verbose, String sampleName, boolean onlyBiologicalCompounds) {

		results = new FragmenterResult();
		List<IAtomContainer> candidates = null;
		Vector<String> forbiddenAtoms = new Vector<String>();
//		forbiddenAtoms.add("Se");
		forbiddenAtoms.add("X");
//		forbiddenAtoms.add("P");
		try
		{
			try {
				candidates = SDFFile.ReadSDFFileIteratively(sdfFile, forbiddenAtoms);
			} catch (CDKException e) {
				e.printStackTrace();
			}
		}
		catch(FileNotFoundException e)
		{
			System.err.println("SDF file not found!");
			return null;
		}
		
		if(verbose) System.out.println(candidates.size()+" hits in database!");
		
		//now fill executor!!!
		//number of threads depending on the available processors
	    int threads = Runtime.getRuntime().availableProcessors();
	    if(numberThreads != -1) threads = numberThreads;
		//thread executor
		ExecutorService threadExecutor = null;
	    threadExecutor = Executors.newFixedThreadPool(threads);
			
	    boolean[] filteredCandidates = null; 
	    int numCandidates = candidates.size();
	    if(massFilter) {
	    	try {
				filteredCandidates = filterCandidates(candidates, spec.getExactMass(), searchppm);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			} catch (CDKException e) {
				e.printStackTrace();
			}
	    	numCandidates = 0;
	    	for(int i = 0; i < filteredCandidates.length; i++)
	    		if(filteredCandidates[i]) numCandidates++;
	    	if(verbose) System.out.println("after mass filter "+numCandidates+" candidates");
	    }
	    FragmenterThread.setVerbose(verbose);
	    FragmenterThread.setSizeCandidates(numCandidates);
	    
	    
		for (int c = 0; c < candidates.size(); c++) {
			
			if(c > limit) {
				if(verbose) System.out.println("stopped at "+c+" compounds");
				break;
			}
				
			if(massFilter && filteredCandidates != null) {
				if(!filteredCandidates[c]) continue;
			}
			 
			threadExecutor.execute(new FragmenterThread(candidates.get(c), Integer.toString(c), "SDF", spec, mzabs, 
					mzppm, molredundancycheck, breakRings, treeDepth, false, hydrogenTest, neutralLossInEveryLayer, 
					bondEnergyScoring, breakOnlySelectedBonds, isStoreFragments, sampleName, onlyBiologicalCompounds, 
					pathToStoreFrags));	
		}

		threadExecutor.shutdown();
		
		while(!threadExecutor.isTerminated())
		{
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}//sleep for 1000 ms
		}

	//	Map<Double, Vector<String>> scoresNormalized = Scoring.getCombinedScore(results.getRealScoreMap(), results.getMapCandidateToEnergy(), results.getMapCandidateToHydrogenPenalty());
		Map<Double, Vector>[] scoreInfo = Scoring.getCombinedScoreMoreInfo(results.getRealScoreMap(), results.getMapCandidateToEnergy(), results.getMapCandidateToHydrogenPenalty());
		Map<Double, Vector> scoresNormalized = scoreInfo[0];
		Double[] scores = new Double[scoresNormalized.size()];
		scores = scoresNormalized.keySet().toArray(scores);
		Arrays.sort(scores);
		
		
		//now collect the result
		Map<String, IAtomContainer> candidateToStructure = results.getMapCandidateToStructure();
		Map<String, Vector<PeakMolPair>> candidateToFragments = results.getMapCandidateToFragments();
	
		List<MetFragResult> res = new ArrayList<MetFragResult>();
		for (int i = scores.length -1; i >=0 ; i--) {
			Vector<String> list = scoresNormalized.get(scores[i]);
			Vector<Double> retPeakCount = scoreInfo[1].get(scores[i]);
			Vector<Double> retBondEnergyCount = scoreInfo[2].get(scores[i]);
			for (int l = 0; l < list.size(); l++) {
				//get corresponding structure
				String string = list.get(l);
				IAtomContainer tmp = candidateToStructure.get(string);
				tmp = AtomContainerManipulator.removeHydrogens(tmp);
				
				MetFragResult curResult = new MetFragResult(string, tmp, scores[i], candidateToFragments.get(string).size(), candidateToFragments.get(string));
				
				try {
					curResult.setRawPeakMatchScore(retPeakCount.get(0));
					curResult.setRawBondEnergyScore(retBondEnergyCount.get(l));
				}
				catch(Exception e) {
					System.out.println(retPeakCount + " " + retBondEnergyCount);
					System.out.println(retPeakCount.size() + " " + retBondEnergyCount.size());
				}
				res.add(curResult);
			}
		}		
		return res;
	}
	
	/**
	 * filter candidates by mass (used for sdf database)
	 * 
	 * @param cands
	 * @param exactMass
	 * @param searchPPM
	 * @return
	 * @throws CloneNotSupportedException
	 * @throws CDKException
	 */
	private static boolean[] filterCandidates(List<IAtomContainer> cands, double exactMass, double searchPPM) throws CloneNotSupportedException, CDKException {
		boolean[] filteredCands = new boolean[cands.size()];
		
		
		IMolecularFormula molFormula;
		for(int i = 0; i < cands.size(); i++) {
			try {
				IAtomContainer molecule = (IAtomContainer)cands.get(i).clone();
				AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
				CDKHydrogenAdder hAdder = CDKHydrogenAdder.getInstance(molecule.getBuilder());
				hAdder.addImplicitHydrogens(molecule);
				AtomContainerManipulator.convertImplicitToExplicitHydrogens(molecule);
				molFormula = MolecularFormulaManipulator.getMolecularFormula(molecule);
				Double massDoubleOrig = MolecularFormulaTools.getMonoisotopicMass(molFormula);
				if(Math.abs(massDoubleOrig - exactMass) <= PPMTool.getPPMDeviation(exactMass, searchPPM)) {
					filteredCands[i] = true;
				}
			}
			catch(Exception e) {
				continue;
			}
		}
		return filteredCands;
	}
	
	public static Query getQuery() {
		return query;
	}
}
