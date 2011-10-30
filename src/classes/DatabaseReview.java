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

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import util.AppLogger;
import util.DatabaseConnection;
import config.Globals;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class DatabaseReview extends Review {
	
	private int			reviewid;
	private String		reviewText;
	private Document	documentForIndexing;
	
	// private static int token_length;

	/**
	 * Constructor for class DatabaseReview
	 */
	public DatabaseReview()
	{
		documentForIndexing = null;
	}
	
	public DatabaseReview(int reviewid) throws SQLException
	{
		DatabaseConnection dc = new DatabaseConnection();
		load(reviewid, dc);
	}
	
	public DatabaseReview(int reviewid, DatabaseConnection dc) throws SQLException
	{
		load(reviewid, dc);
	}
	
	private void load(Integer reviewid, DatabaseConnection dc) throws SQLException
	{
		dc.openQuery("SELECT * FROM reviews WHERE review = " + reviewid);
		ResultSet result = dc.getResults();
		if (result.first()) {
			setReviewid(result.getInt("reviewid"));
			setMovie(result.getString("imdbid"));
			setTitle(result.getString("title"));
			setRating(result.getInt("rating"));
			
			Clob text = result.getClob("text");
			setReviewText(text.getSubString(1, (int)text.length()));
		}
		else {
			AppLogger.error.log(Level.WARNING, "Review #" + reviewid + " not found in database");
		}
		
		dc.closeQuery();
	}

	/**
	 * @param reviewid
	 *            the reviewid to set
	 */
	public void setReviewid(int reviewid)
	{
		this.reviewid = reviewid;
	}

	/**
	 * @return the reviewid
	 */
	public int getReviewid()
	{
		return reviewid;
	}
	
	/**
	 * @param text
	 *            the text to set
	 */
	public void setReviewText(String text)
	{
		reviewText = text;
	}
	
	/*
	 * (non-Javadoc)
	 * @see classes.Review#getReviewText()
	 */
	@Override
	public String getReviewText()
	{
		return reviewText;
	}
	
	public Document getDocumentForIndexing()
	{
		if (documentForIndexing == null) {
			documentForIndexing = new Document();
			documentForIndexing.add(new Field(Globals.IndexFieldNames.reviewid, Integer.toString(getReviewid()),
					Field.Store.YES, Field.Index.NO));
			documentForIndexing.add(new Field(Globals.IndexFieldNames.text, getTitle() + "\n" + getReviewText(),
					Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
			documentForIndexing.add(new NumericField(Globals.IndexFieldNames.rating, Field.Store.YES, false)
				.setIntValue(getRating()));
		}
		
		return documentForIndexing;
	}

}
