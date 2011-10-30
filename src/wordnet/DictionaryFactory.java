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
package wordnet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.commons.lang.ArrayUtils;
import util.AppLogger;
import util.State;
import config.Paths;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class DictionaryFactory {

	public static Dictionary getWordnetInstance()
	{
		if (!JWNL.isInitialized()) {
			try {
				JWNL.initialize(new FileInputStream(Paths.jwnlConfigFile));
			} catch ( FileNotFoundException e ) {
				AppLogger.error.log(Level.SEVERE, "While initializing Wordnet: JWNL config file not found at location "
										+ Paths.jwnlConfigFile);
			} catch ( JWNLException e ) {
				e.printStackTrace();
				AppLogger.error.log(Level.SEVERE, "While initializing Wordnet: Cannot initialize JWNL Library");
			}
		}
		return Dictionary.getInstance();
	}

	public static IndexMap setupCompoundTermsIndex(Dictionary wordnet)
	{
		IndexMap compoundTermsIndex = null;
		State<IndexMap> compound_index_state = new State<IndexMap>("compoundTermsIndex", compoundTermsIndex);
		try {
			compoundTermsIndex = compound_index_state.restoreState();
		} catch ( IOException e ) {
			// Generating an index of compound terms that are lemmatized in Wordnet
			compoundTermsIndex = new IndexMap();

			ArrayList<String[]> compoundTermsList = new ArrayList<String[]>();
			POS[] pos_array = new POS[]{ POS.NOUN, POS.VERB, POS.ADJECTIVE, POS.ADVERB };
			for (POS pos : pos_array) {
				compoundTermsList.addAll(getCompoundTermsList(wordnet, pos));
			}
			compoundTermsIndex.index(compoundTermsList);
			
			compound_index_state.setObj(compoundTermsIndex);
			compound_index_state.saveState();
		}
		
		return compoundTermsIndex;
	}
	
	@SuppressWarnings("unchecked")
	private static ArrayList<String[]> getCompoundTermsList(Dictionary wordnet, POS pos)
	{
		ArrayList<String[]> compoundTermsList = new ArrayList<String[]>();
		Iterator term_iter = null;
		
		try {
			term_iter = wordnet.getIndexWordIterator(pos);
		} catch ( JWNLException e ) {
			AppLogger.error.log(Level.SEVERE, "Cannot get index word set from wordnet dictionary.");
		}
		
		while ( term_iter.hasNext() ) {
			IndexWord current_term_obj = (IndexWord)term_iter.next();
			String current_term_string = current_term_obj.getLemma();
			// current_term_string = current_term_string.replace("'s ", " 's ").replace("s' ",
			// "s ' ");
			String[] current_term = current_term_string.split(" ");
			String[] current_term_nodash = current_term.clone();
			for (int i = 0 ; i < current_term_nodash.length ; i++) {
				String[] current_term_nodash_segment = current_term_nodash[i].split("-");
				if (current_term_nodash_segment.length > 1) {
					current_term_nodash[i] = current_term_nodash_segment[0];
					for (int j = 1 ; j < current_term_nodash_segment.length ; j++) {
						current_term_nodash = (String[])ArrayUtils.add(current_term_nodash, i + j,
																		current_term_nodash_segment[j]);
					}
				}
			}

			if (current_term.length > 1) {
				compoundTermsList.add(parseCompoundTerm(current_term, pos));
			}
			if (current_term_nodash.length > 1) {
				compoundTermsList.add(parseCompoundTerm(current_term_nodash, pos));
			}
		}
		return compoundTermsList;
	}

	private static String[] parseCompoundTerm(String[] compound_term, POS pos)
	{
		String[] compound_term_array = compound_term.clone();
		for (int i = 0 ; i < compound_term_array.length ; i++) {
			
			// Special handling for verb lemmas that contain the possessive placeholder
			// "one's" or "someone's", (e.g. "give_one's_best", "pull_someone's_leg")
			//
			// if (pos == POS.VERB) {
			// if (compound_term_array[i].endsWith("one's")) {
			// compound_term_array[i] = "POS_PHRASE";
			// }
			// }
			
			// Special handling for noun lemmas that contain a word in possessive case form,
			// (e.g. "adam's_apple", "mind's eye", "Achilles' heel")
			if (pos == POS.NOUN) {
				if (compound_term_array[i].endsWith("'s")) {
					compound_term_array[i] = compound_term_array[i].substring(0, compound_term_array[i].length() - 2);
					compound_term_array = (String[])ArrayUtils.add(compound_term_array, i + 1, "'s");
					i++;
				}
				else if (compound_term_array[i].endsWith("s'")) {
					compound_term_array[i] = compound_term_array[i].substring(0, compound_term_array[i].length() - 1);
					compound_term_array = (String[])ArrayUtils.add(compound_term_array, i + 1, "'");
				}
			}
			
		}
		
		return compound_term_array;
	}

}
