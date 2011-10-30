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
package indexing.eval;

import indexing.SynsetTermsAggregator;
import indexing.TermTypeFilter;
import indexing.TermTypeFilter.TermType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import lexicon.Ratings;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.store.SimpleFSDirectory;
import topics.TokenListsCollector;
import util.AppLogger;
import util.State;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import classes.ReviewStats;
import config.Paths;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class Eval {
	
	public static String printReviewsByRating()
	{
		StringBuilder str = new StringBuilder();

		try {
			State<ReviewStats> dataset_stats_state = new State<ReviewStats>("stats", new ReviewStats());
			ReviewStats dataset_stats = dataset_stats_state.restoreState();
			ArrayList<Float> dataset_weights = dataset_stats.getRatingWeights();
			
			str.append("Rating\t# Reviews\t# Words\t#Weight\n");
			for (int rating = Ratings.MIN_RATING ; rating <= Ratings.MAX_RATING ; rating++) {
				str.append(rating + "\t" + dataset_stats.getTotalReviews(rating) + "\t"
					+ dataset_stats.getTotalLength(rating) + "\t" + dataset_weights.get(rating - 1) + "\n");
			}
			str.append("\nTOTAL" + "\t" + dataset_stats.getTotalReviews() + "\t" + dataset_stats.getTotalLength()
				+ "\n");
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to load ReviewStats object");
		}
		
		return str.toString();
	}
	
	public static String printIndexTermCounts()
	{
		StringBuilder str = new StringBuilder();
		
		try {
			State<SynsetTermsAggregator> aggregator_state = new State<SynsetTermsAggregator>("synsets",
					new SynsetTermsAggregator());
			SynsetTermsAggregator aggregator = aggregator_state.restoreState();
			
			System.err.println("Cleared " + aggregator.clearJunkTerms() + " junk terms");

			SynsetCategory[] synsetcats = Synset.getSynsetCategories();
			str.append("Category\t# Synsets\t# Terms\n");
			for (SynsetCategory synsetcat : synsetcats) {
				str.append(synsetcat + "\t" + aggregator.getSynsetCount(synsetcat) + "\t"
					+ aggregator.getTermCount(synsetcat) + "\n");
			}
			str.append("ALL\t" + aggregator.getSynsetCount() + "\t" + aggregator.getTermCount() + "\n");
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to load SynsetTermsAggregator object");
		}
		
		return str.toString();
	}
	
	public static int countTokens()
	{
		try {
			State<TokenListsCollector> sentences_state = new State<TokenListsCollector>("sentences",
					new TokenListsCollector());
			TokenListsCollector sentences = sentences_state.restoreState();
			
			return sentences.countTokens();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to load SynsetTermsAggregator object");
		}
		
		return 0;
	}
	
	private static float[] calculateVocabularyGrowth(int per_number_of_reviews, boolean cumulative, TermType type)
	{
		try {
			IndexReader ir = IndexReader.open(new SimpleFSDirectory(new File(Paths.luceneIndex)), true);
			String field = "text";

			float num_docs = ir.maxDoc();
			int num_agegroups = (int)num_docs / per_number_of_reviews;
			float age_totals[] = new float[num_agegroups];

			String internedField = field.intern();
			TermEnum te = ir.terms(new Term(internedField, ""));
			Term term = te.term();

			while ( term != null ) {
				if (TermTypeFilter.isTermType(term.text(), type)) {
					if (internedField != term.field()) {
						break;
					}
					TermDocs td = ir.termDocs(term);
					td.next();
					float firstdocid = td.doc();
					int age_bracket = (int)(firstdocid / num_docs * num_agegroups);
					age_totals[age_bracket]++;
				}

				if (te.next()) {
					term = te.term();
				}
				else {
					term = null;// ends loop
				}
			}

			float total = 0.0f;
			float max = 0.0f;
			for (int i = 0 ; i < age_totals.length ; i++) {
				if (age_totals[i] > max) {
					max = age_totals[i];
				}
				total += age_totals[i];
				if (i > 0 && cumulative) {
					age_totals[i] += age_totals[i - 1]; // make totals cumulative
				}
			}
			
			return age_totals;
		} catch ( Exception e ) {
			AppLogger.error.log(Level.SEVERE, "analyzeVocabularyGrowth failed\n" + e.getMessage());
		}
		
		return null;
	}
	
	public static String printVocabularyGrowth(int per_number_of_reviews, boolean cumulative, TermType type)
	{
		StringBuilder str = new StringBuilder();
		float[] vocabulary_growth = calculateVocabularyGrowth(per_number_of_reviews, cumulative, type);
		int reviews = 0;
		
		str.append("#Reviews\tNew Terms\n");
		for (float vocabulary_growth_value : vocabulary_growth) {
			reviews += per_number_of_reviews;
			str/* .append(reviews).append("\t") */.append((int)vocabulary_growth_value).append("\n");
		}
		
		return str.toString();
	}
	
	private static class Bucket {
		
		int	maxDf;
		int	minDf	= Integer.MAX_VALUE;
		int	totalDf;
		int	numTermsInThisBucket;
		
		public void addTermDf(int df)
		{
			totalDf += df;
			numTermsInThisBucket++;
			maxDf = Math.max(df, maxDf);
			minDf = Math.min(df, minDf);
		}
		
		public float getAverageDf()
		{
			if (numTermsInThisBucket == 0)
				return 0;
			return (float)totalDf / (float)numTermsInThisBucket;
		}
		
		public String toString()
		{
			return maxDf + " maxDf in " + numTermsInThisBucket + " terms";
		}
		
	}
	
	private static class TermCount implements Comparable<TermCount> {
		
		int				df			= 0;
		int				termCount	= 0;
		private Term	term;
		
		public TermCount(Term term, int df)
		{
			super();
			this.term = term;
			this.df = df;
		}
		
		public int compareTo(TermCount o)
		{
			TermCount other = o;
			if (df > other.df)
				return -1;
			if (df < other.df)
				return 1;
			return 0;
		}
	}
	
	private static float[] analyzePopularityVsRank(int rank_group_size, TermType type)
	{
		try {
			IndexReader ir = IndexReader.open(new SimpleFSDirectory(new File(Paths.luceneIndex)), true);
			String field = "text";

			Term startTerm = new Term(field, "");
			TermEnum te = ir.terms(startTerm);
			ArrayList<TermCount> terms = new ArrayList<TermCount>();
			
			// most terms occur very infrequently - just keep group totals for the DFs
			// representing these "long tail" terms.
			int longTailDfStart = 1;
			int longTailDfEnd = 1000;
			int longTailTermDfCounts[] = new int[longTailDfEnd - longTailDfStart + 1];
			
			// For "short tail" terms there are less of them and they represent a lot
			// of different
			// DFs (typically in the thousands) so unlike long-tail terms we can't
			// predict what common DF buckets to accumulate counts in. For this reason
			// we don't attempt
			// to total them and keep a list of them individually (shouldn't occupy
			// too much ram)
			
			int numUniqueTerms = 0;
			while ( te.next() ) {
				Term currTerm = te.term();
				if (currTerm.field() != startTerm.field()) {
					break;
				}
				
				if (TermTypeFilter.isTermType(currTerm.text(), type)) {
					numUniqueTerms++;
					int df = te.docFreq();
					if (df <= longTailDfEnd) {
						int i = df - longTailDfStart;
						longTailTermDfCounts[i]++;
					}
					else {
						terms.add(new TermCount(currTerm, df));
					}
				}
			}
			
			TermCount sortedTerms[] = terms.toArray(new TermCount[terms.size()]);
			Arrays.sort(sortedTerms);
			
			ArrayList<Bucket> buckets = new ArrayList<Bucket>();
			Bucket currentBucket = new Bucket();
			buckets.add(currentBucket);
			for (int i = 0 ; i < sortedTerms.length ; i++) {
				currentBucket.addTermDf(sortedTerms[i].df);
				if (currentBucket.numTermsInThisBucket >= rank_group_size) {
					// start a new bucket
					currentBucket = new Bucket();
					buckets.add(currentBucket);
				}
			}
			// now work through the aggregated long-tail terms - start from
			// most common DF down to least common DF
			for (int i = longTailTermDfCounts.length - 1 ; i >= 0 ; i--) {
				int df = i + longTailDfStart;
				int numTerms = longTailTermDfCounts[i];
				for (int t = 0 ; t < numTerms ; t++) {
					currentBucket.addTermDf(df);
					if (currentBucket.numTermsInThisBucket >= rank_group_size) {
						// start a new bucket
						currentBucket = new Bucket();
						buckets.add(currentBucket);
					}
				}
			}
			if (currentBucket.numTermsInThisBucket == 0) {
				buckets.remove(currentBucket);
			}
			Bucket bucketsResult[] = buckets.toArray(new Bucket[buckets.size()]);
			float termBucketTotals[] = new float[bucketsResult.length];
			for (int i = 0 ; i < bucketsResult.length ; i++) {
				termBucketTotals[i] = bucketsResult[i].getAverageDf();
			}
			
			return termBucketTotals;
		} catch ( Exception e ) {
			AppLogger.error.log(Level.SEVERE, "analyzePopularityVsRank failed\n" + e.getMessage());
			return null;
		}
	}
	
	public static String printPopularityVsRank(int rank_group_size, TermType type)
	{
		StringBuilder str = new StringBuilder();
		float[] term_buckets = analyzePopularityVsRank(rank_group_size, type);
		int counter = 0;
		
		str.append("#Rank Group\tAvg Frequency\n");
		for (float term_bucket : term_buckets) {
			counter++;
			str.append(counter).append("\t\t").append((int)term_bucket).append("\n");
		}
		
		return str.toString();
	}
	
	public static void printReviewIds()
	{
		try {
			IndexReader ir = IndexReader.open(new SimpleFSDirectory(new File(Paths.luceneIndex)), true);
			String field = "reviewid";
			
			int ndocs = ir.maxDoc();
			for (int i = 0 ; i < ndocs ; i++) {
				Document doc = ir.document(i);
				System.out.println(doc.get(field));
			}
			
		} catch ( Exception e ) {
			// TODO: handle exception
		}
		
	}

	public static void main(String[] args)
	{
		// printReviewIds();
		System.out.println(printPopularityVsRank(27, TermType.JUNK));
		// System.out.println(printVocabularyGrowth(1500, false, TermType.JUNK));
		// System.out.println(printIndexTermCounts());
		// System.out.println(printReviewsByRating());
	}
}
