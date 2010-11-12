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

import indexing.ReviewTermPayload;
import indexing.Token;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import util.AppLogger;
import config.Globals;


/**
 * A {@link TokenFilter} which <strong>consumes</strong> a POS-tagged {@link TokenStream} where
 * part-of-speech tags are provided as {@link TypeAttribute} values and <strong>produces</strong> a
 * {@code TokenStream} with the following features:
 * <ul>
 * <li>all named entities, i.e. proper nouns, are marked as such in the token's payload
 * <li>consecutive proper nouns, such as a first name followed by a a last name, are concatenated
 * into a single named entity
 * <li>all other tokens are left intact
 * </ul>
 * 
 * @author Stelios Karabasakis
 */
public class NamedEntityFilter extends TokenFilter {

	private static final Integer	FLAG_PROPER			= 1;
	private static final Integer	FLAG_CONJUNCTION	= 2;
	
	private ArrayList<Token>		currentTokens		= new ArrayList<Token>();
	
	private TermAttribute			input_term;
	private TypeAttribute			input_type;
	private PayloadAttribute		input_payload;
	private TermAttribute			output_term;
	private TypeAttribute			output_type;
	private PayloadAttribute		output_payload;
	
	/**
	 * Constructor for class NamedEntityFilter
	 * 
	 * @param input
	 *            The input {@link TokenStream}
	 */
	public NamedEntityFilter(TokenStream input)
	{
		super(input);
		
		// Getting attributes from input stream
		input_term = input.getAttribute(TermAttribute.class);
		input_type = input.getAttribute(TypeAttribute.class);
		input_payload = input.getAttribute(PayloadAttribute.class);
		
		// Setting attributes for this stream
		output_term = this.addAttribute(TermAttribute.class);
		output_type = this.addAttribute(TypeAttribute.class);
		output_payload = this.addAttribute(PayloadAttribute.class);
		

	}

	/* (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		// Load next phrase into the buffer, if necessary. And if no next phrase exists, return false.
		if (currentTokens.isEmpty()) {
			if (incrementPhrase()) {
				unifyAllNamedEntities();
			}
			else
				return false;
		}

		// Return current token
		Token token_to_return = currentTokens.remove(0);
		output_term.setTermBuffer(token_to_return.term);
		output_type.setType(token_to_return.type);
		output_payload.setPayload(token_to_return.payload.getPayload());
		
		return true;
	}
	
	
	/*
	 * Default value for members neStart and neEnd
	 */
	private static final Integer	UNSET	= -1;
	
	/*
	 * Marks the start position (inclusive) of a named entity in the list of currentTokens
	 */
	private Integer					neStart	= UNSET;
	
	/*
	 * Marks the end position (inclusive) of a named entity in the list of currentTokens
	 */
	private Integer					neEnd	= UNSET;

	private void unifyAllNamedEntities()
	{
		int base = 0;
		while ( findNextNamedEntity(base) ) {
			concatNamedEntity();
			base = neStart + 1;
		}
	}

	private boolean findNextNamedEntity(Integer base)
	{
		Token current_token;
		resetNamedEntityPosition();
		for (int index = base ; index < currentTokens.size() ; index++) {
			current_token = currentTokens.get(index);
			
			if (isPotentialNamedEntityMember(current_token)) {
				if (neStart == UNSET) {
					neStart = index;
				}
				neEnd = index;
			}
			else if (!isPotentialNamedEntityGlue(current_token)) {
				if (neStart != UNSET)
					return true;
			}
		}
		
		return neStart != UNSET ? true : false;
	}
	
	private void resetNamedEntityPosition()
	{
		neStart = UNSET;
		neEnd = UNSET;
	}
	
	private void concatNamedEntity()
	{
		if (neStart == UNSET) {
			AppLogger.error.log(Level.WARNING, "In method concatNamedEntity: Unspecified index values");
			return;
		}
		
		Token concat_token = currentTokens.get(neStart);
		int current_token_index = neStart + 1;
		for (int index = neStart + 1 ; index <= neEnd ; index++) {
			Token current_token = currentTokens.get(current_token_index);
			
			concat_token.term = concat_token.term + " " + current_token.term;
			concat_token.type = "NNP";
			concat_token.payload.setProper(true);
			
			currentTokens.remove(current_token_index);
		}
	}

	/**
	 * 
	 */
	private void markPotentialNamedEntity()
	{
		Iterator<Token> tokens = currentTokens.iterator();
		Token current_token = null;
		Token previous_token = null;
		while ( tokens.hasNext() ) {
			current_token = tokens.next();
			if (isPotentialNamedEntityMember(current_token)) {
				current_token.flags = FLAG_PROPER;
			}
			else if (isPotentialNamedEntityGlue(current_token)) {
				current_token.flags = FLAG_CONJUNCTION;
			}
		}
	}
	
	/*
	 * Returns whether a given token could be a named entity -- or take part in one
	 */
	private boolean isPotentialNamedEntityMember(Token token)
	{
		// A token is a potential Named Entity member iff it has been tagged as proper noun by the
		// POS tagger or begins with a capital letter
		return token.isProper() || beginsWithCapitalLetter(token.term);
	}
	
	
	private static final String[]	glueTypes	= { "CC", "DT", "IN", "TO" };

	/*
	 * Returns whether a given token could be named entity glue, i.e. a preposition, article,
	 * conjunction or "to" which is part of a named entity (such as the title of a movie)
	 */
	private boolean isPotentialNamedEntityGlue(Token token)
	{
		// A non-capitalized word could be part of a named entity iff it is 4 characters long or
		// less and is carries one of the following POS-tags: CC, DT, IN, TO
		// For explanation, see http://grammartips.homestead.com/caps.html
		return token.term.length() <= 4 && ArrayUtils.contains(glueTypes, token.type);
	}

	/*
	 * Returns whether a given word's first letter is capital. English only.
	 */
	private boolean beginsWithCapitalLetter(String word)
	{
		try {
			char first_letter = word.charAt(0);
			return first_letter >= 'A' && first_letter <= 'Z';
		} catch ( ArrayIndexOutOfBoundsException e ) {
			return false;
		}
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
				currentTokens.add(current_token);
				
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
					}
				}
			}
			else {
				if (currentTokens.isEmpty())
					return false;
			}
		}
		
		return true;
	}
	
	/*
	 * Reads the next token from the input {@code TokenStream}
	 */
	private Token loadNextToken()
	{
		Token next_token = new Token(input_term.term(), input_type.type(), Globals.NO_FLAGS, new ReviewTermPayload(
				input_payload.getPayload()));
		return next_token;
	}
}
