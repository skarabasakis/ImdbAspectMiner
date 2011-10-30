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
package filters.dependencies;

import indexing.Token;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.index.Payload;
import util.AppLogger;
import edu.stanford.nlp.util.ArrayUtils;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class NegationWordFilter extends TokenFilter {
	
	private static String[]		negationTerms	= { "n't",
		"no",
		"not",
		"never",
		"none",
		"nothing",
		"nobody",
		"noone",
		"nowhere",
		"without",
		"hardly",
		"barely",
		"rarely",
		"seldom"							};
	
	private boolean				negative		= false;

	private TermAttribute		input_term;
	private TypeAttribute		input_type;
	private FlagsAttribute		input_flags;
	private PayloadAttribute	input_payload;
	private TermAttribute		output_term;
	private TypeAttribute		output_type;
	private FlagsAttribute		output_flags;
	private PayloadAttribute	output_payload;


	/**
	 * Constructor for class NegationScopeFilter
	 * @param input
	 */
	public NegationWordFilter(TokenStream input)
	{
		super(input);
		
		// Getting attributes from input token stream
		input_term = input.getAttribute(TermAttribute.class);
		input_type = input.getAttribute(TypeAttribute.class);
		input_flags = input.getAttribute(FlagsAttribute.class);
		input_payload = input.getAttribute(PayloadAttribute.class);
		
		// Setting attributes for this token stream
		output_term = this.getAttribute(TermAttribute.class);
		output_type = this.getAttribute(TypeAttribute.class);
		output_flags = this.addAttribute(FlagsAttribute.class);
		output_payload = this.getAttribute(PayloadAttribute.class);
		
	}
	
	/* (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		// Load next token from input stream
		Token current_token;
		if ((current_token = getNextToken()) == null)
			return false;

		// If current token term belongs to the set of negation terms, switch the state of the
		// negation flag (i.e. prepare to mark the next open class term as negated). Or if this
		// is already the case, reverse the effect of the negation, to account for double negatives)
		if ( ArrayUtils.contains(negationTerms, current_token.term)) {
			current_token.payload.setNegation(true);
		}
		
		// Setting attributes for output stream
		output_term.setTermBuffer(current_token.term);
		output_type.setType(current_token.type);
		output_flags.setFlags(current_token.flags);
		output_payload.setPayload(current_token.payload.getPayload());

		return true;
	}
	
	private Token getNextToken() throws IOException
	{
		if (input.incrementToken()) {
			// Reading term attributes from input stream
			String term = input_term.term();
			String postag = input_type.type();
			int flags = input_flags.getFlags();
			Payload payload = input_payload.getPayload();

			// Adding token into buffer
			Token t = new Token(term, postag, flags);
			if (t.payload.decode(payload.getData()) == false) {
				AppLogger.error.log(Level.WARNING, "Invalid payload data for token [" + term + ":" + postag
					+ "] detected while reading input stream for NegationScopeFilter");
			}
			
			return t;
		}
		else
			return null;

	}
}
