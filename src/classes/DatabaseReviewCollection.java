// The MIT License
//
// Copyright (conn) 2010 Stelios Karabasakis
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
import util.DatabaseConnection;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class DatabaseReviewCollection extends ReviewCollection {
	
	private DatabaseConnection	conn				= null;
	private int					maxSegmentSize		= 0;
	private int					segmentOffset		= 0;
	private int					min_reviewid		= 0;
	private int					max_reviewid		= 0;
	private boolean				has_next_segment	= true;
	
	public DatabaseReviewCollection() throws SQLException
	{
		conn = new DatabaseConnection();
	}
	
	public DatabaseReviewCollection(int maxSegmentSize) throws SQLException
	{
		this.maxSegmentSize = maxSegmentSize;
		conn = new DatabaseConnection();
	}
	
	public DatabaseReviewCollection(int maxSegmentSize, DatabaseConnection c)
	{
		this.maxSegmentSize = maxSegmentSize;
		conn = c;
	}
	
	public void setLimits(int min_reviewid, int max_reviewid)
	{
		this.min_reviewid = min_reviewid;
		this.max_reviewid = max_reviewid;
		has_next_segment = max_reviewid > min_reviewid;
	}

	public void loadNextSegment() throws SQLException
	{
		String reviewsQuery = formReviewsQuery();
		conn.openQuery(reviewsQuery);
		
		ResultSet results = conn.getResults();
		while (results.next()) {
			DatabaseReview r = new DatabaseReview();
			r.setReviewid(results.getInt("reviewid"));
			r.setMovie(results.getString("imdbid"));
			r.setTitle(results.getString("title"));
			r.setRating(results.getInt("rating"));
			
			// TODO This needs to be tested
			Clob text = results.getClob("text");
			r.setReviewText(text.toString());
			
			// Insert current review into collection
			insertReview(r);
		}
		
		segmentOffset++;
		has_next_segment = getCount() == maxSegmentSize;
		conn.closeQuery();
	}
	
	/**
	 * @return
	 */
	private String formReviewsQuery()
	{
		return "SELECT * FROM `" + table //
			+ "` WHERE `rating` > 0 " //
			+ (min_reviewid == 0 ? "" : "AND `reviewid` > " + min_reviewid) //
			+ (max_reviewid == 0 ? "" : "AND `reviewid` <= " + max_reviewid) //
			+ (segmentOffset == 0 ? "" : "LIMIT " + maxSegmentSize + " OFFSET " + segmentOffset * maxSegmentSize);
	}

	public boolean hasNextSegment()
	{
		return has_next_segment;
	}
	
}
