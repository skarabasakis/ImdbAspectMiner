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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import lexicon.TermSentiment.Intensity;
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;
import util.AppLogger;
import util.State;
import wordnet.Synset;
import classes.Counter;
import config.Paths;
import filters.LemmatizationFilter.Lemmatizer;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class GeneralInquirerLexicon extends SentimentLexicon {
	
	/**
	 * 
	 */
	private static final long				serialVersionUID	= 3316809160871555259L;
	
	private HashMap<String, TermSentiment>	polarityMapping		= getPolarityMappingInstance();

	private static HashMap<String, TermSentiment> getPolarityMappingInstance()
	{
		HashMap<String, TermSentiment> polarityMapping = new HashMap<String, TermSentiment>();
		polarityMapping.put("p", new TermSentiment(new Sentiment(1.0f, Polarity.POSITIVE, Intensity.NORMAL)));
		polarityMapping.put("p+", new TermSentiment(new Sentiment(1.0f, Polarity.POSITIVE, Intensity.STRONG)));
		polarityMapping.put("p-", new TermSentiment(new Sentiment(1.0f, Polarity.NEGATIVE, Intensity.WEAK)));
		polarityMapping.put("n", new TermSentiment(new Sentiment(1.0f, Polarity.NEGATIVE, Intensity.NORMAL)));
		polarityMapping.put("n+", new TermSentiment(new Sentiment(1.0f, Polarity.NEGATIVE, Intensity.STRONG)));
		polarityMapping.put("n-", new TermSentiment(new Sentiment(1.0f, Polarity.NEGATIVE, Intensity.WEAK)));
		
		return polarityMapping;
		
	}
	

	/**
	 * Constructor for class SentiWordnet
	 */
	public GeneralInquirerLexicon()
	{
		super(true);
		
		int entries = 0;
		try {
			entries = readFromFile(new BufferedReader(new FileReader(new File(Paths.GeneralInquirerFile))));
		} catch ( FileNotFoundException e ) {
			AppLogger.error.log(Level.SEVERE, "GeneralInquirer input file not found at location "
				+ Paths.GeneralInquirerFile);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to access GeneralInquirer input file.");
		}
		
		AppLogger.error.log(Level.INFO, "Loaded " + entries + " of 3259 entries from GeneralInquirer file.");
	}
	
	private Integer readFromFile(BufferedReader gi_file) throws IOException
	{
		Lemmatizer lemmatizer = new Lemmatizer();
		Counter entries_counter = new Counter();
		
		String gi_line = null;
		while ( (gi_line = gi_file.readLine()) != null ) {
			// Ignoring line comments
			if (gi_line.charAt(0) != '#') {
				String[] gi_line_elements = gi_line.split(" ");
				if (gi_line_elements.length == 2) {
					// Processing synset of current entry
					String word = gi_line_elements[0];
					TermSentiment sentiment = polarityMapping.get(gi_line_elements[1]);

					// Inserting synset-classification pair into lexicon
					Synset[] synsets = lemmatizer.lookupWord(word);
					for (Synset synset : synsets) {
						if (synset != null) {
							this.insertEntry(synset, sentiment);
							entries_counter.increment();
						}
					}
				}
				else {
					AppLogger.error.log(Level.WARNING, "GeneralInquirer line format error: " + gi_line_elements);
				}
			}
		}
		
		return entries_counter.get();
	}
	
	public static GeneralInquirerLexicon loadGeneralInquirer()
	{
		GeneralInquirerLexicon gi = null;
		State<GeneralInquirerLexicon> lexicon_state = new State<GeneralInquirerLexicon>("lexicon_gi", gi);
		try {
			gi = lexicon_state.restoreState();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to load sentiwordnet");
			System.exit(-1);
		}
		return gi;
	}
	
	public static void main(String[] args)
	{
		GeneralInquirerLexicon gi = new GeneralInquirerLexicon();
		State<GeneralInquirerLexicon> lexicon_state = new State<GeneralInquirerLexicon>("lexicon_gi", gi);
		lexicon_state.saveState();
	}
}
