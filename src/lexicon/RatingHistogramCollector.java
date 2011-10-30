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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import lexicon.PayloadFilters.PayloadFilter;
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
public class RatingHistogramCollector implements Serializable {
	
	private static final long												serialVersionUID	= 1613013463379217050L;

	private HashMap<SynsetCategory, LinkedHashMap<Synset, RatingHistogram>>	lemmaHistograms		= null;
	private HashMap<SynsetCategory, LinkedHashMap<String, RatingHistogram>>	nonLemmaHistograms	= null;
	
	public RatingHistogramCollector()
	{
		lemmaHistograms = new HashMap<SynsetCategory, LinkedHashMap<Synset, RatingHistogram>>();
		nonLemmaHistograms = new HashMap<SynsetCategory, LinkedHashMap<String, RatingHistogram>>();
		for (SynsetCategory synsetcat : Synset.getSynsetCategories()) {
			lemmaHistograms.put(synsetcat, new LinkedHashMap<Synset, RatingHistogram>());
			nonLemmaHistograms.put(synsetcat, new LinkedHashMap<String, RatingHistogram>());
		}
	}
	
	public void insertHistogram(Synset synset, RatingHistogram histogram)
	{
		lemmaHistograms.get(synset.getPos()).put(synset, histogram);
	}
	
	public void insertHistogram(SynsetCategory synsetcat, String term, RatingHistogram histogram)
	{
		nonLemmaHistograms.get(synsetcat).put(term, histogram);
	}
	
	public RatingHistogram getHistogram(Synset synset)
	{
		try {
			return lemmaHistograms.get(synset.getPos()).get(synset);
		} catch ( NullPointerException e ) {
			return null;
		}
	}
	
	public RatingHistogram getHistogram(SynsetCategory synsetcat, String term)
	{
		try {
			return nonLemmaHistograms.get(synsetcat).get(term);
		} catch ( Exception e ) {
			return null;
		}
	}
	
	public Iterator<Entry<Synset, RatingHistogram>> getLemmaHistograms(SynsetCategory synsetcat)
	{
		return lemmaHistograms.get(synsetcat).entrySet().iterator();
	}
	
	public Iterator<Entry<String, RatingHistogram>> getNonLemmaHistograms(SynsetCategory synsetcat)
	{
		return nonLemmaHistograms.get(synsetcat).entrySet().iterator();
	}
	
	public String printSummary()
	{
		StringBuilder summary = new StringBuilder();
		SynsetCategory[] synsetcats = { SynsetCategory.N, SynsetCategory.V, SynsetCategory.J, SynsetCategory.R };
		Integer[] lemmaHistograms_size = new Integer[synsetcats.length];
		Integer[] nonLemmaHistograms_size = new Integer[synsetcats.length];
		Integer lemmaHistograms_size_total = 0;
		Integer nonLemmaHistograms_size_total = 0;
		
		// Print header line and retrieve lengths
		summary.append("                                 \tTOTAL");
		for (int cat = 0 ; cat < synsetcats.length ; cat++) {
			summary.append("\t" + synsetcats[cat]);
			
			try {
				lemmaHistograms_size[cat] = lemmaHistograms.get(synsetcats[cat]).size();
				lemmaHistograms_size_total += lemmaHistograms_size[cat];
			} catch ( NullPointerException e ) {
				lemmaHistograms_size[cat] = 0;
			}

			try {
				nonLemmaHistograms_size[cat] = nonLemmaHistograms.get(synsetcats[cat]).size();
				nonLemmaHistograms_size_total += nonLemmaHistograms_size[cat];
			} catch ( NullPointerException e ) {
				nonLemmaHistograms_size[cat] = 0;
			}
		}
		summary.append("\n");

		// Print Lemma summary line
		summary.append(" *     Lemmatized term histograms\t" + lemmaHistograms_size_total);
		for (int cat = 0 ; cat < synsetcats.length ; cat++) {
			summary.append("\t" + lemmaHistograms_size[cat]);
		}
		summary.append("\n");
		summary.append(" * Non-lemmatized term histograms\t" + nonLemmaHistograms_size_total);
		for (int cat = 0 ; cat < synsetcats.length ; cat++) {
			summary.append("\t" + nonLemmaHistograms_size[cat]);
		}
		summary.append("\n");
		
		return summary.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return printSummary();
	}

	public static void main(String[] args) throws IOException
	{
		// Load stats from indexing phase
		SynsetTermsAggregator aggregator = new State<SynsetTermsAggregator>("synsets", new SynsetTermsAggregator())
			.restoreState();
		ReviewStats stats = new State<ReviewStats>("stats", new ReviewStats()).restoreState();

		PayloadFilter[] filters = { PayloadFilters.FILTER_PLAIN,
			PayloadFilters.FILTER_NEGATED,
			PayloadFilters.FILTER_COMPARATIVE,
			PayloadFilters.FILTER_SUPERLATIVE };
		for (PayloadFilter filter : filters) {
			// Load Histograms
			RatingHistogramCollector histograms = new State<RatingHistogramCollector>("histograms_" + filter.name(),
					new RatingHistogramCollector()).restoreState();
			
			// Open output file
			FileWriter file = new FileWriter(Paths.WORKDIR_ROOT + "/histograms/histograms_" + filter.name()
				+ "_abs.csv", false);
			
			System.out.println("Writing file histograms_" + filter.name() + "_abs.csv");

			for (SynsetCategory synsetcat : Synset.getSynsetCategories()) {
				Iterator<Entry<Synset, RatingHistogram>> iter = histograms.getLemmaHistograms(synsetcat);
				while ( iter.hasNext() ) {
					Entry<Synset, RatingHistogram> entry = iter.next();
					Synset synset = entry.getKey();
					RatingHistogram histogram = entry.getValue();
					
					String file_entry = /* histogram.getRangeThreshold(stats) + "," + */synset + ","
						+ histogram.prettyPrintFreq() + aggregator.getSynsetTermsString(synset, true);
					file.write(file_entry + "\n");
				}
			}
			
			file.close();
		}

		System.out.println("DONE");
	}

}
