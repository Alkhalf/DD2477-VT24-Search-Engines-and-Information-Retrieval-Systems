/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Map;
import java.nio.charset.*;
import java.io.*;

/**
 * A class for representing a query as a list of words, each of which has
 * an associated weight.
 */
public class Query {

    /**
     * Help class to represent one query term, with its associated weight.
     */
    class QueryTerm {
        String term;
        double weight;

        QueryTerm(String t, double w) {
            term = t;
            weight = w;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }

    /**
     * Representation of the query as a list of terms with associated weights.
     * In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**
     * Relevance feedback constant alpha (= weight of original query terms).
     * Should be between 0 and 1.
     * (only used in assignment 3).
     */
    double alpha = 0.2;

    /**
     * Relevance feedback constant beta (= weight of query terms obtained by
     * feedback from the user).
     * (only used in assignment 3).
     */
    double beta = 1 - alpha;

    /**
     * Creates a new empty Query
     */
    public Query() {
    }

    /**
     * Creates a new Query from a string of words
     */
    public Query(String queryString) {
        StringTokenizer tok = new StringTokenizer(queryString);
        while (tok.hasMoreTokens()) {
            queryterm.add(new QueryTerm(tok.nextToken(), 1.0));
        }
    }

    /**
     * Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }

    /**
     * Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for (QueryTerm t : queryterm) {
            len += t.weight;
        }
        return len;
    }

    /**
     * Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for (QueryTerm t : queryterm) {
            queryCopy.queryterm.add(new QueryTerm(t.term, t.weight));
        }
        return queryCopy;
    }

    /**
     * Expands the Query using Relevance Feedback
     *
     * @param results       The results of the previous query.
     * @param docIsRelevant A boolean array representing which query results the
     *                      user deemed relevant.
     * @param engine        The search engine object
     */

    public void relevanceFeedback(PostingsList results, boolean[] docIsRelevant, Engine engine) {
        HashMap<String, Double> updatedWeights = new HashMap<>();
        HashMap<String, Integer> termFreqs = new HashMap<>();

        int numOfRelevantDocs = 0;
        for (boolean isRelevant : docIsRelevant) {
            if (isRelevant) {
                numOfRelevantDocs++;
            }
        }

        // Scale down the weights of the original query terms by alpha
        for (QueryTerm term : queryterm) {
            updatedWeights.put(term.term, term.weight * alpha);

        }

        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                PostingsEntry pe = results.get(i);
                int docID = pe.docID;
                String docPath = engine.index.docNames.get(docID);

                if (docPath != null) {
                    try {
                        File file = new File(docPath);
                        Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                        Tokenizer tok = new Tokenizer(reader, true, false, true, engine.patterns_file);
                        while (tok.hasMoreTokens()) {
                            String token = tok.nextToken();
                            int dd = termFreqs.getOrDefault(token, 0);

                            if (dd != 0) {
                                termFreqs.put(token, termFreqs.get(token) + 1);
                            } else {
                                termFreqs.put(token, 1);
                            }
                        }
                        reader.close();

                        // Update query terms based on the tokenized content
                        for (Map.Entry<String, Integer> entry : termFreqs.entrySet()) {
                            String term = entry.getKey();
                            int tf = entry.getValue();

                            double termWeight = (beta * tf) / numOfRelevantDocs;

                            if (updatedWeights.containsKey(term)) {
                                double currentWeight = updatedWeights.get(term);
                                updatedWeights.put(term, (currentWeight) + termWeight);
                            } else {
                                updatedWeights.put(term, termWeight);
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + docPath);
                    }
                }
            }
        }

        // Update the query terms with the new weights

        queryterm.clear();
        for (Map.Entry<String, Double> q : updatedWeights.entrySet()) {
            queryterm.add(new QueryTerm(q.getKey(), q.getValue()));

        }

    }
}
