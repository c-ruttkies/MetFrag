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

public class Distance {
	
	private String bond;
	private Double bondLength;
	private String bondID = "";
	
	/**
	 * Instantiates a new distance.
	 * 
	 * @param bond the bond
	 * @param bondLength the bond length
	 */
	public Distance(String bond, Double bondLength)
	{
		setBond(bond);
		setBondLength(bondLength);
	}
	
	/**
	 * Instantiates a new distance.
	 * 
	 * @param bond the bond
	 * @param bondLength the bond length
	 * @param bondID the bond id
	 */
	public Distance(String bond, Double bondLength, String bondID)
	{
		setBond(bond);
		setBondLength(bondLength);
		setBondID(bondID);
	}

	/**
	 * Sets the bond length.
	 * 
	 * @param bondLength the new bond length
	 */
	public void setBondLength(Double bondLength) {
		this.bondLength = bondLength;
	}

	/**
	 * Gets the bond length.
	 * 
	 * @return the bond length
	 */
	public Double getBondLength() {
		return (Math.round(bondLength * 1000.0)/1000.0);
	}

	/**
	 * Sets the bond.
	 * 
	 * @param bond the new bond
	 */
	public void setBond(String bond) {
		this.bond = bond;
	}

	/**
	 * Gets the bond.
	 * 
	 * @return the bond
	 */
	public String getBond() {
		return bond;
	}

	public void setBondID(String bondID) {
		this.bondID = bondID;
	}

	public String getBondID() {
		return bondID;
	}

}
