/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class PostingsList {

    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();

    /** Number of postings in this list. */
    public int size() {
        return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    //
    // YOUR CODE HERE
    //

    public ArrayList<PostingsEntry> getPostingEntry() {
        return list;
    }

    private HashMap<Integer, PostingsEntry> hashMap = new HashMap<>();

    public PostingsEntry searchEntry(int docID) {
        /*
         * for (PostingsEntry pe : list) {
         * if (pe.docID == docID) {
         * return pe;
         * }
         * }
         * return null;
         */
        return hashMap.get(docID);

    }

    public void addPostingEntry(PostingsEntry postingsEntry) {
        list.add(postingsEntry);
        hashMap.put(postingsEntry.docID, postingsEntry);
    }

    public void sortScore() {
        Collections.sort(list);
    }

    public void deduplication() {

        for (int i = 0; i < list.size() - 1; i++) {
            if (list.get(i).docID == list.get(i + 1).docID) {
                list.remove(i);
                i--;
            }
        }
    }
}
