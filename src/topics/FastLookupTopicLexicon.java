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
package topics;

import indexing.PosTag;
import indexing.SynsetTermsAggregator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import net.didion.jwnl.data.POS;
import topics.Topic.KeywordPrinter;
import util.AppLogger;
import util.State;
import wordnet.Synset;
import filters.LemmatizationFilter.Lemmatizer;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class FastLookupTopicLexicon<Keyword> implements TopicLexicon<Keyword>, Serializable {

	private static final long			serialVersionUID	= 1738374292017537313L;

	private TreeMap<Keyword, String>	lexicon;
	
	/**
	 * Constructor for class FastLookupTopicLexicon
	 */
	public FastLookupTopicLexicon(Map<Keyword, String> entries)
	{
		lexicon = new TreeMap<Keyword, String>();
		lexicon.putAll(entries);
	}


	public boolean isTopic(Keyword keyword)
	{
		return lexicon.containsKey(keyword);
	}

	@Override
	public int lookupTopicId(Keyword keyword)
	{
		return lexicon.get(keyword) == null ? 0 : lexicon.get(keyword).hashCode();
	}
	
	/*
	 * (non-Javadoc)
	 * @see topics.TopicLexicon#lookupTopicName(java.lang.Object)
	 */
	@Override
	public String lookupTopicName(Keyword keyword)
	{
		return lexicon.get(keyword);
	}
	
	/*
	 * (non-Javadoc)
	 * @see topics.TopicLexicon#renameTopic(java.lang.String, java.lang.String)
	 */
	@Override
	public void renameTopic(String oldname, String newname)
	{
		Set<Entry<Keyword, String>> entries = lexicon.entrySet();
		for (Entry<Keyword, String> entry : entries) {
			if (entry.getValue().equals(oldname)) {
				entry.setValue(newname);
			}
		}
	}
	
	public void assign(Keyword keyword, String topic)
	{
		if (topic.equals("-")) {
			lexicon.remove(keyword);
		}
		else {
			lexicon.put(keyword, topic);
		}
	}
	
	public void remove(Keyword keyword)
	{
		lexicon.remove(keyword);
	}
	
	public String toString(KeywordPrinter<Keyword> printer)
	{
		StringBuilder str = new StringBuilder();
		
		Set<Entry<Keyword, String>> lex_entries = lexicon.entrySet();
		for (Entry<Keyword, String> lex_entry : lex_entries) {
			str.append(lex_entry.getValue() + "\t" + printer.print(lex_entry.getKey()) + "\n");
		}
		
		return str.toString();
	}
	
	public static FastLookupTopicLexicon<Synset> loadLexicon()
	{
		FastLookupTopicLexicon<Synset> topiclexicon = null;
		State<FastLookupTopicLexicon<Synset>> topiclexicon_state = new State<FastLookupTopicLexicon<Synset>>(
				"topiclexicon", null);
		try {
			topiclexicon = topiclexicon_state.restoreState();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Unable to load topic lexicon");
			System.exit(-1);
		}
		
		return topiclexicon;
	}

	public static void main(String[] args) throws IOException
	{
		FastLookupTopicLexicon<Synset> topiclexicon = new State<FastLookupTopicLexicon<Synset>>("topiclexicon", null)
			.restoreState();
		Set<String> topics = new HashSet<String>(topiclexicon.lexicon.values());
		topics.add("-");
		Lemmatizer lemmatizer = new Lemmatizer();
		KeywordPrinter<Synset> printer = TopicSetFile.getSynsetPrinter();
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = null;
		
		System.out.print("\nLookup: ");
		while ( !(line = in.readLine()).isEmpty() ) {
			args = line.split(" ");
			Synset synset = null;
			switch (args.length) {
				case 1:
					if (Synset.matchesPattern(args[0])) {
						synset = new Synset(args[0]);
						if (topiclexicon.isTopic(synset)) {
							System.out.println(topiclexicon.lookupTopicName(synset) + "\t" + printer.print(synset));
						}
					}
					else {
						Synset[] synsets = lemmatizer.lookupWord(args[0]);
						for (Synset synsett : synsets) {
							if (topiclexicon.isTopic(synsett)) {
								System.out.println(topiclexicon.lookupTopicName(synsett) + "\t"
									+ printer.print(synsett));
							}
						}
					}
					break;
				case 2:
					if (Synset.matchesPattern(args[0])) {
						synset = new Synset(args[0]);
					}
					else {
						synset = lemmatizer.lookupWord(args[0], POS.NOUN);
						if (synset == null) {
							synset = lemmatizer.lookupWord(args[0], POS.VERB);
						}
					}
					
					if (synset != null) {
						if (topics.contains(args[1])) {
							topiclexicon.assign(synset, args[1]);
							System.out.println(topiclexicon.lookupTopicName(synset) + "\t" + printer.print(synset));
						}
						else {
							AppLogger.error.log(Level.WARNING, args[1] + " not in topic list");
						}
					}
					else {
						AppLogger.error.log(Level.WARNING, args[0] + " not found in lexicon");
					}
					break;
				case 3:
					synset = lemmatizer.lookupWord(args[0], PosTag.toPOS(args[1].toUpperCase()));
					if (topics.contains(args[2])) {
						topiclexicon.assign(synset, args[2]);
						System.out.println(topiclexicon.lookupTopicName(synset) + "\t" + printer.print(synset));
					}
					else {
						AppLogger.error.log(Level.WARNING, args[2] + " not in topic list");
					}
					break;
				default:
					AppLogger.error.log(Level.WARNING, "Command not recognized");
			}
			
			System.out.print("\nLookup: ");
		}
		
		topiclexicon.toString(new KeywordPrinter<Synset>() {

			private SynsetTermsAggregator	synsets	= new State<SynsetTermsAggregator>("synsets", null).restoreState();

			@Override
			public String print(Synset synset)
			{
				return synsets.getSynsetTerms(synset) + "(" + synset.getPos().toString().toLowerCase() + ") \n";
			}
		});
		State<FastLookupTopicLexicon<Synset>> topiclexicon_state = new State<FastLookupTopicLexicon<Synset>>(
				"topiclexicon_fixed", topiclexicon);
		topiclexicon_state.saveState();

	}
}
