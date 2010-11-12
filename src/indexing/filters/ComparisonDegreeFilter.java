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
import indexing.PosTag;
import indexing.Token;
import indexing.PosTag.PosCategory;
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


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class ComparisonDegreeFilter extends TokenFilter {
	
	private ComparisonDegree	degree;
	
	private TermAttribute		input_term;
	private TypeAttribute		input_type;
	private FlagsAttribute		input_flags;
	private PayloadAttribute	input_payload;
	private TermAttribute		output_term;
	private TypeAttribute		output_type;
	private FlagsAttribute		output_flags;
	private PayloadAttribute	output_payload;


	/**
	 * Constructor for class NegationFilter
	 * 
	 * @param input
	 */
	public ComparisonDegreeFilter(TokenStream input)
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
		
		// Default degree
		degree = ComparisonDegree.NONE;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		// Load next token from input stream
		Token current_token;
		if ((current_token = getNextToken()) == null)
			return false;
		

		if ( !tokenAllowsDegree(current_token)) {
			degree = ComparisonDegree.NONE;
		}
		else {
			if (tokenHasDegree(current_token)) {
				degree = current_token.payload.getDegree();
			}
			else {
				current_token.payload.setDegree(degree);
			}
		}

		// Setting attributes for output stream
		output_term.setTermBuffer(current_token.term);
		output_type.setType(current_token.type);
		output_flags.setFlags(current_token.flags);
		output_payload.setPayload(current_token.payload.getPayload());
		
		//
		return true;
	}
	
	private boolean tokenAllowsDegree(Token token)
	{
		PosTag.PosCategory pos_cat = token.payload.getPosCat();
		return pos_cat == PosCategory.J || pos_cat == PosCategory.R;
	}
	
	private boolean tokenHasDegree(Token token)
	{
		ComparisonDegree degree = token.payload.getDegree();
		return degree == ComparisonDegree.COMPARATIVE || degree == ComparisonDegree.SUPERLATIVE;
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
					+ "] detected while reading input stream for NegationFilter");
			}
			
			return t;
		}
		else
			return null;
		
	}
}
