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

import indexing.SynsetTermsAggregator;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map.Entry;
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;
import lexicon.TermSentiment.Sentiment.SentimentFormat;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import classes.Counter;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class SentimentLexiconComparer {
	
	private SentimentLexicon	testLexicon					= null;
	private SentimentLexicon	referenceLexicon			= null;
	
	private SentimentLexicon	testLexiconFiltered			= null;
	private SentimentLexicon	referenceLexiconFiltered	= null;
	private SentimentLexicon	junctionLexicon				= null;
	
	private SentimentLexicon	unmatchedLexicon			= null;
	
	/**
	 * Constructor for class SentimentLexiconComparer
	 */
	// public SentimentLexiconComparer(SentimentLexicon candidate, SentimentLexicon base)
	// {
	// testLexiconFiltered = testLexicon = separateUnmatchedTerms(candidate, base);
	// referenceLexiconFiltered = referenceLexicon = synsetJunction(base, testLexicon);
	// junctionLexicon = synsetJunction(testLexiconFiltered, referenceLexiconFiltered);
	// }
	//
	// public SentimentLexiconComparer(SentimentLexiconComparer comparer)
	// {
	// testLexiconFiltered = testLexicon = cloneLexicon(comparer.testLexiconFiltered);
	// referenceLexiconFiltered = referenceLexicon =
	// cloneLexicon(comparer.referenceLexiconFiltered);
	// junctionLexicon = synsetJunction(testLexiconFiltered, referenceLexiconFiltered);
	// }

	public SentimentLexiconComparer(SentimentLexicon candidate, SentimentLexicon base)
	{
		testLexiconFiltered = testLexicon = synsetJunction(candidate, base);
		referenceLexiconFiltered = referenceLexicon = synsetJunction(base, testLexicon);
		junctionLexicon = synsetJunction(testLexiconFiltered, referenceLexiconFiltered);
	}
	
	public SentimentLexiconComparer(SentimentLexiconComparer comparer)
	{
		testLexiconFiltered = testLexicon = cloneLexicon(comparer.testLexiconFiltered);
		referenceLexiconFiltered = referenceLexicon = cloneLexicon(comparer.referenceLexiconFiltered);
		junctionLexicon = synsetJunction(testLexiconFiltered, referenceLexiconFiltered);
	}
	
	private SentimentLexicon cloneLexicon(SentimentLexicon original)
	{
		return original.filter(new TermSentimentFilter());
	}
	
	private SentimentLexicon separateUnmatchedTerms(SentimentLexicon candidate, SentimentLexicon base)
	{
		SentimentLexicon matchedLexicon = new SentimentLexicon(candidate.isSynsetsOnly() && base.isSynsetsOnly());
		unmatchedLexicon = new SentimentLexicon(candidate.isSynsetsOnly());
		
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		for (SynsetCategory synsetcat : synsetcats) {
			Iterator<Entry<Synset, TermSentiment>> lemma_entries = candidate.getLemmaEntries(synsetcat);
			while ( lemma_entries.hasNext() ) {
				Entry<Synset, TermSentiment> lemma_entry = lemma_entries.next();
				Synset synset = lemma_entry.getKey();
				TermSentiment smnt = lemma_entry.getValue();
				if (smnt == null) {
					System.out.println("null");
				}
				if (base.getEntry(synset) == null) {
					unmatchedLexicon.insertEntry(synset, smnt);
				}
				else {
					matchedLexicon.insertEntry(synset, smnt);
				}
			}
			
			if (!candidate.isSynsetsOnly()) {
				Iterator<Entry<String, TermSentiment>> nonlemma_entries = candidate.getNonLemmaEntries(synsetcat);
				while ( nonlemma_entries.hasNext() ) {
					Entry<String, TermSentiment> nonlemma_entry = nonlemma_entries.next();
					String term = nonlemma_entry.getKey();
					TermSentiment smnt = nonlemma_entry.getValue();
					if (base.getEntry(synsetcat, term) == null) {
						unmatchedLexicon.insertEntry(synsetcat, term, smnt);
					}
					else {
						matchedLexicon.insertEntry(synsetcat, term, smnt);
					}
				}
			}
		}
		
		return matchedLexicon;
	}
	
	public void applyFilter(TermSentimentFilter filter)
	{
		applyFilter(filter, filter);
	}
	
	public void applyFilter(TermSentimentFilter candidateFilter, TermSentimentFilter baseFilter)
	{
		testLexiconFiltered = candidateFilter == null ? testLexicon : testLexicon.filter(candidateFilter);
		referenceLexiconFiltered = baseFilter == null ? referenceLexicon : referenceLexicon.filter(baseFilter);
		junctionLexicon = synsetJunction(testLexiconFiltered, referenceLexiconFiltered);
	}
	
	public void refineFilter(TermSentimentFilter filter)
	{
		refineFilter(filter, filter);
	}
	
	public void refineFilter(TermSentimentFilter candidateFilter, TermSentimentFilter baseFilter)
	{
		testLexiconFiltered = candidateFilter == null ? testLexiconFiltered : testLexiconFiltered
			.filter(candidateFilter);
		referenceLexiconFiltered = baseFilter == null ? referenceLexiconFiltered : referenceLexiconFiltered
			.filter(baseFilter);
		junctionLexicon = synsetJunction(testLexiconFiltered, referenceLexiconFiltered);
	}
	
	
	// private Integer countEntries()
	// {
	// return testLexiconFiltered.countLemmaEntries();
	// }
	//
	// private Integer countEntries(SynsetCategory synsetcat)
	// {
	// return testLexiconFiltered.countLemmaEntries(synsetcat);
	// }
	//
	// private Integer countAccurateEntries()
	// {
	// return junctionLexicon.countLemmaEntries();
	// }
	//
	// private Integer countAccurateEntries(SynsetCategory synsetcat)
	// {
	// return junctionLexicon.countLemmaEntries(synsetcat);
	// }
	
	// Returns a lexicon that contains the synsets that are both in testLexicon and
	// referenceLexicon, with their termSentiment as specified in testLexicon
	private static SentimentLexicon synsetJunction(SentimentLexicon testLexicon, SentimentLexicon referenceLexicon)
	{
		SentimentLexicon junction = new SentimentLexicon(true);
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		
		for (SynsetCategory synsetcat : synsetcats) {
			Iterator<Entry<Synset, TermSentiment>> testEntries = testLexicon.getLemmaEntries(synsetcat);
			while ( testEntries.hasNext() ) {
				Entry<Synset, TermSentiment> testEntry = testEntries.next();
				TermSentiment referenceEntrySentiment = referenceLexicon.getEntry(testEntry.getKey());
				if (referenceEntrySentiment != null) {
					junction.insertEntry(testEntry.getKey(), testEntry.getValue());
				}
			}
		}
		
		return junction;
	}
	
	private static void printSynsetJunction(SentimentLexicon testLexicon, SentimentLexicon referenceLexicon,
			SynsetTermsAggregator aggregator)
	{
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		
		for (SynsetCategory synsetcat : synsetcats) {
			Iterator<Entry<Synset, TermSentiment>> testEntries = testLexicon.getLemmaEntries(synsetcat);
			while ( testEntries.hasNext() ) {
				Entry<Synset, TermSentiment> testEntry = testEntries.next();
				TermSentiment referenceEntrySentiment = referenceLexicon.getEntry(testEntry.getKey());
				if (referenceEntrySentiment != null) {
					System.out.println( //
						"[" + testEntry.getValue().toString() + "]\t" + //
							"[" + referenceEntrySentiment.toString() + "]\t" + //
							aggregator.getSynsetTermsString(testEntry.getKey(), true) //
						);
				}
			}
		}
		System.out.println("- - - - - - - - - - - ");
	}
	
	public String recallStr()
	{
		return junctionLexicon.countLemmaEntries() + "/" + referenceLexiconFiltered.countLemmaEntries() + "\t"
			+ doubleFormat.format(recall());
	}
	
	public String recallStr(SynsetCategory synsetcat)
	{
		return junctionLexicon.countLemmaEntries(synsetcat) + "/"
			+ referenceLexiconFiltered.countLemmaEntries(synsetcat) + "\t" + doubleFormat.format(recall(synsetcat));
	}
	
	public Double recall()
	{
		return 1.0 * junctionLexicon.countLemmaEntries() / referenceLexiconFiltered.countLemmaEntries();
	}
	
	public Double recall(SynsetCategory synsetcat)
	{
		return 1.0 * //
			junctionLexicon.countLemmaEntries(synsetcat) / referenceLexiconFiltered.countLemmaEntries(synsetcat);
	}
	
	public String precisionStr()
	{
		return junctionLexicon.countLemmaEntries() + "/" + testLexiconFiltered.countLemmaEntries() + "\t"
			+ doubleFormat.format(precision());
	}
	
	public String precisionStr(SynsetCategory synsetcat)
	{
		return junctionLexicon.countLemmaEntries(synsetcat) + "/" + testLexiconFiltered.countLemmaEntries(synsetcat)
			+ "\t" + doubleFormat.format(precision(synsetcat));
	}
	
	public Double precision()
	{
		return 1.0 * junctionLexicon.countLemmaEntries() / testLexiconFiltered.countLemmaEntries();
	}
	
	public Double precision(SynsetCategory synsetcat)
	{
		return 1.0 * junctionLexicon.countLemmaEntries(synsetcat) / testLexiconFiltered.countLemmaEntries(synsetcat);
	}
	
	public Double fscore()
	{
		Double precision = precision();
		Double recall = recall();
		return fscore(precision, recall);
	}
	
	public Double fscore(SynsetCategory synsetcat)
	{
		Double precision = precision(synsetcat);
		Double recall = recall(synsetcat);
		return fscore(precision, recall);
	}
	
	public static Double fscore(Double precision, Double recall)
	{
		return 2.0 * precision * recall / (precision + recall);
	}
	
	private static NumberFormat	doubleFormat	= getDoubleFormatInstance();
	
	private static NumberFormat getDoubleFormatInstance()
	{
		NumberFormat nf = NumberFormat.getInstance(Locale.US);
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		return nf;
	}
	
	public void printDiscrepanciesDictionary(int min_observations)
	{
		SynsetTermsAggregator aggregator = SynsetTermsAggregator.load();
		
		this.applyFilter(new TermSentimentFilter().setMinObservations(min_observations), null);
		SentimentLexiconComparer comparer = new SentimentLexiconComparer(this);
		TermSentimentFilter testLexiconFilter = new TermSentimentFilter();
		TermSentimentFilter referenceLexiconFilter = new TermSentimentFilter();
		
		System.out.println(Polarity.POSITIVE + "\n");
		comparer.applyFilter(testLexiconFilter.setPolarity(Polarity.POSITIVE, false, true), referenceLexiconFilter
			.setPolarity(Polarity.NEGATIVE, true, false));
		SentimentLexiconComparer.printSynsetJunction(comparer.testLexiconFiltered, comparer.referenceLexiconFiltered,
														aggregator);

		System.out.println(Polarity.NEGATIVE + "\n");
		comparer.applyFilter(testLexiconFilter.setPolarity(Polarity.NEGATIVE, false, true), referenceLexiconFilter
			.setPolarity(Polarity.POSITIVE, true, false));
		SentimentLexiconComparer.printSynsetJunction(comparer.testLexiconFiltered, comparer.referenceLexiconFiltered,
														aggregator);
	}
	
	public String getPrecisionRecallReport(int min_observations)
	{
		StringBuilder report = new StringBuilder();
		
		this.applyFilter(new TermSentimentFilter().setMinObservations(min_observations), null);
		SentimentLexiconComparer comparer = new SentimentLexiconComparer(this);
		Double precision, recall, fscore;
		
		// Report for all synsets
		report.append("ALL").append('\n');
		TermSentimentFilter testLexiconFilter = new TermSentimentFilter();
		TermSentimentFilter referenceLexiconFilter = new TermSentimentFilter();
		
		Polarity[] polarities = { Polarity.POSITIVE, Polarity.NEGATIVE };
		for (Polarity polarity : polarities) {
			report.append("  " + polarity.name()).append('\n');
			
			report.append("    STANDARD").append('\t');
			comparer.applyFilter(testLexiconFilter.setPolarity(polarity, false, false), referenceLexiconFilter
				.setPolarity(polarity, false, false));
			precision = comparer.precision();
			report.append(comparer.precisionStr()).append('\t');
			
			recall = comparer.recall();
			report.append(comparer.recallStr()).append('\t');
			report.append(doubleFormat.format(fscore(precision, recall))).append('\n');
			
			report.append("    PRIMARY  ").append('\t');
			comparer.applyFilter(testLexiconFilter.setPolarity(polarity, true, false), referenceLexiconFilter
				.setPolarity(polarity, false, false));
			precision = comparer.precision();
			report.append(comparer.precisionStr()).append('\t');
			
			recall = comparer.recall();
			report.append(comparer.recallStr()).append('\t');
			report.append(doubleFormat.format(fscore(precision, recall))).append('\n');
		}
		

		// Report per synset category
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		for (SynsetCategory synsetcat : synsetcats) {
			
			report.append(synsetcat.name()).append('\n');
			
			for (Polarity polarity : polarities) {
				report.append("  " + polarity.name()).append('\n');
				
				report.append("    STANDARD").append('\t');
				comparer.applyFilter(testLexiconFilter.setPolarity(polarity, false, false), referenceLexiconFilter
					.setPolarity(polarity, false, false));
				precision = comparer.precision(synsetcat);
				report.append(comparer.precisionStr(synsetcat)).append('\t');
				
				recall = comparer.recall(synsetcat);
				report.append(comparer.recallStr(synsetcat)).append('\t');
				report.append(doubleFormat.format(fscore(precision, recall))).append('\n');
				
				report.append("    PRIMARY  ").append('\t');
				comparer.applyFilter(testLexiconFilter.setPolarity(polarity, true, false), referenceLexiconFilter
					.setPolarity(polarity, false, false));
				precision = comparer.precision(synsetcat);
				report.append(comparer.precisionStr(synsetcat)).append('\t');
				
				recall = comparer.recall(synsetcat);
				report.append(comparer.recallStr(synsetcat)).append('\t');
				report.append(doubleFormat.format(fscore(precision, recall))).append('\n');
			}
		}
		

		return report.toString();
	}
	
	public String getROCCurveReport(int granularity)
	{
		// Score format
		int fraction_digits = (int)Math.ceil(Math.log10(granularity));
		NumberFormat scoreFormat = NumberFormat.getNumberInstance(Locale.US);
		scoreFormat.setMinimumFractionDigits(fraction_digits);
		scoreFormat.setMaximumFractionDigits(fraction_digits);
		
		// Generate report
		StringBuilder report = new StringBuilder();
		for (Polarity polarity : Polarity.values()) {
			report.append(polarity);
			SentimentLexiconComparer p_comparer = new SentimentLexiconComparer(this);
			SentimentLexiconComparer r_comparer = new SentimentLexiconComparer(this);
			TermSentimentFilter testLexiconFilter = new TermSentimentFilter();
			TermSentimentFilter referenceLexiconFilter = new TermSentimentFilter();
			
			p_comparer.applyFilter(testLexiconFilter.setPolarity(polarity, true, false), referenceLexiconFilter
				.setPolarity(polarity, false, false));
			r_comparer.applyFilter(testLexiconFilter.setPolarity(polarity, true, false), referenceLexiconFilter
				.setPolarity(polarity, false, false));
			

			// Header Row
			SynsetCategory[] synsetcats = Synset.getSynsetCategories();
			report.append("\tALL\t\t");
			for (SynsetCategory synsetcat : synsetcats) {
				report.append(synsetcat.name() + "\t\t");
			}
			report.append("\n\t");
			for (int col = 0 ; col <= synsetcats.length ; col++) {
				report.append("Pre\t\tRec\t\t");
			}
			report.append('\n');
			

			// Table rows
			TermSentimentFilter scoreFilter = new TermSentimentFilter();
			float step = 1.0f / granularity;
			for (float minscore = 0 ; minscore <= 1.0 ; minscore += step) {
				scoreFilter.setScore(minscore);
				p_comparer.refineFilter(scoreFilter, null);
				r_comparer.refineFilter(scoreFilter, scoreFilter);
				
				report.append(scoreFormat.format(minscore)).append('\t');
				
				report.append(p_comparer.precisionStr()).append('\t');
				report.append(r_comparer.recallStr()).append('\t');
				// for (SynsetCategory synsetcat : synsetcats) {
				// report.append(p_comparer.precisionStr(synsetcat)).append('\t');
				// report.append(r_comparer.recallStr(synsetcat)).append('\t');
				// }
				
				report.append('\n');
			}
			report.append('\n');
		}
		
		return report.toString();
	}
	
	public String getObservationsReport(int min_observations, int max_observations, int step)
	{
		// Generate report
		StringBuilder report = new StringBuilder();
		for (Polarity polarity : Polarity.values()) {
			report.append(polarity);
			SentimentLexiconComparer p_comparer = new SentimentLexiconComparer(this);
			SentimentLexiconComparer r_comparer = new SentimentLexiconComparer(this);
			TermSentimentFilter testLexiconFilter = new TermSentimentFilter();
			TermSentimentFilter referenceLexiconFilter = new TermSentimentFilter();
			
			p_comparer.applyFilter(testLexiconFilter.setPolarity(polarity, false, false), referenceLexiconFilter
				.setPolarity(polarity, false, false));
			r_comparer.applyFilter(testLexiconFilter.setPolarity(polarity, false, false), referenceLexiconFilter
				.setPolarity(polarity, false, false));
			

			// Header Row
			SynsetCategory[] synsetcats = Synset.getSynsetCategories();
			report.append("\tALL\t\t");
			for (SynsetCategory synsetcat : synsetcats) {
				report.append(synsetcat.name() + "\t\t");
			}
			report.append("\n\t");
			for (int col = 0 ; col <= synsetcats.length ; col++) {
				report.append("Pre\t\tRec\t\t");
			}
			report.append('\n');
			

			// Table rows
			TermSentimentFilter instanceFilter = new TermSentimentFilter();
			for (int observations = min_observations ; observations <= max_observations ; observations += step) {
				instanceFilter.setMinObservations(observations);
				p_comparer.refineFilter(instanceFilter, null);
				r_comparer.refineFilter(instanceFilter, null);
				
				if (p_comparer.precision() > 0.0) {
					report.append(observations).append('\t');
					
					report.append(p_comparer.precisionStr()).append('\t');
					report.append(r_comparer.recallStr()).append('\t');
					
					// for (SynsetCategory synsetcat : synsetcats) {
					// report.append(p_comparer.precisionStr(synsetcat)).append('\t');
					// report.append(r_comparer.recallStr(synsetcat)).append('\t');
					// }
					
					report.append('\n');
				}
			}
			report.append('\n');
		}
		
		return report.toString();
	}

	public void printComparisonReport()
	{
		// System.out.println(getPrecisionRecallReport(Globals.LexiconParameters.minimumSynsetInstances));
		// printDiscrepanciesDictionary(Globals.LexiconParameters.minimumSynsetInstances);
		// System.out.println(getROCCurveReport(100));
		System.out.println(getObservationsReport(0, 10000, 5));
	}
	
	public void printComparisonReport(int min_observations)
	{
		// System.out.println(getPrecisionRecallReport(min_observations));
		// printDiscrepanciesDictionary(min_observations);
		// System.out.println(getROCCurveReport(25));
	}
	
	
	public static void printCommonPolaritiesReport(SentimentLexicon[] lexicons)
	{
		Integer[] common = new Integer[Polarity.values().length];
		Integer[] total = new Integer[Polarity.values().length];
		for (Polarity polarity : Polarity.values()) {
			common[polarity.ordinal()] = 0;
			total[polarity.ordinal()] = 0;
		}
		
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		for (SynsetCategory synsetcat : synsetcats) {
			Iterator<Synset> lemmas = lexicons[0].getLemmaTerms(synsetcat);
			while ( lemmas.hasNext() ) {
				Synset lemma = lemmas.next();
				try {
					Polarity polarity = lexicons[0].getEntry(lemma).getSentiment().getPolarity();
					++common[polarity.ordinal()];
					++total[polarity.ordinal()];
					for (SentimentLexicon lexicon : lexicons) {
						try {
							if (polarity != lexicon.getEntry(lemma).getSentiment().getPolarity()) {
								--common[polarity.ordinal()];
							}
						} catch ( NullPointerException e ) {
							--common[polarity.ordinal()];
						}
						
					}
				} catch ( NullPointerException e ) {
				}
			}
			
			Iterator<String> nonlemmas = lexicons[0].getNonLemmaTerms(synsetcat);
			while ( nonlemmas.hasNext() ) {
				String nonlemma = nonlemmas.next();
				try {
					Polarity polarity = lexicons[0].getEntry(synsetcat, nonlemma).getSentiment().getPolarity();
					++common[polarity.ordinal()];
					++total[polarity.ordinal()];
					for (SentimentLexicon lexicon : lexicons) {
						try {
							if (polarity != lexicon.getEntry(synsetcat, nonlemma).getSentiment().getPolarity()) {
								--common[polarity.ordinal()];
							}
						} catch ( NullPointerException e ) {
							--common[polarity.ordinal()];
						}
					}
				} catch ( NullPointerException e ) {
				}
			}
		}
		
		Integer commonsum = 0, totalsum = 0;
		for (Polarity polarity : Polarity.values()) {
			commonsum += common[polarity.ordinal()];
			totalsum += total[polarity.ordinal()];
			System.out.println(polarity + "\t" + common[polarity.ordinal()] + " / " + total[polarity.ordinal()] + " = "
				+ (float)common[polarity.ordinal()] / (float)total[polarity.ordinal()]);
		}
		System.out.println("TOTAL\t" + commonsum + " / " + totalsum + " = " + (float)commonsum / (float)totalsum);
		
	}
	
	public void printDiscrepanciesChart()
	{
		printDiscrepanciesChart(Synset.getSynsetCategories());
	}
	
	public void printDiscrepanciesChart(SynsetCategory[] synsetcats)
	{
		Counter[][] dcounts = new Counter[Ratings.N_RATINGS][Ratings.N_RATINGS];
		for (int testr = 0 ; testr < Ratings.N_RATINGS ; testr++) {
			for (int refr = 0 ; refr < Ratings.N_RATINGS ; refr++) {
				dcounts[testr][refr] = new Counter();
			}
		}
		

		for (SynsetCategory synsetcat : synsetcats) {
			Iterator<Synset> lemmas = testLexiconFiltered.getLemmaTerms(synsetcat);
			while ( lemmas.hasNext() ) {
				Synset lemma = lemmas.next();
				try {
					Sentiment test_sentiment = testLexiconFiltered.getEntry(lemma).getSentiment();
					Sentiment ref_sentiment = referenceLexiconFiltered.getEntry(lemma).getSentiment();
					dcounts[test_sentiment.toNumber(SentimentFormat.INDEX_POINTER)][ref_sentiment
						.toNumber(SentimentFormat.INDEX_POINTER)].increment();
				} catch ( Exception e ) {
					continue;
				}
			}
			
			if (!(testLexicon.isSynsetsOnly() || referenceLexicon.isSynsetsOnly())) {
				Iterator<String> nonlemmas = testLexiconFiltered.getNonLemmaTerms(synsetcat);
				while ( nonlemmas.hasNext() ) {
					String nonlemma = nonlemmas.next();
					try {
						Sentiment test_sentiment = testLexiconFiltered.getEntry(synsetcat, nonlemma).getSentiment();
						Sentiment ref_sentiment = referenceLexiconFiltered.getEntry(synsetcat, nonlemma).getSentiment();
						dcounts[test_sentiment.toNumber(SentimentFormat.INDEX_POINTER)][ref_sentiment
							.toNumber(SentimentFormat.INDEX_POINTER)].increment();
					} catch ( Exception e ) {
						continue;
					}
				}
			}
		}
		
		StringBuilder report = new StringBuilder();
		
		report.append('\t');
		for (int refr = 0 ; refr < Ratings.N_RATINGS ; refr++) {
			report.append(refr + 1).append('\t');
		}
		report.append('\n');
		for (int testr = 0 ; testr < Ratings.N_RATINGS ; testr++) {
			report.append(testr + 1).append('\t');
			for (int refr = 0 ; refr < Ratings.N_RATINGS ; refr++) {
				report.append(dcounts[testr][refr].get()).append('\t');
			}
			report.append('\n');
		}
		
		System.out.println(report.toString());
	}
	

}
