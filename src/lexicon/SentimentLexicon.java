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
package lexicon;

import java.io.IOException;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import lexicon.PayloadFilters.PayloadFilter;
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;
import lexicon.TermSentiment.Sentiment.SentimentFormat;
import lexicon.classifiers.SentimentClassifier;
import lexicon.classifiers.WidestWindowSentimentClassifier;
import util.AppLogger;
import util.State;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import classes.Counter;
import config.Globals;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class SentimentLexicon implements Serializable {
	
	private static final long											serialVersionUID	= -7973049097709128863L;
	
	protected Boolean													synsetsOnly			= false;
	
	protected HashMap<SynsetCategory, TreeMap<Synset, TermSentiment>>	lemmaLexicon		= null;
	protected HashMap<SynsetCategory, TreeMap<String, TermSentiment>>	nonLemmaLexicon		= null;
	
	
	/**
	 * Constructor for class SentimentLexicon
	 */
	public SentimentLexicon(boolean synsetsOnly)
	{
		lemmaLexicon = new HashMap<SynsetCategory, TreeMap<Synset, TermSentiment>>();
		for (SynsetCategory synsetcat : Synset.getSynsetCategories()) {
			lemmaLexicon.put(synsetcat, new TreeMap<Synset, TermSentiment>());
		}
		
		nonLemmaLexicon = new HashMap<SynsetCategory, TreeMap<String, TermSentiment>>();
		for (SynsetCategory synsetcat : Synset.getSynsetCategories()) {
			nonLemmaLexicon.put(synsetcat, new TreeMap<String, TermSentiment>());
		}

	}
	
	public void insertEntry(Synset synset, TermSentiment termSentiment)
	{
		if (termSentiment.getSentimentRanks() > 0) {
			lemmaLexicon.get(synset.getPos()).put(synset, termSentiment);
		}
	}
	
	public void insertEntry(SynsetCategory synsetcat, String term, TermSentiment termSentiment)
	{
		if (termSentiment.getSentimentRanks() > 0) {
			if (!synsetsOnly) {
				nonLemmaLexicon.get(synsetcat).put(term, termSentiment);
			}
			else
				throw new RuntimeException("Attempt to insert unlemmatized term entry in a synsets-only lexicon");
		}
	}

	public boolean isSynsetsOnly()
	{
		return synsetsOnly;
	}

	public TermSentiment getEntry(Synset synset)
	{
		try {
			return lemmaLexicon.get(synset.getPos()).get(synset);
		} catch ( NullPointerException e ) {
			return null;
		}
	}
	
	public TermSentiment getEntry(SynsetCategory synsetcat, String term)
	{
		try {
			return nonLemmaLexicon.get(synsetcat).get(term);
		} catch ( NullPointerException e ) {
			return null;
		}
	}
	
	public SentimentLexicon filter(TermSentimentFilter filter)
	{
		SentimentLexicon filtered_lexicon = new SentimentLexicon(synsetsOnly);
		
		// Filtering synsets
		Set<SynsetCategory> synsetcats = lemmaLexicon.keySet();
		for (SynsetCategory synsetcat : synsetcats) {
			Iterator<Entry<Synset, TermSentiment>> entries = getLemmaEntries(synsetcat);
			while ( entries.hasNext() ) {
				Entry<Synset, TermSentiment> entry = entries.next();
				if (filter.accepts(entry.getValue())) {
					filtered_lexicon.insertEntry(entry.getKey(), entry.getValue());
					// System.err.println(entry.getValue());
				}
				else {
					// System.err.println("\t\t\t" + entry.getValue());
				}
			}
		}
		
		// Filtering terms
		if (!synsetsOnly) {
			synsetcats = nonLemmaLexicon.keySet();
			for (SynsetCategory synsetcat : synsetcats) {
				Iterator<Entry<String, TermSentiment>> entries = getNonLemmaEntries(synsetcat);
				while ( entries.hasNext() ) {
					Entry<String, TermSentiment> entry = entries.next();
					if (filter.accepts(entry.getValue())) {
						filtered_lexicon.insertEntry(synsetcat, entry.getKey(), new TermSentiment(entry.getValue(),
								filter.getScore()));
					}
				}
			}
		}
		
		return filtered_lexicon;
	}
	

	public SentimentLexicon filterSentimentDiscrepancies(SentimentLexicon otherlexicon, boolean polarity)
	{
		SentimentLexicon filtered_lexicon = new SentimentLexicon(synsetsOnly && otherlexicon.synsetsOnly);
		
		// Filtering synsets
		Set<SynsetCategory> synsetcats = lemmaLexicon.keySet();
		for (SynsetCategory synsetcat : synsetcats) {
			Iterator<Synset> entries = getLemmaTerms(synsetcat);
			while ( entries.hasNext() ) {
				Synset entry = entries.next();
				
				Sentiment sentiment, othersentiment;
				try {
					sentiment = this.getEntry(entry).getSentiment();
					othersentiment = otherlexicon.getEntry(entry).getSentiment();

					if (!polarity && !sentiment.equals(othersentiment) //
						|| polarity && sentiment.getPolarity() != othersentiment.getPolarity()) {
						filtered_lexicon.insertEntry(entry, this.getEntry(entry));
					}
				} catch ( NullPointerException e ) {
					continue;
				}
			}
		}
		
		// Filtering terms
		if (!(synsetsOnly && otherlexicon.synsetsOnly)) {
			synsetcats = nonLemmaLexicon.keySet();
			for (SynsetCategory synsetcat : synsetcats) {
				Iterator<String> entries = getNonLemmaTerms(synsetcat);
				while ( entries.hasNext() ) {
					String entry = entries.next();
					
					Sentiment sentiment, othersentiment;
					try {
						sentiment = this.getEntry(synsetcat, entry).getSentiment();
						othersentiment = otherlexicon.getEntry(synsetcat, entry).getSentiment();


						if (!polarity && !sentiment.equals(othersentiment) //
							|| polarity && sentiment.getPolarity() != othersentiment.getPolarity()) {
							filtered_lexicon.insertEntry(synsetcat, entry, this.getEntry(synsetcat, entry));
						}
					} catch ( NullPointerException e ) {
						continue;
					}
				}
			}
		}
		
		return filtered_lexicon;
	}
	
	public static SentimentLexicon merge(SentimentLexicon lex1, SentimentLexicon lex2)
	{
		SentimentLexicon lex = new SentimentLexicon(lex1.isSynsetsOnly() || lex2.isSynsetsOnly());
		
		lex.lemmaLexicon.putAll(new HashMap<SynsetCategory, TreeMap<Synset, TermSentiment>>(lex1.lemmaLexicon));
		lex.lemmaLexicon.putAll(new HashMap<SynsetCategory, TreeMap<Synset, TermSentiment>>(lex2.lemmaLexicon));
		
		if (!lex1.isSynsetsOnly()) {
			lex.nonLemmaLexicon
				.putAll(new HashMap<SynsetCategory, TreeMap<String, TermSentiment>>(lex1.nonLemmaLexicon));
		}
		if (!lex2.isSynsetsOnly()) {
			lex.nonLemmaLexicon
				.putAll(new HashMap<SynsetCategory, TreeMap<String, TermSentiment>>(lex2.nonLemmaLexicon));
		}
		
		return lex;
	}

	public SentimentLexicon unifyWith(SentimentLexicon otherlexicon, boolean polarity)
	{
		SentimentLexicon unified_lexicon = new SentimentLexicon(synsetsOnly && otherlexicon.synsetsOnly);
		
		// Filtering synsets
		Set<SynsetCategory> synsetcats = lemmaLexicon.keySet();
		for (SynsetCategory synsetcat : synsetcats) {
			Iterator<Synset> entries = getLemmaTerms(synsetcat);
			while ( entries.hasNext() ) {
				Synset entry = entries.next();
				
				Sentiment sentiment, othersentiment;
				try {
					sentiment = this.getEntry(entry).getSentiment();
					othersentiment = otherlexicon.getEntry(entry).getSentiment();
					
					if (!sentiment.isNeutral() && !othersentiment.isNeutral()) {
						if (!polarity && sentiment.equals(othersentiment) //
							|| polarity && sentiment.getPolarity() == othersentiment.getPolarity()) {
							unified_lexicon.insertEntry(entry, this.getEntry(entry));
						}
					}
				} catch ( NullPointerException e ) {
					continue;
				}
			}
		}
		
		// Filtering terms
		if (!(synsetsOnly && otherlexicon.synsetsOnly)) {
			synsetcats = nonLemmaLexicon.keySet();
			for (SynsetCategory synsetcat : synsetcats) {
				Iterator<String> entries = getNonLemmaTerms(synsetcat);
				while ( entries.hasNext() ) {
					String entry = entries.next();
					
					Sentiment sentiment, othersentiment;
					try {
						sentiment = this.getEntry(synsetcat, entry).getSentiment();
						othersentiment = otherlexicon.getEntry(synsetcat, entry).getSentiment();


						if (!sentiment.isNeutral() && !othersentiment.isNeutral()) {
							if (!polarity && sentiment.equals(othersentiment) //
								|| polarity && sentiment.getPolarity() == othersentiment.getPolarity()) {
								unified_lexicon.insertEntry(synsetcat, entry, this.getEntry(synsetcat, entry));
							}
						}
					} catch ( NullPointerException e ) {
						continue;
					}
				}
			}
		}
		
		return unified_lexicon;
	}

	public Iterator<Synset> getLemmaTerms(SynsetCategory synsetcat)
	{
		return lemmaLexicon.get(synsetcat).keySet().iterator();
	}
	
	public Iterator<String> getNonLemmaTerms(SynsetCategory synsetcat)
	{
		return nonLemmaLexicon.get(synsetcat).keySet().iterator();
	}
	
	public Iterator<Entry<Synset, TermSentiment>> getLemmaEntries(SynsetCategory synsetcat)
	{
		return lemmaLexicon.get(synsetcat).entrySet().iterator();
	}
	
	public Iterator<Entry<String, TermSentiment>> getNonLemmaEntries(SynsetCategory synsetcat)
	{
		return nonLemmaLexicon.get(synsetcat).entrySet().iterator();
	}
	
	public Integer countLemmaEntries(SynsetCategory synsetcat)
	{
		try {
			return lemmaLexicon.get(synsetcat).size();
		} catch ( NullPointerException e ) {
			return 0;
		}
	}
	
	public Integer countLemmaEntries()
	{
		int count = 0;
		Set<SynsetCategory> synsetcats = lemmaLexicon.keySet();
		for (SynsetCategory synsetcat : synsetcats) {
			count += countLemmaEntries(synsetcat);
		}
		return count;
	}

	public String printLexiconSizeSummary()
	{
		StringBuilder summary = new StringBuilder();
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		Integer[] lemmaLexicon_size = new Integer[synsetcats.length];
		Integer[] nonLemmaLexicon_size = new Integer[synsetcats.length];
		Integer lemmaLexicon_size_total = 0;
		Integer nonLemmaLexicon_size_total = 0;
		
		// Print header line and retrieve lengths
		summary.append("                       \tTOTAL");
		for (int cat = 0 ; cat < synsetcats.length ; cat++) {
			summary.append("\t" + synsetcats[cat]);
			
			try {
				lemmaLexicon_size[cat] = lemmaLexicon.get(synsetcats[cat]).size();
				lemmaLexicon_size_total += lemmaLexicon_size[cat];
			} catch ( NullPointerException e ) {
				lemmaLexicon_size[cat] = 0;
			}
			
			if (!synsetsOnly) {
				try {
					nonLemmaLexicon_size[cat] = nonLemmaLexicon.get(synsetcats[cat]).size();
					nonLemmaLexicon_size_total += nonLemmaLexicon_size[cat];
				} catch ( NullPointerException e ) {
					nonLemmaLexicon_size[cat] = 0;
				}
			}
		}
		summary.append("\n");
		
		// Print Lemma summary line
		summary.append(" *     Lemmatized terms\t" + lemmaLexicon_size_total);
		for (int cat = 0 ; cat < synsetcats.length ; cat++) {
			summary.append("\t" + lemmaLexicon_size[cat]);
		}
		summary.append("\n");
		if (!synsetsOnly) {
			summary.append(" * Non-lemmatized terms\t" + nonLemmaLexicon_size_total);
			for (int cat = 0 ; cat < synsetcats.length ; cat++) {
				summary.append("\t" + nonLemmaLexicon_size[cat]);
			}
		}
		summary.append("\n");
		
		return summary.toString();
	}
	
	@SuppressWarnings("unchecked")
	private static ArrayList<Integer> getSentimentDistribution(TreeMap lexicon)
	{
		ArrayList<Counter> counters = new ArrayList<Counter>(Ratings.capacity());
		for (int rating = 0 ; rating <= Ratings.N_RATINGS ; rating++) {
			counters.add(rating, new Counter());
		}
		
		Set<?> lexicon_keys = lexicon.keySet();
		for (Object lexicon_key : lexicon_keys) {
			Sentiment sentiment = ((TermSentiment)lexicon.get(lexicon_key)).getSentiment();
			if (sentiment != null) {
				counters.get(sentiment.toNumber(SentimentFormat.RATING)).increment();
			}
		}

		return Counter.toIntegerArray(counters);
	}
	
	private ArrayList<Integer> getLemmaSentimentDistribution(SynsetCategory synsetcat)
	{
		return getSentimentDistribution(lemmaLexicon.get(synsetcat));
	}
	
	private ArrayList<Integer> getNonLemmaSentimentDistribution(SynsetCategory synsetcat)
	{
		return getSentimentDistribution(nonLemmaLexicon.get(synsetcat));
	}

	private ArrayList<Integer> getSentimentDistribution(boolean for_synsets)
	{
		ArrayList<Integer> total_counts = new ArrayList<Integer>(Ratings.capacity());
		for (int rating = 0 ; rating <= Ratings.MAX_RATING ; rating++) {
			total_counts.add(rating, 0);
		}

		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		for (SynsetCategory synsetcat : synsetcats) {
			for (int rating = Ratings.MIN_RATING ; rating <= Ratings.MAX_RATING ; rating++) {
				ArrayList<Integer> synsetcat_counts = for_synsets ? getLemmaSentimentDistribution(synsetcat)
					: getNonLemmaSentimentDistribution(synsetcat);
				total_counts.set(rating, total_counts.get(rating) + synsetcat_counts.get(rating));
			}
		}
		
		return total_counts;
	}
	
	public String printSentimentDistributionSummary()
	{
		StringBuilder summary = new StringBuilder();
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		
		// Header Line
		summary.append('\t').append("Lemmas\t\t\t\t");
		if (!synsetsOnly) {
			summary.append("Non-Lemmas\t\t\t");
		}
		summary.append('\n').append('\t');
		for (int i = 1 ; i <= 2 ; i++) {
			for (SynsetCategory header_synsetcat : synsetcats) {
				summary.append(header_synsetcat.name()).append('\t');
			}
			summary.append("Sum").append('\t');
		}
		summary.append("\n");
		
		// Calculate sentiment counts
		ArrayList<Integer> lemma_total_results = getSentimentDistribution(true);
		HashMap<SynsetCategory, ArrayList<Integer>> lemma_synsetcat_results = new HashMap<SynsetCategory, ArrayList<Integer>>();
		for (SynsetCategory synsetcat : synsetcats) {
			lemma_synsetcat_results.put(synsetcat, getLemmaSentimentDistribution(synsetcat));
		}
		ArrayList<Integer> nonlemma_total_results = getSentimentDistribution(false);
		HashMap<SynsetCategory, ArrayList<Integer>> nonlemma_synsetcat_results = new HashMap<SynsetCategory, ArrayList<Integer>>();
		for (SynsetCategory synsetcat : synsetcats) {
			nonlemma_synsetcat_results.put(synsetcat, getNonLemmaSentimentDistribution(synsetcat));
		}
		
		// Print sentiment counts
		for (int rating = Ratings.MIN_RATING ; rating <= Ratings.MAX_RATING ; rating++) {
			summary.append(rating).append('\t');
			
			for (SynsetCategory synsetcat : synsetcats) {
				summary.append(lemma_synsetcat_results.get(synsetcat).get(rating)).append('\t');
			}
			summary.append(lemma_total_results.get(rating)).append('\t');
			
			if (!synsetsOnly) {
				for (SynsetCategory synsetcat : synsetcats) {
					summary.append(nonlemma_synsetcat_results.get(synsetcat).get(rating)).append('\t');
				}
				summary.append(nonlemma_total_results.get(rating)).append('\t');
			}
			summary.append('\n');
		}
		summary.append("\n");
		
		return summary.toString();
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	private static ArrayList<Integer> getNumberOfSentiments(TreeMap lexicon, float scoreThreshold)
	{
		return getNumberOfSentiments(lexicon, false, scoreThreshold);
	}
	
	
	@SuppressWarnings("unchecked")
	private static ArrayList<Integer> getNumberOfSentiments(TreeMap lexicon, boolean merge_polarities,
			float scoreThreshold)
	{
		ArrayList<Counter> counters = new ArrayList<Counter>(Globals.SentimentParameters.MaxSentimentsPerTerm);
		for (int n = 0 ; n <= Globals.SentimentParameters.MaxSentimentsPerTerm ; n++) {
			counters.add(new Counter());
		}
		
		Set<?> lexicon_keys = lexicon.keySet();
		for (Object lexicon_key : lexicon_keys) {
			TermSentiment sentiment = (TermSentiment)lexicon.get(lexicon_key);
			if (!merge_polarities) {
				counters.get(sentiment.getSentimentRanks()).increment();
			}
			else {
				HashSet<Polarity> polarities = new HashSet<Polarity>(Polarity.values().length);
				for (int rank = sentiment.getSentimentRanks() ; rank > 0 ; rank--) {
					if (sentiment.getSentiment(rank).getScore() >= scoreThreshold) {
						polarities.add(sentiment.getSentiment(rank).getPolarity());
					}
				}
				counters.get(polarities.size()).increment();
			}
		}
		
		return Counter.toIntegerArray(counters);
	}
	
	private ArrayList<Integer> getNumberOfLemmaSentiments(SynsetCategory synsetcat, float scoreThreshold)
	{
		return getNumberOfSentiments(lemmaLexicon.get(synsetcat), true, scoreThreshold);
	}
	
	private ArrayList<Integer> getNumberOfNonLemmaSentiments(SynsetCategory synsetcat, float scoreThreshold)
	{
		return getNumberOfSentiments(nonLemmaLexicon.get(synsetcat), true, scoreThreshold);
	}
	
	private ArrayList<Integer> getNumberOfSentiments(boolean for_synsets, float scoreThreshold)
	{
		ArrayList<Integer> total_counts = new ArrayList<Integer>(Globals.SentimentParameters.MaxSentimentsPerTerm);
		for (int n = 0 ; n <= Globals.SentimentParameters.MaxSentimentsPerTerm ; n++) {
			total_counts.add(n, 0);
		}
		
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		for (SynsetCategory synsetcat : synsetcats) {
			ArrayList<Integer> synsetcat_counts = for_synsets ? getNumberOfLemmaSentiments(synsetcat, scoreThreshold)
				: getNumberOfNonLemmaSentiments(synsetcat, scoreThreshold);
			for (int n = 0 ; n <= Globals.SentimentParameters.MaxSentimentsPerTerm ; n++) {
				total_counts.set(n, total_counts.get(n) + synsetcat_counts.get(n));
			}
		}
		
		return total_counts;
	}


	public String printNumberOfSentimentsSummary()
	{
		StringBuilder summary = new StringBuilder();
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		
		// Header Line
		summary.append('\t').append("Lemmas\t\t\t\t");
		if (!synsetsOnly) {
			summary.append("Non-Lemmas\t\t\t");
		}
		summary.append('\n').append('\t');
		for (SynsetCategory header_synsetcat : synsetcats) {
			summary.append(header_synsetcat.name()).append('\t');
		}
		summary.append("Sum").append('\t');
		if (!synsetsOnly) {
			for (SynsetCategory header_synsetcat : synsetcats) {
				summary.append(header_synsetcat.name()).append('\t');
			}
			summary.append("Sum").append('\t');
		}
		summary.append("\n");

		// Calculate sentiment counts
		ArrayList<Integer> lemma_total_results = getNumberOfSentiments(true, 0);
		HashMap<SynsetCategory, ArrayList<Integer>> lemma_synsetcat_results = new HashMap<SynsetCategory, ArrayList<Integer>>();
		for (SynsetCategory synsetcat : synsetcats) {
			lemma_synsetcat_results.put(synsetcat, getNumberOfLemmaSentiments(synsetcat, 0));
		}
		ArrayList<Integer> nonlemma_total_results = getNumberOfSentiments(false, 0);
		HashMap<SynsetCategory, ArrayList<Integer>> nonlemma_synsetcat_results = new HashMap<SynsetCategory, ArrayList<Integer>>();
		for (SynsetCategory synsetcat : synsetcats) {
			nonlemma_synsetcat_results.put(synsetcat, getNumberOfNonLemmaSentiments(synsetcat, 0));
		}
		
		// Print sentiment counts
		for (int n = 0 ; n <= Globals.SentimentParameters.MaxSentimentsPerTerm ; n++) {
			summary.append(n).append('\t');

			for (SynsetCategory synsetcat : synsetcats) {
				summary.append(lemma_synsetcat_results.get(synsetcat).get(n)).append('\t');
			}
			summary.append(lemma_total_results.get(n)).append('\t');
			
			if (!synsetsOnly) {
				for (SynsetCategory synsetcat : synsetcats) {
					summary.append(nonlemma_synsetcat_results.get(synsetcat).get(n)).append('\t');
				}
				summary.append(nonlemma_total_results.get(n)).append('\t');
			}
			
			summary.append('\n');
		}
		summary.append("\n");
		
		return summary.toString();
	}
	
	public String printUnambiguousTermsRatioSummary(int granularity)
	{
		StringBuilder report = new StringBuilder();
		
		// Score format
		int decimal_points = (int)Math.ceil(Math.log10(granularity));
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(decimal_points);
		nf.setMinimumFractionDigits(decimal_points);
		
		for (int ithr = 0 ; ithr < granularity ; ithr++) {
			float thr = (float)ithr / (float)granularity;
			
			ArrayList<Integer> sentiment_counts = getNumberOfSentiments(true, thr);
			ArrayList<Integer> nonlemma_sentiment_counts = getNumberOfSentiments(false, thr);
			int size = sentiment_counts.size();
			for (int i = 0 ; i < size ; i++) {
				sentiment_counts.set(i, sentiment_counts.get(i) + nonlemma_sentiment_counts.get(i));
			}

			int sum = 0;
			for (int i = 1 ; i < size ; i++) {
				sum += sentiment_counts.get(i);
			}

			float ratio = (float)sentiment_counts.get(1) / (float)sum;
			report.append(nf.format(thr) + "\t" + nf.format(ratio) + "\t" + sentiment_counts.get(1) + " / " + sum)
				.append('\n');
		}
		
		return report.toString();
	}
	
	public String printUnambiguousTermsRatioSummary()
	{
		return printUnambiguousTermsRatioSummary(100);
	}

	/**
	 * 
	 */
	private ArrayList<String> getSentimentScoreLabels(int granularity)
	{
		ArrayList<String> labels = new ArrayList<String>();
		
		// Establish label format
		int fraction_digits = (int)Math.ceil(Math.log10(granularity));
		NumberFormat lf = NumberFormat.getNumberInstance(Locale.US);
		lf.setMinimumFractionDigits(fraction_digits);
		lf.setMaximumFractionDigits(fraction_digits);
		
		// Output formatted labels
		Float step = 1.0f / granularity;
		for (float score = step ; score <= 1.0f ; score += step) {
			labels.add(lf.format(score));
		}
		
		return labels;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<Integer> getPrimarySentimentScore(TreeMap lexicon, int granularity)
	{
		ArrayList<Counter> counters = new ArrayList<Counter>(granularity);
		for (int score_rank = 0 ; score_rank <= granularity ; score_rank++) {
			counters.add(new Counter());
		}
		
		Set<?> lexicon_keys = lexicon.keySet();
		for (Object lexicon_key : lexicon_keys) {
			Sentiment sentiment = ((TermSentiment)lexicon.get(lexicon_key)).getSentiment();
			if (sentiment != null) {
				int score_rank = (int)(sentiment.getScore() * granularity);
				counters.get(score_rank).increment();
			}
		}
		
		return Counter.toIntegerArray(counters);
	}
	
	private ArrayList<Integer> getLemmaPrimarySentimentScore(int granularity)
	{
		ArrayList<Integer> total_counters = new ArrayList<Integer>();
		for (int score_rank = 0 ; score_rank < granularity ; score_rank++) {
			total_counters.add(0);
		}
		
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		for (SynsetCategory synsetcat : synsetcats) {
			ArrayList<Integer> synsetcat_counters = getPrimarySentimentScore(lemmaLexicon.get(synsetcat), granularity);
			for (int score_rank = 0 ; score_rank < granularity ; score_rank++) {
				total_counters.set(score_rank, total_counters.get(score_rank) + synsetcat_counters.get(score_rank));
			}
		}

		return total_counters;
	}

	private ArrayList<Integer> getNonLemmaPrimarySentimentScore(int granularity)
	{
		ArrayList<Integer> total_counters = new ArrayList<Integer>();
		for (int score_rank = 0 ; score_rank < granularity ; score_rank++) {
			total_counters.add(0);
		}
		
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		for (SynsetCategory synsetcat : synsetcats) {
			ArrayList<Integer> synsetcat_counters = getPrimarySentimentScore(nonLemmaLexicon.get(synsetcat),
																				granularity);
			for (int score_rank = 0 ; score_rank < granularity ; score_rank++) {
				total_counters.set(score_rank, total_counters.get(score_rank) + synsetcat_counters.get(score_rank));
			}
		}
		
		return total_counters;
	}
	
	public String printPrimarySentimentScoreSummary(int granularity)
	{
		StringBuilder summary = new StringBuilder();
		ArrayList<String> labels = getSentimentScoreLabels(granularity);
		ArrayList<Integer> lemma_results = getLemmaPrimarySentimentScore(granularity);
		ArrayList<Integer> nonlemma_results = getNonLemmaPrimarySentimentScore(granularity);
		
		// Header Line
		summary.append('\t').append("Lemmas");
		if (!synsetsOnly) {
			summary.append('\t').append("Non-Lemmas");
		}
		summary.append('\n');
		
		// Table rows
		for (int score_rank = 0 ; score_rank < granularity ; score_rank++) {
			summary.append(labels.get(score_rank)).append('\t').append(lemma_results.get(score_rank));
			if (!synsetsOnly) {
				summary.append('\t').append(nonlemma_results.get(score_rank));
			}
			summary.append('\n');
		}
		summary.append("\n");
		
		return summary.toString();
	}
	
	public String printPrimarySentimentScoreSummary()
	{
		return printPrimarySentimentScoreSummary(100);
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	private TreeMap<Integer, Counter> getSubjectivityScoreHistogram(TreeMap lexicon, int decimal_points)
	{
		int round_factor = 1;
		for (int i = 0 ; i < decimal_points ; i++) {
			round_factor *= 10;
		}
		
		TreeMap<Integer, Counter> counters = new TreeMap<Integer, Counter>();
		
		Set<?> lexicon_keys = lexicon.keySet();
		for (Object lexicon_key : lexicon_keys) {
			Integer subjectivity_score = Math.round(round_factor
				* ((TermSentiment)lexicon.get(lexicon_key)).getSubjectivityScore());
			if (!counters.containsKey(subjectivity_score)) {
				counters.put(subjectivity_score, new Counter());
			}
			counters.get(subjectivity_score).increment();
		}
		
		return counters;
	}
	
	@SuppressWarnings("unchecked")
	private TreeMap<Integer, Counter> getSubjectivityScoreHistogram(int decimal_points)
	{
		TreeMap<Integer, Counter> total_counters = new TreeMap<Integer, Counter>();
		
		SynsetCategory[] synsetcats = { SynsetCategory.V, SynsetCategory.N };
		
		for (SynsetCategory synsetcat : synsetcats) {
			TreeMap<Integer, Counter> lemma_counters = getSubjectivityScoreHistogram(lemmaLexicon. //
				get(synsetcat), decimal_points);
			Set<Entry<Integer, Counter>> lemma_counter_entries = lemma_counters.entrySet();
			for (Entry<Integer, Counter> counter_entry : lemma_counter_entries) {
				if (!total_counters.containsKey(counter_entry.getKey())) {
					total_counters.put(counter_entry.getKey(), new Counter());
				}
				total_counters.get(counter_entry.getKey()).add(counter_entry.getValue().get());
			}
			
			TreeMap<Integer, Counter> nonlemma_counters = getSubjectivityScoreHistogram(nonLemmaLexicon. //
				get(synsetcat), decimal_points);
			Set<Entry<Integer, Counter>> nonlemma_counter_entries = nonlemma_counters.entrySet();
			for (Entry<Integer, Counter> counter_entry : nonlemma_counter_entries) {
				if (!total_counters.containsKey(counter_entry.getKey())) {
					total_counters.put(counter_entry.getKey(), new Counter());
				}
				total_counters.get(counter_entry.getKey()).add(counter_entry.getValue().get());
			}
		}
		return total_counters;
	}
	
	
	public String printSubjectivityScoreHistogram(int decimal_points)
	{
		StringBuilder report = new StringBuilder();
		
		int objcount = 0;
		int totalcount = 0;
		TreeMap<Integer, Counter> subj_histogram = getSubjectivityScoreHistogram(decimal_points);
		

		int round_factor = 1;
		for (int i = 0 ; i < decimal_points ; i++) {
			round_factor *= 10;
		}
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(decimal_points);
		nf.setMinimumFractionDigits(decimal_points);

		report.append("SSCORE").append('\t').append("COUNT").append('\n');

		Set<Entry<Integer, Counter>> entries = subj_histogram.entrySet();
		for (Entry<Integer, Counter> entry : entries) {
			Float sscore = entry.getKey() / (float)round_factor;
			Integer freq_count = entry.getValue().get();
			
			if (sscore >= Globals.TopicParameters.subjectivityThreshold) {
				objcount += freq_count;
			}
			totalcount += freq_count;
			
			report.append(nf.format(sscore)).append('\t').append(freq_count).append('\n');
		}
		
		System.out.println((float)objcount / (float)totalcount + " of lemmas are candidate topical");
		return report.toString();
	}
	
	public String printSubjectivityScoreHistogram()
	{
		return printSubjectivityScoreHistogram(2);
	}


	/**
	 * 
	 */
	public void printSummary()
	{
		System.out.println(printSubjectivityScoreHistogram());
		System.out.println(printLexiconSizeSummary());
		System.out.println(printPrimarySentimentScoreSummary());
		System.out.println(printNumberOfSentimentsSummary());
		System.out.println(printUnambiguousTermsRatioSummary());
		System.out.println(printSentimentDistributionSummary());
	}

	public String toString()
	{
		return printLexiconSizeSummary();
	}
	
	public static String name(SentimentClassifier classifier, PayloadFilter filter)
	{
		return "lexicon_" + classifier.name() + "_" + filter.name();
	}
	
	public static SentimentLexicon loadLexicon()
	{
		return loadLexicon(new WidestWindowSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
	}

	public static SentimentLexicon loadLexicon(SentimentClassifier classifier, PayloadFilter filter)
	{
		String name = name(classifier, filter);
		SentimentLexicon lexicon = null;
		State<SentimentLexicon> lexicon_state = new State<SentimentLexicon>(name, lexicon);
		try {
			lexicon = lexicon_state.restoreState();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to load lexicon " + name);
			System.exit(-1);
		}
		return lexicon;
	}
	
	/**
	 * @param classifier
	 * @param fILTERPLAIN
	 * @return
	 */
	public static SentimentLexicon loadDiscrepancies(SentimentClassifier classifier, PayloadFilter filter)
	{
		String name = "discrepancies_" + name(classifier, filter);
		SentimentLexicon lexicon = null;
		State<SentimentLexicon> lexicon_state = new State<SentimentLexicon>(name, lexicon);
		try {
			lexicon = lexicon_state.restoreState();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to load lexicon " + name);
			System.exit(-1);
		}
		return lexicon;
	}
	
	/**
	 * 
	 */
	public void fixSentimentOrder()
	{
		for (TreeMap<Synset, TermSentiment> map : lemmaLexicon.values()) {
			for (Entry<Synset, TermSentiment> entry : map.entrySet()) {
				entry.getValue().fixSentimentOrder();
			}
		}
		
		for (TreeMap<String, TermSentiment> map : nonLemmaLexicon.values()) {
			for (Entry<String, TermSentiment> entry : map.entrySet()) {
				entry.getValue().fixSentimentOrder();
			}
		}
	}


}
