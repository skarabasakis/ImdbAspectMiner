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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.DatabaseConnection;

/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class MovieIdCollectorTool {
	
	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		try {
			// AppLogger.setup();
			DatabaseConnection dc = new DatabaseConnection();

			while ( true ) {
				dc.openQuery("SELECT movieid, title FROM movies_enabled WHERE imdbid is null LIMIT 50");
				ResultSet rs = dc.getResults();
				
				if (rs.first() == true) {
					
					do {
						String request_url_string = "http://www.imdb.com/find?s=tt&q="
							+ URLEncoder.encode(rs.getString("title"), "ISO-8859-1");
						URL request_url = new URL(request_url_string);
						HttpURLConnection uc = (HttpURLConnection)request_url.openConnection();
						uc.setRequestProperty("User-Agent", "Mozilla/5.0"
								+ " (Windows NT 6.0; en-GB; Gecko/20100401 Firefox/3.6.3");
						//uc.setRequestMethod("HEAD");
						//uc.setConnectTimeout(5000);
						
						uc.getContentLength();
						String loc = uc.getURL().toString();
						String imdb_id = loc.substring(loc.indexOf("/tt") + 1, loc.lastIndexOf('/'));
						//imdb_ids.put(rs.getLong("movieid"), imdb_id);
						
						if (imdb_id.length() == 9) {
							dc.addQueryToQueue("UPDATE movies SET imdbid = \"" + imdb_id
									+ "\" WHERE movieid = " + rs.getLong("movieid"));
						}
						else {
							dc.addQueryToQueue("UPDATE movies SET disabled = 1 "
									+ "WHERE movieid = " + rs.getLong("movieid"));
						}
					} while ( rs.next() == true );
					
					System.out.println("Writing to the database");
					dc.executeQueue();
					dc.closeQuery();
				}
				else
				{
					break;
				}
			}
			
			dc.closeQuery();
		}
		catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( SQLException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}