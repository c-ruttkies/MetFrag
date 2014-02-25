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

package de.ipbhalle.metfrag.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.iterator.IteratingMDLConformerReader;
import org.openscience.cdk.io.iterator.IteratingSMILESReader;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import de.ipbhalle.metfrag.main.Config;



import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class PubChemToDatabaseParallel implements Runnable {
	
	private static Connection con;
	private String path;
	private String file;
	
	public PubChemToDatabaseParallel(String path, String filename) {
		this.path = path;
		this.file = filename;
	}
	
	//insert the data in postgres database
	@Override public void run()
	{
		long start = System.currentTimeMillis();
		
		File sdfFile = new File(path + file);
		IteratingMDLConformerReader reader = null;
		try {
			reader = new IteratingMDLConformerReader(new GZIPInputStream(new FileInputStream(sdfFile)), DefaultChemObjectBuilder.getInstance());
		} catch (FileNotFoundException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		int count = 0;
		int countPackage = 0;
		
		System.out.println("Processing: " + path + file);
		
		PreparedStatement pstmtCompound = null;
		PreparedStatement pstmtSubstance = null;
		PreparedStatement pstmtName = null;
		Statement stmt = null;
		
		try
		{
			while (reader.hasNext()) {
				
				try {
					if(con == null || con.isClosed())
					{
						String driver = "org.postgresql.Driver"; 
						try {
							Class.forName(driver);
							DriverManager.registerDriver(new org.postgresql.Driver()); 
					        //database data
					        Config c = new Config("outside");
					        String url = c.getJdbcPostgres();
					        String username = c.getUsernamePostgres();
					        String password = c.getPasswordPostgres();
					        con = DriverManager.getConnection(url, username, password);
						    con.setAutoCommit(false);
						    
						    
						    stmt = con.createStatement();
							pstmtCompound = con.prepareStatement("INSERT INTO compound (compound_id, mol_structure, exact_mass, formula, smiles, inchi, inchi_key_1, inchi_key_2, inchi_key_3) VALUES (?,cast(? as molecule),?,?,?,?,?,?,?)");
							pstmtSubstance = con.prepareStatement("INSERT INTO substance (substance_id, library_id, compound_id, accession) VALUES (?,?,?,?)");
							pstmtName = con.prepareStatement("INSERT INTO name (substance_id, name) VALUES (?,?)");
	
						} catch (ClassNotFoundException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 
					}
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
					
	
				IAtomContainer molecule = (IAtomContainer)reader.next();
				  
			    Map<Object, Object> properties = molecule.getProperties();
			    
			    //RECORD data
			    String smiles = (String)properties.get("PUBCHEM_OPENEYE_CAN_SMILES");
			    Integer pubChemID = Integer.parseInt((String)properties.get("PUBCHEM_COMPOUND_CID"));			    
			    double exactMass = Double.parseDouble((String)properties.get("PUBCHEM_EXACT_MASS"));
			    String molecularFormula = (String)properties.get("PUBCHEM_MOLECULAR_FORMULA");
			    String inchi = (String)properties.get("PUBCHEM_NIST_INCHI");
			    String inchiKey = (String)properties.get("PUBCHEM_NIST_INCHIKEY");
			    //this should plit in 3 parts
			    String inchiKeyArray[] = inchiKey.split("-");
			    
			    //first check if the compound already exists (do not insert the same compound from different databases in the compound table)
	//			    PreparedStatement pstmtCheck = con.prepareStatement("SELECT compound_id from compound where inchi_key_1 = ? and inchi_key_2 = ? and inchi_key_3 = ?");
	//		        pstmtCheck.setString(1, inchiKeyArray[0]);
	//		        pstmtCheck.setString(2, inchiKeyArray[1]);
	//		        pstmtCheck.setString(3, inchiKeyArray[2]);
	//		        ResultSet res = pstmtCheck.executeQuery();
		        Integer compoundID = null;
	//		        while(res.next()){
	//		        	compoundID = res.getInt(1);
	//		        }
		        
		        //no previously inserted compound matches
		        if(compoundID == null || compoundID == 0)
		        {
		        	//now get the next insert id
				    ResultSet rs = stmt.executeQuery("SELECT nextval('compound_compound_id_seq')");
				    rs.next();
				    compoundID = rs.getInt(1);
								    
				    
			        pstmtCompound.setInt(1, compoundID);
			        pstmtCompound.setString(2, smiles);
			        pstmtCompound.setDouble(3, exactMass);
			        pstmtCompound.setString(4, molecularFormula);
			        pstmtCompound.setString(5, smiles);
			        pstmtCompound.setString(6, inchi);
			        pstmtCompound.setString(7, inchiKeyArray[0]);
			        pstmtCompound.setString(8, inchiKeyArray[1]);
			        pstmtCompound.setString(9, inchiKeyArray[2]);
	//			        pstmt.executeUpdate();
			        pstmtCompound.addBatch();
				    
		        }
		        
		        //now get the next substance_id
			    ResultSet rs = stmt.executeQuery("SELECT nextval('substance_substance_id_seq')");
			    rs.next();
			    Integer substanceID = rs.getInt(1);
		        
		        //insert the information also in substance table
		        
		        //pubchem has library id = 2
		        pstmtSubstance.setInt(1, substanceID);
		        pstmtSubstance.setInt(2, 2);
		        pstmtSubstance.setInt(3, compoundID);
		        pstmtSubstance.setString(4, Integer.toString(pubChemID));	
		        pstmtSubstance.addBatch();
	//		        pstmt.executeUpdate();
	
		        //iupac name data
			    String name = (String)properties.get("PUBCHEM_IUPAC_NAME");
				if(name != null && name != "")
				{
			        pstmtName.setInt(1, substanceID);
			        pstmtName.setString(2, name);
	//			        pstmtName.executeUpdate();
			        pstmtName.addBatch();
				}
			    count++;
			    countPackage++;
			    
			    if(countPackage >= 1000)
			    {
			    	pstmtCompound.executeBatch();
					pstmtSubstance.executeBatch();
					pstmtName.executeBatch();
					con.commit();
					con.close();
					countPackage = 0;
			    }			    
			    
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			try {
				if(!con.isClosed())
				{
					pstmtCompound.executeBatch();
					pstmtSubstance.executeBatch();					
					pstmtName.executeBatch();
					con.commit();
					con.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				try {
					con.close();
				} catch (SQLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
				
		long end = System.currentTimeMillis();
		System.out.println("Execution time was "+(end-start)+" ms.");
		System.out.println("DONE: " + path + file);
		System.out.println("Got " + count + " structures!");	
		
	}
	
	
	//insert the data in postgres database
	public void createTSV(String outputFolder)
	{
		long start = System.currentTimeMillis();
		
		try {
			System.out.println("Processing: " + path + file);
						
			File sdfFile = new File(path + file);
			IteratingMDLConformerReader reader = new IteratingMDLConformerReader(new GZIPInputStream(new FileInputStream(sdfFile)), DefaultChemObjectBuilder.getInstance());
			int count = 0;
			
			//read in last id inserted
			File fCount = new File(outputFolder + "count.tsv");
			if(fCount.exists())
			{
				FileReader fr = new FileReader(fCount);
				BufferedReader br = new BufferedReader(fr); 
				String s;
				while((s = br.readLine()) != null) {
					count = Integer.parseInt(s);
				}
			}			
		
			FileWriter fwCompound = new FileWriter(outputFolder + "compound.tsv", true);
			FileWriter fwSubstance = new FileWriter(outputFolder + "substance.tsv", true);
			FileWriter fwName = new FileWriter(outputFolder + "name.tsv", true);
			FileWriter fwCount = new FileWriter(outputFolder + "count.tsv", false);
			
			
			while (reader.hasNext()) {
				IAtomContainer molecule = (IAtomContainer)reader.next();
				  
			    Map<Object, Object> properties = molecule.getProperties();
			    
			    //RECORD data
			    String smiles = (String)properties.get("PUBCHEM_OPENEYE_CAN_SMILES");
			    Integer pubChemID = Integer.parseInt((String)properties.get("PUBCHEM_COMPOUND_CID"));			    
			    double exactMass = Double.parseDouble((String)properties.get("PUBCHEM_EXACT_MASS"));
			    String molecularFormula = (String)properties.get("PUBCHEM_MOLECULAR_FORMULA");
			    String inchi = (String)properties.get("PUBCHEM_NIST_INCHI");
			    String inchiKey = (String)properties.get("PUBCHEM_NIST_INCHIKEY");
			    //this should plit in 3 parts
			    String inchiKeyArray[] = inchiKey.split("-");
			    
			    /*
			    COMPOUND_ID SERIAL NOT NULL,
				MOL_STRUCTURE MOLECULE NOT NULL,
				EXACT_MASS DECIMAL(8,4) NOT NULL,
				FORMULA VARCHAR NOT NULL,
				SMILES VARCHAR NOT NULL,
				INCHI VARCHAR NOT NULL,
				-- First part of the InChi key (skeleton)
				INCHI_KEY_1 VARCHAR(14) NOT NULL,
				-- Second part
				INCHI_KEY_2 VARCHAR(10) NOT NULL,
				-- The last part of the InChI key.
				INCHI_KEY_3 VARCHAR(1)
				*/
			    
			    fwCompound.write(count + "\t" + inchi + "\t" + exactMass + "\t" + molecularFormula + "\t" + smiles + "\t" + inchi + "\t" + inchiKeyArray[0] + "\t"  + inchiKeyArray[1] + "\t"  + inchiKeyArray[2] + "\n");
			    
		        
		        //insert the information also in substance table
		        
			    /*
			    SUBSTANCE_ID SERIAL NOT NULL,
				LIBRARY_ID INTEGER NOT NULL,
				COMPOUND_ID INTEGER NOT NULL,
				ACCESSION VARCHAR,
			    */
			    fwSubstance.write(count + "\t" + "2" + "\t" + count + "\t" + Integer.toString(pubChemID) + "\n");
			    
			    /*
			    NAME VARCHAR,
				SUBSTANCE_ID INTEGER NOT NULL
			    */

		        //iupac name data
			    String name = (String)properties.get("PUBCHEM_IUPAC_NAME");
			    fwName.write(count + "\t" + name + "\n");
				
			    
			    count++;			    
			}
			
			fwCompound.close();
			fwSubstance.close();
			fwName.close();
			
			fwCount.write(Integer.toString(count));
			fwCount.close();
			
			
			long end = System.currentTimeMillis();
			System.out.println("Execution time was "+(end-start)+" ms.");
			System.out.println("DONE: " + path + file);
			System.out.println("Got " + count + " structures!");
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
//	@Override public void run()
//	{
//		try {
//			System.out.println("Processing: " + path + file);
//			
//			Statement stmt = null;
//		    stmt = con.createStatement();
//			
//			File sdfFile = new File(path + file);
//			IteratingMDLReader reader = new IteratingMDLReader(new GZIPInputStream(new FileInputStream(sdfFile)), DefaultChemObjectBuilder.getInstance());
//
//			int count = 0;
//					
//			while (reader.hasNext()) {
//				IAtomContainer molecule = (IAtomContainer)reader.next();
//				  
//			    Map<Object, Object> properties = molecule.getProperties();
//			    
//			    //RECORD data
//			    String smiles = (String)properties.get("PUBCHEM_OPENEYE_CAN_SMILES");
//			    Integer pubChemID = Integer.parseInt((String)properties.get("PUBCHEM_COMPOUND_CID"));			    
//			    double exactMass = Double.parseDouble((String)properties.get("PUBCHEM_EXACT_MASS"));
//			    String molecularFormula = (String)properties.get("PUBCHEM_MOLECULAR_FORMULA");
//			    String inchi = (String)properties.get("PUBCHEM_NIST_INCHI");
//			    int chonsp = Tools.checkCHONSP(molecularFormula);
//			    
//		        Date date = new Date();
//		        java.sql.Date dateSQL = new java.sql.Date(date.getTime());
//			    
//		        
////		        stmt.executeUpdate("INSERT INTO RECORD (ID, DATE, FORMULA, EXACT_MASS, SMILES, IUPAC, CHONSP) " +
////		        		"VALUES ('" + pubChemID.toString() +  "','" + dateSQL + "','" + molecularFormula + "'," + exactMass + ",'" + smiles + "','" + inchi + "',"  + chonsp + ")");
//		        
//		        PreparedStatement pstmt = con.prepareStatement("INSERT INTO RECORD (ID, DATE, FORMULA, EXACT_MASS, SMILES, IUPAC, CHONSP) VALUES (?,?,?,?,?,?,?)");
//		        pstmt.setString(1, pubChemID.toString());
//		        pstmt.setDate(2, dateSQL);
//		        pstmt.setString(3, molecularFormula);
//		        pstmt.setDouble(4, exactMass);
//		        pstmt.setString(5, smiles);
//		        pstmt.setString(6, inchi);
//		        pstmt.setInt(7, chonsp);
//		        
//		        pstmt.executeUpdate();
//
//		        
//		        
//		        //name data
//			    Map<String, String> names = new HashMap<String, String>();
//			    names.put("PUBCHEM_IUPAC_CAS_NAME", (String)properties.get("PUBCHEM_IUPAC_CAS_NAME"));
//			    names.put("PUBCHEM_IUPAC_OPENEYE_NAME", (String)properties.get("PUBCHEM_IUPAC_OPENEYE_NAME"));
//			    names.put("PUBCHEM_IUPAC_CAS_NAME", (String)properties.get("PUBCHEM_IUPAC_CAS_NAME"));
//			    names.put("PUBCHEM_IUPAC_NAME", (String)properties.get("PUBCHEM_IUPAC_NAME"));
//			    names.put("PUBCHEM_IUPAC_SYSTEMATIC_NAME", (String)properties.get("PUBCHEM_IUPAC_SYSTEMATIC_NAME"));
//			    names.put("PUBCHEM_IUPAC_TRADITIONAL_NAME", (String)properties.get("PUBCHEM_IUPAC_TRADITIONAL_NAME"));
//			    for (String name : names.values()) {
//					if(name != null && name != "")
//					{
//						PreparedStatement pstmtName = con.prepareStatement("INSERT INTO CH_NAME (ID, NAME) VALUES (?,?)");
//				        pstmtName.setString(1, pubChemID.toString());
//				        pstmtName.setString(2, name);
//				        pstmtName.executeUpdate();
////						stmt.executeUpdate("INSERT INTO CH_NAME (ID, NAME) VALUES (\"" + pubChemID.toString() + "\",\"" + name + "\")");
//					}
//				}
//			    
//			    
//			    //link data
//			    PreparedStatement pstmtLink = con.prepareStatement("INSERT INTO CH_LINK (ID, PUBCHEM) VALUES (?,?)");
//		        pstmtLink.setString(1, pubChemID.toString());
//		        pstmtLink.setString(2, "CID:" + pubChemID.toString());
//		        pstmtLink.executeUpdate();
////			    stmt.executeUpdate("INSERT INTO CH_LINK (ID, PUBCHEM) VALUES ('" + pubChemID.toString() + "','" + "CID:" + pubChemID.toString() + "')");
//
//
//			    count++;
//			}
//			System.out.println("DONE: " + path + file);
//			System.out.println("Got " + count + " structures!");
//		} catch (NumberFormatException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SQLException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
	
	public static void main(String[] args) {
		
		String[] files = null;
		
		if(args[0] != null)
		{
			files = args[0].split(";");
		}
		else
		{
			System.err.println("Error no argument (filename) given!");
			System.exit(1);
		}
		
		boolean isMysql = false;
		boolean isWriteTSV = true;
		String outputFolder = "/home/swolf/sgeQsubScripts/PubChemDataTSV/";
		
		String path = "/vol/mirrors/pubchem/";		
		//starting from this id the files are read in again!
		//be shure to delete larger ids!!!
		String startID = "000000001";
		
		// Connection
	    java.sql.Connection conTemp = null; 
	    
	    //number of threads depending on the available processors
//	    int threads = Runtime.getRuntime().availableProcessors();
	    int threads = 1;
	    
	    //thread executor
	    ExecutorService threadExecutor = null;
		
		try
		{
			if(isWriteTSV)
			{
				for (int i = 0; i < files.length; i++) {
					PubChemToDatabaseParallel p = new PubChemToDatabaseParallel(path, files[i]);
					p.createTSV(outputFolder);
				}				
			}
			else if(isMysql)
			{
				String driver = "com.mysql.jdbc.Driver"; 
				Class.forName(driver); 
				DriverManager.registerDriver(new com.mysql.jdbc.Driver()); 
		        // JDBC-driver
		        Class.forName(driver);
		        //databse data
		        Config c = new Config();
		        String url = c.getJdbc();
		        String username = c.getUsername();
		        String password = c.getPassword();
		        con = DriverManager.getConnection(url, username, password);
			    
		
				//readCompounds = new HashMap<Integer, IAtomContainer>();
				
				//loop over all files in folder
//				File f = new File(path);
//				File files[] = f.listFiles();
//				Arrays.sort(files);
				
				//queue stores all files to be read in
				Queue<String> queue = new LinkedList<String>();
				for (int i = 0; i < files.length; i++) {
					queue.offer(files[i]);
				}
				
				threadExecutor = Executors.newFixedThreadPool(threads);
				
				while(!queue.isEmpty())
				{
					threadExecutor.execute(new PubChemToDatabaseParallel(path, queue.poll()));
				}
				
				threadExecutor.shutdown();
			}
			else
			{		
				//readCompounds = new HashMap<Integer, IAtomContainer>();
				
				//loop over all files in folder
//				File f = new File(path);
//				File files[] = f.listFiles();
//				Arrays.sort(files);
				
				//queue stores all files to be read in
				Queue<String> queue = new LinkedList<String>();
				for (int i = 0; i < files.length; i++) {
					queue.offer(files[i]);
				}
				
				threadExecutor = Executors.newFixedThreadPool(threads);
				
				while(!queue.isEmpty())
				{
					threadExecutor.execute(new PubChemToDatabaseParallel(path, queue.poll()));
				}
				threadExecutor.shutdown();
				
				while(!threadExecutor.isTerminated())
				{
					try {
					       Thread.currentThread().sleep(1000);
					}
					catch (InterruptedException e) {
					    e.printStackTrace();
					}
				}
				
			}
			
			
				
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally 
		{
		      try 
		      {
		    	  if(con != null)
		    		  con.close();
		      } 
		      catch(SQLException e) {
		    	  e.printStackTrace();
		      }
		}
		
	}

}
