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
import java.util.Date;
import util.DatabaseConnection;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public abstract class Review {
	
	protected Movie		movie;
	protected String	title;
	protected int		rating;
	protected Date		publicationDate;
	protected String	authorId;
	protected int		votesUseful;
	protected int		votesTotal;
	protected boolean	spoilers;
	
	/**
	 * Constructor for class Review
	 */
	public Review()
	{
	}
	
	/**
	 * @return the movie
	 */
	public Movie getMovie()
	{
		return movie;
	}
	
	/**
	 * @param imdbid
	 */
	public void setMovie(String imdbid)
	{
		movie = new Movie(imdbid);
	}
	
	public void loadMovieFromDatabase(DatabaseConnection c) throws SQLException
	{
		movie.loadFromDatabase(c);
	}

	/**
	 * @return the title
	 */
	public String getTitle()
	{
		return title;
	}
	
	/**
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title)
	{
		this.title = title;
	}
	

	
	/**
	 * @return the rating
	 */
	public int getRating()
	{
		return rating;
	}
	
	/**
	 * @param rating
	 *            the rating to set
	 */
	public void setRating(int rating)
	{
		this.rating = rating;
	}
	
	/**
	 * @return the publicationDate
	 */
	public Date getPublicationDate()
	{
		return publicationDate;
	}
	
	/**
	 * @param publicationDate
	 *            the publicationDate to set
	 */
	public void setPublicationDate(Date publicationDate)
	{
		this.publicationDate = publicationDate;
	}
	
	/**
	 * @return the authorId
	 */
	public String getAuthorId()
	{
		return authorId;
	}
	
	/**
	 * @param authorId
	 *            the authorId to set
	 */
	public void setAuthorId(String authorId)
	{
		this.authorId = authorId;
	}
	
	/**
	 * @return the votesUseful
	 */
	public int getVotesUseful()
	{
		return votesUseful;
	}
	
	/**
	 * @param votesUseful
	 *            the votesUseful to set
	 */
	public void setVotesUseful(int votesUseful)
	{
		this.votesUseful = votesUseful;
	}
	
	/**
	 * @return the votesTotal
	 */
	public int getVotesTotal()
	{
		return votesTotal;
	}
	
	/**
	 * @param usefulTotal
	 *            the usefulTotal to set
	 */
	public void setVotesTotal(int usefulTotal)
	{
		votesTotal = usefulTotal;
	}
	
	/**
	 * @return the spoilers
	 */
	public boolean isSpoilers()
	{
		return spoilers;
	}
	
	/**
	 * @param spoilers
	 *            the spoilers to set
	 */
	public void setSpoilers(boolean spoilers)
	{
		this.spoilers = spoilers;
	}

	public abstract String getReviewText();
}
