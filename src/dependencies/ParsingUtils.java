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

import config.Paths;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.GrammaticalStructureFactory;
import edu.stanford.nlp.trees.PennTreebankLanguagePack;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class ParsingUtils {
	
	public static final LexicalizedParser			lexicalizedParser			= getLexicalizedParserInstance();
	public static final GrammaticalStructureFactory	grammaticalStructureFactory	= getGrammaticalStructureFactoryInstance();
	
	private static LexicalizedParser getLexicalizedParserInstance()
	{
		LexicalizedParser lp = new LexicalizedParser(Paths.depParserPath + "/englishPCFG.ser.gz");
		lp.setOptionFlags(new String[]{ "-maxLength", "60", "-retainTmpSubcategories" });
		return lp;
	}
	
	private static GrammaticalStructureFactory getGrammaticalStructureFactoryInstance()
	{
		return new PennTreebankLanguagePack().grammaticalStructureFactory();
	}
}
