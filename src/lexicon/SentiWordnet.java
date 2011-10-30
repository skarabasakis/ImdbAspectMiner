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

import indexing.PosTag;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import lexicon.TermSentiment.Polarity;
import lexicon.TermSentiment.Sentiment;
import util.AppLogger;
import util.State;
import wordnet.Synset;
import classes.Counter;
import config.Paths;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class SentiWordnet extends SentimentLexicon {
	
	
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 3316809160871555257L;
	
	/**
	 * Constructor for class SentiWordnet
	 */
	public SentiWordnet()
	{
		super(true);
		
		int entries = 0;
		try {
			entries = readFromFile(new BufferedReader(new FileReader(new File(Paths.sentiWordnetFile))));
		} catch ( FileNotFoundException e ) {
			AppLogger.error
				.log(Level.SEVERE, "Sentiwordnet input file not found at location " + Paths.sentiWordnetFile);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to access sentiwordnet input file.");
		}
		
		AppLogger.error.log(Level.INFO, "Loaded " + entries + " of 117659 entries from sentiwordnet file.");
	}
	
	public SentiWordnet(String sentiWordnetPath)
	{
		super(true);

	}
	
	private Integer readFromFile(BufferedReader swn_file) throws IOException
	{
		Counter entries_counter = new Counter();

		String swn_line = null;
		while ( (swn_line = swn_file.readLine()) != null ) {
			// Ignoring line comments
			if (swn_line.charAt(0) != '#') {
				String[] swn_line_elements = swn_line.split("\t");
				if (swn_line_elements.length == 6) {
					// Processing synset of current entry
					Synset swn_line_synset = new Synset(PosTag.toCategory(swn_line_elements[0].toUpperCase()), Long
						.parseLong(swn_line_elements[1]));
					
					// Processing sentiment classification of current entry
					Float pos_score = Float.parseFloat(swn_line_elements[2]);
					Float neg_score = Float.parseFloat(swn_line_elements[3]);
					if (pos_score != 0 || neg_score != 0) {
						TermSentiment swn_line_sentiment = new TermSentiment();
						swn_line_sentiment.setSubjectivityScore(1 - pos_score - neg_score);
						if (pos_score > 0) {
							swn_line_sentiment.addSentiment(new Sentiment(pos_score, Polarity.POSITIVE));
						}
						if (neg_score > 0) {
							swn_line_sentiment.addSentiment(new Sentiment(neg_score, Polarity.NEGATIVE));
						}
						
						// Inserting synset-classification pair into lexicon
						this.insertEntry(swn_line_synset, swn_line_sentiment);
						entries_counter.increment();
					}
				}
				else {
					AppLogger.error.log(Level.WARNING, "Sentiwordnet line format error: " + swn_line_elements);
				}
			}
		}
		
		return entries_counter.get();
	}
	
	public static SentiWordnet loadSentiwordnet()
	{
		SentiWordnet sentiwordnet = null;
		State<SentiWordnet> lexicon_state = new State<SentiWordnet>("lexicon_sentiwordnet", sentiwordnet);
		try {
			sentiwordnet = lexicon_state.restoreState();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to load sentiwordnet");
			System.exit(-1);
		}
		return sentiwordnet;
	}

	public static void main(String[] args)
	{
		SentiWordnet sentiwordnet = new SentiWordnet();
		State<SentiWordnet> sentiwordnet_state = new State<SentiWordnet>("lexicon_sentiwordnet", sentiwordnet);
		sentiwordnet_state.saveState();
	}
}
