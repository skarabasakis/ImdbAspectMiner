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
package classes;

import java.sql.SQLException;
import util.DatabaseConnection;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class Movie {
	
	private int		movieid;
	private String	imdbid;
	private String	title;
	
	
	/**
	 * @param imdbid
	 */
	public Movie(String imdbid)
	{
		this.imdbid = imdbid;
	}
	
	public void loadFromDatabase(DatabaseConnection c) throws SQLException
	{
		// Open a database connection if required
		if (c == null) {
			c = new DatabaseConnection();
		}
		
		String sql = "SELECT movieid, title FROM movies_enabled WHERE imdbid = " + imdbid;
		c.openQuery(sql);
		if (c.getResults().next()) {
			movieid = Integer.parseInt(c.getResults().getRowId("movieid").toString());
			title = c.getResults().getRowId("title").toString();
		}
		c.closeQuery();
	}

	/**
	 * @return the imdbid
	 */
	public String getImdbid()
	{
		return imdbid;
	}
	
	/**
	 * @param imdbid
	 *            the imdbid to set
	 */
	public void setImdbid(String imdbid)
	{
		this.imdbid = imdbid;
	}
	
	/**
	 * @return the title
	 */
	public String getTitle()
	{
		return title;
	}

	/**
	 * @return the movieid
	 */
	public int getMovieid()
	{
		return movieid;
	}
}
