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
package dependencies;

import indexing.ReviewTermPayload;
import indexing.Token;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
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
import util.AppLogger;
import wordnet.DictionaryFactory;
import wordnet.IndexMap;
import classes.Review;
import filters.ComparisonDegreeFilter;
import filters.LemmatizationFilter;
import filters.NamedEntityFilter;
import filters.PosTaggingFilter;
import filters.StopLemmaFilter;
import filters._TokenLogFilter;
import filters.dependencies.NegationWordFilter;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public final class ReviewDependencyAnalyzer extends Analyzer {
	
	private Dictionary	wordnet;
	private IndexMap	compoundWordnetIndex;
	private Tokenizer	source_document;

	
	/**
	 * Constructor for class ReviewTextAnalyzer
	 */
	public ReviewDependencyAnalyzer()
	{
		super();
		wordnet = DictionaryFactory.getWordnetInstance();
		compoundWordnetIndex = DictionaryFactory.setupCompoundTermsIndex(wordnet);
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
		TokenFilter tokf_3 = new LemmatizationFilter(tokf_2, false, wordnet, compoundWordnetIndex);
		TokenFilter tokf_4 = new ComparisonDegreeFilter(tokf_3);
		TokenFilter tokf_5 = new NegationWordFilter(tokf_4);
		TokenFilter tokf_6 = new StopLemmaFilter(tokf_5, false);
		// TokenFilter tokf_6 = new TopicModelInputFilter(tokf_5, indexer.theTokenLists,
		// indexer.theReviewId);
		// TokenFilter tokf_7 = new IndexableFilter(tokf_6, true);
		// TokenFilter tokf_8 = new StatsFilter(tokf_7, indexer);
		
		TokenFilter tokf_p = new _TokenLogFilter(tokf_6);

		return tokf_5;
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

	public ArrayList<ArrayList<Token>> getSentences(Reader reader)
	{

		try {
			// Send reader data through the analyzer
			TokenStream tokstr = reusableTokenStream("", reader);
			TermAttribute tok_term = tokstr.getAttribute(TermAttribute.class);
			TypeAttribute tok_type = tokstr.getAttribute(TypeAttribute.class);
			FlagsAttribute tok_flags = tokstr.getAttribute(FlagsAttribute.class);
			PayloadAttribute tok_payload = tokstr.getAttribute(PayloadAttribute.class);
			
			// Split the tokenstream returned by the analyzer into sentences. Convert each sentence
			// into a linked list of tokens
			ArrayList<ArrayList<Token>> sentence_list = new ArrayList<ArrayList<Token>>();
			ArrayList<Token> current_sentence = new ArrayList<Token>();
			
			while ( tokstr.incrementToken() ) {
				Token current_token = new Token(tok_term.term(), tok_type.type(), tok_flags.getFlags(),
						new ReviewTermPayload(tok_payload.getPayload()));
				current_sentence.add(current_token);
				
				// End of sentence reached. Add current sentence to the sentence list
				if (current_token.isDelim(true)) {
					if (current_sentence.size() > 1) {
						sentence_list.add(current_sentence);
					}
					current_sentence = new ArrayList<Token>();
				}
			}
			
			// At the end of the token stream, if there is an incomplete sentence, add it to the
			// sentence list.
			// This case could occur when the last sentence of a given passage does not end with a
			// period or other sentence delimiter.
			if (!current_sentence.isEmpty()) {
				sentence_list.add(current_sentence);
			}

			return sentence_list;
		} catch ( IOException e ) {
			AppLogger.error
				.log(Level.SEVERE,
						"Error reading data from reader. Analyzing text for typed dependencies could not be completed");
			return null;
		}
	}
	
	public ArrayList<ArrayList<Token>> getSentences(Review review)
	{
		// return getSentences(new StringReader(review.getTitle() + ". " + review.getReviewText()));
		return getSentences(new StringReader(review.getReviewText()));
	}
	
	public ArrayList<ArrayList<Token>> getSentences(String text)
	{
		return getSentences(new StringReader(text));
	}


	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		ReviewDependencyAnalyzer r = new ReviewDependencyAnalyzer();
		String[] filenames = { "tt0108052_review1.txt" };
		for (String filename : filenames) {
			try {
				ArrayList<ArrayList<Token>> sentences = r.getSentences(new FileReader(filename));
				
				Iterator<ArrayList<Token>> s_i = sentences.iterator();
				while ( s_i.hasNext() ) {
					
					Iterator<Token> t_i = s_i.next().iterator();
					while ( t_i.hasNext() ) {

						Token t = t_i.next();
						System.out.print(t);

						if (t.isDelim(false)) {
							System.out.println();
						}
					}
					System.out.println("..................................................................\n");
				}

				System.out.println();
				

			} catch ( IOException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out
				.println("\n\n\n\n\n\n\n\n==================================================================\n\n\n\n\n\n\n\n");
		}
		
		return;
	}
	

}
