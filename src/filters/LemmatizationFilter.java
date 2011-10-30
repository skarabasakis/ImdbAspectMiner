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
package filters;

import indexing.ComparisonDegree;
import indexing.CompoundLemmaTagger;
import indexing.Lemma;
import indexing.PosTag;
import indexing.ReviewTermPayload;
import indexing.Token;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import util.AppLogger;
import wordnet.DictionaryFactory;
import wordnet.IndexMap;
import wordnet.Synset;
import config.Globals;


/**
 * A {@link TokenFilter} that looks up the base lemma type of all open class words in a given
 * english language text, by looking them up into the wordnet dictionary.
 * <p>
 * In linguistics, words in open class are the ones that primarily communicate the meaning of a
 * sentence, as opposed to closed class words which provide the structure of a sentence. English
 * open class words fall into the following four grammatical types: verbs, nouns, adjectives and
 * adverbs.
 * <p>
 * <code>LemmatizationFilter</code> <strong>consumes</strong> a pos-tagged {@link TokenStream} where
 * part-of-speech tags are provided as {@link TypeAttribute} values and <strong>produces</strong>
 * {@code TokenStream} with each word available in lexicon reduced to its base term and annotated
 * with its respective synset.:
 * 
 * @author Stelios Karabasakis
 */
public class LemmatizationFilter extends TokenFilter {
	
	public static class Lemmatizer {
		
		private Dictionary	wordnet;
		public IndexMap		compoundTermsIndex;
		
		/**
		 * Constructor for class filters.Lemmatizer
		 */
		public Lemmatizer()
		{
			wordnet = DictionaryFactory.getWordnetInstance();
			compoundTermsIndex = DictionaryFactory.setupCompoundTermsIndex(wordnet);
		}
		
		public Lemmatizer(Dictionary wordnet, IndexMap compoundTermsIndex)
		{
			this.wordnet = wordnet;
			this.compoundTermsIndex = compoundTermsIndex;
		}
		
		public Synset[] lookupWord(String word)
		{
			try {
				IndexWord[] lookup_results = wordnet.lookupAllIndexWords(word).getIndexWordArray();
				Synset[] synsets = new Synset[lookup_results.length];
				for (int i = 0 ; i < lookup_results.length ; i++) {
					IndexWord lookup_result = lookup_results[i];
					synsets[i] = new Synset(PosTag.toCategory(lookup_result.getSense(1).getPOS()), lookup_result
						.getSense(1).getOffset());
				}
				
				return synsets;
			} catch ( JWNLException e ) {
				AppLogger.error
					.log(Level.SEVERE,
							"In method lookupWord: A problem occured when attempting to read data from Wordnet");
				return new Synset[0];
			}

		}
		
		
		public Synset lookupWord(String word, POS pos)
		{
			try {
				IndexWord lookup_result = wordnet.lookupIndexWord(pos, word);
				if (lookup_result != null)
					return new Synset(PosTag.toCategory(pos), lookup_result.getSense(1).getOffset());
				else
					return null;
			} catch ( JWNLException e ) {
				AppLogger.error
					.log(Level.SEVERE,
							"In method lookupWord: A problem occured when attempting to read data from Wordnet");
				return null;
			}
		}
		
		public Lemma lookupSingleWordLemma(Lemma lemma)
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
		
		public Lemma lookupCompoundLemma(Lemma lemma) throws RuntimeException
		{
			if (lemma.getLength() > 1) {
				try {
					IndexWordSet candidate_lemma_set = wordnet.lookupAllIndexWords(lemma.getLemma());
					IndexWord[] candidate_indexwords = candidate_lemma_set.getIndexWordArray();
					
					Lemma lemma_to_return = new Lemma();
					for (IndexWord candidate_indexword : candidate_indexwords) {
						Lemma candidate_lemma = new Lemma();
						candidate_lemma.appendTokensToLemma(candidate_indexword.getLemma());
						candidate_lemma.setSynset(candidate_indexword.getSense(1));
						if (candidate_lemma.getLength() > lemma_to_return.getLength()
							|| candidate_lemma.getWordLength() > lemma_to_return.getWordLength()) {
							lemma_to_return = candidate_lemma;
						}
					}
					
					if (lemma_to_return.getLength() != 0) {
						lemma_to_return.overrideLength(lemma.getLength());
						return lemma_to_return;
					}
				} catch ( JWNLException e ) {
					AppLogger.error
						.log(Level.SEVERE,
								"In method lookupCompoundLemma: A problem occured when attempting to read data from Wordnet");
				}
				
				AppLogger.error.log(Level.WARNING, "An indexed compound lemma (" + lemma.getLemma()
					+ ") was not found in Wordnet");
				return lemma;
				// throw new RuntimeException("Lookup error");
			}
			else
				return lemma;
			
		}
	}
	
	
	private Lemmatizer			lemmatizer			= new Lemmatizer();
	
	/*
	 * A temporary buffer for the tokens that make up the phrase that is currently being processed
	 * by the TokenFilter
	 */
	private LinkedList<Token>	currentTokens		= new LinkedList<Token>();
	
	/*
	 * Attributes of the input stream that is consumed by this filter
	 */
	private TermAttribute		input_term;
	private TypeAttribute		input_type;
	private PayloadAttribute	input_payload;
	
	/*
	 * Attributes for the output TokenStream that is produced by this filter
	 */
	private TermAttribute		output_term;
	private TypeAttribute		output_type;
	private FlagsAttribute		output_flags;
	private PayloadAttribute	output_payload;
	
	private boolean				lemmatized_output	= false;
	
	/**
	 * Constructor for class LemmatizationFilter
	 * 
	 * @param input
	 */
	public LemmatizationFilter(TokenStream input, boolean lemmatized_output)
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
		
		this.lemmatized_output = lemmatized_output;
		
		lemmatizer = new Lemmatizer();
	}
	
	
	/**
	 * Constructor for class LemmatizationFilter
	 */
	public LemmatizationFilter(TokenStream input, boolean lemmatized_output, Dictionary wordnet,
			IndexMap compoundTermsIndex)
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
		
		this.lemmatized_output = lemmatized_output;
		
		lemmatizer = new Lemmatizer(wordnet, compoundTermsIndex);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		// Load next phrase into the buffer, if necessary. And if no next phrase exists, return
		// false.
		if (currentTokens.isEmpty()) {
			if (!incrementPhrase())
				return false;
		}
		
		// Load first available token from currentTokens list
		Token current_token = currentTokens.get(0);
		Token token_to_return = null;
		CompoundLemmaTagger tag_to_return = new CompoundLemmaTagger();
		

		if (current_token.isDelim(false)) {
			token_to_return = currentTokens.remove(0);
			token_to_return.payload.setPosCat(PosTag.toCategory(current_token.type));
		}
		else if (current_token.payload.isProper()) {
			token_to_return = currentTokens.remove(0);
			token_to_return.payload.setUnlemmatizable(true);
		}
		else {
			Lemma lemma = null;
			
			// If the first token is an open class word, then lemmatize it (i.e. look up the base
			// form of the word into wordnet)
			if (current_token.isOpenClass()) {
				lemma = lemmatizer.lookupSingleWordLemma(new Lemma(current_token.term, current_token.getPOS(),
						Lemma.NO_OFFSET));
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
			Lemma base_lemma = lemmatizer.lookupCompoundLemma(getNextLemma(lemma.getLemma()));
			Lemma instance_lemma = lemma.wasFound() && !lemma.getLemma().equals(current_token.term) ? lemmatizer
				.lookupCompoundLemma(getNextLemma(current_token.term)) : base_lemma;
			
			// The lenghtiest among the candidate lemmas becomes the active lemma
			lemma = lemma.getLength() < base_lemma.getLength() ? (base_lemma.getLength() < instance_lemma.getLength() ? instance_lemma
				: base_lemma)
				: lemma;
			
			// Remove used tokens from token stream. Also, collect token types from tokens as you
			// remove them.
			Lemma instance = new Lemma();
			ComparisonDegree degree = current_token.getDegree();
			boolean proper = current_token.isProper();
			int lemma_length = lemma.getLength();
			while ( lemma_length > 0 ) {
				current_token = currentTokens.remove();
				instance.appendToLemma(current_token.term);
				tag_to_return.collectType(current_token.type);
				lemma_length--;
			}
			
			// Convert discovered lemma to a token that we can return
			String pos_to_return = lemma.hasPos() ? tag_to_return.combineTypes(lemma.getPos()) : current_token.type;
			
			token_to_return = new Token(lemmatized_output ? lemma.getLemma() : instance.getLemma(), pos_to_return,
					lemma.getOffset());
			token_to_return.payload.setPosCat(PosTag.toCategory(pos_to_return));
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
	
	private Lemma getNextLemma(String first_token)
	{
		IndexMap index = lemmatizer.compoundTermsIndex.get(first_token);
		Lemma lemma = new Lemma(first_token);
		Lemma lemma_suffix = new Lemma();
		
		if (index == null)
			return lemma;
		
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
						lemma_suffix.appendToLemma(possessive_phrase_term, lemma_suffix.getLength());
						if (index.isEndNode()) {
							lemma.appendToLemma(lemma_suffix);
							lemma_suffix = new Lemma(); // Delete and reinitialize suffix_lemma
						}
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
	
	
	/**
	 * Loads the next phrase from the input {@code TokenStream} as a list of {@code Token}s into a
	 * temporary buffer. A phrase is defined as a a self-contained text segment between two
	 * punctuation marks.
	 * 
	 * @return false if there are no more phrases to be read from the input {@code TokenStream},
	 *         otherwise true
	 * @throws IOException
	 *             if an error occurs while reading a token from the input {@code TokenStream}
	 */
	private boolean incrementPhrase() throws IOException
	{
		boolean end_of_phrase = false;
		boolean between_quotemarks = false;
		
		while ( !end_of_phrase ) {
			if (input.incrementToken()) {
				// Reading term attributes from input stream and adding token into buffer
				Token current_token = loadNextToken();
				

				// Act upon encountering delimiting punctuation
				if (current_token.isDelim(false)) {
					// Check if current punctuation is a quotation mark
					if (current_token.isQuotation(between_quotemarks)) {
						// Quotation marks do not end the phrase. We just use them to switch the
						// state of open quotation marks from open to closed or vice versa.
						between_quotemarks = between_quotemarks ? false : true;
					}
					else {
						// Delimiters do not end the phrase if we encounter them between open
						// punctuation marks, but they do end the phrase in all other cases
						end_of_phrase = between_quotemarks ? false : true;
						if (!between_quotemarks) {
							currentTokens.add(current_token);
						}
					}
				}
				else {
					currentTokens.add(current_token);
				}
			}
			else {
				end_of_phrase = true;
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
