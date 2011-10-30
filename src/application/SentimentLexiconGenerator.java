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
import indexing.SynsetTermsAggregator;
import indexing.TermTypeFilter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import lexicon.PayloadFilters;
import lexicon.RatingHistogram;
import lexicon.RatingHistogramCollector;
import lexicon.SentimentLexicon;
import lexicon.SentimentLexiconFile;
import lexicon.TermSentiment;
import lexicon.PayloadFilters.PayloadFilter;
import lexicon.classifiers.HybridSentimentClassifier;
import lexicon.classifiers.PNSentimentClassifier;
import lexicon.classifiers.PeakSentimentClassifier;
import lexicon.classifiers.SentimentClassifier;
import lexicon.classifiers.WidestWindowSentimentClassifier;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.search.spans.TermSpans;
import org.apache.lucene.store.SimpleFSDirectory;
import util.AppLogger;
import util.State;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import classes.ReviewStats;
import config.Globals;
import config.Paths;


/**
 * Generates an opinion lexicon for every indexed term in the movie review dataset
 * 
 * @author Stelios Karabasakis
 */
public class SentimentLexiconGenerator {
	
	private IndexReader				reader					= null;
	private IndexSearcher			searcher				= null;
	private ReviewStats				reviewStats				= null;
	private SynsetTermsAggregator	indexedSynsets			= null;


	private FileWriter				discardedSynsetsFile	= null;
	private FileWriter				discardedTermsFile		= null;
	private FileWriter				discardedOtherFile		= null;
	
	private PayloadFilter			filter					= null;

	/**
	 * Constructor for class SentimentLexiconGenerator
	 */
	public SentimentLexiconGenerator()
	{
		// Load term index for reading
		try {
			reader = IndexReader.open(new SimpleFSDirectory(new File(Paths.luceneIndex)), true);
			searcher = new IndexSearcher(reader);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Error reading index");
		}
		
		// Load index metadata
		try {
			reviewStats = new State<ReviewStats>("stats", null).restoreState();
			indexedSynsets = new State<SynsetTermsAggregator>("synsets", null).restoreState();
		} catch ( IOException e1 ) {
			AppLogger.error.log(Level.SEVERE, "Error reading index metadata\n" + e1.getMessage());
		}

		// Prepare files that collected discarded terms from the index
		try {
			discardedSynsetsFile = new FileWriter(Paths.lexiconDiscardedPath + "synsets.txt", false);
			discardedTermsFile = new FileWriter(Paths.lexiconDiscardedPath + "terms.txt", false);
			discardedOtherFile = new FileWriter(Paths.lexiconDiscardedPath + "other.txt", false);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Error initializing discarded terms files");
		}
		
		// Initialize filter to default state;
		filter = PayloadFilters.FILTER_NONE;


	}
	
	public void unsetFilter()
	{
		filter = PayloadFilters.FILTER_NONE;
	}
	
	public void setFilter(PayloadFilter filter)
	{
		this.filter = filter;
	}
	
	public PayloadFilter getFilter()
	{
		return filter;
	}

	public TermSpans getIndexTermInstances(String term) throws IOException
	{
		SpanTermQuery stq_text = new SpanTermQuery(new Term("text", term));
		return (TermSpans)stq_text.getSpans(reader);
	}

	// private boolean applyDuplicateTermFilter(ReviewId currentId, ArrayList<ReviewId> previousIds)
	// {
	// return previousIds.contains(currentId);
	// }

	// private Integer getTermInstanceHistogramId(ReviewTermPayload termPayload)
	// {
	// return termPayload.isNegation() ? 0 : 1;
	// }
	
	public RatingHistogram makeRatingHistogramForSynset(Synset synset, ReviewStats stats) throws IOException
	{
		RatingHistogram histogram = new RatingHistogram(stats);
		TermSpans ts = getIndexTermInstances(synset.toString());
		
		while ( ts.next() ) {
			histogram.incrementUniqueDocuments();
			
			Document doc = searcher.doc(ts.doc());
			int doc_rating = Integer.parseInt(doc.getField(Globals.IndexFieldNames.rating).stringValue());
			
			Iterator<byte[]> p_i = ts.getPayload().iterator();
			while ( p_i.hasNext() ) {
				ReviewTermPayload p = new ReviewTermPayload();
				p.decode(p_i.next());
				if (filter.filterPayload(p)) {
					histogram.addObeservation(doc_rating, 1);
				}
			}
		}

		return histogram;
	}
	
	public HashMap<SynsetCategory, RatingHistogram> makeRatingHistogramsForTerm(String term, ReviewStats stats)
			throws IOException
	{
		HashMap<SynsetCategory, RatingHistogram> histograms = new HashMap<SynsetCategory, RatingHistogram>();
		for (SynsetCategory synsetcat : Synset.getSynsetCategories()) {
			histograms.put(synsetcat, new RatingHistogram(stats));
		}

		TermSpans ts = getIndexTermInstances(term);
		
		while ( ts.next() ) {
			Document doc = searcher.doc(ts.doc());
			int doc_rating = Integer.parseInt(doc.getField(Globals.IndexFieldNames.rating).stringValue());
			
			Iterator<byte[]> p_i = ts.getPayload().iterator();
			while ( p_i.hasNext() ) {
				ReviewTermPayload p = new ReviewTermPayload();
				p.decode(p_i.next());
				if (filter.filterPayload(p)) {
					RatingHistogram histogram = histograms.get(Synset.convertPosCategory(p.getPosCat()));
					if (histogram != null) {
						histogram.incrementUniqueDocuments();
						histogram.addObeservation(doc_rating, 1);
					}
					else {
						// TODO Remove message??
						AppLogger.error.log(Level.SEVERE, "Histogram not found");
					}
				}
			}
		}
		
		return histograms;
	}


	public RatingHistogramCollector makeAllHistograms()
	{
		// Initialize histogram collector
		RatingHistogramCollector collector = new RatingHistogramCollector();


		// For each term in index
		try {
			TermEnum terms = reader.terms();
			while ( terms.next() ) {
				
				// Extract term text. This will usually be a 9-byte synset identifier (e.g.
				// n00348562), but it could also be an English word or phrase that does not have a
				// synset equivalent
				String term = terms.term().text();
				
				// For each extracted term, generate a histogram for each of the specified filters
				// If current term has the form of a synset identifier
				if (TermTypeFilter.isLemma(term)) {

					// Make histograms for all filter-matching occurrences of current synset
					Synset synset = new Synset(term);
					RatingHistogram term_histogram = makeRatingHistogramForSynset(synset, reviewStats);
					collector.insertHistogram(synset, term_histogram);
				}
				else if (TermTypeFilter.isNonLemma(term)) {
					HashMap<SynsetCategory, RatingHistogram> histograms = makeRatingHistogramsForTerm(term, reviewStats);
					Set<SynsetCategory> synsetcats = histograms.keySet();
					for (SynsetCategory synsetcat : synsetcats) {
						if (histograms.get(synsetcat).getTotalObservations() > 0) {
							collector.insertHistogram(synsetcat, term, histograms.get(synsetcat));
						}
					}
				}
				else {
					discardedOtherFile.write(term + "\n");
				}
			}
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE,
								"Histogram generation was interrupted because of the following I/O error: \n"
									+ e.getMessage());
			return null;
		}
		
		return collector;
	}
	
	
	public SentimentLexicon generateLexicon(String name, RatingHistogramCollector histograms,
			SentimentClassifier classifier)
	{
		SentimentLexiconFile lex = new SentimentLexiconFile(name, false, indexedSynsets);

		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		for (SynsetCategory synsetcat : synsetcats) {
			
			System.out.println("Processing category: " + synsetcat);
			
			Iterator<Entry<Synset, RatingHistogram>> lemma_histograms_i = histograms.getLemmaHistograms(synsetcat);
			while ( lemma_histograms_i.hasNext() ) {
				Entry<Synset, RatingHistogram> histogram_entry = lemma_histograms_i.next();
				
				if (SentimentClassifier.isClassifiable(histogram_entry.getValue())) {
					TermSentiment sentiment = classifier.classify(histogram_entry.getValue());
					if (sentiment.getSentimentRanks() > 0) {
						lex.insertEntry(histogram_entry.getKey(), classifier.classify(histogram_entry.getValue()));
					}
					else {
						System.err.println("Empty classification for "
							+ indexedSynsets.getSynsetTermsString(histogram_entry.getKey(), true));
					}
				}
				else {
					try {
						discardedSynsetsFile.write(histogram_entry.getKey() + "\n");
					} catch ( IOException e ) {
						AppLogger.error.log(Level.INFO, "Synset " + histogram_entry.getKey()
							+ " was discarded by classifier");
					}
				}
			}
			
			Iterator<Entry<String, RatingHistogram>> nonlemma_histograms_i = histograms
				.getNonLemmaHistograms(synsetcat);
			while ( nonlemma_histograms_i.hasNext() ) {
				Entry<String, RatingHistogram> histogram_entry = nonlemma_histograms_i.next();
				
				if (SentimentClassifier.isClassifiable(histogram_entry.getValue())) {
					lex.insertEntry(synsetcat, histogram_entry.getKey(), classifier
						.classify(histogram_entry.getValue()));
				}
				else {
					try {
						discardedTermsFile.write(histogram_entry.getKey() + "\n");
					} catch ( IOException e ) {
						AppLogger.error.log(Level.INFO, "Term \"" + histogram_entry.getKey()
							+ "\" was discarded by classifier");
					}
				}
			}
		}

		return lex.getObj();
	}

	public static void main(String[] args) throws IOException
	{
		// Initialize sentiment lexicon generator
		System.out.println("Initializing...");
		SentimentLexiconGenerator lexGen = new SentimentLexiconGenerator();
		
		// Specify a filter for the term occurrences to be filtered against while producing the
		// histograms. For example, when enabling FILTER_PLAIN, words that do not occur in a plain
		// context (i.e. are negated or in comparative/superlative form) will be excluded from the
		// produced histogram
		PayloadFilter[] filters = { PayloadFilters.FILTER_PLAIN,
			PayloadFilters.FILTER_NEGATED,
			PayloadFilters.FILTER_COMPARATIVE,
			PayloadFilters.FILTER_SUPERLATIVE };

		for (PayloadFilter current_filter : filters) {
			lexGen.setFilter(current_filter);
			

			RatingHistogramCollector histograms = null;
			State<RatingHistogramCollector> histograms_state;
			
			// Generate and store histograms
			// System.out.println("Generating frequency histograms for indexed terms matching filter: "
			// + current_filter.name());
			// histograms = lexGen.makeAllHistograms();
			//
			// System.out.println("\nHistogram collection completed successfully.\n\n" + histograms
			// + "\n");
			// System.out.println("Saving generated histograms...");
			// histograms_state = new State<RatingHistogramCollector>("histograms_" +
			// current_filter.name(), histograms);
			// histograms_state.saveState();
			

			// Load histogram state
			System.out.println("Loading generated histograms...");
			histograms_state = new State<RatingHistogramCollector>("histograms_" + current_filter.name(), histograms);
			histograms = histograms_state.restoreState();


			// Initialize sentiment classifiers
			System.out.println("Initializing sentiment classifiers...");
			SentimentClassifier[] classifiers = { new PeakSentimentClassifier(), //
				new PNSentimentClassifier(), //
				new WidestWindowSentimentClassifier(), //
				new HybridSentimentClassifier() //
			};
			
			// Generating sentiment lexicon
			System.out.println("*** Sentiment lexicon files generation has started: ");
			
			String current_lexicon_name;
			for (SentimentClassifier classifier : classifiers) {
				System.out.println("Generating lexicon using classifier " + classifier.name());
				
				current_lexicon_name = SentimentLexicon.name(classifier, current_filter);
				SentimentLexicon lexicon = lexGen.generateLexicon(current_lexicon_name, histograms, classifier);
				State<SentimentLexicon> lex_state = new State<SentimentLexicon>(current_lexicon_name, lexicon);
				lex_state.saveState();
				
				System.out.println("\n" + lexicon + "\n");
			}
			
			System.out.println("DONE! All lexicons generated");
		}
	}
}