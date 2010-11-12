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

import java.util.ArrayList;
import java.util.Iterator;
import util.DatabaseConnection;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class ParsedReview extends Review {
	
	private ArrayList<String>	textBody;

	/**
	 * Constructor for class ParsedReview
	 */
	public ParsedReview()
	{
		super();
		textBody = new ArrayList<String>();
	}
	
	/**
	 * @return the textBody
	 */
	public ArrayList<String> getTextBody()
	{
		return textBody;
	}
	
	public void insertParagraph(String paragraph)
	{
		textBody.add(paragraph);
	}

	public String generateInsertQueryValues()
	{
		String sql = "('" + movie.getImdbid() + "', " //
			+ "'" + title.replace("'", "\\'") + "'" + ", " //
			+ "'" + getReviewText().replace("'", "\\'") + "'" + ", " //
			+ Integer.toString(rating) + ", " //
			+ "'" + DatabaseConnection.databaseDateFormatter.format(publicationDate) + "', " //
			+ "'" + authorId + "', " //
			+ (spoilers == true ? "1" : "0") + ", " //
			+ Integer.toString(votesUseful) + ", " //
			+ Integer.toString(votesTotal) + ")\n"; //
		
		return sql;
	}
	
	/*
	 * (non-Javadoc)
	 * @see classes.Review#getReviewText()
	 */
	@Override
	public String getReviewText()
	{
		StringBuilder text = new StringBuilder();
		Iterator<String> i = textBody.iterator();
		while ( i.hasNext() ) {
			text.append(i.next() + "\n");
		}
		
		return text.toString();
	}
}
