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
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;
import net.didion.jwnl.data.POS;
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
public class MPQALexicon extends SentimentLexicon {
	
	/**
	 * 
	 */
	private static final long		serialVersionUID	= 3316809160871555258L;
	
	private HashMap<String, POS>	posMapping			= getPosMappingInstance();
	
	private static HashMap<String, POS> getPosMappingInstance()
	{
		HashMap<String, POS> posMapping = new HashMap<String, POS>();
		posMapping.put("verb", POS.VERB);
		posMapping.put("noun", POS.NOUN);
		posMapping.put("adj", POS.ADJECTIVE);
		posMapping.put("adverb", POS.ADVERB);
		posMapping.put("anypos", null);
		
		return posMapping;
	}
	
	private HashMap<String, TermSentiment>	polarityMapping	= getPolarityMappingInstance();

	private static HashMap<String, TermSentiment> getPolarityMappingInstance()
	{
		HashMap<String, TermSentiment> polarityMapping = new HashMap<String, TermSentiment>();
		polarityMapping.put("positive", new TermSentiment(new Sentiment(1.0f, Polarity.POSITIVE)));
		polarityMapping.put("negative", new TermSentiment(new Sentiment(1.0f, Polarity.NEGATIVE)));
		polarityMapping.put("both", new TermSentiment(new Sentiment(0.5f, Polarity.NEGATIVE), //
				new Sentiment(0.5f, Polarity.POSITIVE)));
		polarityMapping.put("neutral", TermSentiment.NEUTRAL_SENTIMENT);
		
		return polarityMapping;
		
	}
	

	/**
	 * Constructor for class SentiWordnet
	 */
	public MPQALexicon()
	{
		super(true);
		
		int entries = 0;
		try {
			entries = readFromFile(new BufferedReader(new FileReader(new File(Paths.MPQAFile))));
		} catch ( FileNotFoundException e ) {
			AppLogger.error.log(Level.SEVERE, "MPQA input file not found at location " + Paths.MPQAFile);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to access MPQA input file.");
		}
		
		AppLogger.error.log(Level.INFO, "Loaded " + entries + " of 7638 entries from MPQA file.");
	}
	
	public MPQALexicon(String MPQALexiconPath)
	{
		super(true);
		
	}
	
	private Integer readFromFile(BufferedReader mpqa_file) throws IOException
	{
		Lemmatizer lemmatizer = new Lemmatizer();
		Counter entries_counter = new Counter();
		
		String mpqa_line = null;
		while ( (mpqa_line = mpqa_file.readLine()) != null ) {
			// Ignoring line comments
			if (mpqa_line.charAt(0) != '#') {
				String[] mpqa_line_elements = mpqa_line.split("\t");
				if (mpqa_line_elements.length == 3) {
					// Processing synset of current entry
					String word = mpqa_line_elements[0];
					POS pos = posMapping.get(mpqa_line_elements[1]);
					TermSentiment sentiment = polarityMapping.get(mpqa_line_elements[2]);

					// Inserting synset-classification pair into lexicon
					if (pos != null) {
						Synset synset = lemmatizer.lookupWord(word, pos);
						if (synset != null) {
							if (sentiment == null) {
								System.out.println(word);
							}
							this.insertEntry(synset, sentiment);
							entries_counter.increment();
						}
					}
					else {
						Synset[] synsets = lemmatizer.lookupWord(word);
						for (Synset synset : synsets) {
							if (synset != null) {
								if (sentiment == null) {
									System.out.println(word);
								}
								this.insertEntry(synset, sentiment);
								entries_counter.increment();
							}
						}
					}
				}
				else {
					AppLogger.error.log(Level.WARNING, "Sentiwordnet line format error: " + mpqa_line_elements);
				}
			}
		}
		
		return entries_counter.get();
	}
	
	public static MPQALexicon loadMPQA()
	{
		MPQALexicon mpqa = null;
		State<MPQALexicon> lexicon_state = new State<MPQALexicon>("lexicon_mpqa", mpqa);
		try {
			mpqa = lexicon_state.restoreState();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to load sentiwordnet");
			System.exit(-1);
		}
		return mpqa;
	}
	
	public static void main(String[] args)
	{
		MPQALexicon mpqa = new MPQALexicon();
		State<MPQALexicon> lexicon_state = new State<MPQALexicon>("lexicon_mpqa", mpqa);
		lexicon_state.saveState();
	}
}
