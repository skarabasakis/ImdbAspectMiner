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

import java.util.ArrayList;
import edu.stanford.nlp.trees.GrammaticalRelation;
import edu.stanford.nlp.trees.TypedDependency;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class TypedDependencyWrapper implements Comparable<TypedDependencyWrapper> {
	
	// private TypedDependency theDependency = null;
	private static final int	BEFORE		= -1;
	private static final int	AFTER		= 1;
	private static final int	EQUALS		= 0;
	
	private int					govIndex;
	private String				govLabel;
	private int					depIndex;
	private String				depLabel;
	private GrammaticalRelation	relation;
	private ArrayList<Integer>	pathToRoot	= null;

	/**
	 * Constructor for class TypedDependencyWrapper
	 */
	public TypedDependencyWrapper(TypedDependency d)
	{
		govIndex = d.gov().index();
		govLabel = d.gov().value().toLowerCase();
		depIndex = d.dep().index();
		depLabel = d.dep().value().toLowerCase();
		relation = d.reln();
	}
	
	public int getGovIndex()
	{
		return govIndex;
	}
	
	public String getGovLabel()
	{
		return govLabel;
	}
	
	public int getDepIndex()
	{
		return depIndex;
	}
	
	public String getDepLabel()
	{
		return depLabel;
	}
	
	public GrammaticalRelation getRelation()
	{
		return relation;
	}
	
	private int LCAChild(TypedDependencyWrapper other)
	{
		Integer thischild;
		int index = 0;
		try {
			while ( (thischild = pathToRoot.get(index)) == other.pathToRoot.get(index) ) {
				index++;
			}
		} catch ( IndexOutOfBoundsException e ) {
			if (index == pathToRoot.size())
				return govIndex;
			else
				return pathToRoot.get(index);
		}
		
		return thischild;
	}
	

	/*
	 * (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TypedDependencyWrapper other)
	{
		if (govIndex == other.govIndex) {
			// Relations with common governor: trailing dependent is evaluated first
			if (depIndex > other.depIndex)
				return BEFORE;
			else if (depIndex < other.depIndex)
				return AFTER;
		}
		else {
			int thischild = LCAChild(other);
			int otherchild = other.LCAChild(this);
			
			// Same-branch relations: bottom relation is evaluated first
			if (thischild == otherchild) {
				if (thischild == govIndex)
					return AFTER;
				else if (thischild == other.govIndex)
					return BEFORE;
			}
			
			// Foreign relations: relation on the rightmost branch is evaluated first
			else {
				if (thischild > otherchild)
					return BEFORE;
				else if (thischild < otherchild)
					return AFTER;
			}
		}

		System.out.println("Unhandled comparison case for " + this + "\t\tand " + other);
		return EQUALS;
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return relation.getShortName() + "(" + govLabel + "-" + govIndex + ", " + depLabel + "-" + depIndex + ")";
	}
	
	public ArrayList<Integer> getPathToRoot()
	{
		return pathToRoot;
	}

	/**
	 * @param pathToRoot
	 */
	public void setPathToRoot(ArrayList<Integer> pathToRoot)
	{
		this.pathToRoot = pathToRoot;
	}
}
