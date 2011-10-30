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

import indexing.Token;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import lexicon.GeneralInquirerLexicon;
import lexicon.MPQALexicon;
import lexicon.PayloadFilters;
import lexicon.Ratings;
import lexicon.SentimentLexicon;
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;
import lexicon.TermSentiment.Sentiment.SentimentFormat;
import lexicon.classifiers.WidestWindowSentimentClassifier;
import topics.FastLookupTopicLexicon;
import topics.TopicLexicon;
import util.AppLogger;
import util.DatabaseConnection;
import wordnet.Synset;
import classes.Counter;
import classes.DatabaseReview;
import classes.Review;
import classes.ReviewCollection;
import config.Globals;
import config.Paths;
import dependencies.ReviewDependencyAnalyzer;
import dependencies.rules.DependencyRule;
import dependencies.rules.PatternCollectorRuleSet;
import dependencies.rules.SimpleEnglishRules;
import dependencies.rules.StatsRuleSet;
import dependencies.visualisation.DependencyTreeParseViz;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class ReviewClassifierShell {
	
	private ReviewDependencyAnalyzer	analyzer		= null;
	private SentimentLexicon			mainLexicon		= null;
	private SentimentLexicon			auxLexicon		= null;
	private TopicLexicon<Synset>		topicLexicon	= null;
	
	private PatternCollectorRuleSet		patternset		= null;
	private StatsRuleSet				ruleset			= null;
	
	/**
	 * Constructor for class ReviewClassifierShell
	 */
	public ReviewClassifierShell()
	{
		analyzer = new ReviewDependencyAnalyzer();
		// mainLexicon = new SentimentLexicon(false);
		mainLexicon = SentimentLexicon.loadLexicon(new WidestWindowSentimentClassifier(), PayloadFilters.FILTER_PLAIN);
		// auxLexicon = new SentimentLexicon(false);
		auxLexicon = SentimentLexicon.merge(MPQALexicon.loadMPQA(), GeneralInquirerLexicon.loadGeneralInquirer());
		// topicLexicon = new FastLookupTopicLexicon<Synset>(new HashMap<Synset, String>());
		topicLexicon = FastLookupTopicLexicon.loadLexicon();
		ruleset = new StatsRuleSet(SimpleEnglishRules.defaultRules, SimpleEnglishRules.clausalRelations);
	}
	
	/**
	 * Constructor for class ReviewClassifierShell
	 */
	public ReviewClassifierShell(SentimentLexicon mainLexicon, SentimentLexicon auxLexicon)
	{
		analyzer = new ReviewDependencyAnalyzer();
		this.mainLexicon = mainLexicon;
		this.auxLexicon = auxLexicon;
		topicLexicon = FastLookupTopicLexicon.loadLexicon();
		ruleset = new StatsRuleSet(SimpleEnglishRules.defaultRules, SimpleEnglishRules.clausalRelations);
	}
	
	/**
	 * 
	 */
	private Map<String, Sentiment> classify(DependencyTreeParseViz sentenceTree)
	{
		// sentenceTree.initSentiments(mainLexicon);
		// sentenceTree.initTopics(topicLexicon);
		return sentenceTree.parse(ruleset, mainLexicon, auxLexicon, topicLexicon, false);
	}

	private Map<String, Sentiment> classifySentence(ArrayList<Token> sentence)
	{
		try {
			return classify(new DependencyTreeParseViz(sentence));
		} catch ( IllegalArgumentException e ) {
			return new HashMap<String, Sentiment>();
		}
	}
	
	private Map<String, ArrayList<Sentiment>> classifySentences(ArrayList<ArrayList<Token>> sentences)
	{
		HashMap<String, ArrayList<Sentiment>> sentiments = new HashMap<String, ArrayList<Sentiment>>();
		for (ArrayList<Token> sentence : sentences) {
			try {
				Set<Entry<String, Sentiment>> sentence_topics = classify(new DependencyTreeParseViz(sentence))
					.entrySet();
				
				for (Entry<String, Sentiment> sentence_topic : sentence_topics) {
					if (!sentence_topic.getValue().isNeutral()) {
						if (!sentiments.containsKey(sentence_topic.getKey())) {
							sentiments.put(sentence_topic.getKey(), new ArrayList<Sentiment>());
						}
						sentiments.get(sentence_topic.getKey()).add(sentence_topic.getValue());
					}
				}
			} catch ( IllegalArgumentException e ) {
				continue;
			}
		}
		
		return sentiments;
	}

	private Map<String, ArrayList<Sentiment>> classifyReview(Review review)
	{
		return classifySentences(analyzer.getSentences(review));
	}
	
	private Map<String, ArrayList<Sentiment>> classifyCorpus(ReviewCollection corpus)
	{
		LinkedHashMap<String, ArrayList<Sentiment>> corpus_sentiments = new LinkedHashMap<String, ArrayList<Sentiment>>();
		Iterator<Review> review_iterator = corpus.getIterator();
		while ( review_iterator.hasNext() ) {
			Review review = review_iterator.next();
			Map<String, Sentiment> review_sentiment = aggregateSentiments(classifyReview(review));
			Set<Entry<String, Sentiment>> review_sentiment_entries = review_sentiment.entrySet();
			for (Entry<String, Sentiment> review_sentiment_entry : review_sentiment_entries) {
				if (!review_sentiment_entry.getValue().isNeutral()) {
					if (!corpus_sentiments.containsKey(review_sentiment_entry.getKey())) {
						corpus_sentiments.put(review_sentiment_entry.getKey(), new ArrayList<Sentiment>());
					}
					
					corpus_sentiments.get(review_sentiment_entry.getKey()).add(review_sentiment_entry.getValue());
				}
			}
		}
		
		return corpus_sentiments;
	}
	
	private Map<String, Sentiment> aggregateSentiments(Map<String, ArrayList<Sentiment>> sentiments)
	{
		LinkedHashMap<String, Sentiment> aggregate_map = new LinkedHashMap<String, Sentiment>();
		Set<Entry<String, ArrayList<Sentiment>>> topics = sentiments.entrySet();
		for (Entry<String, ArrayList<Sentiment>> topic : topics) {
			aggregate_map.put(topic.getKey(), Sentiment.average(topic.getValue()));
		}
		return aggregate_map;
	}
	
	private void printResult(Map<String, Sentiment> topic_map)
	{
		Sentiment global_sentiment = topic_map.get(Globals.TopicParameters.globalTopicName);
		if (global_sentiment != null) {
			System.out.println(global_sentiment.toString(SentimentFormat.TEXT, true));
			System.out.println();
		}
		topic_map.remove(Globals.TopicParameters.globalTopicName);
		for (Entry<String, Sentiment> topicentry : topic_map.entrySet()) {
			System.out.println(topicentry.getKey() + "\t" + topicentry.getValue().toString(SentimentFormat.TEXT, true));
		}
		System.out.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
	}
	
	private void printResults(Map<String, ArrayList<Sentiment>> topic_map)
	{
		printPolarities(topic_map.get(Globals.TopicParameters.globalTopicName));
		System.out.println("\n\n");
		topic_map.remove(Globals.TopicParameters.globalTopicName);
		printTopicSummary(topic_map);
		System.out.println("\n\n");
		printRuleStats(ruleset.getCounts());
		System.out.println("\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
	}
	
	private static void printPolarities(ArrayList<Sentiment> sentiments)
	{
		for (Polarity polarity : Polarity.values()) {
			System.out.println(polarity + ":\t" + Sentiment.filterPolarity(sentiments, polarity).size() + " / "
				+ sentiments.size());
		}
	}
	
	private static void printTopicSummary(Map<String, ArrayList<Sentiment>> topic_map)
	{
		for (Entry<String, ArrayList<Sentiment>> topicentry : topic_map.entrySet()) {
			System.out.println(topicentry.getKey() + "\t" + Sentiment.printSummary(topicentry.getValue()));
		}
	}
	
	private static void printRuleStats(HashMap<DependencyRule, Integer> rulecounts)
	{
		for (Entry<DependencyRule, Integer> ruleentry : rulecounts.entrySet()) {
			System.out.println(ruleentry.getValue() + "\t" + ruleentry.getKey());
		}
	}
	
	private static void printCounterMatrix(Counter[][] matrix)
	{
		// Print header row
		for (int header = 0 ; header <= matrix[0].length ; header++) {
			System.out.print("\t" + header);
		}
		System.out.println();
		
		// Print rows
		int value;
		for (int row = 0 ; row <= matrix.length ; row++) {
			System.out.print(row);
			for (int column = 0 ; column <= matrix[row].length ; column++) {
				System.out.println("\t" + ((value = matrix[row][column].get()) != 0 ? value : ""));
			}
			System.out.println();
		}
		
		System.out.println();
	}
	
	private void processShellCommand(String input)
	{
		if (input.isEmpty()) {
			System.exit(0);
		}
		else if (input.startsWith("~")) {
			String[] args = input.substring(1).trim().split(" ", 2);
			if (args[0].equals("file")) {
				File inputfile = new File(args[0]);
				if (!inputfile.isAbsolute()) {
					inputfile = new File(Paths.BIN_ROOT + "/" + args[1]);
				}
				
				try {
					BufferedReader r = new BufferedReader(new FileReader(inputfile));
					printResults(classifySentences(analyzer.getSentences(r)));
				} catch ( FileNotFoundException e ) {
					AppLogger.error.log(Level.SEVERE, "File not found or cannot be opened");
				}
			}
			else if (args[0].equals("review")) {
				String[] arg_array = args[1].split("\\D"); // Split on non-digit characters
				ArrayList<Integer> reviewids = new ArrayList<Integer>();
				for (String arg : arg_array) {
					Integer reviewid = Integer.parseInt(arg);
					if (reviewid > 0 && reviewid <= Globals.Corpus.TotalReviews) {
						if (!reviewids.contains(reviewid)) {
							reviewids.add(reviewid);
						}
					}
				}

				if (!reviewids.isEmpty()) {
					Counter[][] originalVsInferred = new Counter[Ratings.N_RATINGS + 1][Ratings.N_RATINGS + 1];
					for (Counter[] row : originalVsInferred) {
						for (Counter counter : row) {
							counter = new Counter();
						}
					}

					try {
						DatabaseConnection dc = new DatabaseConnection();

						for (Integer reviewid : reviewids) {
							Review review = new DatabaseReview(reviewid, dc);
							Map<String, ArrayList<Sentiment>> review_results = classifyReview(review);
							Map<String, Sentiment> aggregate_results = aggregateSentiments(review_results);
							
							originalVsInferred[review.getRating()][aggregate_results
								.get(Globals.TopicParameters.globalTopicName).toNumber(SentimentFormat.RATING_WZERO)]
								.increment();
							
							if (reviewids.size() <= 3) {
								System.out.println("Author's rating:\t" + review.getRating());
								System.out.println("AspectMiner rating:\t"
									+ aggregateSentiments(review_results).get(Globals.TopicParameters.globalTopicName)
										.toString(SentimentFormat.RATING_WZERO, true));
								
								System.out.println(".................................");
								System.out.println(review.getTitle().toUpperCase() + "\n");
								System.out.println(review.getReviewText());
								System.out.println(".................................\n\n");
								
								printResults(review_results);
							}
						}
						
						if (reviewids.size() > 3) {
							System.out.print("INF->");
							printCounterMatrix(originalVsInferred);
						}

					} catch ( SQLException e ) {
						AppLogger.error.log(Level.SEVERE, "Error connecting to database: " + e.getMessage());
					}
				}
			}
			else if (args[0].equals("movie")) {
				System.err.println("Not Implemented");
			}
		}
		else {
			ArrayList<ArrayList<Token>> sentences = analyzer.getSentences(input);
			switch (sentences.size()) {
				case 0:
					AppLogger.error.log(Level.WARNING, "Input is not a sentence");
					break;
				case 1:
					printResult(classifySentence(sentences.get(0)));
					break;
				default:
					printResults(classifySentences(sentences));
					break;
			}
		}
	}

	public static void main(String[] args) throws IOException
	{
		System.out.println("Initializing classifier...");
		ReviewClassifierShell reviewClassifier = new ReviewClassifierShell();
		
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
		String input;
		System.out.print("\n\n>>> ");
		while ( (input = r.readLine()) != null ) {
			reviewClassifier.processShellCommand(input);
			System.out.print(">>> ");
		}
	}
}
