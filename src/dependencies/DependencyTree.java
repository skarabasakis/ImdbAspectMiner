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

import indexing.Token;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import util.AppLogger;
import edu.stanford.nlp.trees.TypedDependency;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class DependencyTree {
	
	protected ArrayList<Token>					tokenIndex;
	protected ArrayList<TypedDependencyWrapper>	relationIndex;
	protected HashSet<Integer>					relationTerms;
	protected HashSet<Integer>					leafNodes;
	private HashMap<Integer, Integer>			parents;
	

	
	public DependencyTree(ArrayList<Token> sentence) throws IllegalArgumentException
	{
		try {
			
			// Saving sentence tokens so that we can reference them by index id later
			tokenIndex = sentence;

			// Sending sentence through the Stanford parser
			ParsingUtils.lexicalizedParser.parse(sentence);

			// Retrieving the dependency tree of the sentence from the parser
			List<TypedDependency> dependency_tree = ParsingUtils.grammaticalStructureFactory //
				.newGrammaticalStructure(ParsingUtils.lexicalizedParser.getBestParse()).typedDependencies(false);
			

			// Storing the dependency tree as an ordered list of binary grammatical relations
			relationIndex = new ArrayList<TypedDependencyWrapper>();
			for (TypedDependency dependency : dependency_tree) {
				relationIndex.add(new TypedDependencyWrapper(dependency));
			}
			
			// Extracting set of depIndex -> govIndex pointers
			parents = new HashMap<Integer, Integer>();
			for (TypedDependencyWrapper relation : relationIndex) {
				parents.put(relation.getDepIndex(), relation.getGovIndex());
			}

			for (TypedDependencyWrapper relation : relationIndex) {
				relation.setPathToRoot(pathToRoot(relation.getGovIndex()));
			}

			Collections.sort(relationIndex);

			// Retrieving nodes that partake in dependencies
			relationTerms = new HashSet<Integer>();
			for (TypedDependencyWrapper relation : relationIndex) {
				relationTerms.add(relation.getGovIndex());
				relationTerms.add(relation.getDepIndex());
			}
			if (relationTerms.isEmpty()) {
				relationTerms.add(1);
			}


			// Retrieving leaf nodes
			HashSet<Integer> depNodes = new HashSet<Integer>();
			HashSet<Integer> govNodes = new HashSet<Integer>();
			for (TypedDependencyWrapper relation : relationIndex) {
				govNodes.add(relation.getGovIndex());
				depNodes.add(relation.getDepIndex());
			}
			depNodes.removeAll(govNodes);
			leafNodes = depNodes;
			
		} catch ( UnsupportedOperationException e ) {
			AppLogger.error.log(Level.SEVERE, "\"" + getSentence() + "\"\n" + e.getMessage());
			throw new IllegalArgumentException(e);
		} catch ( OutOfMemoryError e ) {
			AppLogger.error.log(Level.SEVERE, "\"" + getSentence() + "\"\n" + e.getMessage());
			throw new IllegalArgumentException(e);
		} catch ( Exception e ) {
			throw new IllegalArgumentException(e);
		}
	}
	
	/**
	 * Constructor for class DependencyTree
	 */
	public DependencyTree()
	{
	}

	public String getTerm(int index)
	{
		return tokenIndex.get(index - 1).term;
	}

	public Token getToken(int index)
	{
		return tokenIndex.get(index - 1);
	}
	
	protected Integer root() throws Exception
	{
		if (relationIndex.size() == 0) {
			for (int index = tokenIndex.size() ; index > 0 ; index--) {
				if (!tokenIndex.get(index - 1).isDelim(false))
					return index;
			}
			
			throw new Exception("No root!");
		}
		else {
			int first_nodeid = relationIndex.get(0).getGovIndex();
			try {
				return pathToRoot(first_nodeid).get(0);
			} catch ( IndexOutOfBoundsException e ) {
				return first_nodeid;
			}
		}
	}
	
	protected ArrayList<Integer> pathToRoot(int node)
	{
		ArrayList<Integer> pathToRoot = new ArrayList<Integer>();
		Integer current_node = node;
		while ( (current_node = parents.get(current_node)) != null ) {
			pathToRoot.add(0, current_node);
		}
		return pathToRoot;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		
		for (Token token : tokenIndex) {
			str.append(token);
			// str.append(token.word()).append("/").append(token.tag()).append(" ");
		}
		str.append("\n\n");
		
		for (TypedDependencyWrapper relation : relationIndex) {
			str.append(relation).append("\n");
		}
		str.append("\n-----------------------------------------\n");
		
		return str.toString();
	}

	protected String getSentence()
	{
		StringBuilder sentence = new StringBuilder();
		for (Token token : tokenIndex) {
			sentence.append(token.isProper() ? token.word().toUpperCase() : token.word()).append(' ');
		}
		
		return sentence.toString().replaceAll("\"", "\\\"");
	}

	protected String getSentence(TypedDependencyWrapper reln)
	{
		return getSentence(reln, 1, tokenIndex.size());
	}

	protected String getSentence(TypedDependencyWrapper reln, int size)
	{
		int min = Math.min(reln.getDepIndex(), reln.getGovIndex());
		int max = Math.max(reln.getDepIndex(), reln.getGovIndex());
		int dist = max - min + 1;
		int side = dist < size ? (size - (max - min + 1)) / 2 : 2;
		int min_index = Math.max(min - side, 1);
		int max_index = Math.min(max + side, tokenIndex.size());
		
		return getSentence(reln, min_index, max_index);
	}

	private String getSentence(TypedDependencyWrapper reln, int minIndex, int maxIndex)
	{
		StringBuilder sentence = new StringBuilder();
		
		sentence.append(minIndex != 1 ? "..." : "");
		for (int i = minIndex ; i <= maxIndex ; i++) {
			if (reln != null && (reln.getDepIndex() == i || reln.getGovIndex() == i)) {
				sentence.append('[').append(getTerm(i)).append(']').append(' ');
			}
			else {
				sentence.append(getTerm(i)).append(' ');
			}
		}
		sentence.append(maxIndex != tokenIndex.size() ? "..." : "");

		return sentence.toString();
	}


	public static void main(String[] args)
	{

	}
	
}
