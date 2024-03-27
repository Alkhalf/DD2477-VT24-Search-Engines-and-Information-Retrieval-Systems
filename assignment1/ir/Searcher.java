/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.io.BufferedReader;

import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import ir.Query.QueryTerm;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    // name , score of document
    HashMap<String, Double> docPageRanks = new HashMap<>();

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
        loadPageRanks("/Users/ahmed/Desktop/DD2477/assignment2/pagerank/pagerank_scores.txt");
    }

    private PostingsList intersect(PostingsList pos1, PostingsList pos2) {

        PostingsList resultPostings = new PostingsList();

        ArrayList<PostingsEntry> entries1 = pos1.getPostingEntry();
        ArrayList<PostingsEntry> entries2 = pos2.getPostingEntry();

        int pointer1 = 0;
        int pointer2 = 0;

        while (pointer1 < entries1.size() && pointer2 < entries2.size()) {
            PostingsEntry entry1 = entries1.get(pointer1);
            PostingsEntry entry2 = entries2.get(pointer2);

            if (entry1.docID == entry2.docID) {
                // If docIDs match, add to the result
                resultPostings.addPostingEntry(entry1);
                pointer1++;
                pointer2++;
            } else if (entry1.docID < entry2.docID) {
                // If docID in postings1 is smaller, move pointer1
                pointer1++;
            } else {
                // If docID in postings2 is smaller, move pointer2
                pointer2++;
            }
        }

        return resultPostings;
    }

    private PostingsList intersectPostingsWithPositions(PostingsList postings1, PostingsList postings2) {
        // Implement the intersection logic for postings with positions
        // Iterate over the postings and keep only those that appear in both lists with
        // the correct positions

        PostingsList resultPostings = new PostingsList();

        ArrayList<PostingsEntry> entries1 = postings1.getPostingEntry();
        ArrayList<PostingsEntry> entries2 = postings2.getPostingEntry();

        int pointer1 = 0;
        int pointer2 = 0;

        while (pointer1 < entries1.size() && pointer2 < entries2.size()) {
            PostingsEntry entry1 = entries1.get(pointer1);
            PostingsEntry entry2 = entries2.get(pointer2);

            if (entry1.docID == entry2.docID) {
                // If docIDs match, check for phrase positions
                ArrayList<Integer> positions1 = entry1.offsetsList;
                ArrayList<Integer> positions2 = entry2.offsetsList;

                ArrayList<Integer> phrasePositions = findPhrasePositions(positions1, positions2);

                if (!phrasePositions.isEmpty()) {
                    // If phrase positions found, add to the result
                    PostingsEntry resultEntry = new PostingsEntry(entry1.docID);
                    resultEntry.offsetsList = phrasePositions;
                    resultPostings.addPostingEntry(resultEntry);
                }

                pointer1++;
                pointer2++;
            } else if (entry1.docID < entry2.docID) {
                pointer1++;
            } else {
                pointer2++;
            }
        }

        return resultPostings;
    }

    private ArrayList<Integer> findPhrasePositions(ArrayList<Integer> positions1, ArrayList<Integer> positions2) {

        ArrayList<Integer> result = new ArrayList<>();
        int i = 0;
        int j = 0;

        while (i < positions1.size() && j < positions2.size()) {
            int position1 = positions1.get(i);
            int position2 = positions2.get(j);

            if (position2 - position1 == 1) {
                // If positions are adjacent, consider it as part of the phrase
                result.add(position2);
                i++;
                j++;
            } else if (position1 < position2) {
                i++;
            } else {
                j++;
            }
        }

        return result;
    }

    private PostingsList scoreSorting(PostingsList pl, int N) {
        int df_t = pl.size(); // Documents in the corpus which contain t
        double idf_t = Math.log((double) N / df_t); // Calculate once outside the loop

        for (int i = 0; i < pl.size(); i++) {
            int tf_dt = pl.get(i).offsetsList.size(); // Occurrences of t in d
            int len_d = index.docLengths.get(pl.get(i).docID); // Words in d
            double tf_idf = (tf_dt * idf_t) / len_d; // Apply length normalization

            pl.get(i).score = tf_idf;
        }

        Collections.sort(pl.getPostingEntry()); // Ensure sorting by score
        return pl;

    }

    private void loadPageRanks(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String docName = parts[0]; // Use document name as the key
                    double score = Double.parseDouble(parts[1]);
                    docPageRanks.put(docName, score);
                }
            }
            reader.close();
        } catch (IOException e) {
            System.err.println("Error reading PageRank file: " + e.getMessage());
        }
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normType) {
        if (query.queryterm.size() >= 1 && (queryType == QueryType.RANKED_QUERY)
                && (rankingType == RankingType.COMBINATION)) {
            HashSet<Integer> processedDocIDs = new HashSet<>();
            int N = Index.docLengths.size();
            PostingsList resultPostingsList = new PostingsList();

            // Iterate through each query term
            for (QueryTerm qterm : query.queryterm) {
                String term = qterm.term;
                PostingsList termPostings = index.getPostings(term);

                if (termPostings != null) {
                    int df_t = termPostings.size();
                    double idf_t = Math.log((double) N / df_t);

                    for (PostingsEntry pe : termPostings.getPostingEntry()) {
                        if (!processedDocIDs.contains(pe.docID)) {
                            processedDocIDs.add(pe.docID);

                            String docName = index.docNames.get(pe.docID);
                            int lastSlashIndex = docName.lastIndexOf('/');
                            if (lastSlashIndex != -1) {
                                String fileName = docName.substring(lastSlashIndex + 1);
                                Double pageRankScore = docPageRanks.get(fileName);
                                int tf_dt = pe.offsetsList.size();
                                int len_d = index.docLengths.get(pe.docID);
                                double tfIdfScore = (tf_dt * idf_t) / len_d;
                                double alpha = 0.01;
                                double combinedScore = alpha * tfIdfScore + (1 - alpha) * pageRankScore;
                                PostingsEntry combinedEntry = resultPostingsList.searchEntry(pe.docID);
                                if (combinedEntry != null) {
                                    combinedEntry.score += combinedScore;
                                } else {
                                    resultPostingsList.addPostingEntry(new PostingsEntry(pe.docID, combinedScore));
                                }
                            }
                        }

                    }
                }
            }

            Collections.sort(resultPostingsList.getPostingEntry());

            return resultPostingsList;
        }

        else if (query.queryterm.size() > 1 && (queryType == QueryType.RANKED_QUERY)
                && (rankingType == RankingType.PAGERANK)) {
            PostingsList resultPostingsList = new PostingsList();
            HashSet<Integer> processedDocIDs = new HashSet<>();
            // int adjusmentTerm = 2000;
            for (QueryTerm qterm : query.queryterm) {
                String term = qterm.term;
                PostingsList termPostings = index.getPostings(term);

                if (termPostings != null) {
                    for (PostingsEntry pe : termPostings.getPostingEntry()) {
                        if (!processedDocIDs.contains(pe.docID)) {
                            processedDocIDs.add(pe.docID);
                            String docName = index.docNames.get(pe.docID);
                            int lastSlashIndex = docName.lastIndexOf('/');
                            if (lastSlashIndex != -1) {
                                String fileName = docName.substring(lastSlashIndex + 1);
                                Double pageRankScore = docPageRanks.get(fileName);
                                if (pageRankScore != null) {
                                    resultPostingsList.addPostingEntry(new PostingsEntry(pe.docID, pageRankScore));
                                }
                            }
                        }
                    }
                }
            }

            // Sort by PageRank score
            Collections.sort(resultPostingsList.getPostingEntry());

            return resultPostingsList;
        }

        // 2.2
        else if (query.queryterm.size() > 1 && (queryType == QueryType.RANKED_QUERY)
                && (rankingType == RankingType.TF_IDF)) {
            int N = Index.docLengths.size();
            String[] terms = new String[query.queryterm.size()];

            for (int i = 0; i < query.queryterm.size(); i++) {
                terms[i] = query.queryterm.get(i).term;
            }

            PostingsList resultPostingsList = new PostingsList();

            for (int j = 0; j < query.queryterm.size(); j++) {
                PostingsList wordPostingList = scoreSorting(index.getPostings(terms[j]), N);

                for (PostingsEntry pe : wordPostingList.getPostingEntry()) {
                    PostingsEntry entry = resultPostingsList.searchEntry(pe.docID);
                    if (entry != null) {
                        entry.score += pe.score;

                    } else {
                        resultPostingsList.addPostingEntry(new PostingsEntry(pe.docID, pe.score));

                    }
                }
            }

            Collections.sort(resultPostingsList.getPostingEntry());
            return resultPostingsList;
        }

        // for assignmnet 2.1
        else if (query.queryterm.size() == 1 && (queryType == QueryType.RANKED_QUERY)
                && (rankingType == RankingType.TF_IDF)) {

            String token = query.queryterm.get(0).term;
            PostingsList resultPostingsList = index.getPostings(token);
            // #documents in the corpus
            int N = index.docLengths.size();
            resultPostingsList = scoreSorting(resultPostingsList, N);
            // double idf = java.lang.Math.log(N / resultPostingsList.size());
            // DecimalFormat df = new DecimalFormat("#.####");

            // System.out.println(token + " idf: " + df.format(idf));
            return resultPostingsList;
        }
        // for assignment 1.3 where we have more then one word
        else if (query.queryterm.size() > 1 && queryType == QueryType.INTERSECTION_QUERY) {
            String[] terms = new String[query.queryterm.size()];
            PostingsList[] postingsListArray = new PostingsList[query.queryterm.size()];

            for (int i = 0; i < query.queryterm.size(); i++) {

                terms[i] = query.queryterm.get(i).term;
                postingsListArray[i] = new PostingsList();
                postingsListArray[i] = index.getPostings(terms[i]);

            }
            PostingsList resultPostings = index.getPostings(terms[0]);

            for (int i = 1; i < terms.length; i++) {
                PostingsList termPostings = index.getPostings(terms[i]);
                resultPostings = intersect(resultPostings, termPostings);
            }

            return resultPostings;

        }
        // for assignment 1.4
        else if (query.queryterm.size() > 1 && queryType == QueryType.PHRASE_QUERY) {
            String[] terms = new String[query.queryterm.size()];

            // Iterate over each query term
            for (int i = 0; i < query.queryterm.size(); i++) {
                terms[i] = query.queryterm.get(i).term;
            }

            PostingsList resultPostings = index.getPostings(terms[0]);

            // Iterate over the remaining terms
            for (int i = 1; i < terms.length; i++) {
                PostingsList termPostings = index.getPostings(terms[i]);
                resultPostings = intersectPostingsWithPositions(resultPostings, termPostings);
            }

            return resultPostings;
        }

        // for assignment 1.2 (one word)
        else {
            PostingsList postingsList = index.getPostings(query.queryterm.get(0).term);
            return postingsList;

        }
    }
}