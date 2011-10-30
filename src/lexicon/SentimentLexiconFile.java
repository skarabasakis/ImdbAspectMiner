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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Level;
import util.AppLogger;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import config.Paths;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class SentimentLexiconFile extends SentimentLexicon implements Cloneable {
	
	private static final long				serialVersionUID		= -7339933298564037348L;
	
	private String							lemmaLexiconFilePath	= null;
	private String							nonLemmaLexiconFilePath	= null;

	private transient SynsetTermsAggregator	aggregator				= null;
	private transient BufferedWriter		lemmaLexiconWriter		= null;
	private transient BufferedWriter		nonLemmaLexiconWriter	= null;
	
	/**
	 * Constructor for class SentimentLexiconFile
	 * 
	 * @param name
	 * @param synsetsOnly
	 */
	public SentimentLexiconFile(String name, boolean synsetsOnly, SynsetTermsAggregator aggregator)
	{
		super(synsetsOnly);
		this.aggregator = aggregator;
		lemmaLexiconFilePath = Paths.lexiconPath + name + "_lemmas.txt";
		nonLemmaLexiconFilePath = Paths.lexiconPath + name + "_nonlemmas.txt";
	
		try {
			// Opening lemma lexicon and writing headers
			lemmaLexiconWriter = new BufferedWriter(new FileWriter(lemmaLexiconFilePath, false));
			lemmaLexiconWriter.write(getLemmaHeader());
			lemmaLexiconWriter.newLine();
			
			if (!synsetsOnly) {
				nonLemmaLexiconWriter = new BufferedWriter(new FileWriter(nonLemmaLexiconFilePath, false));
				nonLemmaLexiconWriter.write(getNonLemmaHeader());
				nonLemmaLexiconWriter.newLine();
			}
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot access or write to sentiment lexicon file(s)");
		}
	}
	
	public void populate(SentimentLexicon lexicon) throws IOException
	{
		SynsetCategory[] synsetcats = Synset.getSynsetCategories();
		for (SynsetCategory synsetcat : synsetcats) {
			Iterator<Entry<Synset, TermSentiment>> lemma_entries = lexicon.getLemmaEntries(synsetcat);
			while ( lemma_entries.hasNext() ) {
				Entry<Synset, TermSentiment> lemma_entry = lemma_entries.next();
				this.insertEntry(lemma_entry.getKey(), lemma_entry.getValue());
			}
			
			if (!synsetsOnly && !lexicon.synsetsOnly) {
				Iterator<Entry<String, TermSentiment>> nonlemma_entries = lexicon.getNonLemmaEntries(synsetcat);
				while ( nonlemma_entries.hasNext() ) {
					Entry<String, TermSentiment> nonlemma_entry = nonlemma_entries.next();
					this.insertEntry(synsetcat, nonlemma_entry.getKey(), nonlemma_entry.getValue());
				}
			}
		}
		
		lemmaLexiconWriter.flush();
		lemmaLexiconWriter.close();
		nonLemmaLexiconWriter.flush();
		nonLemmaLexiconWriter.close();
	}


	private String getLemmaHeader() throws IOException
	{
		return "# SYNSET \t" + "SENTIMENT_LIST" + "\t" + "SYNSET_TERMS_&_FREQUENCIES";
	}
	
	private String getNonLemmaHeader() throws IOException
	{
		return "# TERM   \t" + "SENTIMENT_LIST";
	}

	private String getEntryString(Synset synset, TermSentiment termSentiment)
	{
		StringBuilder file_entry = new StringBuilder();
		file_entry.append(synset.toString()) //
			.append("\t") //
			.append(termSentiment.toString()) //
			.append("\t") //
			.append(aggregator.getSynsetTermsString(synset, true));
		
		return file_entry.toString();
	}
	
	private String getEntryString(SynsetCategory synsetcat, String term, TermSentiment termSentiment)
	{
		StringBuilder file_entry = new StringBuilder();
		file_entry.append(Synset.getSynsetCategoryString(synsetcat) + " " + term) //
			.append("\t") //
			.append(termSentiment.toString());
		
		return file_entry.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see lexicon.SentimentLexicon#insertEntry(wordnet.Synset, lexicon.TermSentiment)
	 */
	@Override
	public void insertEntry(Synset synset, TermSentiment termSentiment)
	{
		super.insertEntry(synset, termSentiment);
		
		try {
			lemmaLexiconWriter.write(getEntryString(synset, termSentiment));
			lemmaLexiconWriter.newLine();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot write entry to sentiment lemma lexicon file");
		}
	}

	
	/*
	 * (non-Javadoc)
	 * @see lexicon.SentimentLexicon#insertEntry(wordnet.Synset.SynsetCategory, java.lang.String,
	 * lexicon.TermSentiment)
	 */
	@Override
	public void insertEntry(SynsetCategory synsetcat, String term, TermSentiment termSentiment)
	{
		if (!isSynsetsOnly()) {
			super.insertEntry(synsetcat, term, termSentiment);

			try {
				nonLemmaLexiconWriter.write(getEntryString(synsetcat, term, termSentiment));
				nonLemmaLexiconWriter.newLine();
			} catch ( IOException e ) {
				AppLogger.error.log(Level.SEVERE, "Cannot write entry to sentiment non-lemma lexicon file");
			}
		}
		else
			throw new RuntimeException("Attempt to insert unlemmatized term entry in a synsets-only lexicon");
	}
	
	private String lookup(String arg)
	{
		String entry = "";
		
		if (Synset.matchesPattern(arg)) {
			Synset synset = new Synset(arg);
			TermSentiment termSentiment = this.getEntry(synset);
			entry = (termSentiment == null ? "Not found" : getEntryString(synset, termSentiment)) + "\n";
		}
		else {
			for (SynsetCategory synsetcat : Synset.getSynsetCategories()) {
				String term = arg.trim();
				TermSentiment termSentiment = getEntry(synsetcat, term);
				entry += termSentiment == null ? "" : getEntryString(synsetcat, term, termSentiment) + "\n";
			}
			
			if (entry.isEmpty()) {
				entry += "Not found\n";
			}
		}
		

		return entry;
	}
	
	public SentimentLexicon getObj()
	{
		SentimentLexicon sl = new SentimentLexicon(synsetsOnly);
		sl.lemmaLexicon = lemmaLexicon;
		sl.nonLemmaLexicon = nonLemmaLexicon;
		
		return sl;
	}
	
	public static void main(String[] args) throws IOException
	{
		// TODO Load lexicon from disk
		SentimentLexiconFile slf = new SentimentLexiconFile("", false, new SynsetTermsAggregator());
		
		// Processing user input
		String[] lookup_strings = null;
		if (args.length == 0) {
			System.out.print("Enter one or more synsets to look-up (separated by comma)\nSynsets: ");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			lookup_strings = in.readLine().split(",");
		}
		else {
			lookup_strings = args;
		}
		
		for (String lookup_string : lookup_strings) {
			System.out.print(lookup_string + "\t-- " + slf.lookup(lookup_string));
		}
	}
}
