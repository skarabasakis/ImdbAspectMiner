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
package topics;

import filters.LemmatizationFilter.Lemmatizer;
import indexing.SynsetTermsAggregator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import topics.Topic.KeywordPrinter;
import util.AppLogger;
import util.State;
import wordnet.Synset;
import config.Paths;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class TopicSetFile {
	
	/**
	 * 
	 */
	// private static final long serialVersionUID = -5005440524487992523L;
	private TopicSet<Synset>					topicset		= null;
	private static final KeywordPrinter<Synset>	synsetPrinter	= getSynsetPrinter();

	/**
	 * @return the synsetprinter
	 */
	public static KeywordPrinter<Synset> getSynsetPrinter()
	{
		try {
			return new KeywordPrinter<Synset>() {
				
				private SynsetTermsAggregator	synsets	= new State<SynsetTermsAggregator>("synsets", null)
															.restoreState();
				
				@Override
				public String print(Synset synset)
				{
					return synset + "\t" + synsets.getSynsetTermsString(synset, false) + "\n";
				}
				
			};
		} catch ( IOException e ) {
			AppLogger.error.log(Level.WARNING,
								"Failed to properly initialize custom keyword printer for synset-based topics. "
									+ "Serialized synsets object could not be read from disk.");
			return null;
		}
	}
	
	public static KeywordPrinter<Synset> getTopTermPrinter()
	{
		try {
			return new KeywordPrinter<Synset>() {
				
				private SynsetTermsAggregator	synsets		= new State<SynsetTermsAggregator>("synsets", null)
																.restoreState();
				private Lemmatizer				lemmatizer	= new Lemmatizer();

				@Override
				public String print(Synset synset)
				{
					String topterm = synsets.getSynsetTopTerm(synset);
					return topterm.replace(" ", ".") //
						+ (topterm.indexOf(' ') == -1 && lemmatizer.lookupWord(topterm).length > 1
						? "(" + Synset.getSynsetCategoryString(synset.getPos()) + ")"
							: "") + " ";
				}
				
			};
		} catch ( IOException e ) {
			AppLogger.error.log(Level.WARNING,
								"Failed to properly initialize custom keyword printer for synset-based topics. "
									+ "Serialized synsets object could not be read from disk.");
			return null;
		}
	}
	public TopicSetFile(File input_file) throws FileNotFoundException
	{
		this(input_file, Integer.MAX_VALUE);
	}

	public TopicSetFile(File input_file, int cutoff) throws FileNotFoundException
	{
		// Initializing TopicSet
		topicset = new TopicSet<Synset>(input_file.getParentFile().getName());
		
		// Populating topicset with topic data read from text file
		BufferedReader reader = new BufferedReader(new FileReader(input_file));
		try {
			String topic_line;
			while ( (topic_line = reader.readLine()) != null ) {
				String[] topic_synsets_array = topic_line.split("\t")[2].split(" ");
				SingleTopic<Synset> topic = new SingleTopic<Synset>();
				cutoff = Math.min(topic_synsets_array.length, cutoff);
				int weight = cutoff;
				for (int i = 0 ; i < cutoff ; i++) {
					try {
						topic.addKeyword(new Synset(topic_synsets_array[i]), weight);
						weight--;
					} catch ( IllegalArgumentException e ) {
						// AppLogger.error.log(Level.WARNING, e.getMessage());
					}
				}
				
				topic.setKeywordPrinter(synsetPrinter);
				
				topicset.addTopic(topic);
			}

		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Input error occured while reading file " + input_file + "\n"
				+ e.getMessage());
		}
	}
	
	public TopicSet<Synset> getTopicSet()
	{
		return topicset;
	}

	private static String getFoldFilePath(String fold_category, String fold_id)
	{
		return Paths.tokenListPath + "/" + fold_category + "/" + fold_id + "/output-topic-keys.txt";
	}

	public static TopicSet<Synset> readAndMerge(String fold_category, int cutoff)
	{

		try {

			TopicSet<Synset> topics = new TopicSetFile(new File(getFoldFilePath(fold_category, "fold1")), cutoff)
				.getTopicSet() //
				.mergeWith(new TopicSetFile(new File(getFoldFilePath(fold_category, "fold2")), cutoff).getTopicSet(),
							true) //
				.mergeWith(new TopicSetFile(new File(getFoldFilePath(fold_category, "fold3")), cutoff).getTopicSet(),
							true) //
				.mergeWith(new TopicSetFile(new File(getFoldFilePath(fold_category, "fold4")), cutoff).getTopicSet(),
							true) //
				.mergeWith(new TopicSetFile(new File(getFoldFilePath(fold_category, "fold5")), cutoff).getTopicSet(),
							true);
			
			FileWriter topics_file = new FileWriter(new File(Paths.tokenListPath + "/" + fold_category
				+ "/merged_topics.txt"));
			topics_file.write(topics.toString());
			topics_file.close();
			
			return topics;
		} catch ( FileNotFoundException e ) {
			AppLogger.error.log(Level.SEVERE, e.getMessage());
			return null;
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot write merged topic file");
			return null;
		}


	}
	
	public static void makeKeywordLexicon(String fold_category)
	{
		try {
			TopicSet<Synset> topics = new State<TopicSet<Synset>>("topicset_nolabels", null).restoreState();
			
			FileWriter topics_keyword_list = new FileWriter(new File(Paths.tokenListPath + "/" + fold_category
				+ "/keyword_list.txt"));
			topics_keyword_list.write(topics.toString());
			topics_keyword_list.close();

		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot read topicset");
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		// TopicSet<Synset> topics = readAndMerge("_training_filtered_nv", 25);
		// State<TopicSet<Synset>> topicset_state = new State<TopicSet<Synset>>("topicset_nolabels",
		// topics);
		// topicset_state.saveState();
		//
		// topics.printClusterSizeReport(5);
		//
		// topics.disableUnnamed(true);
		// System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		// topics.assignNames(1);
		//
		// topicset_state = new State<TopicSet<Synset>>("topicset_labels_v1", topics);
		// topicset_state.saveState();
		// topics.indexAll();
		// topicset_state.setObj(topics);
		// topicset_state.saveState();
		//
		// System.out.println("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
		
		State<TopicSet<Synset>> topicset_state = new State<TopicSet<Synset>>("topicset_labels_v1", null);
		TopicSet<Synset> topics = topicset_state.restoreState();
		// topics.disableUnnamed(true);
		// topics.indexAll();
		// topicset_state.setObj(topics);
		// topicset_state.saveState();
		topics.setKeywordPrinter(getTopTermPrinter());
		System.out.println(topics.getTopicsString(true));
		
		System.out.println("\n------------------------\n");
		
		// topics.printClusterSizeReport(5);
		// System.out.println("\n------------------------\n");
		// topics.printAspectReport(0, TopicSetFile.getSynsetPrinter());
		// System.out.println("\n------------------------\n");


		FastLookupTopicLexicon<Synset> topiclexicon = topics.exportLexicon();
		topiclexicon.toString(getSynsetPrinter());
		State<FastLookupTopicLexicon<Synset>> topiclexicon_state = new State<FastLookupTopicLexicon<Synset>>(
				"topiclexicon", topiclexicon);
		topiclexicon_state.saveState();
	}
	

}
