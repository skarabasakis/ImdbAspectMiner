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
package topics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import lexicon.PayloadFilters;
import lexicon.SentimentLexicon;
import lexicon.TermSentiment;
import lexicon.classifiers.WidestWindowSentimentClassifier;
import org.apache.commons.lang.ArrayUtils;
import util.AppLogger;
import wordnet.Synset;
import wordnet.Synset.SynsetCategory;
import config.Globals;
import config.Paths;


/**
 * Filters a collection of document files used for topic extraction, so that only certain synset
 * types are included
 * 
 * @author Stelios Karabasakis
 */
public class TopicDocRewriter {
	
	File	inputDir	= null;
	File	outputDir	= null;
	File[]	docs		= null;
	

	/**
	 * Constructor for class TopicDocRewriter
	 * 
	 * @throws IOException
	 */
	public TopicDocRewriter(String inputDir, String outputDir) throws IOException
	{
		this.inputDir = new File(inputDir);
		if (!(this.inputDir.exists() && this.inputDir.isDirectory()))
			throw new IOException("Directory does not exist: " + inputDir);

		this.outputDir = new File(outputDir);
		if (!(this.outputDir.exists() && this.outputDir.isDirectory()))
			throw new IOException("Directory does not exist: " + outputDir);
		
		docs = this.inputDir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name)
			{
				return name.matches("docs[0-9]+\\.txt");
			}
		});
	}
	
	public static interface TopicDocTermFilter {
		
		public boolean acceptSynset(Synset synset);
		
		public boolean acceptWord(String word);
	}

	private void filterFile(BufferedReader input, BufferedWriter output, TopicDocTermFilter filter) throws IOException
	{
		String inputline;
		while ( (inputline = input.readLine()) != null ) {
			String[] inputline_array = inputline.split(" ");
			
			// Passing document terms through the TopicDocTermFilter to determine whether they will
			// be copied to the output file or not
			for (int i = 2 ; i < inputline_array.length ; i++) {
				if (Synset.matchesPattern(inputline_array[i])) {
					if (!filter.acceptSynset(new Synset(inputline_array[i]))) {
						inputline_array = (String[])ArrayUtils.remove(inputline_array, i);
					}
				}
				// Term is a word
				else {
					if (!filter.acceptWord(inputline_array[i])) {
						inputline_array = (String[])ArrayUtils.remove(inputline_array, i);
					}
				}
			}
			
			if (inputline_array.length > 2) {
				// Writing document id and length to the output file
				output.write(inputline_array[0] + " " + (inputline_array.length - 2));
			
				// Writing terms to file
				for (int i = 2 ; i < inputline_array.length ; i++) {
					output.write(" " + inputline_array[i]);
				}

				// Completing the document with a newline
				output.newLine();
			}
		}
	}
	
	public void filterAll(TopicDocTermFilter filter)
	{
		try {
			for (File current_docs : docs) {
				System.out.println("Now filtering " + current_docs.getName());
				BufferedReader input = new BufferedReader(new FileReader(current_docs));
				BufferedWriter output = new BufferedWriter(new FileWriter(new File(outputDir, current_docs.getName()),
						false));
				
				filterFile(input, output, filter);
				
				input.close();
				output.close();
			}
		} catch ( FileNotFoundException e ) {
			AppLogger.error.log(Level.SEVERE, e.getMessage());
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, e.getMessage());
		}
	}
	
	public static void main(String[] args)
	{
		try {
			// TopicDocRewriter rewriter = new TopicDocRewriter(Paths.tokenListPath + "/original",
			// Paths.tokenListPath
			// + "/filtered");
			// rewriter.filterAll(rewriter.SUBJECTIVITY_FILTER);
			//
			// System.out.println("\n------------------------------------------------------------------");

			TopicDocRewriter rewriter_nv = new TopicDocRewriter(Paths.tokenListPath + "/filtered_nv",
					Paths.tokenListPath + "/filtered_nv_1");
			rewriter_nv.filterAll(rewriter_nv.VERBS_AND_NOUNS_FILTER);
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private TopicDocTermFilter	VERBS_AND_NOUNS_FILTER	= new TopicDocTermFilter() {
															
															private SentimentLexicon	lexicon	= SentimentLexicon
																									.loadLexicon(
																													new WidestWindowSentimentClassifier(),
																													PayloadFilters.FILTER_PLAIN);

															@Override
															public boolean acceptWord(String word)
															{
																return lexicon.getEntry(SynsetCategory.N, word) != null
																	|| lexicon.getEntry(SynsetCategory.V, word) != null;
															}
															
															@Override
															public boolean acceptSynset(Synset synset)
															{
																SynsetCategory cat = synset.getPos();
																return cat == SynsetCategory.V
																	|| cat == SynsetCategory.N;
															}
														};

	SentimentLexicon			lexicon					= new SentimentLexicon(false);

	private TopicDocTermFilter	SUBJECTIVITY_FILTER		= new TopicDocTermFilter() {
															
															private SynsetCategory[]	synsetcats	= Synset
																										.getSynsetCategories();
															private SentimentLexicon	lexicon		= SentimentLexicon
																										.loadLexicon(
																														new WidestWindowSentimentClassifier(),
																														PayloadFilters.FILTER_PLAIN);
															
															@Override
															public boolean acceptWord(String word)
															{
																for (SynsetCategory synsetcat : synsetcats) {
																	TermSentiment s = lexicon.getEntry(synsetcat, word);
																	if (s != null)
																		return s.getSubjectivityScore() >= Globals.TopicParameters.subjectivityThreshold;
																}
																
																return false;
															}
															
															@Override
															public boolean acceptSynset(Synset synset)
															{
																TermSentiment s = lexicon.getEntry(synset);
																if (s != null)
																	return s.getSubjectivityScore() >= Globals.TopicParameters.subjectivityThreshold;
																else
																	return false;
															}
														};

}
