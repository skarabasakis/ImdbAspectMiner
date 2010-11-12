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
package indexing.filters;

import indexing.ComparisonDegree;
import indexing.CompoundLemmaTagger;
import indexing.Lemma;
import indexing.PosTag;
import indexing.ReviewTermPayload;
import indexing.Token;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import util.AppLogger;
import wordnet.DictionaryFactory;
import wordnet.IndexMap;
import config.Globals;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class LemmatizationFilter extends TokenFilter {
	
	private Dictionary			wordnet;
	private IndexMap			compoundTermsIndex;

	private LinkedList<Token> currentTokens = new LinkedList<Token>();
	
	private TermAttribute		input_term;
	private TypeAttribute		input_type;
	private PayloadAttribute	input_payload;
	private TermAttribute		output_term;
	private TypeAttribute		output_type;
	private FlagsAttribute		output_flags;
	private PayloadAttribute	output_payload;

	/**
	 * Constructor for class LemmatizationFilter
	 * @param input
	 */
	public LemmatizationFilter(TokenStream input)
	{
		super(input);
		
		// Getting attributes from input stream
		input_term = input.getAttribute(TermAttribute.class);
		input_type = input.getAttribute(TypeAttribute.class);
		input_payload = input.getAttribute(PayloadAttribute.class);
		
		// Setting attributes for this stream
		output_term = this.addAttribute(TermAttribute.class);
		output_type = this.addAttribute(TypeAttribute.class);
		output_flags = this.addAttribute(FlagsAttribute.class);
		output_payload = this.addAttribute(PayloadAttribute.class);

		wordnet = DictionaryFactory.getWordnetInstance();
		compoundTermsIndex = setupCompoundTermsIndex(wordnet);
	}
	
	
	/**
	 * Constructor for class LemmatizationFilter
	 */
	public LemmatizationFilter(TokenStream input, Dictionary wordnet, IndexMap compoundTermsIndex)
	{
		super(input);
		
		// Getting attributes from input stream
		input_term = input.getAttribute(TermAttribute.class);
		input_type = input.getAttribute(TypeAttribute.class);
		input_payload = input.getAttribute(PayloadAttribute.class);
		
		// Setting attributes for this stream
		output_term = this.getAttribute(TermAttribute.class);
		output_type = this.getAttribute(TypeAttribute.class);
		output_flags = this.addAttribute(FlagsAttribute.class);
		output_payload = this.addAttribute(PayloadAttribute.class);
		
		this.wordnet = wordnet;
		this.compoundTermsIndex = compoundTermsIndex;
	}
	
	public static IndexMap setupCompoundTermsIndex(Dictionary wordnet)
	{
		IndexMap compoundTermsIndex = new IndexMap();

		// Generating an index of compound terms that are lemmatized in Wordnet
		ArrayList<String[]> compoundTermsList = new ArrayList<String[]>();
		POS[] pos_array = new POS[] {POS.NOUN, POS.VERB, POS.ADJECTIVE, POS.ADVERB};
		for (POS pos : pos_array) {
			compoundTermsList.addAll(getCompoundTermsList(wordnet, pos));
		}
		compoundTermsIndex.index(compoundTermsList);
		
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
			//current_term_string = current_term_string.replace("'s ", " 's ").replace("s' ", "s ' ");
			String[] current_term = current_term_string.split(" ");
			String[] current_term_nodash = current_term.clone();
			for (int i = 0 ; i < current_term_nodash.length ; i++) {
				String[] current_term_nodash_segment = current_term_nodash[i].split("-");
				if (current_term_nodash_segment.length > 1) {
					current_term_nodash[i] = current_term_nodash_segment[0];
					for (int j = 1; j < current_term_nodash_segment.length; j++) {
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
			//if (pos == POS.VERB) {
			//	if (compound_term_array[i].endsWith("one's")) {
			//		compound_term_array[i] = "POS_PHRASE";
			//	}
			//}
			
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

	/*
	 * (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		// Load next phrase into the buffer, if necessary. And if no next phrase exists, return false.
		if (currentTokens.isEmpty()) {
			if (!incrementPhrase())
				return false;
		}
		
		// Load first available token from currentTokens list
		Token current_token = currentTokens.get(0);
		Token token_to_return = null;
		CompoundLemmaTagger tag_to_return = new CompoundLemmaTagger();
		// boolean token_is_lemmatized = false;
		
		
		if (current_token.isDelim(false)) {
			currentTokens.remove(0);
			token_to_return = new Token(current_token.term, current_token.type, current_token.flags,
					current_token.payload);
			token_to_return.payload.setPosCat(PosTag.toCategory(current_token.type));
		}
		else {
			Lemma lemma = null;
			
			// If the first token is an open class word, then lemmatize it (i.e. look up the base
			// form of the word into wordnet)
			if (current_token.isOpenClass()) {
				lemma = lookupSingleWordLemma(new Lemma(current_token.term, current_token.getPOS(), Lemma.NO_OFFSET));
			}
			else {
				lemma = new Lemma(current_token.term);
			}

			 
			
			// Follow the chain of successive tokens from the source stream, starting with
			// first_token. Look up each increasingly long phrase into wordnet,
			// until you find a corresponding lemma or until you hit a sentence delimiter (this is
			// all done by the getNextLemma function). If necessary, try to do this with both the
			// lemmatized (i.e. base) as well as the original (i.e. instance) form of the token. If
			// conflicting lemmatizations are discovered, the longest lemma wins.
			Lemma base_lemma = lookupCompoundLemma(getNextLemma(lemma.getLemma()));
			Lemma instance_lemma = new Lemma();
			if (lemma.wasFound() && !lemma.getLemma().equals(current_token.term)) {
				instance_lemma = lookupCompoundLemma(getNextLemma(current_token.term));
			}
			
			// The lenghtiest among the candidate lemmas becomes the active lemma
			lemma = lemma.getLength() < base_lemma.getLength() ?
				( base_lemma.getLength() < instance_lemma.getLength() ?
					instance_lemma
				: base_lemma)
			: lemma;
			
			// Remove used tokens from token stream. Also, collect token types from tokens as you
			// remove them.
			ComparisonDegree degree = current_token.getDegree();
			boolean proper = current_token.isProper();
			int lemma_length = lemma.getLength();
			while ( lemma_length > 0 ) {
				current_token = currentTokens.remove();
				tag_to_return.collectType(current_token.type);
				lemma_length--;
			}

			// Convert discovered lemma to a token that we can return
			token_to_return = new Token(lemma.getLemma(), tag_to_return.combineTypes(lemma.getPos()), lemma.getOffset());
			token_to_return.payload.setPosCat(PosTag.toCategory(lemma.getPos()));
			token_to_return.payload.setUnlemmatizable(lemma.getPos() != null && !lemma.wasFound());
			token_to_return.payload.setDegree(degree);
			token_to_return.payload.setProper(proper);
		}
		
		// Setting attributes for output stream
		output_term.setTermBuffer(token_to_return.term);
		output_type.setType(token_to_return.type);
		output_flags.setFlags(token_to_return.flags);
		output_payload.setPayload(token_to_return.payload.getPayload());
		
		return true;
	}
	
	/**
	 * @param lemma
	 * @return
	 */
	private Lemma lookupSingleWordLemma(Lemma lemma)
	{
		Lemma lemma_to_return = null;
		try {
			IndexWord lookup_result = wordnet.lookupIndexWord(lemma.getPos(), lemma.getLemma());
			if (lookup_result != null) {
				lemma_to_return = new Lemma(lookup_result.getLemma(), lookup_result.getSense(1));
			}
			else {
				lemma_to_return = new Lemma(lemma.getLemma());
				lemma_to_return.setPos(lemma.getPos());
				lemma_to_return.setNotFound();
			}
			
			return lemma_to_return;
		} catch ( JWNLException e ) {
			AppLogger.error
				.log(Level.SEVERE,
						"In method lookupSingleWordLemma: A problem occured when attempting to read data from Wordnet");
			return null;
		}
	}
	
	/**
	 * @param lemma
	 * @return
	 */
	private Lemma lookupCompoundLemma(Lemma lemma) throws RuntimeException
	{
		if (lemma.getLength() > 1) {
			try {
				IndexWordSet candidate_lemma_set = wordnet.lookupAllIndexWords(lemma.getLemma());
				IndexWord[] candidate_indexwords = candidate_lemma_set.getIndexWordArray();

				for (int i = 0 ; i < candidate_indexwords.length; i++ ) {
					Lemma candidate_lemma = new Lemma();
					candidate_lemma.appendTokensToLemma(candidate_indexwords[i].getLemma());
					candidate_lemma.setSynset(candidate_indexwords[i].getSense(1));
					if (candidate_lemma.getLength() == lemma.getLength())
						return candidate_lemma;
				}
			} catch ( JWNLException e ) {
				AppLogger.error
					.log(Level.SEVERE,
							"In method lookupCompoundLemma: A problem occured when attempting to read data from Wordnet");
			}
			
			AppLogger.error.log(Level.SEVERE, "An indexed compound lemma (" + lemma.getLemma()
				+ ") was not found in Wordnet");
			throw new RuntimeException("Lookup error");
		}
		else
			return lemma;

	}

	private Lemma getNextLemma(String first_token)
	{
		
		IndexMap index = compoundTermsIndex.get(first_token);
		Lemma lemma = new Lemma(first_token);
		Lemma lemma_suffix = new Lemma();
		
		Token current_token;
		int current_token_offset;
		
		String possessive_phrase_term = null;
		boolean search_for_possessive_phrase = false;
		boolean first_token_of_possessive_phrase = false;
		
		while ( (current_token_offset = lemma.getLength() + lemma_suffix.getLength()) < currentTokens.size() ) {
			current_token = currentTokens.get(current_token_offset);
			
			if (!current_token.isDelim(false)) {
				
				// Get index subtree for current term
				if (search_for_possessive_phrase) {
					lemma_suffix.appendToLemma(current_token.term);

					if (first_token_of_possessive_phrase ? current_token.type.equals("PRP$") : current_token.type
						.equals("POS")) {
						// Search for possessive phrase is successfully over
						search_for_possessive_phrase = false;
						lemma.appendToLemma(possessive_phrase_term, lemma_suffix.getLength());
						lemma_suffix = new Lemma();
					}
					first_token_of_possessive_phrase = false;
				}
				else if (index.has(current_token.term)) {
					index = index.get(current_token.term);
					lemma_suffix.appendToLemma(current_token.term);
					
					// Check that current chain of tokens forms compound term
					if (index.isEndNode()) {
						lemma.appendToLemma(lemma_suffix);
						lemma_suffix = new Lemma(); // Delete and reinitialize suffix_lemma
					}
				}
				else if (index.has("one's")) {
					possessive_phrase_term = "one's";
					index = index.get("one's");
					search_for_possessive_phrase = true;
					first_token_of_possessive_phrase = true;
				}
				else if (index.has("someone's")) {
					possessive_phrase_term = "someone's";
					index = index.get("someone's");
					search_for_possessive_phrase = true;
					first_token_of_possessive_phrase = true;
				}
				else {
					break;
				}
				
			}
			else {
				break;
			}
		}
		
		return lemma;
	}


	private boolean incrementPhrase() throws IOException
	{
		boolean end_of_phrase = false;
		
		while ( !end_of_phrase ) {
			if (input.incrementToken()) {
				// Reading term attributes from input stream
				// Adding token into buffer
				Token current_token = loadNextToken();
				currentTokens.add(current_token);
				
				// Check if we have reached the end of phrase
				if (current_token.isDelim(false)) {
					end_of_phrase = true;
				}
			}
			else {
				if (currentTokens.isEmpty())
					return false;
			}
		}
		
		return true;
	}
	
	private Token loadNextToken()
	{
		Token next_token = new Token(input_term.term(), input_type.type(), Globals.NO_FLAGS, new ReviewTermPayload(
				input_payload.getPayload()));
		return next_token;
	}

}
