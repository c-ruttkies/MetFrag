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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.ipbhalle.metfrag.main.Config;

public class PubChemCheck {

	private static Connection con;
	
	public static void main(String[] args) {
			
		String[] files = null;
		
		String driver = "com.mysql.jdbc.Driver"; 
		String path = "/vol/mirrors/pubchem/";	
		StringBuilder sb = new StringBuilder();
		
		
		try
		{
			Class.forName(driver); 
			DriverManager.registerDriver (new com.mysql.jdbc.Driver()); 
	        // JDBC-driver
	        Class.forName(driver);
	        //databse data
	        Config c = new Config();
	        String url = c.getJdbc();
	        String username = c.getUsername();
	        String password = c.getPassword();
	        con = DriverManager.getConnection(url, username, password);
		    
	        
	        UpdateData ud = new UpdateData();
	        Map<String, Boolean> map = ud.getMap();
	        for (String string : map.keySet()) {
	        	PreparedStatement pstmt = con.prepareStatement("SELECT ID FROM RECORD WHERE ID = ? and DATE < '2010' limit 1");
			    pstmt.setString(1, string);
			    ResultSet rs = pstmt.executeQuery();
			    while(rs.next())
	            {
			    	sb.append(rs.getString("ID") + "\n");
	            }
			}
	        
	        
	        try{
	            // Create file 
	            FileWriter fstream = new FileWriter("/home/swolf/sgeQsubScripts/pubchemMissing.txt");
	            BufferedWriter out = new BufferedWriter(fstream);
	            out.write(sb.toString());
	            //Close the output stream
	            out.close();
            }catch (Exception e){//Catch exception if any
              System.err.println("Error: " + e.getMessage());
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
	}
	
}
