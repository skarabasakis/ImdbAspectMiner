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
package lexicon.eval;

import indexing.SynsetTermsAggregator;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Level;
import lexicon.GeneralInquirerLexicon;
import lexicon.MPQALexicon;
import lexicon.PayloadFilters;
import lexicon.SentiWordnet;
import lexicon.SentimentLexicon;
import lexicon.SentimentLexiconComparer;
import lexicon.SentimentLexiconFile;
import lexicon.TermSentiment;
import lexicon.TermSentimentFilter;
import lexicon.PayloadFilters.PayloadFilter;
import lexicon.TermSentiment.Sentiment;
import lexicon.classifiers.HybridSentimentClassifier;
import lexicon.classifiers.PNSentimentClassifier;
import lexicon.classifiers.PeakSentimentClassifier;
import lexicon.classifiers.SentimentClassifier;
import lexicon.classifiers.WidestWindowSentimentClassifier;
import util.AppLogger;
import util.State;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class LexiconEval {
	
	/**
	 * @param args
	 * @throws IOException
	 */
	private void produceLexiconFiles() throws IOException
	{
		SentimentClassifier[] classifiers = { new WidestWindowSentimentClassifier() };
		for (SentimentClassifier classifier : classifiers) {
			SentimentLexicon lexicon = SentimentLexicon.loadLexicon(classifier, PayloadFilters.FILTER_PLAIN);
			SentimentLexiconFile lexicon_file = new SentimentLexiconFile(SentimentLexicon
				.name(classifier, PayloadFilters.FILTER_PLAIN), lexicon.isSynsetsOnly(),
					new State<SynsetTermsAggregator>("synsets", new SynsetTermsAggregator()).restoreState());
			lexicon_file.populate(lexicon);
		}
	}
	
	public static void produceAllLexiconFiles() throws IOException
	{
		SynsetTermsAggregator aggregator = new State<SynsetTermsAggregator>("synsets", new SynsetTermsAggregator())
			.restoreState();
		PayloadFilter[] filters = { PayloadFilters.FILTER_PLAIN,
			PayloadFilters.FILTER_NEGATED,
			PayloadFilters.FILTER_COMPARATIVE,
			PayloadFilters.FILTER_SUPERLATIVE };
		SentimentClassifier[] classifiers = {
		// new PeakSentimentClassifier(), //
		// new PNSentimentClassifier(), //
		// new WidestWindowSentimentClassifier(), //
		new HybridSentimentClassifier() //
		};
		
		for (SentimentClassifier classifier : classifiers) {
			for (PayloadFilter filter : filters) {
				SentimentLexicon lex = SentimentLexicon.loadLexicon(classifier, filter);
				SentimentLexiconFile lexfile = new SentimentLexiconFile(SentimentLexicon.name(classifier, filter), lex
					.isSynsetsOnly(), aggregator);
				lexfile.populate(lex);
			}
		}
	}
	

	// public static void main1(String[] args) throws IOException
	// {
	// SentimentClassifier cl = new WidestWindowSentimentClassifier();
	// RatingHistogram h = new RatingHistogram(new State<ReviewStats>("stats", new
	// ReviewStats()).restoreState());
	//
	// int documents = 3;
	// for (int d = 0 ; d < documents ; d++) {
	// h.incrementUniqueDocuments();
	// }
	//
	// int[] freqs = { 139, 101, 142, 131, 187, 297, 320, 297, 176, 210 };
	// for (int rank = 1 ; rank <= Ratings.N_RATINGS ; rank++) {
	// h.addObeservation(rank, freqs[rank - 1]);
	// }
	//
	// System.out.println(cl.classify(h));
	// }
	
	public static void evalPlainLexicons(boolean discrepancies) throws IOException
	{
		SentimentClassifier[] classifiers = { new PeakSentimentClassifier(),
			new PNSentimentClassifier(),
			new WidestWindowSentimentClassifier() };
		for (SentimentClassifier classifier : classifiers) {
			SentimentLexicon lex = discrepancies ? //
			SentimentLexicon.loadDiscrepancies(classifier, PayloadFilters.FILTER_PLAIN)
				: SentimentLexicon.loadLexicon(classifier, PayloadFilters.FILTER_PLAIN);
			lex.printSummary();
			System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
			// System.in.read();
		}
		
		SentimentLexicon lex = SentiWordnet.loadSentiwordnet();
		lex.printSummary();
	}
	
	@SuppressWarnings("unchecked")
	public static void generateDiscrepancyLexicons(boolean polarity)
	{
		SentimentClassifier[] classifiers = { //
		new PeakSentimentClassifier(), //
			new PNSentimentClassifier(), //
			new WidestWindowSentimentClassifier(), //
		};
		
		SentimentLexicon[] lexicons = { //
		SentimentLexicon.loadLexicon(classifiers[0], PayloadFilters.FILTER_PLAIN),
			SentimentLexicon.loadLexicon(classifiers[1], PayloadFilters.FILTER_PLAIN),
			SentimentLexicon.loadLexicon(classifiers[2], PayloadFilters.FILTER_PLAIN) };

		boolean pol = polarity;
		SentimentLexicon[] discrepancies = { //
		lexicons[0].filterSentimentDiscrepancies(lexicons[1], pol).filterSentimentDiscrepancies(lexicons[2], pol),
			lexicons[1].filterSentimentDiscrepancies(lexicons[2], pol).filterSentimentDiscrepancies(lexicons[0], pol),
			lexicons[2].filterSentimentDiscrepancies(lexicons[0], pol).filterSentimentDiscrepancies(lexicons[1], pol) };
		
		State[] lexicon_states = { //
		new State<SentimentLexicon>("discrepancies_"
			+ SentimentLexicon.name(classifiers[0], PayloadFilters.FILTER_PLAIN), discrepancies[0]),
			new State<SentimentLexicon>("discrepancies_"
				+ SentimentLexicon.name(classifiers[1], PayloadFilters.FILTER_PLAIN), discrepancies[1]),
			new State<SentimentLexicon>("discrepancies_"
				+ SentimentLexicon.name(classifiers[2], PayloadFilters.FILTER_PLAIN), discrepancies[2]) };

		for (State<SentimentLexicon> lexicon_state : lexicon_states) {
			lexicon_state.saveState();
		}
	}

	public static SentimentLexicon loadDiscrepancies(boolean polarity)
	{
		SentimentLexicon discrepancies = null;
		State<SentimentLexicon> discrepancies_state = new State<SentimentLexicon>("discrepancies", discrepancies);

		try {
			discrepancies = discrepancies_state.restoreState();
		} catch ( IOException e ) {
			SentimentLexicon[] lexicons = { //
			SentimentLexicon.loadLexicon(new PeakSentimentClassifier(), PayloadFilters.FILTER_PLAIN),
				SentimentLexicon.loadLexicon(new PNSentimentClassifier(), PayloadFilters.FILTER_PLAIN),
				SentimentLexicon.loadLexicon(new WidestWindowSentimentClassifier(), PayloadFilters.FILTER_PLAIN) };
			
			discrepancies = lexicons[0] //
				.filterSentimentDiscrepancies(lexicons[1], polarity) //
				.filterSentimentDiscrepancies(lexicons[2], polarity);
			discrepancies_state.setObj(discrepancies);
			discrepancies_state.saveState();
			
			SentimentLexiconFile discrepancies_file = new SentimentLexiconFile("discrepancies_plain", false,
					SynsetTermsAggregator.load());
			try {
				discrepancies_file.populate(discrepancies);
			} catch ( IOException e1 ) {
				AppLogger.error.log(Level.WARNING, "Cannot write discrepancies lexicon file");
			}
		}
		
		return discrepancies;
	}

	public static void comparePlainLexiconsToSentiwordnet(boolean discrepancies) throws IOException
	{
		SentimentLexicon sentiwordnet = SentiWordnet.loadSentiwordnet();
		SentimentClassifier[] classifiers = { new PeakSentimentClassifier(), //
			new PNSentimentClassifier(), //
			new WidestWindowSentimentClassifier(),
			new HybridSentimentClassifier(), //
			new HybridSentimentClassifier() };
		
		for (SentimentClassifier classifier : classifiers) {
			SentimentLexicon lexicon = discrepancies ? //
			SentimentLexicon.loadDiscrepancies(classifier, PayloadFilters.FILTER_PLAIN)
				: SentimentLexicon.loadLexicon(classifier, PayloadFilters.FILTER_PLAIN);
			for (float score = 0.0f ; score < 1.0f ; score += 0.1f) {
				new SentimentLexiconComparer(lexicon.filter(new TermSentimentFilter().setScore(score)), sentiwordnet)
					.printComparisonReport(100);
			}
			System.out.println("\n- - - - - - - - -\n");

		}
	}
	
	public static void comparePlainLexiconsToMPQA(boolean discrepancies) throws IOException
	{
		SentimentLexicon mpqa = MPQALexicon.loadMPQA();
		SentimentClassifier[] classifiers = { new PeakSentimentClassifier(), //
			new PNSentimentClassifier(), //
			new WidestWindowSentimentClassifier(), //
			new HybridSentimentClassifier() //
		};
		
		for (SentimentClassifier classifier : classifiers) {
			SentimentLexicon lexicon = discrepancies ? //
			SentimentLexicon.loadDiscrepancies(classifier, PayloadFilters.FILTER_PLAIN)
				: SentimentLexicon.loadLexicon(classifier, PayloadFilters.FILTER_PLAIN);
			new SentimentLexiconComparer(lexicon, mpqa).printComparisonReport();
			System.out.println("\n- - - - - - - - -\n");
		}
	}
	
	public static void comparePlainLexiconsToGI(boolean discrepancies) throws IOException
	{
		SentimentLexicon gi = GeneralInquirerLexicon.loadGeneralInquirer();
		SentimentClassifier[] classifiers = { new PeakSentimentClassifier(), //
			new PNSentimentClassifier(), //
			new WidestWindowSentimentClassifier(), //
			new HybridSentimentClassifier() //
		};
		
		for (SentimentClassifier classifier : classifiers) {
			SentimentLexicon lexicon = discrepancies ? //
			SentimentLexicon.loadDiscrepancies(classifier, PayloadFilters.FILTER_PLAIN)
				: SentimentLexicon.loadLexicon(classifier, PayloadFilters.FILTER_PLAIN);
			new SentimentLexiconComparer(lexicon, gi).printComparisonReport();
			System.out.println("\n- - - - - - - - -\n");
		}
	}
	
	public static void comparePlainLexiconsToCombo(boolean discrepancies) throws IOException
	{
		SentimentLexicon combo = getComboLexicon();
		SentimentClassifier[] classifiers = { // new PeakSentimentClassifier(), //
		// new PNSentimentClassifier(), //
		new WidestWindowSentimentClassifier() //
		// new HybridSentimentClassifier() //
		};
		
		for (SentimentClassifier classifier : classifiers) {
			SentimentLexicon lexicon = discrepancies ? //
			SentimentLexicon.loadDiscrepancies(classifier, PayloadFilters.FILTER_PLAIN)
				: SentimentLexicon.loadLexicon(classifier, PayloadFilters.FILTER_PLAIN);
			new SentimentLexiconComparer(lexicon, combo).printComparisonReport();
			System.out.println("\n- - - - - - - - -\n");
		}
	}

	public static void compareSentiwordnetToMPQA() throws IOException
	{
		SentimentLexicon mpqa = MPQALexicon.loadMPQA();
		SentimentLexicon sentiwordnet = SentiWordnet.loadSentiwordnet();
		
		new SentimentLexiconComparer(sentiwordnet, mpqa).printComparisonReport();
		System.out.println("\n- - - - - - - - -\n");
	}
	
	public static void compareSentiwordnetToGI() throws IOException
	{
		SentimentLexicon gi = GeneralInquirerLexicon.loadGeneralInquirer();
		SentimentLexicon sentiwordnet = SentiWordnet.loadSentiwordnet();
		
		new SentimentLexiconComparer(sentiwordnet, gi).printComparisonReport();
		System.out.println("\n- - - - - - - - -\n");
	}
	
	public static void compareSentiwordnetToCombo() throws IOException
	{
		SentimentLexicon combo = getComboLexicon();
		SentimentLexicon sentiwordnet = SentiWordnet.loadSentiwordnet();
		
		new SentimentLexiconComparer(sentiwordnet, combo).printComparisonReport(5);
		System.out.println("\n- - - - - - - - -\n");
	}

	public static void compareMPQAToGI() throws IOException
	{
		SentimentLexicon gi = GeneralInquirerLexicon.loadGeneralInquirer();
		SentimentLexicon mpqa = MPQALexicon.loadMPQA();
		
		new SentimentLexiconComparer(mpqa, gi).printComparisonReport();
		System.out.println("\n- - - - - - - - -\n");
	}
	
	public static SentimentLexicon getComboLexicon() throws IOException
	{
		SentimentLexicon gi = GeneralInquirerLexicon.loadGeneralInquirer();
		SentimentLexicon mpqa = MPQALexicon.loadMPQA();
		for (SynsetCategory synsetcat : Synset.getSynsetCategories()) {
			Iterator<Entry<Synset, TermSentiment>> entries = gi.getLemmaEntries(synsetcat);
			while ( entries.hasNext() ) {
				Entry<Synset, TermSentiment> gi_entry = entries.next();
				Sentiment gi_sent = gi_entry.getValue().getSentiment();
				
				try {
					Sentiment mpqa_sent = mpqa.getEntry(gi_entry.getKey()).getSentiment();
					if (!gi_sent.isNeutral() && mpqa_sent.getPolarity() == gi_sent.getPolarity()) {
						mpqa.insertEntry(gi_entry.getKey(), gi_entry.getValue());
					}
					else {
						mpqa.insertEntry(gi_entry.getKey(), TermSentiment.NEUTRAL_SENTIMENT);
					}
				} catch ( NullPointerException e ) {
					mpqa.insertEntry(gi_entry.getKey(), gi_entry.getValue());
				}
			}
		}
		
		return mpqa;
	}

	public static void comparePlainLexiconsToBaseline() throws IOException
	{
		SentimentLexicon peak = SentimentLexicon //
			.loadLexicon(new PeakSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon pn = SentimentLexicon //
			.loadLexicon(new PNSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon ww = SentimentLexicon //
			.loadLexicon(new WidestWindowSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		// SentimentLexicon[] lexicons = { peak, pn, ww };

		new SentimentLexiconComparer(pn, peak).printComparisonReport();
		System.out.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
		new SentimentLexiconComparer(ww, peak).printComparisonReport();
		System.out.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
		new SentimentLexiconComparer(ww, pn).printComparisonReport();
		System.out.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
	}
	
	public static void comparePlainLexiconsPolarities() throws IOException
	{
		SentimentLexicon peak = SentimentLexicon //
			.loadLexicon(new PeakSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon pn = SentimentLexicon //
			.loadLexicon(new PNSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon ww = SentimentLexicon //
			.loadLexicon(new WidestWindowSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		
		SentimentLexicon[] lexicons12 = { peak, pn };
		SentimentLexiconComparer.printCommonPolaritiesReport(lexicons12);
		System.out.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
		SentimentLexicon[] lexicons13 = { peak, ww };
		SentimentLexiconComparer.printCommonPolaritiesReport(lexicons13);
		System.out.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
		SentimentLexicon[] lexicons23 = { pn, ww };
		SentimentLexiconComparer.printCommonPolaritiesReport(lexicons23);
		System.out.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
		SentimentLexicon[] lexicons = { peak, pn, ww };
		SentimentLexiconComparer.printCommonPolaritiesReport(lexicons);
	}
	
	public static void compareIntensitiesOfPlainLexiconToGI(boolean discrepancies) throws IOException
	{
		SentimentLexicon gi = GeneralInquirerLexicon.loadGeneralInquirer();
		SentimentClassifier[] classifiers = { /*
											 * new PeakSentimentClassifier(), new
											 * PNSentimentClassifier(),
											 */
		new WidestWindowSentimentClassifier() };
		
		for (SentimentClassifier classifier : classifiers) {
			SentimentLexicon lexicon = discrepancies ? //
			SentimentLexicon.loadDiscrepancies(classifier, PayloadFilters.FILTER_PLAIN)
				: SentimentLexicon.loadLexicon(classifier, PayloadFilters.FILTER_PLAIN);
			SentimentLexiconComparer comp = new SentimentLexiconComparer(gi, lexicon);
			
			comp.printDiscrepanciesChart();
			for (SynsetCategory synsetcat : Synset.getSynsetCategories()) {
				System.out.println("\n- - - - - - - - -\n\n" + synsetcat + "\n\n");
				SynsetCategory[] synsetcats = { synsetcat };
				comp.printDiscrepanciesChart(synsetcats);
			}
			System.out.println("\n- - - - - - - - -\n");
		}
	}

	public static void comparePlainLexiconErrorsToSentiwordnetErrors(SentimentLexicon standard) throws IOException
	{
		SentimentLexicon ww = SentimentLexicon //
			.loadLexicon(new WidestWindowSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon swn = SentiWordnet.loadSentiwordnet();

		new SentimentLexiconComparer( //
				ww.filterSentimentDiscrepancies(standard, true),//
				swn //
		).printComparisonReport(300);
		System.out.println("\n- - - - - - - - -\n");
	}
	
	public static void comparePlainCombinationToMPQA() throws IOException
	{
		SentimentLexicon pn = SentimentLexicon //
			.loadLexicon(new PNSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon ww = SentimentLexicon //
			.loadLexicon(new WidestWindowSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon mpqa = MPQALexicon.loadMPQA();
		
		SentimentLexicon wwpn = ww.unifyWith(pn, true);
		new SentimentLexiconComparer(wwpn, mpqa).printComparisonReport();
		System.out.println("\n- - - - - - - - -\n");
	}
	
	public static void comparePlainCombinationToGI() throws IOException
	{
		SentimentLexicon pn = SentimentLexicon //
			.loadLexicon(new PNSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon ww = SentimentLexicon //
			.loadLexicon(new WidestWindowSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon gi = GeneralInquirerLexicon.loadGeneralInquirer();

		SentimentLexicon wwpn = ww.unifyWith(pn, true);
		new SentimentLexiconComparer(wwpn, gi).printComparisonReport();
		System.out.println("\n- - - - - - - - -\n");
	}
	
	public static SentimentLexicon getHybridLexicon()
	{
		SentimentLexicon pn = SentimentLexicon //
			.loadLexicon(new PNSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		SentimentLexicon ww = SentimentLexicon //
			.loadLexicon(new WidestWindowSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		
		return ww.unifyWith(pn, true);
	}
	
	public static void writeHybridLexicon() throws IOException
	{
		SentimentLexicon hybrid = getHybridLexicon();
		SentimentLexiconFile hybrid_file = new SentimentLexiconFile("hybrid_plain", false, //
				new State<SynsetTermsAggregator>("synsets", new SynsetTermsAggregator()).restoreState());
		hybrid_file.populate(hybrid);
		
		hybrid.printSummary();
	}

	public static void main(String[] args)
	{
		try {
			// generateDiscrepancyLexicons(false);
			// loadDiscrepancies(true);
			// produceAllLexiconFiles();
			
			// comparePlainLexiconsPolarities();
			// evalPlainLexicons(false);
			// comparePlainLexiconsToSentiwordnet(false);
			// comparePlainLexiconsToMPQA(false);
			// comparePlainLexiconsToGI(false);
			// comparePlainLexiconsToBaseline();
			comparePlainLexiconsToCombo(false);
			// compareSentiwordnetToMPQA();
			// compareSentiwordnetToGI();
			// compareSentiwordnetToCombo();
			// compareMPQAToGI();
			
			// comparePlainCombinationToMPQA();
			// comparePlainCombinationToGI();
			
			// compareIntensitiesOfPlainLexiconToGI(false);
			// comparePlainLexiconErrorsToSentiwordnetErrors(MPQALexicon.loadMPQA());
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
