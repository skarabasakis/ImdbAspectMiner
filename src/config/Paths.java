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
package config;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class Paths {
	
	public static final String	root				= "C:/Users/Skarab/Documents/Code/Java";

	public static final String	jwnlConfigFile		= root + "/lib/jwnl14-rc2/config/file_properties.xml";
	public static final String	taggerPath		= root + "/lib/postagger/stanford-postagger-full-2010-05-26";
	public static final String	depParserPath		= root + "/lib/stanford-parser-2010-07-09";

	public static final String	luceneIndex			= root + "/AspectMiner/reviewindex";
	public static final String	luceneBackupIndex	= root + "/AspectMiner/reviewindex.bak";
	public static final String	indexerState		= root + "/AspectMiner/state";
	public static final String	indexerBackupState	= root + "/AspectMiner/state.bak";
	
	public static final String	tokenListFile		= root + "/AspectMiner/tokenlist.txt";




}
