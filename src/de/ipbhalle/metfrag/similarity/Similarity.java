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

package de.ipbhalle.metfrag.similarity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.similarity.Tanimoto;
import org.openscience.cdk.smiles.SmilesParser;

import de.ipbhalle.metfrag.molDatabase.PubChemLocal;

public class Similarity {
	
	private float[][] matrix = null;
	private Map<String, IAtomContainer> candidateToStructure = null;
	private Map<String, Integer> candidateToPosition = null;
	private Map<String, String> candidatesToSmiles = null;
	private StringBuilder allSimilarityValues = new StringBuilder();
	private boolean hasAtomContainer = false;
	
	public Similarity(Map<String,String> candidatesToSmiles, boolean completeMatrix) throws CDKException
	{
		matrix = new float[candidatesToSmiles.size()][candidatesToSmiles.size()];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				matrix[i][j] = Float.POSITIVE_INFINITY;
			}
		}
		this.candidateToStructure = new HashMap<String, IAtomContainer>(candidatesToSmiles.size());
		this.candidatesToSmiles = candidatesToSmiles;
		initializePositions();
		calculateSimilarity(completeMatrix);
	}
	
	public Similarity(Map<String, IAtomContainer> candidateToStructure, boolean check, boolean completeMatrix) throws CDKException
	{
		matrix = new float[candidateToStructure.size()][candidateToStructure.size()];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix.length; j++) {
				matrix[i][j] = Float.POSITIVE_INFINITY;
			}
		}
		this.candidateToStructure = candidateToStructure;
		this.hasAtomContainer = check;
		initializePositions();
		calculateSimilarity(completeMatrix);
	}
	
	//initialize the position of the candidates in the matrix
	private void initializePositions()
	{
		int i = 0;
		candidateToPosition = new HashMap<String, Integer>();
		if(hasAtomContainer)
		{
			for (String candidate : candidateToStructure.keySet()) {
				candidateToPosition.put(candidate, i);
				i++;
			}
		}
		else
		{
			for (String candidate : candidatesToSmiles.keySet()) {
				candidateToPosition.put(candidate, i);
				i++;
			}
		}
	}
	
	
	/**
	 * Returns a similarity matrix based on the tanimoto distance
	 * of the fingerprints. (Upper triangular matrix)
	 * 
	 * @param molList the mol list
	 * @param similarityThreshold the similarity threshold
	 * 
	 * @return the float[][]
	 * 
	 * @throws CDKException the CDK exception
	 */
	private float[][] calculateSimilarity(boolean completeMatrix) throws CDKException
	{
		Map<String, BitSet> candidateToFingerprint = new HashMap<String, BitSet>();
		
		if(hasAtomContainer)
		{
			for (String strCandidate : candidateToStructure.keySet()) {
				Fingerprinter f = new Fingerprinter();
				IBitFingerprint fp = f.getBitFingerprint(candidateToStructure.get(strCandidate));
				candidateToFingerprint.put(strCandidate, fp.asBitSet());
			}
		}
		else
		{
			SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
			for (String string : candidatesToSmiles.keySet()) {
				try
				{
					IAtomContainer mol = sp.parseSmiles(candidatesToSmiles.get(string));
					Fingerprinter f = new Fingerprinter();
					IBitFingerprint fp = f.getBitFingerprint(mol);
					candidateToFingerprint.put(string, fp.asBitSet());
				}
				catch(Exception e)
				{
					System.err.println("Error in smiles!" + e.getMessage());
				}
			}
		}
		

		int countJ = 0;
		int countI = 0;
		
		if(!hasAtomContainer)
		{
			for (String candidate1 : candidatesToSmiles.keySet()) {
	//			System.out.print(candidate1 + " ");
				for (String candidate2 : candidatesToSmiles.keySet()) {
					if((countJ < countI || candidate1.equals(candidate2)) && !completeMatrix)
					{
	//					System.out.print("x ");
						countJ++;
						continue;
					}		
					float similarity = compareFingerprints(candidateToFingerprint.get(candidate1), candidateToFingerprint.get(candidate2));
					matrix[countI][countJ] = similarity;
					//allSimilarityValues.append(similarity + "\n");
					
					countJ++;
	//				System.out.print(compareFingerprints(candidateToFingerprint.get(candidate1), candidateToFingerprint.get(candidate2)) + " ");
				}
				countJ = 0;
				countI++;
	//			System.out.println("");
			}
		}
		else
		{
			for (String candidate1 : candidateToStructure.keySet()) {
	//			System.out.print(candidate1 + " ");
				for (String candidate2 : candidateToStructure.keySet()) {
					if((countJ < countI || candidate1.equals(candidate2))  && !completeMatrix)
					{
	//					System.out.print("x ");
						countJ++;
						continue;
					}		
					float similarity = compareFingerprints(candidateToFingerprint.get(candidate1), candidateToFingerprint.get(candidate2));
					matrix[countI][countJ] = similarity;
					//allSimilarityValues.append(similarity + "\n");
					
					countJ++;
	//				System.out.print(compareFingerprints(candidateToFingerprint.get(candidate1), candidateToFingerprint.get(candidate2)) + " ");
				}
				countJ = 0;
				countI++;
	//			System.out.println("");
			}
		}
		
		
		return matrix;
	}
	
	/**
	 * Gets the all similarity values.
	 * 
	 * @return the all similarity values
	 */
	public StringBuilder getAllSimilarityValues()
	{
		return allSimilarityValues;
	}
	
	/**
	 * Gets the tanimoto distance between two candidate structures.
	 * 
	 * @param candidate1 the candidate1
	 * @param candidate2 the candidate2
	 * 
	 * @return the tanimoto distance
	 */
	public float getTanimotoDistance(String candidate1, String candidate2)
	{
		int pos1 = 0;
		int pos2 = 0;
		try
		{
			pos1 = candidateToPosition.get(candidate1);
			pos2 = candidateToPosition.get(candidate2);
		}
		catch(NullPointerException e)
		{
			return 0;
		}
		if(matrix[pos1][pos2] != Float.POSITIVE_INFINITY)
			return matrix[pos1][pos2];
		else
			return matrix[pos2][pos1];
	}
	
	
	/**
	 * Checks if is identical.
	 * 
	 * @param similar1 the similar1
	 * @param similar2 the similar2
	 * 
	 * @return true, if is identical
	 */
	private boolean isIdentical(List<String> similar1, List<String> similar2)
	{		
		String[] candidatesToCompare = new String[similar1.size()];
		candidatesToCompare = similar1.toArray(candidatesToCompare);
		Arrays.sort(candidatesToCompare);
		
		boolean isAlreadyContained = false;
		
		String[] candidateListTemp = new String[similar2.size()];
		candidateListTemp = similar2.toArray(candidateListTemp);
		Arrays.sort(candidateListTemp);
		
		if(Arrays.equals(candidatesToCompare, candidateListTemp))
		{
			isAlreadyContained = true;
		}

		return isAlreadyContained;
	}
	

	public Map<String, Integer> getCandidateToPosition() {
		return candidateToPosition;
	}

	public void setCandidateToPosition(Map<String, Integer> candidateToPosition) {
		this.candidateToPosition = candidateToPosition;
	}

	
	/**
	 * Checks if the given structures are isomorph.
	 * 
	 * @param candidate1 the candidate1
	 * @param candidate2 the candidate2
	 * 
	 * @return true, if is isomorph
	 * 
	 * @throws CDKException the CDK exception
	 */
	public boolean isIsomorph(String candidate1, String candidate2) throws CDKException
	{
		IAtomContainer cand1 = null;
		IAtomContainer cand2 = null;
		
		if(hasAtomContainer)
		{
			cand1 = this.candidateToStructure.get(candidate1);
			cand2 = this.candidateToStructure.get(candidate2);
		}
		else
		{
			SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
			cand1 = sp.parseSmiles(candidatesToSmiles.get(candidate1));
			cand2 = sp.parseSmiles(candidatesToSmiles.get(candidate2));
		}
		UniversalIsomorphismTester uit = new UniversalIsomorphismTester();
		return uit.isIsomorph(cand1, cand2);
	}
	
	
	
	
	
	/**
	 * Compare fingerprints by returning the Tanimoto distance.
	 * 
	 * @param bitSet1 the bit set1
	 * @param bitSet2 the bit set2
	 * 
	 * @return the float
	 * 
	 * @throws CDKException the CDK exception
	 */
	private float compareFingerprints(BitSet bitSet1, BitSet bitSet2) throws CDKException
	{
		return Tanimoto.calculate(bitSet1, bitSet2);
	}
	
	
	/**
	 * Gets the similarity matrix.
	 * 
	 * @return the similarity matrix
	 */
	public float[][] getSimilarityMatrix()
	{
		return this.matrix;
	}
	
	
	
	public static void main(String[] args) {
		SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
		try {
			String mol = "C1C(OC2=CC(=CC(=C2C1=O)O)O)C3=CC=CC=C3O";
			String mol1 = "C1C(OC2=CC(=CC(=C2C1=O)O)O)C3=CC=C(C=C3)O";
			String mol2 = "C1C(OC(=O)C2=C(C=C(C=C21)O)O)C3=CC=C(C=C3)O";
			String mol3 = "C1C(OC2=CC(=CC(=C2C1=O)O)O)C3=CC=C(C=C3)O";
			String mol4 = "C1C(OC2=CC(=CC(=C2C1=O)O)O)C3=CC=C(C=C3)O";
			String mol5 = "C1C(OC2=CC(=CC(=C2C1=O)O)O)C3=CC(=CC=C3)O";
			String mol6 = "C1C(OC2=CC(=C(C=C2C1=O)O)O)C3=CC=C(C=C3)O";
			String mol7 = "C1C(OC2=CC(=CC(=C2C1=O)O)O)C3=CC=CC=C3O";
			String mol8 = "C1C(C(=O)C2=CC(=C(C=C2O1)O)O)C3=CC=C(C=C3)O";
			Map<String, String> testMap = new HashMap<String, String>();
			testMap.put("179999", mol);
			testMap.put("932", mol1);
			testMap.put("10333412", mol2);
			testMap.put("439246", mol3);
			testMap.put("667495", mol4);
			testMap.put("113638", mol5);
			testMap.put("23724670", mol6);
			testMap.put("13889010", mol7);
			testMap.put("125100", mol8);
			
			List<String> cands = new ArrayList<String>();
			cands.add("179999");
			cands.add("932");
			cands.add("10333412");
			cands.add("439246");
			cands.add("667495");
			cands.add("113638");
			cands.add("23724670");
			cands.add("13889010");
			cands.add("125100");
			
			Similarity sim = new Similarity(testMap, false);
//			System.out.println(sim.getTanimotoDistance("cand3", "cand1"));
//			DEPRECATED
//			List<SimilarityGroup> groupedCandidates = sim.getTanimotoDistanceList(cands);
//			for (SimilarityGroup similarityGroup : groupedCandidates) {
//				if(similarityGroup.getSimilarCompounds().size() == 0)
//					System.out.print("Single: " + similarityGroup.getCandidateTocompare() + "\n");
//				else
//				{
//					System.out.print("Group of " + similarityGroup.getSimilarCompounds().size() + " " + similarityGroup.getCandidateTocompare() +  ": ");
//					for (int i = 0; i < similarityGroup.getSimilarCompounds().size(); i++) {
//						System.out.print(similarityGroup.getSimilarCompounds().get(i).getCompoundID() + "(" + similarityGroup.getTanimotoSimilarities().get(i) + ") ");
//					}
//					System.out.println("");
//				}
//			}
			
		} catch (InvalidSmilesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CDKException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
