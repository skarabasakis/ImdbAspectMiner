// The MIT License
//
// Copyright (c) 2011 Stelios Karabasakis
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
package dependencies;

import indexing.Token;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Level;
import lexicon.SentimentLexicon;
import util.AppLogger;
import classes.DatabaseReviewCollection;
import classes.Review;
import config.Globals;
import dependencies.visualisation.DependencyTreeParseNoViz;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class DependencyExampleGenerator {
	
	Integer							exampleCount	= 50;
	ReviewDependencyAnalyzer		analyzer		= new ReviewDependencyAnalyzer();
	ArrayList<DependencyTreeParse>	trees			= new ArrayList<DependencyTreeParse>();
	SentimentLexicon				sentiments		= SentimentLexicon.loadLexicon();

	/**
	 * Constructor for class DependencyExampleGenerator
	 */
	public DependencyExampleGenerator(int examplePoolSize)
	{
		Random rand = new Random();
		rand.setSeed(System.currentTimeMillis());
		Integer min_id = rand.nextInt(Globals.Corpus.TotalReviews / 2) + 1;
		
		try {
			DatabaseReviewCollection coll = new DatabaseReviewCollection(examplePoolSize);
			coll.setLimits(min_id, 0);
			while ( coll.hasNextSegment() && trees.size() < examplePoolSize ) {
				coll.loadNextSegment();
				Iterator<Review> review_i = coll.getIterator();
				while ( review_i.hasNext() && trees.size() < examplePoolSize ) {
					ArrayList<ArrayList<Token>> sentences = analyzer.getSentences(review_i.next());
					for (ArrayList<Token> sentence : sentences) {
						try {
							trees.add(new DependencyTreeParseNoViz(sentence));
						} catch ( Exception e ) {
							AppLogger.error.log(Level.FINE, e.getMessage());
						}
					}
				}
			}
		} catch ( SQLException e ) {
			AppLogger.error.log(Level.SEVERE, "An exception occured while trying to access the database.\n"
				+ e.getMessage());
			return;
		}
		
	}
	
	public DependencyExampleGenerator(String file)
	{
		try {
			ArrayList<ArrayList<Token>> sentences = analyzer.getSentences(new FileReader(file));
			for (ArrayList<Token> sentence : sentences) {
				trees.add(new DependencyTreeParseNoViz(sentence));
			}
		} catch ( FileNotFoundException e ) {
			AppLogger.error.log(Level.SEVERE, "File not found or not accessible: " + file);
			return;
		}
	}

	/**
	 * @return the exampleCount
	 */
	public Integer getExampleCount()
	{
		return exampleCount;
	}
	
	
	/**
	 * @param exampleCount
	 *            the exampleCount to set
	 */
	public void setExampleCount(Integer exampleCount)
	{
		this.exampleCount = exampleCount;
	}
	
	public ArrayList<DependencyExample> generateExamples(DependencyQuery query)
	{
		ArrayList<DependencyExample> examples = new ArrayList<DependencyExample>();
		Iterator<DependencyTreeParse> tree_i = trees.iterator();
		while ( examples.size() < exampleCount && tree_i.hasNext() ) {
			examples.addAll(tree_i.next().findExamples(query, sentiments));
		}
		
		return examples;
	}
	
	public static void main(String[] args)
	{

		DependencyExampleGenerator gen = new DependencyExampleGenerator(500);
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

		String query;
		System.out.print("\n\nQuery: ");
		try {
			while ( !(query = input.readLine()).isEmpty() ) {
				try {
					// System.out.println(new DependencyQuery(query));
					ArrayList<DependencyExample> examples = gen.generateExamples(new DependencyQuery(query));
					for (DependencyExample example : examples) {
						System.out.println(example);
					}
				} catch ( IllegalArgumentException e ) {
					System.err.println(e.getMessage());
				}
				
				System.err.flush();
				System.out.print("\n\nQuery: ");
			}
		} catch ( IOException e ) {
			e.printStackTrace();
		}
	}
}
