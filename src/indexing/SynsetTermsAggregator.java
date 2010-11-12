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

import java.io.Serializable;
import java.util.HashMap;
import java.util.logging.Level;
import util.AppLogger;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import classes.Counter;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
@SuppressWarnings("serial")
public class SynsetTermsAggregator implements Serializable {
	
	private HashMap<SynsetCategory, HashMap<Synset, HashMap<String, Counter>>>	synsetmap;
	private HashMap<SynsetCategory, HashMap<String, Counter>>					unlemmatizedMap;
	// private HashMap<Synset, HashMap<String, Counter>> synsetmap;
	
	
	/**
	 * Constructor for class SynsetTermsAggregator
	 */
	public SynsetTermsAggregator()
	{
		synsetmap = new HashMap<Synset.SynsetCategory, HashMap<Synset, HashMap<String, Counter>>>();
		for (SynsetCategory synsetcat : SynsetCategory.values()) {
			synsetmap.put(synsetcat, new HashMap<Synset, HashMap<String, Counter>>());
		}
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
					current_synset_terms = category_map.put(synset, new HashMap<String, Counter>());
				}
			}
			else {
				current_synset_terms = unlemmatizedMap.get(synset.getPos());
			}
			
			// Increment the counter associated with the term instance for current term
			Counter c = current_synset_terms.get(term);
			if (c == null) {
				c = current_synset_terms.put(term, new Counter());
			}
			c.increment();
		}
		else {
			AppLogger.error.log(Level.WARNING, "Synset " + term + "(" + synset.toString() + ")"
				+ "cannot be aggregated in SynsetTermsAggregator because it has an invalid POS category");
			return;
		}
	}
}
