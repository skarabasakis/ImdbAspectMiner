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

import indexing.PosTag.PosCategory;
import java.util.ArrayList;
import java.util.Iterator;
import net.didion.jwnl.data.POS;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class CompoundLemmaTagger {

	private ArrayList<String>	typeArray;
	
	
	/**
	 * Constructor for class CompoundLemmaTagger
	 */
	public CompoundLemmaTagger()
	{
		typeArray = new ArrayList<String>();
	}
	
	public void collectType(String type)
	{
		if (PosTag.isOpenClass(PosTag.toCategory(type))) {
			typeArray.add(type);
		}
	}
	
	public String combineTypes(POS pos)
	{
		return combineTypes(PosTag.toCategory(pos));
	}

	public String combineTypes(PosCategory pos)
	{
		Iterator<String> type_iterator = typeArray.iterator();
		while ( type_iterator.hasNext() ) {
			if (PosTag.toCategory(type_iterator.next()) != pos) {
				type_iterator.remove();
			}
		}
		
		if (typeArray.isEmpty()) {
			// If no matching types remain, return a default type that matches the provided POS
			// category.
			switch (pos) {
				case V:
					return "VB";
				case N:
					return "NN";
				case J:
					return "JJ";
				case R:
					return "RB";
				case other:
					return null;
			}
		}
		else {
			// Return the most significant type depending on the provided POS category
			switch (pos) {
				case N:
				case J:
				case R:
					return typeArray.get(typeArray.size() - 1);
				case V:
				case other:
					return typeArray.get(0);
			}
		}
		
		return null;
	}
}
