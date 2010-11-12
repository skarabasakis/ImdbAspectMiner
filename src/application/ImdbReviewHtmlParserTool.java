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
package application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import util.AppLogger;
import util.DatabaseConnection;
import classes.Movie;
import classes.ParsedReview;
import classes.ParsedReviewCollection;


/**
 * Utility that scrapes tha review content out of a set of HTML files downloaded from the imdb.com
 * website. Each HTML file contains all the user-submitted reviews on a single movie.
 * 
 * @author Stelios Karabasakis
 */
public class ImdbReviewHtmlParserTool {
	
	/** Defines the stages the parser goes through while parsing the HTML document */
	enum DocumentParsingStage {
		START_OF_DOCUMENT, START_OF_REVIEW, ON_REVIEW_HEADERS, ON_SPOILER_TAG, ON_REVIEW_CONTENT, ON_REVIEW_FOOTER, END_OF_REVIEW, END_OF_DOCUMENT
	};
	
	/**
	 * Defines the stages the parser goes through while parsing a single review withing the
	 * document. These stages are repeated for each and every review
	 */
	enum HeadersParsingStage {
		START_OF_HEADERS, USER_VOTES, TITLE, DATE, RATING, AUTHOR, END_OF_HEADERS
	};
	
	/** Cyberneko DOM Parser for parsing HTML documents with the DOM model */
	private static DOMParser			domp					= new DOMParser();
	
	/** File system directory where downloaded review documents are located */
	public static String				rootDirLocation			= "C:/Users/Skarab/Documents/Code/Java/AspectMiner/reviewdata";
	
	/** Location of current HTML document to be parsed */
	public static String				currentDocumentLocation	= "file:///C:/Users/Skarab/Documents/Code/Java/AspectMiner/reviewdata/001/tt0044231";
	
	public static void main(String[] args)
	{
		// Initializing variables
		ParsedReviewCollection currentReviewDocument = null; // Stores all the information that is
																// parsed
														// from the HTML document at the current
														// location
		
		// Connecting to the database
		DatabaseConnection c;
		try {
			c = new DatabaseConnection();
		} catch ( SQLException e ) {
			AppLogger.error.log(Level.SEVERE, "An exception occured while trying to connect to database.\n"
				+ e.getMessage());
			return;
		}


		// Retrieving a list of all subdirectories of root directory
		System.out.println("Scanning root directory " + rootDirLocation + " for subdirectories");
		int dir_counter = 0; // Counts the number of subdirectories under rootDir
		File rootDir = new File(rootDirLocation);
		String[] rootDirContents = rootDir.list();
		for (int i = 0 ; i < rootDirContents.length ; i++) {
			File dir = new File(rootDirContents[i] = rootDirLocation + "/" + rootDirContents[i]);
			if (dir.isDirectory()) {
				dir_counter++;
			}
		}
		System.out.println(dir_counter + " subdirectories found\n\n");
		
		// For every subdirectory, process all review documents detected inside
		for (int i = 0 ; i < rootDirContents.length ; i++) {
			
			// Open the subdirecotry and read it's contents.. Directory contains a number of review
			// documents. Filenames of the review documents are in the form og an imdbId, i.e.
			// "ttXXXXXXX".
			File currentDir = new File(rootDirContents[i]);
			if (currentDir.isDirectory()) {
				// Create or truncate sql script files in subdirectory
				FileWriter reviewContents = null;
				FileWriter reviewCounts = null;
				try {
					reviewContents = new FileWriter(new File(rootDirLocation + "/sql_scripts/" + currentDir.getName()
						+ "_review_contents.sql"), true);
					reviewCounts = new FileWriter(new File(rootDirLocation + "/sql_scripts/" + currentDir.getName()
						+ "_review_counts.sql"), true);
				} catch ( IOException e1 ) {
					AppLogger.error.log(Level.SEVERE, "Cannot create script files for review directory # "
						+ currentDir.getName() + " inside directory " + rootDirLocation + "/sql_scripts/");
					break;
				}

				// Read contents of subdirectory
				String[] currentDirContents = currentDir.list();
				
				// Count review documents in directory, i.e. files with names in the form "ttXXXXXXX".
				// Display the result
				int document_counter = 0;
				for (int f=0; f<currentDirContents.length; f++) {
					// Only process files with names in the form "ttXXXXXXX"
					if ( currentDirContents[f].startsWith("tt") && currentDirContents[f].length() == 9 ) {
						document_counter++;
					}
				}
				System.out.println("Directory " + rootDirContents[i] +
				               "\nProcessing " + document_counter + " review documents");
						
				// Open review documents inside this directory one by one and send them to the
				// parser.
				for (int f=0; f<currentDirContents.length; f++) {
					// Only process files with names in the form "ttXXXXXXX"
					if ( currentDirContents[f].startsWith("tt") && currentDirContents[f].length() == 9 ) {
						
						// Open current review document
						String imdbid = currentDirContents[f];
						File currentDocument = new File(currentDocumentLocation = currentDir + "/" + currentDirContents[f]);
						if (currentDocument.isFile()) {
							try {
								System.out.print(imdbid + ": ");
								currentReviewDocument = parseReviewDocument(imdbid, currentDocumentLocation);
								System.out.println(currentReviewDocument.getCount() == 0 ? "--- no reviews ---"
									: currentReviewDocument.getCount() + " reviews parsed");

								// TODO Add a command line argument to choose database commit or
								// script output

								// Add review collection to query queue. Delayed database updates
								// are enabled, unless this is the last file in the directory
								currentReviewDocument.commitToDatabase(c, f < currentDirContents.length - 1 ? true
									: false);
								
								// Write insert query to script file
								currentReviewDocument.writeQueriesToScript(reviewContents, reviewCounts);
							} catch ( Exception e ) {
								System.out.print("[an exception occured)\n");
								System.out.flush();
								AppLogger.error.log(Level.WARNING, currentDir.getName() + "/"
									+ currentDocument.getName() + " was not commited to database. (" + e.getClass()
									+ ")");
								e.printStackTrace();
								System.err.flush();
							}
						}
					}
				}

				try {
					reviewContents.close();
					reviewCounts.close();
				} catch ( IOException e ) {
					AppLogger.error.log(Level.WARNING, "Cannot close script files for review directory # "
						+ currentDir.getName() + " inside directory " + rootDirLocation + "/sql_scripts/");

				}
				System.out.println();
			}
		}
		
	}
	
	/**
	 * @throws IOException
	 * @throws SAXException
	 */
	public static ParsedReviewCollection parseReviewDocument(String imdbid, String documentLocation)
			throws SAXException,
			IOException
	{
		// Initializing data collection to store the parsing results
		ParsedReviewCollection currentDocument = new ParsedReviewCollection(new Movie(imdbid));
		ParsedReview currentReview = null; // Stores all content and metadata of the review that is
										// currently being parsed
		
		// Parsing the document at the current location
		domp.parse(documentLocation);
		Node content = domp.getDocument().getElementById("tn15content");
		
		// Retrieve and store total number of reviews in the current document.
		// We will be using this later for verification purposes
		Node number_of_reviews_node = content.getChildNodes().item(9).getFirstChild().getFirstChild().getChildNodes()
			.item(1).getFirstChild();
		if (number_of_reviews_node != null
				&& number_of_reviews_node.getNodeType() == Node.TEXT_NODE) {
			currentDocument.setCount(Integer.parseInt(number_of_reviews_node.getNodeValue().split(" ")[0]
			                                                                                           .substring(1)));
		}
		
		// Initializing the loop that parses the document reviews one-by-one
		currentReview = new ParsedReview();
		Node n = content.getFirstChild();
		DocumentParsingStage stage = DocumentParsingStage.START_OF_DOCUMENT;
		
		// Entering the main review-parsing loop
		while ( stage != DocumentParsingStage.END_OF_DOCUMENT && (n = n.getNextSibling()) != null ) {
			
			// Process HTML elements only, ignore everything else: #text nodes, comments etc.
			if (n.getNodeType() == Node.ELEMENT_NODE) {
				
				switch (stage) {
					/*
					 * Sample HTML to parse START ON_REVIEW_HEADERS: <p>......</p>
					 * ON_REVIEW_CONTENT: <p><b>*** This review may contain spoilers ***</b></p>
					 * <p></p> <p>....</p> ON_FEEDBACK_FORM: <div class="yn"
					 * id="ynd_2026394">........</div> <script type="text/javascript">.....</script>
					 * <div ... id="sponsored_links_afc_div_MIDDLE_CENTER"></div> <iframe ...
					 * id="sponsored_links_afc_iframe_MIDDLE_CENTER"></iframe> END_OF_REVIEW: <hr
					 * noshade="1" size="1" width="50%" align="center">
					 * .......................................................................
					 * END_OF_DOCUMENT: <hr size="1" noshade="1">
					 */
					case START_OF_DOCUMENT:
						// Ignoring the element and staying on the same stage, unless we
						// have reached tha part of the document where reviews begin, in
						// which case we proceed with the next stage.
						if (n.getNodeName().equals("HR") && n.getAttributes().getLength() == 2) {
							stage = DocumentParsingStage.START_OF_REVIEW;
						}
						break;
						
					case START_OF_REVIEW:
						if (n.getNodeName().equals("HR")
								&& n.getAttributes().getLength() == 2) {
							stage = DocumentParsingStage.END_OF_DOCUMENT;
							break;
						}
						else {
							// Retrieve the imdbId of the movie this review is about
							currentReview = new ParsedReview();
							currentReview.setMovie(currentDocument.getMovie().getImdbid());
							
							// Proceed to next stage
							stage = DocumentParsingStage.ON_REVIEW_HEADERS;
						}
						
						
					case ON_REVIEW_HEADERS:
						// Next parsing stage is ON_SPOILER_TAG.
						stage = DocumentParsingStage.ON_SPOILER_TAG;
						
						// Process review headers
						if (n.getNodeName().equals("P")) {
							/*
							 * Sample HTML to parse. (Some elements may be missing) <small>1 out of
							 * 1 people found the following review useful:</small><br> <b>Superman,
							 * Superman, Superman!!</b>, <small>26 May 2007</small><br> <img
							 * width="102" height="12" alt="10/10"
							 * src="http://i.media-imdb.com/images/showtimes/100.gif"><br>
							 * <small>Author:</small> <a href="/user/ur3509760/comments"> Zach
							 * Kucala (zach@kucalafamily.net)</a> <small>from Georgia</small><br>
							 */
							NodeList headers = n.getChildNodes();
							Node hn = headers.item(0);
							HeadersParsingStage hstage = HeadersParsingStage.START_OF_HEADERS;
							
							// Looping over and parsing Review Headers
							while ( hstage != HeadersParsingStage.END_OF_HEADERS
									&& (hn = hn.getNextSibling()) != null ) {
								
								// Process HTML elements only, ignore everything else: #text
								// nodes, comments etc.
								if (hn.getNodeType() == Node.ELEMENT_NODE) {
									
									// Ignore line breaks when processing
									if (!hn.getNodeName().equals("BR")) {
										switch (hstage) {
											
											case START_OF_HEADERS:
												// Go to USER_VOTES immediately
												hstage = HeadersParsingStage.USER_VOTES;
												
											case USER_VOTES:
												// Next parsing stage is TITLE. If a USER_VOTES
												// element is not encountered
												// at this parsing stage, don't break. Move on
												// to next stage immediately.
												hstage = HeadersParsingStage.TITLE;
												
												// If a USER_VOTES element, identiified by
												// <small>, is encountered at this
												// parsing stage
												if (hn.getNodeName().equals("SMALL")) {
													// Process USER_VOTES element.
													String[] uservotes = hn.getFirstChild().getNodeValue()
													.split(" ");
													currentReview.setVotesUseful(Integer.parseInt(uservotes[0]));
													currentReview.setVotesTotal(Integer.parseInt(uservotes[3]));
													
													// Fetch next header, then move on to next
													// parsing stage
													break;
												}
												
											case TITLE:
												// Next parsing stage is DATE. If a TITLE
												// element is not encountered
												// at this parsing stage, don't break. Move on
												// to next stage immediately.
												hstage = HeadersParsingStage.DATE;
												
												if (hn.getNodeName().equals("B")) {
													// Process TITLE element
													if (hn.getFirstChild() != null) {
														currentReview.setTitle(hn.getFirstChild().getNodeValue());
													}
													else {
														currentReview.setTitle("");
													}
													// Fetch next header, then move on to next
													// parsing stage
													break;
												}
												else {
													// Unexpected condition. No TITLE
													System.err.println("ERROR: Expected TITLE element is missing.");
												}
												
											case DATE:
												// Next parsing stage is RATING. If a DATE
												// element is not encountered
												// at this parsing stage, don't break. Move on
												// to next stage immediately.
												hstage = HeadersParsingStage.RATING;
												
												if (hn.getNodeName().equals("SMALL")) {
													// Process and parse publication date of
													// review
													try {
														DateFormat dateparser = new SimpleDateFormat(
														                                             "d MMMMM yyyy", new Locale("en"));
														Date publicationDate = dateparser.parse(hn.getFirstChild()
														                                        .getNodeValue());
														currentReview.setPublicationDate(publicationDate);
													} catch ( ParseException e ) {
														System.err.println("WARNING: Unable to parse date: "
														                   + e.getMessage());
														currentReview.setPublicationDate(new Date(0));
													}
													
													// Fetch next header, then move on to next
													// parsing stage
													break;
												}
												
											case RATING:
												// Next parsing stage is RATING. If a DATE
												// element is not encountered
												// at this parsing stage, don't break. Move on
												// to next stage immediately.
												hstage = HeadersParsingStage.AUTHOR;
												
												if (hn.getNodeName().equals("IMG")) {
													// Process RATING element
													String ratingURI = hn.getAttributes().getNamedItem("src")
													.getNodeValue();
													currentReview.setRating(Integer.parseInt(ratingURI
														.substring(ratingURI.lastIndexOf('/') + 1, ratingURI
															.lastIndexOf('0'))));
													
													// Fetch next header, then move on to next
													// parsing stage
													break;
												}
												
											case AUTHOR:
												if (hn.getNodeName().equals("A")) {
													// Process AUTHOR element
													String authorURI = hn.getAttributes().getNamedItem("href")
													.getNodeValue();
													currentReview.setAuthorId(authorURI.split("/")[2]);
													
													// Next parsing stage is END_OF_HEADERS. If
													// a DATE element is not encountered
													// at this parsing stage, don't break. Move on
													// to next stage immediately.
													hstage = HeadersParsingStage.END_OF_HEADERS;
												}
												break;
												
											default:
												// Execution flow should never reach this default
												// case
												throw new RuntimeException("Internal parser error");
										}
									}
								}
							}
							break;
						}
						else {
							System.err
								.println("ERROR: REVIEW_HEADER element is missing, or does not match expected format.");
						}
						
					case ON_SPOILER_TAG:
						
						// Next parsing stage is ON_REVIEW_CONTENT. If a SPOILER_TAG is not
						// encountered
						// at this parsing stage, don't break. Move on to next stage
						// immediately.
						stage = DocumentParsingStage.ON_REVIEW_CONTENT;
						
						if (n.getNodeName().equals("P") && n.getFirstChild() != null
								&& n.getFirstChild().getNodeName().equals("B")
								&& n.getFirstChild().getFirstChild().getNodeValue().endsWith("spoilers ***")) {
							// Process spoiler tag
							currentReview.setSpoilers(true);
							break;
						}
						
					case ON_REVIEW_CONTENT:
						
						// Next parsing stage is ON_REVIEW_FOOTER.
						stage = DocumentParsingStage.ON_REVIEW_FOOTER;
						
						if (n.getNodeName().equals("P")) {
							if (n.getFirstChild() == null) {
								// In case of empty paragraph, stay on the same stage
								stage = DocumentParsingStage.ON_REVIEW_CONTENT;
							}
							else {
								// Process paragraph
								Node paragraph = n.getFirstChild();
								while ( paragraph != null ) {
									if (paragraph.getNodeType() == Node.TEXT_NODE) {
										// Merge lines, filter extra whitespace on first line
										String ptext = paragraph.getNodeValue().replace('\n', ' ').trim();
										
										// Insert paragraph into review text
										currentReview.insertParagraph(ptext);
									}
									paragraph = paragraph.getNextSibling();
								}
							}
							break;
						}
						else {
							System.err
							.println("ERROR: REVIEW_CONTENT is missing, or does not match expected format.");
						}
						
					case ON_REVIEW_FOOTER:
						if (n.getNodeName().equals("DIV")
								&& n.getAttributes().getNamedItem("class").getNodeValue().equals("yn")) {
							// Bypass feedback form
							stage = DocumentParsingStage.END_OF_REVIEW;
						}
						else {
							System.err
							.println("ERROR: REVIEW_FOOTER is missing, or does not match expected format.");
						}
						break;
						
					case END_OF_REVIEW:
						if (n.getNodeName().equals("HR")
								&& n.getAttributes().getLength() > 2) {
							currentDocument.insertReview(currentReview);
							stage = DocumentParsingStage.START_OF_REVIEW;
						}
						break;
						
					default:
						break;
				}
			}
		}
		
		return currentDocument;
	}
}
