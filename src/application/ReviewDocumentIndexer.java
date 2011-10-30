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

import indexing.ReviewTextAnalyzer;
import indexing.SynsetTermsAggregator;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.logging.Level;
import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import topics.TokenListsCollector;
import util.AppLogger;
import util.State;
import classes.DatabaseReview;
import classes.DatabaseReviewCollection;
import classes.Review;
import classes.ReviewId;
import classes.ReviewStats;
import config.Paths;


/**
 * TODO Description missing
 * @author Stelios Karabasakis
 */
public class ReviewDocumentIndexer {
	
	/**
	 * Default configuration options used by {@link ReviewDocumentIndexer}
	 * 
	 * @author Stelios Karabasakis
	 */
	public static final class Config {

		/** Maximum number of docs per index segment */
		public static int		maxMergeDocs	= 10000;
		/** Minimum number of index segments required before merging takes place */
		public static int		mergeFactor		= 10;
	}
	
	/**
	 * Command line help text. Use "--help" option to display
	 */
	private static String	command_help	= "ReviewDocumentIndexer: Generate a Lucene-based lemma index for a set of text reviews in the database\n\n"
												+ "ReviewDocumentIndexer [options]\n\n"
												+ "--new            \tStarts indexing from the beginning. Previous index will be overwritten.\n"
												+ "--resume         \tResumes indexing from the point it was last stopped\n"
																	+ "--update         \tResumes indexing from the beginning of the database\n"
												+ "--restore        \tRestores index from the latest backup copy, then resumes indexing\n"
												+ "--stop-after {n} \tSave state, backup index and exit after processing {n} reviews\n"
												+ "--pause-every {n}\tPause indexing and save state every {n} reviews";

	// private Document reviewDocument = null;
	// private Analyzer reviewDocumentAnalyzer = null;
	


	public ReviewId							theReviewId			= new ReviewId(0);
	public ReviewStats						theStats			= new ReviewStats();
	public TokenListsCollector				theTokenLists		= new TokenListsCollector();
	public SynsetTermsAggregator			theSynsets			= new SynsetTermsAggregator();
	private State<ReviewId>					reviewId_state		= null;
	private State<ReviewStats>				stats_state			= null;
	private State<TokenListsCollector>		tokenlists_state	= null;
	private State<SynsetTermsAggregator>	synsets_state		= null;
	
	private boolean							new_index			= false;
	private int								min_reviewid		= 0;
	private int								stop_after			= 0;
	private int								pause_every			= 10000;

	public ReviewDocumentIndexer()
	{
	}

	/**
	 * Constructor for class ReviewDocumentIndexer
	 * 
	 * @param args
	 */
	public ReviewDocumentIndexer(String[] args)
	{
		// Initialize state objects. These are used for persistence between runs of the indexer
		reviewId_state = new State<ReviewId>("reviewid", theReviewId);
		stats_state = new State<ReviewStats>("stats", theStats);
		tokenlists_state = new State<TokenListsCollector>("sentences", theTokenLists);
		synsets_state = new State<SynsetTermsAggregator>("synsets", theSynsets);
		
		// Process command line arguments
		try {
			setArgs(args);
		} catch ( RuntimeException e ) {
			AppLogger.error.log(Level.INFO, command_help);
		}

	}
	
	public void backupIndex()
	{
		// Making a backup of the lucene index
		File luceneIndex = new File(Paths.luceneIndex);
		File luceneBackup = new File(Paths.luceneBackupIndex);
		File indexerState = new File(Paths.stateFiles);
		File indexerStateBackup = new File(Paths.backupStateFiles);

		if (luceneIndex.exists() && indexerState.exists()) {
			State.backupDirectoryTree(indexerState, indexerStateBackup);
			State.backupDirectoryTree(luceneIndex, luceneBackup);
		}
	}
	

	public void restoreIndex() throws IOException
	{
		// Copy backup files back to index location
		File luceneIndex = new File(Paths.luceneIndex);
		File luceneBackup = new File(Paths.luceneBackupIndex);
		File indexerState = new File(Paths.stateFiles);
		File indexerStateBackup = new File(Paths.backupStateFiles);

		if (luceneBackup.exists() && indexerStateBackup.exists()) {
			State.backupDirectoryTree(luceneBackup, luceneIndex);
			State.backupDirectoryTree(indexerStateBackup, indexerState);
		}
		else
			throw new IOException("Index backup files and/or associated state files not found");
	}
	
	public void restoreState() throws IOException
	{
		// Deserializing objects
		theReviewId = reviewId_state.restoreState();
		theStats = stats_state.restoreState();
		theTokenLists = tokenlists_state.restoreState();
		theSynsets = synsets_state.restoreState();
	}
	
	public void saveState()
	{
		// updating object references
		reviewId_state.setObj(theReviewId);
		stats_state.setObj(theStats);
		tokenlists_state.setObj(theTokenLists);
		synsets_state.setObj(theSynsets);

		// Serializing objects
		reviewId_state.saveState();
		stats_state.saveState();
		tokenlists_state.saveState();
		synsets_state.saveState();
	}
	
	public void setCurrentReview(DatabaseReview r)
	{
		int review_id = r.getReviewid();
		int rating = r.getRating();

		theReviewId.set(review_id);
		theStats.setCurrent(review_id, rating);
	}
	
	private void setArgs(String[] args) throws RuntimeException
	{
		// Parse and process command line arguments
		for (String arg : args) {
			arg = arg.toLowerCase();
		}
		
		if (args.length == 1 || ArrayUtils.contains(args, "--help"))
			throw new RuntimeException("Command line syntax error");
		
		if (ArrayUtils.contains(args, "--new")) {
			min_reviewid = 0;
			new_index = true;
		}
		else if (ArrayUtils.contains(args, "--resume")) {
			try {
				restoreState();
				min_reviewid = theReviewId.get();
			} catch ( IOException e ) {
				AppLogger.error.log(Level.SEVERE,
									"Cannot restore indexer state. Some files are missing or are unreadable.");
			}
		}
		else if (ArrayUtils.contains(args, "--update")) {
			try {
				restoreState();
				min_reviewid = 0;
			} catch ( IOException e ) {
				AppLogger.error.log(Level.SEVERE,
									"Cannot restore indexer state. Some files are missing or are unreadable.");
			}
		}
		else if (ArrayUtils.contains(args, "--restore")) {
			try {
				restoreIndex();
				restoreState();
				min_reviewid = theReviewId.get();
			} catch ( IOException e ) {
				AppLogger.error.log(Level.SEVERE,
									"Cannot restore index from backup. Some files are missing or are unreadable.");
			}
		}
		else
			throw new RuntimeException("Command line syntax error");
		
		int pos = -1;
		try {
			if ((pos = ArrayUtils.indexOf(args, "--stop-after")) != ArrayUtils.INDEX_NOT_FOUND) {
				stop_after = min_reviewid + Integer.parseInt(args[pos + 1]);
			}
			if ((pos = ArrayUtils.indexOf(args, "--pause-every")) != ArrayUtils.INDEX_NOT_FOUND) {
				pause_every = Integer.parseInt(args[pos + 1]);
			}
		} catch ( ArrayIndexOutOfBoundsException e ) {
			throw new RuntimeException("Command line syntax error");
		}
	}
	
	/**
	 * @param args
	 */
	@SuppressWarnings("deprecation")
	public static void main(String[] args)
	{
		// Parse command line arguments. Exit program is provided arguments are insufficient
		ReviewDocumentIndexer indexer = new ReviewDocumentIndexer(args);
		if (indexer == null)
			return;

		// Open a new index
		IndexWriter index = null;
		try {
			index = new IndexWriter(new SimpleFSDirectory(new File(Paths.luceneIndex)),
					new ReviewTextAnalyzer(indexer), indexer.new_index ? true : false, MaxFieldLength.UNLIMITED);
			if (indexer.pause_every > 2) {
				index.setMaxBufferedDocs(indexer.pause_every);
			}
			index.setMaxMergeDocs(Config.maxMergeDocs);
			index.setMergeFactor(Config.mergeFactor);
		} catch ( CorruptIndexException e ) {
			AppLogger.error.log(Level.SEVERE, "Lucene detected an inconsistency upon opening the index located at "
				+ Paths.luceneIndex);
			throw new RuntimeException("Exiting application", e);
		} catch ( LockObtainFailedException e ) {
			AppLogger.error.log(Level.SEVERE, "Index located at " + Paths.luceneIndex
				+ " is already open by another Lucene process");
			throw new RuntimeException("Exiting application", e);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Could not access location " + Paths.luceneIndex);
			throw new RuntimeException("Exiting application", e);
		}
		
		// Load a number of reviews from database
		NumberFormat docIdFormat = TokenListsCollector.defaultDocIdFormat();
		try {
			DatabaseReviewCollection reviews = new DatabaseReviewCollection(indexer.pause_every);
			reviews.setLimits(indexer.min_reviewid, indexer.stop_after);
			int indexed_counter = 0;

			while (reviews.hasNextSegment()) {
				
				System.out.print(Calendar.getInstance().getTime().toGMTString());
				
				System.out.print(" Loading from DB... ");
				reviews.loadNextSegment();
				Iterator<Review> reviewsIterator = reviews.getIterator();
				
				System.out.print(" Indexing... ");
				while(reviewsIterator.hasNext()) {
					DatabaseReview dbr = (DatabaseReview)reviewsIterator.next();
					int dbr_id = dbr.getReviewid();
					int dbr_rating = dbr.getRating();

					try {
						indexer.theReviewId.set(dbr_id);
						indexer.theStats.setCurrent(dbr_id, dbr_rating);

						index.addDocument(dbr.getDocumentForIndexing());
						indexed_counter++;
						
						// Also, keep track of the rating and length of this review
						indexer.theStats.storeCurrent();
						
					} catch ( CorruptIndexException e ) {
						AppLogger.error.log(Level.SEVERE, "Lucene detected an inconsistency upon saving review #"
							+ Integer.toString(dbr.getReviewid()) + "to the index located at " + Paths.luceneIndex);
						return;
					} catch ( IOException e ) {
						AppLogger.error.log(Level.WARNING, "Review #" + Integer.toString(dbr.getReviewid())
							+ " could not be indexed");
					}
				}
				
				// Backup everything
				System.out.print("Indexed " + indexed_counter + " reviews total. ");
				if (indexer.pause_every > 0) {
					System.out.print("Saving tokenlists... ");
					indexer.theTokenLists.writeNextFile(docIdFormat);

					System.out.print("Saving state... ");
					try {
						index.commit();
						indexer.saveState();
					} catch ( CorruptIndexException e ) {
						AppLogger.error.log(Level.SEVERE, "Committing index changes failed on review #"
							+ indexer.theReviewId.get() + "due to CorruptIndexException");
						return;
					} catch ( IOException e ) {
						AppLogger.error.log(Level.WARNING, "Committing index changes failed on review #"
							+ indexer.theReviewId.get() + "due to IOException");
					}
				}
				System.out.print("DONE\n");
				
				reviews.reset();
			}
		} catch ( SQLException e ) {
			AppLogger.error.log(Level.SEVERE, "An exception occured while trying to access the database.\n"
				+ e.getMessage());
			return;
		}
		
		try {
			index.close();
			indexer.backupIndex();
		} catch ( CorruptIndexException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.err.println("Indexing successfully completed!");
		return;
	}

}
