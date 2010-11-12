// The MIT License
//
// Copyright (c) 2010 Stelios Karabasakis
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
//
package application;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.DatabaseConnection;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class ImdbLinksExtractionTool {
	
	private static String	dir				= "movielinks\\new\\";
	private static String	filename		= "imdb_";
	private static String	url_format		= "http://www.imdb.com/title/%s/usercomments?filter=chrono&count=50000&id=%s";
	private static Integer	links_per_file	= 1000;
	
	public static void main(String[] args)
	{
		System.out.println("Initializing\n");
		
		Integer link_counter = 0;
		Integer fileid = 1;
		String links = new String();
		FileWriter fw = null;
		
		try {
			DatabaseConnection db = new DatabaseConnection();
			db.openQuery("SELECT imdbid FROM movies_enabled");
			ResultSet imdbid_data = db.getResults();

			while ( !imdbid_data.isAfterLast() ) {
				while ( link_counter < links_per_file && imdbid_data.next() ) {
					links += String.format(url_format, imdbid_data.getString(1),
							imdbid_data.getString(1))
							+ "\n";
					link_counter++;
				}
				
				try {
					fw = new FileWriter(dir + filename + String.format("%03d", fileid) + ".txt");
					fw.write(links);
					fw.close();
				} catch ( IOException e ) {
					System.out.println("Cannot write to file " + dir + filename
							+ String.format("%d", fileid) + ".txt" + " for writing");
					e.printStackTrace();
				}
				
				links = "";
				link_counter = 0;
				fileid++;
			}
			
			db.closeQuery();
			
		} catch ( SQLException e ) {
			// TODO Auto-generated catch block
			System.out.println("Problem accessing database");
			e.printStackTrace();
		}
		
		
	}
	
}

