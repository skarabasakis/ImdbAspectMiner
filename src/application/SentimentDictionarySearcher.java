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

import indexing.ReviewTermPayload;
import indexing.PosTag.PosCategory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spans.SpanOrQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.store.SimpleFSDirectory;
import search.ScoreDistributionHistogram;
import config.Globals;
import config.Paths;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class SentimentDictionarySearcher {
	
	private IndexReader	reader	= null;
	private IndexSearcher searcher = null;
	
	
	/**
	 * Constructor for class SentimentDictionarySearcher
	 * 
	 * @throws IOException
	 * @throws CorruptIndexException
	 */
	public SentimentDictionarySearcher() throws CorruptIndexException, IOException
	{
		// TODO Copy Exception handling from somewhere
		reader = IndexReader.open(new SimpleFSDirectory(new File(Paths.luceneIndex)), true);
		searcher = new IndexSearcher(reader);
	}
	
	public TermSpans getIndexTermInstances(String term) throws IOException
	{
		SpanTermQuery stq_title = new SpanTermQuery(new Term("title", term));
		SpanTermQuery stq_text  = new SpanTermQuery(new Term("text", term));
		SpanOrQuery soq = new SpanOrQuery(stq_title, stq_text);
		return (TermSpans)soq.getSpans(reader);
	}

	public ScoreDistributionHistogram makeRatingHistogramForTerm(String term, PosCategory posCat) throws IOException
	{
		ScoreDistributionHistogram histogram = new ScoreDistributionHistogram();
		TermSpans ts = getIndexTermInstances(term);

		while (ts.next()) {
			String rating_str = searcher.doc(ts.doc()).getField(Globals.IndexFieldNames.rating).stringValue();
			int rating = Integer.parseInt(rating_str);
			
			Iterator<byte[]> p_i = ts.getPayload().iterator();
			while ( p_i.hasNext() ) {
				ReviewTermPayload p = new ReviewTermPayload();
				if (p.decode(p_i.next())) {
					if (p.getPosCat() == posCat) {
						histogram.addObeservation(rating, 1);
					}
				}
			}
		}

		return histogram;
	}
	
	public HashMap<PosCategory, ScoreDistributionHistogram> makeRatingHistogramForTerm(String term) throws IOException
	{
		HashMap<PosCategory, ScoreDistributionHistogram> histograms = new HashMap<PosCategory, ScoreDistributionHistogram>();
		PosCategory[] posCatArray = { PosCategory.V, PosCategory.N, PosCategory.J, PosCategory.R };
		for (PosCategory posCat : posCatArray) {
			histograms.put(posCat, new ScoreDistributionHistogram());
		}

		TermSpans ts = getIndexTermInstances(term);
		
		while ( ts.next() ) {
			String rating_str = searcher.doc(ts.doc()).getField(Globals.IndexFieldNames.rating).stringValue();
			int rating = Integer.parseInt(rating_str);
			
			Iterator<byte[]> p_i = ts.getPayload().iterator();
			while ( p_i.hasNext() ) {
				ReviewTermPayload p = new ReviewTermPayload();
				if (p.decode(p_i.next())) {
					ScoreDistributionHistogram histogram = histograms.get(p.getPosCat());
					histogram.addObeservation(rating, 1);
				}
			}
		}
		
		return histograms;
	}

	public static void main(String[] args)
	{
		try {
			SentimentDictionarySearcher ss = new SentimentDictionarySearcher();
			TermSpans ts = ss.getIndexTermInstances("hello");
			while (ts.next()) {
				System.out.println(ss.searcher.doc(ts.doc()).get("rating"));
				ArrayList<byte[]> tsp = (ArrayList<byte[]>)ts.getPayload();
				for (byte[] tspb : tsp) {
					ReviewTermPayload p = new ReviewTermPayload();
					p.decode(tspb);
				}
			}
		} catch ( CorruptIndexException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
