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
package indexing;

import indexing.TermTypeFilter.TermType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import util.AppLogger;
import util.State;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import classes.Counter;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class SynsetTermsAggregator implements Serializable {
	
	private static final long													serialVersionUID	= 2389213657783968906L;
	
	private HashMap<SynsetCategory, HashMap<Synset, HashMap<String, Counter>>>	synsetmap;
	private HashMap<SynsetCategory, HashMap<String, Counter>>					unlemmatizedMap;
	
	/**
	 * Constructor for class SynsetTermsAggregator
	 */
	public SynsetTermsAggregator()
	{
		synsetmap = new HashMap<Synset.SynsetCategory, HashMap<Synset, HashMap<String, Counter>>>();
		unlemmatizedMap = new HashMap<SynsetCategory, HashMap<String, Counter>>();
		for (SynsetCategory synsetcat : SynsetCategory.values()) {
			synsetmap.put(synsetcat, new HashMap<Synset, HashMap<String, Counter>>());
			unlemmatizedMap.put(synsetcat, new HashMap<String, Counter>());
		}
	}

	public Iterator<Synset> getSynsetIterator(SynsetCategory synsetcat)
	{
		return synsetmap.get(synsetcat).keySet().iterator();
	}
	
	public String getSynsetTopTerm(Synset synset)
	{
		Set<Entry<String, Counter>> terms = getSynsetTerms(synset).entrySet();
		Counter topfreq = new Counter();
		String topterm = " . . . . . . . . . ";
		for (Entry<String, Counter> term : terms) {
			if (term.getValue().get() > topfreq.get()) {
				if (term.getKey().split(" ").length <= topterm.split(" ").length) {
					topterm = term.getKey();
					topfreq = term.getValue();
				}
			}
		}
		
		return topterm;
	}

	public HashMap<String, Counter> getSynsetTerms(Synset synset)
	{
		HashMap<Synset, HashMap<String, Counter>> category_synsets = synsetmap.get(synset.getPos());
		if (category_synsets != null)
			return category_synsets.get(synset);
		else
			return new HashMap<String, Counter>();
	}
	
	public String getSynsetTermsString(Synset synset, boolean with_frequencies)
	{
		HashMap<String, Counter> synset_terms_map = getSynsetTerms(synset);
		if (synset_terms_map == null)
			return "?";

		Set<String> synset_terms = synset_terms_map.keySet();
		Iterator<String> synset_terms_iter = synset_terms.iterator();

		int synset_terms_size = synset_terms.size();
		if (synset_terms_size == 0)
			return "";
		else {
			ArrayList<Integer> term_freqs = new ArrayList<Integer>();
			ArrayList<String> terms = new ArrayList<String>();

			String current_term;
			Integer current_term_freq;
			
			while ( synset_terms_iter.hasNext() ) {
				current_term = synset_terms_iter.next();
				current_term_freq = synset_terms_map.get(current_term).get();
				
				int insert_pos = 0;
				for (insert_pos = 0 ; insert_pos < term_freqs.size() ; insert_pos++) {
					if (current_term_freq > term_freqs.get(insert_pos)) {
						break;
					}
				}
				term_freqs.add(insert_pos, current_term_freq);
				terms.add(insert_pos, current_term);
			}
			
			// Build the string to return
			return with_frequencies ? generateTermsString(terms, " ", term_freqs, "|")
				: generateTermsString(terms, " ");
		}
	}
	
	private static String generateTermsString(ArrayList<String> terms, String term_delim)
	{
		StringBuilder terms_string = new StringBuilder();
		
		try {
			Iterator<String> term_i = terms.iterator();
			terms_string.append(term_i.next().replace(' ', '_'));
			while ( term_i.hasNext() ) {
				terms_string.append(term_delim) //
					.append(term_i.next().replace(' ', '_'));
			}
		} catch ( NoSuchElementException e ) {
			return "";
		}
		
		return terms_string.toString();
	}


	private static String generateTermsString(ArrayList<String> terms, String term_delim,
			ArrayList<Integer> term_freqs, String freq_delim)
	{
		StringBuilder terms_string = new StringBuilder();
		
		// Calculating total term frequency
		int total_freq = 0;
		for (Integer cur_freq : term_freqs) {
			total_freq += cur_freq;
		}
		
		try {
			Iterator<String> term_i = terms.iterator();
			Iterator<Integer> term_freq_i = term_freqs.iterator();

			int abs_freq = term_freq_i.next(); // Absolute frequency of current term
			float rel_freq = abs_freq * 1000 / total_freq / (float)10;
			terms_string.append(term_i.next().replaceAll(" ", "_")) //
				.append(freq_delim) //
				.append(abs_freq) //
				.append("(") //
				.append(rel_freq) //
				.append("%)");

			while ( term_i.hasNext() && term_freq_i.hasNext() ) {
				abs_freq = term_freq_i.next(); // Absolute frequency of current term
				rel_freq = abs_freq * 1000 / total_freq / (float)10;

				terms_string.append(term_delim) //
					.append(term_i.next()) //
					.append(freq_delim) //
					.append(abs_freq) //
					.append("(") //
					.append(rel_freq) //
					.append("%)");
			}
		} catch ( NoSuchElementException e ) {
			return "";
		}
		
		return terms_string.toString();
	}


	public void addTerm(Synset synset, String term)
	{
		if (synset.getPos() != SynsetCategory.NONE) {
			HashMap<String, Counter> current_synset_terms;
			if (synset.hasOffset()) {
				// Retrieve the index that corresponds to the POS of the provided synset
				HashMap<Synset, HashMap<String, Counter>> category_map = synsetmap.get(synset.getPos());
				
				// Retrieve (and create if necessary) the term set for the specified synset
				current_synset_terms = category_map.get(synset);
				if (current_synset_terms == null) {
					current_synset_terms = new HashMap<String, Counter>();
					category_map.put(synset, current_synset_terms);
				}
			}
			else {
				current_synset_terms = unlemmatizedMap.get(synset.getPos());
			}
			
			// Increment the counter associated with the term instance for current term
			Counter c = current_synset_terms.get(term);
			if (c == null) {
				c = new Counter();
				current_synset_terms.put(term, c);
			}
			c.increment();
		}
		else {
			AppLogger.error.log(Level.WARNING, "Synset " + term + "(" + synset.toString() + ")"
				+ "cannot be aggregated in SynsetTermsAggregator because it has an invalid POS category");
			return;
		}
	}
	
	public int getSynsetCount(SynsetCategory synsetcat)
	{
		return synsetmap.get(synsetcat).size();
	}
	
	public int getTermCount(SynsetCategory synsetcat)
	{
		return unlemmatizedMap.get(synsetcat).size();
	}
	
	public int getSynsetCount()
	{
		int count = 0;
		Set<SynsetCategory> synsetcats = synsetmap.keySet();
		for (SynsetCategory synsetcat : synsetcats) {
			count += synsetmap.get(synsetcat).size();
		}
		return count;
	}
	
	public int getTermCount()
	{
		int count = 0;
		Set<SynsetCategory> synsetcats = unlemmatizedMap.keySet();
		for (SynsetCategory synsetcat : synsetcats) {
			count += unlemmatizedMap.get(synsetcat).size();
		}
		return count;
	}
	
	public int clearJunkTerms()
	{
		Counter removed_terms = new Counter();

		Set<SynsetCategory> synsetcats = unlemmatizedMap.keySet();
		for (SynsetCategory synsetcat : synsetcats) {
			HashMap<String, Counter> map = unlemmatizedMap.get(synsetcat);
			Iterator<Entry<String, Counter>> map_iter = map.entrySet().iterator();
			while ( map_iter.hasNext() ) {
				if (TermTypeFilter.isTermType(map_iter.next().getKey(), TermType.JUNK)) {
					map_iter.remove();
					removed_terms.increment();
				}
			}
		}
		
		return removed_terms.get();
	}

	public static void main(String[] args) throws IOException
	{
		// Loading term aggregator data
		SynsetTermsAggregator synset_terms = null;
		try {
			State<SynsetTermsAggregator> synset_terms_state = new State<SynsetTermsAggregator>("synsets", null);
			synset_terms = synset_terms_state.restoreState();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Could not restore synset terms data");
		}
		

		// Processing user input
		while ( true ) {
			String[] synsets_to_lookup = null;
			if (args.length == 0) {
				System.out.print("Enter one or more synsets to look-up (separated by space characters)\nSynsets: ");
				BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
				synsets_to_lookup = in.readLine().split(" ");
			}
			else {
				synsets_to_lookup = args;
			}
			
			for (String synset_str : synsets_to_lookup) {
				try {
					Synset synset = new Synset(synset_str);
					System.out.println(synset + " " + synset_terms.getSynsetTermsString(synset, true));
				} catch ( IllegalArgumentException e ) {
					System.out.println(e.getMessage());
				}
			}
			
			System.out.println("\n\n\n------------------------------------------------------\n\n\n");
		}
	}
	
	/**
	 * @return
	 */
	public static SynsetTermsAggregator load()
	{
		State<SynsetTermsAggregator> state = new State<SynsetTermsAggregator>("synsets", null);
		try {
			return state.restoreState();
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot load synset aggregator object");
			return new SynsetTermsAggregator();
		}
		
	}
}
