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
package filters.indexing;

import indexing.ReviewTermPayload;
import indexing.Token;
import java.io.IOException;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class IndexableFilter extends TokenFilter {
	
	private boolean				set_synset_terms	= false;

	private TermAttribute		input_term;
	private TypeAttribute		input_type;
	private FlagsAttribute		input_flags;
	private PayloadAttribute	input_payload;
	private TermAttribute		output_term;
	private TypeAttribute		output_type;
	private FlagsAttribute		output_flags;
	private PayloadAttribute	output_payload;

	/**
	 * Constructor for class IndexableFilter
	 * @param input
	 */
	public IndexableFilter(TokenStream input, boolean set_synset_terms)
	{
		super(input);
		
		// Getting attributes from input token stream
		input_term = input.getAttribute(TermAttribute.class);
		input_type = input.getAttribute(TypeAttribute.class);
		input_flags = input.getAttribute(FlagsAttribute.class);
		input_payload = input.getAttribute(PayloadAttribute.class);
		
		// Setting attributes for this token stream
		output_term = this.addAttribute(TermAttribute.class);
		output_type = this.addAttribute(TypeAttribute.class);
		output_flags = this.addAttribute(FlagsAttribute.class);
		output_payload = input.addAttribute(PayloadAttribute.class);
		
		this.set_synset_terms = set_synset_terms;
	}
	
	/* (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException
	{
		Token current_token;
		while ((current_token = getNextToken()) != null) {
			if (current_token.isIndexable()) {
				if (set_synset_terms) {
					if (current_token.isLemmatized()) {
						output_term.setTermBuffer(current_token.synset().toString());
						output_type.setType(current_token.term);
					}
					else {
						output_term.setTermBuffer(current_token.term);
						output_type.setType(current_token.term);
					}
				}
				else {
					output_term.setTermBuffer(current_token.term);
					output_type.setType(current_token.synset().toString());
				}
				output_flags.setFlags(current_token.flags);
				output_payload.setPayload(current_token.payload.getPayload());
				
				return true;
			}
		}

		return false;
	}
	
	private Token getNextToken() throws IOException
	{
		if (input.incrementToken())
			return new Token(input_term.term(), input_type.type(), input_flags.getFlags(), new ReviewTermPayload(
					input_payload.getPayload()));
		else
			return null;
	}
}
