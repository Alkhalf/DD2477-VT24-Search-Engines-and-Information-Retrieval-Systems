/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {

    /** The index as a hashtable. */
    private HashMap<String, PostingsList> index = new HashMap<String, PostingsList>();

    /**
     * Inserts this token in the hashtable.
     */
    public void insert(String token, int docID, int offset) {
        //
        // YOUR CODE HERE
        //
        if (index.containsKey(token)) {
            PostingsList postingList = index.get(token);
            PostingsEntry postingsEntry = postingList.searchEntry(docID);

            if (postingsEntry == null) {
                PostingsEntry newEntry = new PostingsEntry(docID);
                newEntry.addOffset(offset);
                postingList.addPostingEntry(newEntry);

            } else {

                postingsEntry.addOffset(offset);
            }
        }
        if (!index.containsKey(token)) {
            PostingsEntry entry = new PostingsEntry(docID);
            PostingsList postingList = new PostingsList();
            postingList.addPostingEntry(entry);
            entry.addOffset(offset);
            index.put(token, postingList);
        }
    }

    /**
     * Returns the postings for a specific term, or null
     * if the term is not in the index.
     */
    public PostingsList getPostings(String token) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        PostingsList postingList = null;
        if (index.containsKey(token)) {
            postingList = index.get(token);
        }
        return postingList;

    }

    /**
     * No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
