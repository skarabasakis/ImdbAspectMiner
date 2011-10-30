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

import net.didion.jwnl.data.POS;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public final class PosTag {
	
	public enum PosCategory {
		NONE, V, J, R, N, other, phrase_delim, sentence_delim
	}
	
	public static boolean isProper(String PosTag)
	{
		return PosTag.startsWith("NNP");
	}
	
	public static PosCategory toCategory(String PosTag)
	{
		switch (PosTag.toUpperCase().charAt(0)) {
			case 'N': /* Nouns: NN, NNS, NNP, NNPS */
			case 'F': /* foreign word nouns: FW */
				return PosCategory.N;
				
			case 'V': /* Verbs: VB, VBD, VBG, VBN, VBP, VBZ */
				return PosCategory.V;
				
			case 'A':
			case 'J': /* Adjectives: JJ, JJR, JJS */
				return PosCategory.J;
				
			case 'R': /* Adverbs and particles: RB, RBR, RBS, RP */
				return PosCategory.R;
			
			case 'C':
			case 'D':
			case 'E':
			case 'I':
			case 'M':
			case 'P':
			case 'T':
			case 'U':
			case 'W':
			case '*':
				return PosCategory.other;
				
			case '.': /* Includes punctuation marks . ! ? */
				return PosCategory.sentence_delim;
			
			case '$':
			case '`':
			case '\'':
			case ',':
			case '-':
			case ':':
			case 'L': /* list delimiters */
			case 'S': /* symbols */
				return PosCategory.phrase_delim;

			default:
				return PosCategory.NONE;
		}
	}

	public static PosCategory toCategory(POS pos) {
		return toCategory(getTypeString(pos));
	}

	public static boolean isDelim(PosCategory category)
	{
		return category == PosCategory.phrase_delim || category == PosCategory.sentence_delim;
	}
	
	public static boolean isSentenceDelim(PosCategory category)
	{
		return category == PosCategory.sentence_delim;
	}

	public static POS toPOS(String type)
	{
		switch (toCategory(type)) {
			case N:
				return POS.NOUN;
			case V:
				return POS.VERB;
			case J:
				return POS.ADJECTIVE;
			case R:
				return POS.ADVERB;
				
			default:
				return null;
		}
	}

	public static String getTypeString(POS pos)
	{
		if (pos == POS.NOUN)
			return getTypeString(PosCategory.N);
		else if (pos == POS.VERB)
			return getTypeString(PosCategory.V);
		else if (pos == POS.ADJECTIVE)
			return getTypeString(PosCategory.J);
		else if (pos == POS.ADVERB)
			return getTypeString(PosCategory.R);
		else if (pos == null)
			return getTypeString(PosCategory.other);
		else
			return getTypeString(PosCategory.NONE);
	}
	
	public static String getTypeString(PosCategory posCat)
	{
		switch (posCat) {
			case N:
				return "N";
			case V:
				return "V";
			case J:
				return "J";
			case R:
				return "R";
			case other:
				return "*";
			case phrase_delim:
				return "/";
			case sentence_delim:
				return ".";
			case NONE:
				return "0";
		}
		return null;
	}

	public static boolean isOpenClass(PosCategory posCat)
	{
		return posCat == PosCategory.V || posCat == PosCategory.N || posCat == PosCategory.J || posCat == PosCategory.R;
	}

	public static boolean hasDegree(POS pos)
	{
		return pos == POS.ADJECTIVE || pos == POS.ADVERB;
	}

	public static ComparisonDegree getDegree(String type)
	{
		switch (type.charAt(0)) {
			case 'J': /* Adjectives: JJ, JJR, JJS */
				if (type.equals("JJR")) return ComparisonDegree.COMPARATIVE;
				else if (type.equals("JJS")) return ComparisonDegree.SUPERLATIVE;
				else return ComparisonDegree.POSITIVE;
			case 'R': /* Adverbs and particles: RB, RBR, RBS, RP */
				if (type.equals("RBR"))
					return ComparisonDegree.COMPARATIVE;
				else if (type.equals("RBS")) return ComparisonDegree.SUPERLATIVE;
				else return ComparisonDegree.POSITIVE;
			default: /* All other non-adjective, non-adverb types */
				return ComparisonDegree.NONE;
		}
	}



}
