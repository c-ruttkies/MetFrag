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

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import de.ipbhalle.metfrag.bondPrediction.AtomProperty;
import de.ipbhalle.metfrag.bondPrediction.ChargeResult;
import de.ipbhalle.metfrag.bondPrediction.Charges;
import de.ipbhalle.metfrag.similarity.Subgraph;
import de.ipbhalle.metfrag.tools.renderer.StructureRenderer.MoleculePanel;


import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.RendererModel.ColorHash;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.font.IFontManager;
import org.openscience.cdk.renderer.generators.AtomNumberGenerator;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.ExtendedAtomGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.generators.IGeneratorParameter;
import org.openscience.cdk.renderer.generators.RadicalGenerator;
import org.openscience.cdk.renderer.generators.RingGenerator;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator.KekuleStructure;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;

import java.awt.*;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class WritePDFTable extends MoleculeCell {
    private PdfPTable table;
    private Document document;
    private int width;
    private int height;
    int ncol = 3;
    private int imageCount = 0;
    
    
    
    /**
     * Instantiates a new write pdf table. This is mainly for debugging the gasteiger marsili charges
     * 
     * @param odir the odir
     * @param width the width
     * @param height the height
     * @param chargeResults the charge results
     */
    public WritePDFTable(String odir, int width, int height, List<ChargeResult> chargeResults) {
        
    	this.width = width;
    	this.height = height;
    	
        try {
            File file = new File(odir);
            document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));

            float[] widths = new float[ncol];
            for (int i = 0; i < ncol; i += 3) {
                    widths[i] = 2.5f;
                    widths[i + 1] = 0.75f;
                    widths[i + 2] = 0.75f;
            }
            table = new PdfPTable(widths);
            document.open();
            
            boolean drawPartialCharges = true;
            
            for (ChargeResult result : chargeResults) {
            	
            	
            	if(drawPartialCharges)
            	{
            		PdfPCell cellBonds = new PdfPCell();
                	PdfPCell cellBondsDist = new PdfPCell();
                    Phrase phraseBonds = new Phrase();
                    Phrase phraseBondsDist = new Phrase();

                    com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(writeMOL2PNGFile(result.getOriginalMol()).getAbsolutePath());
                    image.setAbsolutePosition(0, 0);
                    table.addCell(image);
                    
                    String stringAtoms = "";
                    String stringAtomsCharge = "";
                    for (IAtom atom : result.getOriginalMol().atoms()) {			
                    	if(!atom.getSymbol().equals("H") && !atom.getSymbol().equals("C"))
                    	{
	                    	stringAtoms += atom.getSymbol() + (Integer.parseInt(atom.getID()) + 1) + "\n";   
	                    	stringAtomsCharge += Math.round(atom.getCharge() * 100.0) / 100.0 + "\n";
                    	}
            		}
                    
                    addProperty(phraseBonds, stringAtoms);
                    addProperty(phraseBondsDist, stringAtomsCharge);
                    cellBonds.addElement(phraseBonds);
                    cellBondsDist.addElement(phraseBondsDist);
                    table.addCell(cellBonds);
                    table.addCell(cellBondsDist);
                    drawPartialCharges = false;
            	}        	
            	
            	PdfPCell cellBonds = new PdfPCell();
            	PdfPCell cellBondsDist = new PdfPCell();
                Phrase phraseBonds = new Phrase();
                Phrase phraseBondsDist = new Phrase();

                com.lowagie.text.Image image = com.lowagie.text.Image.getInstance(writeMOL2PNGFile(result.getOriginalMol(), result.getMolWithProton()).getAbsolutePath());
                image.setAbsolutePosition(0, 0);
                table.addCell(image);
                
                String stringPDFBonds = "";
                String stringPDFBondsDist = "";
                String[] lines = result.getChargeString().split("\n");
                for (int i = 0; i < lines.length; i++) {
                	boolean carbonHydrogenBond = lines[i].matches("[A-Z]+[0-9]+-H[0-9]+.*");
					if(!carbonHydrogenBond)
					{
						String[] linesArr = lines[i].split("\t");
						stringPDFBondsDist += linesArr[1] + "\n";
						stringPDFBonds += linesArr[0] + "\n";
					}
				}
                
                addProperty(phraseBonds, stringPDFBonds);
                addProperty(phraseBondsDist, stringPDFBondsDist);
                cellBonds.addElement(phraseBonds);
                cellBondsDist.addElement(phraseBondsDist);
                table.addCell(cellBonds);
                table.addCell(cellBondsDist);
			}
            
            document.add(table);
            document.close();
            
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    
    private void addProperty(Phrase phrase, String text) {
        phrase.add(new Chunk(
                text + "\n",
                FontFactory.getFont(FontFactory.HELVETICA)
        ));
    }
    
    private File writeMOL2PNGFile(IAtomContainer originalMol, IAtomContainer protonatedMol) {

        RenderedImage rImage = null;
        File tempFile = null;
        
        try {
        	tempFile = File.createTempFile(imageCount + "_Temp", ".png");
            rImage = (BufferedImage) getImage4MOL(originalMol, protonatedMol);
            ImageIO.write(rImage, "png", tempFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return tempFile;
    }
    
    
    private File writeMOL2PNGFile(IAtomContainer originalMol) {

        RenderedImage rImage = null;
        File tempFile = null;
        
        try {
        	tempFile = File.createTempFile(imageCount + "_Temp", ".png");
            rImage = (BufferedImage) getImage4MOL(originalMol);
            ImageIO.write(rImage, "png", tempFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return tempFile;
    }
    

    /**
     * Gets the image 4 mol.
     * 
     * @param protonatedMol the mol ac
     * 
     * @return the image4 mol
     * 
     * @throws Exception the exception
     */
    private RenderedImage getImage4MOL(IAtomContainer mol) throws Exception {
    	    	
//    	protonatedMol = AtomContainerManipulator.removeHydrogens(protonatedMol);
        // creates CDK Molecule object and get the renderer
    	IAtomContainer molSource = mol.clone();
    	
    	StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		sdg.setMolecule(molSource);
		try {
	       sdg.generateCoordinates();
		} catch (Exception e) { }
		molSource = sdg.getMolecule();
		
		List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
		generators.add(new BasicSceneGenerator());
        generators.add(new BasicBondGenerator());
        generators.add(new BasicAtomGenerator());
        generators.add(new RingGenerator());
        generators.add(new AtomNumberGenerator());
//        generators.add(new RadicalGenerator());
        
        IFontManager fm = new AWTFontManager();
        AtomContainerRenderer renderer = new AtomContainerRenderer(generators, fm);
        RendererModel rm = renderer.getRenderer2DModel();
        rm.set(KekuleStructure.class, true); 
        rm.set(AtomNumberGenerator.Offset.class, new javax.vecmath.Vector2d(10,0));
        
        			
        this.setPreferredSize(new Dimension(this.width, this.height));
        this.setBackground(Color.WHITE);
        
		Image image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
		Rectangle drawArea = new Rectangle(0, 0, this.width, this.height);
		
		renderer.setup(molSource, drawArea);
		
		Rectangle diagramRectangle = renderer.calculateDiagramBounds(molSource);
        
        Rectangle result = renderer.shift(drawArea, diagramRectangle);
        this.setPreferredSize(new Dimension(result.width, result.height));
        this.revalidate();
        
        
        // paint the background
		Graphics2D g2 = (Graphics2D)image.getGraphics();
	   	g2.setColor(Color.WHITE);
	   	g2.fillRect(0, 0, this.width, this.height);
        renderer.paint(molSource, new AWTDrawVisitor((Graphics2D) g2), drawArea, true);

	   	return (RenderedImage) image;       

    }
    
    
    /**
     * Gets the image 4 mol.
     * 
     * @param protonatedMol the mol ac
     * 
     * @return the image4 mol
     * 
     * @throws Exception the exception
     */
    private RenderedImage getImage4MOL(IAtomContainer originalMol, IAtomContainer protonatedMol) throws Exception {
    	    	
//    	protonatedMol = AtomContainerManipulator.removeHydrogens(protonatedMol);
        // creates CDK Molecule object and get the renderer
    	IAtomContainer molSource = protonatedMol.clone();
    	
    	StructureDiagramGenerator sdg = new StructureDiagramGenerator();
		sdg.setMolecule(molSource);
		try {
	       sdg.generateCoordinates();
		} catch (Exception e) { }
		molSource = sdg.getMolecule();
		
		List<IGenerator<IAtomContainer>> generators = new ArrayList<IGenerator<IAtomContainer>>();
		generators.add(new BasicSceneGenerator());
        generators.add(new BasicBondGenerator());
        generators.add(new BasicAtomGenerator());
        generators.add(new RingGenerator());
        generators.add(new AtomNumberGenerator());
//        generators.add(new RadicalGenerator());
        
        IFontManager fm = new AWTFontManager();
        AtomContainerRenderer renderer = new AtomContainerRenderer(generators, fm);
        RendererModel rm = renderer.getRenderer2DModel();
        rm.set(KekuleStructure.class, true); 
        rm.set(AtomNumberGenerator.Offset.class, new javax.vecmath.Vector2d(10,0));
        
        
        //Highlight the structure
        IAtomContainer subgraph = Subgraph.getSubgraphsSMSD(originalMol, molSource, true, false);
	   	IAtomContainer invertedSubgraph = Subgraph.getInvertedSubgraph(molSource, subgraph);
	   	
	   	//now remove all hydrogens and add those which were protonated
	   	molSource = AtomContainerManipulator.removeHydrogens(molSource);
	   	for (IBond bond : invertedSubgraph.bonds()) {
			molSource.addBond(bond);
		}
	   	for (IAtom atom : invertedSubgraph.atoms()) {
			molSource.addAtom(atom);
		}
	   	
	   	
	   	Map<IChemObject, Color> colorMap = new HashMap<IChemObject, Color>();
        for (IBond bond : invertedSubgraph.bonds()) {
        	colorMap.put(bond, new Color(0, 255, 0));
        }
        
//        for (IAtom atom : subgraph.atoms()) {
//            colorMap.put(atom, new Color(0, 255, 0));
//        }
        
        rm.getParameter(ColorHash.class).setValue(colorMap);

			
        this.setPreferredSize(new Dimension(this.width, this.height));
        this.setBackground(Color.WHITE);
        
		Image image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
		Rectangle drawArea = new Rectangle(0, 0, this.width, this.height);
		
		renderer.setup(molSource, drawArea);
		
		Rectangle diagramRectangle = renderer.calculateDiagramBounds(molSource);
        
        Rectangle result = renderer.shift(drawArea, diagramRectangle);
        this.setPreferredSize(new Dimension(result.width, result.height));
        this.revalidate();
        
        
        // paint the background
		Graphics2D g2 = (Graphics2D)image.getGraphics();
	   	g2.setColor(Color.WHITE);
	   	g2.fillRect(0, 0, this.width, this.height);
        renderer.paint(molSource, new AWTDrawVisitor((Graphics2D) g2), drawArea, true);

	   	return (RenderedImage) image;       

    }
   
}
