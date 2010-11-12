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

import indexing.filters.ComparisonDegreeFilter;
import indexing.filters.IndexableFilter;
import indexing.filters.LemmatizationFilter;
import indexing.filters.NamedEntityFilter;
import indexing.filters.NegationFilter;
import indexing.filters.PosTaggingFilter;
import indexing.filters.SentenceCollectorFilter;
import indexing.filters.StatsFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.KeywordTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import wordnet.DictionaryFactory;
import wordnet.IndexMap;
import application.ReviewDocumentIndexer;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public final class ReviewTextAnalyzer extends Analyzer {
	
	private Dictionary				wordnet;
	private IndexMap				compundWordnetIndex;
	private ReviewDocumentIndexer	indexer	= null;
	private Tokenizer				source_document;
	

	/**
	 * Constructor for class ReviewTextAnalyzer
	 */
	public ReviewTextAnalyzer(ReviewDocumentIndexer indexer)
	{
		super();
		this.indexer = indexer;
		
		wordnet = DictionaryFactory.getWordnetInstance();
		compundWordnetIndex = LemmatizationFilter.setupCompoundTermsIndex(wordnet);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.lucene.analysis.Analyzer#tokenStream(java.lang.String, java.io.Reader)
	 */
	@Override
	public TokenStream tokenStream(String fieldName, Reader reader)
	{
		source_document = new KeywordTokenizer(reader);
		
		TokenFilter tokf_1 = new PosTaggingFilter(source_document);
		TokenFilter tokf_2 = new NamedEntityFilter(tokf_1);
		TokenFilter tokf_3 = new LemmatizationFilter(tokf_2, wordnet, compundWordnetIndex);
		TokenFilter tokf_4 = new ComparisonDegreeFilter(tokf_3);
		TokenFilter tokf_5 = new NegationFilter(tokf_4);
		TokenFilter tokf_6 = new IndexableFilter(tokf_5);
		TokenFilter tokf_7 = new SentenceCollectorFilter(tokf_6, indexer.theSentences, indexer.theReviewId);
		TokenFilter tokf_8 = new StatsFilter(tokf_7, indexer);
		
		return tokf_2;
	}
	
	@Override
	public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException
	{
		
		TokenStream tok_str = (TokenStream)getPreviousTokenStream();
		if (tok_str == null) {
			tok_str = tokenStream(fieldName, reader);
			setPreviousTokenStream(tok_str);
		}
		else {
			source_document.reset(reader);
			tok_str.reset();
		}
		return tok_str;

	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ReviewTextAnalyzer r = new ReviewTextAnalyzer(new ReviewDocumentIndexer());
		String[] filenames = { "review.txt" };
		for (String filename : filenames) {
			try {
				TokenStream tokstr = r.tokenStream(null, new FileReader(filename));
								
				TermAttribute output_term = tokstr.addAttribute(TermAttribute.class);
				TypeAttribute output_type = tokstr.addAttribute(TypeAttribute.class);
				FlagsAttribute output_flags = tokstr.addAttribute(FlagsAttribute.class);
				PayloadAttribute output_payload = tokstr.addAttribute(PayloadAttribute.class);
				
				while ( tokstr.incrementToken() ) {
					
					Token current_token = new Token(output_term.term(), output_type.type(), output_flags.getFlags(),
							new ReviewTermPayload(output_payload.getPayload()));
					
					System.out.print(current_token);
					
					if (current_token.isDelim(false)) {
						System.out.println();
					}
					else if (current_token.isDelim(true)) {
						System.out.println("..................................................................");
					}
				}
				System.out.println();
				

			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
}
