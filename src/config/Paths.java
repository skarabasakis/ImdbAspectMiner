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
 * 
 * @author Stelios Karabasakis
 */
public class Paths {
	
	public static final String	BIN_ROOT				= "C:/Users/Skarab/Documents/Code/Java/AspectMiner";
	public static final String	stopLemmasFile			= BIN_ROOT + "/stoplemmas.txt";
	public static final String	sentiWordnetFile		= BIN_ROOT + "/SentiWordNet_3.0.0_20100908.txt";
	public static final String	MPQAFile				= BIN_ROOT + "/subjclueslen1-HLTEMNLP05.txt";
	public static final String	GeneralInquirerFile		= BIN_ROOT + "/GeneralInquirer.txt";
	
	public static final String	LIB_ROOT				= "C:/Users/Skarab/Documents/Code/Java/lib";
	public static final String	jwnlConfigFile			= LIB_ROOT + "/jwnl14-rc2/config/file_properties.xml";
	// = LIB_ROOT + "/jwnl14-rc2/config/ramdisk_properties.xml";
	public static final String	taggerPath				= LIB_ROOT + "/postagger/stanford-postagger-full-2010-05-26";
	public static final String	depParserPath			= LIB_ROOT + "/stanford-parser-2010-07-09";
	
	public static final String	WORKDIR_ROOT			= "C:/Users/Skarab/Documents/Code/Java/AspectMiner/output";
	public static final String	V_WORKDIR_ROOT			= "D:/output_2";
	public static final String	luceneIndex				= WORKDIR_ROOT + "/reviewindex";
	public static final String	luceneBackupIndex		= WORKDIR_ROOT + "/reviewindex.bak";
	public static final String	stateFiles				= WORKDIR_ROOT + "/state";
	public static final String	backupStateFiles		= WORKDIR_ROOT + "/state.bak";
	public static final String	tokenListPath			= WORKDIR_ROOT + "/tokenlists";
	public static final String	lexiconPath				= WORKDIR_ROOT + "/sentilexicon/";
	public static final String	lexiconDiscardedPath	= WORKDIR_ROOT + "/sentilexicon/discarded/";
	public static final String	ruleSetFilesPath		= WORKDIR_ROOT + "/ruleset/";
	public static final String	dotFilesPath			= WORKDIR_ROOT + "/treeviz/";


	

}
