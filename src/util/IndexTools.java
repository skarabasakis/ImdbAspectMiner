// The MIT License
//
// Copyright (c) 2011 Stelios Karabasakis
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
package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import application.ReviewDocumentIndexer.Config;
import classes.Counter;
import classes.ReviewId;
import config.Paths;


/**
 * TODO Description missing
 * 
 * @author Stelios Karabasakis
 */
public class IndexTools {
	
	private IndexReader		ir	= null;
	private IndexSearcher	is	= null;

	/**
	 * Constructor for class IndexTools
	 */
	public IndexTools()
	{
		try {
			ir = IndexReader.open(new SimpleFSDirectory(new File(Paths.luceneIndex)), false);
			is = new IndexSearcher(ir);
		} catch ( CorruptIndexException e ) {
			AppLogger.error.log(Level.SEVERE, "Lucene detected an inconsistency upon opening the index located at "
				+ Paths.luceneIndex);
			throw new RuntimeException("Exiting application", e);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Could not access location " + Paths.luceneIndex);
			throw new RuntimeException("Exiting application", e);
		}
	}
	
	public IndexTools(IndexWriter indexWriter)
	{
		try {
			ir = indexWriter.getReader();
			is = new IndexSearcher(ir);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Could not access index at location " + indexWriter.getDirectory()
				+ " for reading");
			throw new RuntimeException("Exiting application", e);
		}
	}
	
	public static void mergeIndexes(String destIndexPath, String[] srcIndexPaths)
	{
		
		// Open destination index
		IndexWriter index = null;
		try {
			index = new IndexWriter(new SimpleFSDirectory(new File(destIndexPath)), new KeywordAnalyzer(), true,
					MaxFieldLength.UNLIMITED);
			index.setMaxMergeDocs(Config.maxMergeDocs);
			index.setMergeFactor(Config.mergeFactor);
		} catch ( CorruptIndexException e ) {
			AppLogger.error.log(Level.SEVERE, "Lucene detected an inconsistency upon opening the index located at "
				+ destIndexPath);
			throw new RuntimeException("Exiting application", e);
		} catch ( LockObtainFailedException e ) {
			AppLogger.error.log(Level.SEVERE, "Index located at " + destIndexPath
				+ " is already open by another Lucene process");
			throw new RuntimeException("Exiting application", e);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Could not access location " + destIndexPath);
			throw new RuntimeException("Exiting application", e);
		}
		
		// Open scource indices
		String currentSrcPath = "";
		try {
			for (String srcIndexPath : srcIndexPaths) {
				currentSrcPath = srcIndexPath;
				SimpleFSDirectory src = new SimpleFSDirectory(new File(srcIndexPath));
				System.out.println("Merging index " + currentSrcPath);
				index.addIndexesNoOptimize(src);
			}
			index.optimize();
			index.close();
		} catch ( CorruptIndexException e ) {
			AppLogger.error.log(Level.SEVERE, "Lucene detected an inconsistency upon opening index located at "
				+ currentSrcPath + " for merging");
			throw new RuntimeException("Exiting application", e);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "I/O error while merging indexes");
			throw new RuntimeException("Exiting application", e);
		}
	}
	
	public HashSet<Integer> removeDuplicatesByReviewId()
	{

		String field = "reviewid";
		HashSet<Integer> encounteredReviewIds = new HashSet<Integer>();
		HashSet<Integer> duplicateReviewIds = new HashSet<Integer>();
		Counter duplicates = new Counter();
		
		// Go through all documents in the index and retrieve reviewId value
		int ndocs = ir.maxDoc();
		for (int i = 0 ; i < ndocs ; i++) {
			try {
				Document doc = ir.document(i);
				Integer reviewid = Integer.parseInt(doc.get(field));
				if (encounteredReviewIds.contains(reviewid)) {
					// ir.deleteDocument(i);
					duplicateReviewIds.add(reviewid);
					duplicates.increment();
					System.out.println("Document was deleted as duplicate: " + i);
				}
				else {
					encounteredReviewIds.add(reviewid);
				}
			} catch ( CorruptIndexException e ) {
				AppLogger.error.log(Level.SEVERE, "Lucene detected an inconsistency upon retrieving document " + i);
				throw new RuntimeException("Exiting application", e);
			} catch ( IOException e ) {
				AppLogger.error.log(Level.SEVERE, "I/O error upon retrieving document " + i);
			}
		}
		
		System.out.println("Deleted a total of " + duplicates.get() + " duplicate documents.");
		return duplicateReviewIds;
	}

	public Integer[] getReview(ReviewId id)
	{
		ArrayList<Integer> docs = new ArrayList<Integer>();
		String field = "reviewid";
		try {
			int maxdocs = ir.maxDoc();
			for (int doc = 0 ; doc < maxdocs ; doc++) {
				String reviewid = ir.document(doc).get(field);
				if (Integer.parseInt(reviewid) == id.get()) {
					docs.add(doc);
				}
			}
		} catch ( CorruptIndexException e ) {
			AppLogger.error.log(Level.SEVERE, "Lucene detected an inconsistency upon opening the index located at "
				+ Paths.luceneIndex);
			throw new RuntimeException("Exiting application", e);
		} catch ( IOException e ) {
			AppLogger.error.log(Level.SEVERE, "Could not access location " + Paths.luceneIndex);
			throw new RuntimeException("Exiting application", e);
		}
		

		if (docs.isEmpty())
			return null;
		else
			return docs.toArray(new Integer[1]);
	}
	
	public static void main(String[] args)
	{
		/*
		 * String root_path = "C:\\Users\\Skarab\\Documents\\Code\\Java\\AspectMiner\\"; String[]
		 * srcIndexPaths = { root_path + "output_1\\reviewindex\\", root_path +
		 * "output_2\\reviewindex\\" }; IndexTools.mergeIndexes(Paths.luceneIndex, srcIndexPaths);
		 * System.out.println("Merge complete!");
		 */

		IndexTools index = new IndexTools();
		HashSet<Integer> duplicates = index.removeDuplicatesByReviewId();
		for (Integer duplicate : duplicates) {
			System.out.print(duplicate + " : ");
			Integer[] duplicate_ids = index.getReview(new ReviewId(duplicate));
			for (int duplicate_id : duplicate_ids) {
				System.out.print(duplicate_id + " ");
			}
			System.out.println();
		}
	}
}
