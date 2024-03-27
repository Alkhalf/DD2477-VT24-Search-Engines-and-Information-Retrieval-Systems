/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;

    /**
     * PostingsEntries are compared by their score (only relevant
     * in ranked retrieval).
     *
     * The comparison is defined so that entries will be put in
     * descending order.
     */
    public int compareTo(PostingsEntry other) {
        return Double.compare(other.score, score);
    }

    //
    // YOUR CODE HERE
    //

    public ArrayList<Integer> offsetsList = new ArrayList<>();

    public PostingsEntry(int docID) {
        this.docID = docID;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public PostingsEntry(int docID, double score) {
        this.docID = docID;
        this.score = score;
    }

    public void addOffset(int offset) {
        offsetsList.add(offset);
    }

}
