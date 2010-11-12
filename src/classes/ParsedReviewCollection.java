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

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.logging.Level;
import util.AppLogger;
import util.DatabaseConnection;



/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class ParsedReviewCollection extends ReviewCollection {
	
	private Movie		movie;
	private static int	rowcount	= 0;
	private static int	maxrows		= 100;
	private int			count;

	public ParsedReviewCollection(Movie m)
	{
		super();
		movie = m;
	}
	
	/**
	 * @return the movie
	 */
	public Movie getMovie()
	{
		return movie;
	}
	
	/**
	 * @param movie
	 *            the movie to set
	 */
	public void setMovie(Movie movie)
	{
		this.movie = movie;
	}
	
	public String generateInsertQueryHeaders()
	{
		return "INSERT INTO "
			+ table
			+ " (imdbid, title, text, rating, publication_date, authorid, spoilers, votes_useful, votes_total) VALUES\n";
	}
	
	public void commitToDatabase(DatabaseConnection c, boolean delayed) throws SQLException
	{
		if (c == null) {
			c = new DatabaseConnection();
		}
		

		// Perform a sanity check: Is the explicit review count equal to the actual number of parsed
		// reviews in document?
		if (count != reviewList.size()) {
			AppLogger.error.log(Level.WARNING, "Parser detected " + reviewList.size()
				+ " reviews in document, expected " + count);
		}


		Iterator<Review> i = reviewList.iterator();
		if (i.hasNext()) {
			StringBuilder insertQueryBuilder = new StringBuilder(generateInsertQueryHeaders());
			while ( i.hasNext() ) {
				insertQueryBuilder.append(((ParsedReview)i.next()).generateInsertQueryValues());
				if (i.hasNext()) {
					insertQueryBuilder.append(", ");
				}
				rowcount++;
			}
			c.addQueryToQueue(insertQueryBuilder.toString());
		}
		c.addQueryToQueue("UPDATE movies SET has_reviews = 1, review_count = " + getCount() + " WHERE imdbid = '"
			+ movie.getImdbid() + "'");
		

		if (rowcount >= maxrows || delayed == false) {
			System.out.println(">> Committing to database, please wait...");
			c.executeQueue();
			rowcount = 0;
		}
	}
	
	public void writeQueriesToScript(FileWriter reviewContents, FileWriter reviewCounts)
	{
		// Perform a sanity check: Is the explicit review count equal to the actual number of parsed
		// reviews in document?
		if (count != reviewList.size()) {
			AppLogger.error.log(Level.WARNING, "Parser detected " + reviewList.size()
				+ " reviews in document, expected " + count);
		}

		try {
			Iterator<Review> i = reviewList.iterator();
			if (i.hasNext()) {
				StringBuilder insertQueryBuilder = new StringBuilder(generateInsertQueryHeaders());
				while ( i.hasNext() ) {
					insertQueryBuilder.append(((ParsedReview)i.next()).generateInsertQueryValues());
					if (i.hasNext()) {
						insertQueryBuilder.append(",\n");
					}
					else {
						insertQueryBuilder.append(";\n");
					}
				}
				reviewContents.write(insertQueryBuilder.toString());
			}
			reviewCounts.write("UPDATE movies SET has_reviews = 1, review_count = " + count + " WHERE imdbid = '"
				+ movie.getImdbid() + "';\n");
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot access script file for writing queries.");
		}
	}
	

	public void setCount(int count)
	{
		this.count = count;
	}
}
