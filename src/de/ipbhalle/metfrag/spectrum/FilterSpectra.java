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

package de.ipbhalle.metfrag.spectrum;

import java.io.File;
import java.util.Arrays;

import de.ipbhalle.metfrag.database.Tools;

public class FilterSpectra {
	
	public static void main(String[] args) {
		
		String folder = "/home/swolf/MassBankData/MetFragSunGrid/RikenDataMergedCorrect/";
		//loop over all files in folder
		File f = new File(folder);
		File files[] = f.listFiles();
		Arrays.sort(files);
		
		//create new folder
		String path = folder + "fewPeaks";
		String path1 = folder + "negativeMode";
		String path2 = folder + "NoCHONPS";
		String path3 = folder + "No[M+H]+";
		String path4 = folder + "NoPubChemID";
		String path5 = folder + "OnlyCH";
		new File(path).mkdir();
		new File(path1).mkdir();
		new File(path2).mkdir();
		new File(path3).mkdir();
		new File(path4).mkdir();
		new File(path5).mkdir();
		

		for(int i=0;i<files.length-1;i++)
		{
			if(files[i].isFile())
			{
				WrapperSpectrum spectrum = new WrapperSpectrum(files[i].toString());
				spectrum.setPeakList(new CleanUpPeakList(spectrum.getPeakList()).getCleanedPeakList(spectrum.getExactMass()));
				
				if(spectrum.getPeakList().size() <= 3)
				{
					files[i].renameTo(new File(folder + "fewPeaks", files[i].getName()));
					continue;
				}
				else if(spectrum.getPeakList().size() <= 3)
				{
					files[i].renameTo(new File(folder + "fewPeaks", files[i].getName()));
					continue;
				}
				
				else if(spectrum.getMode() != 1)
				{
					files[i].renameTo(new File(folder + "negativeMode", files[i].getName()));
				}
				
				//only CHONPS
				else if(Tools.checkCHONSP(spectrum.getFormula()) == 0)
				{
					files[i].renameTo(new File(folder + "NoCHONPS", files[i].getName()));
				}
				
				//no pubchemid
				else if(spectrum.getCID() == 0)
				{
					files[i].renameTo(new File(folder + "NoPubChemID", files[i].getName()));
					continue;
				}
				
				//only [M+H]+ ions
				else if(!spectrum.getPrecursorType().equals("[M+H]+"))
				{
					files[i].renameTo(new File(folder + "No[M+H]+", files[i].getName()));
					continue;
				}
				
				//filter compounds which only contain C or H
				else if(spectrum.getFormula().contains("C") && (!spectrum.getFormula().contains("O") && !spectrum.getFormula().contains("N") && !spectrum.getFormula().contains("S") && !spectrum.getFormula().contains("P")))
				{
					files[i].renameTo(new File(folder + "OnlyCH", files[i].getName()));
					continue;
				}
				
				
				
			}
		}
	}

}
