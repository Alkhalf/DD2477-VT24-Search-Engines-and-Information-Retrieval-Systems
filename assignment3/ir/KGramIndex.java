/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer, String> id2term = new HashMap<Integer, String>();

    /** Mapping from term strings to term ids */
    HashMap<String, Integer> term2id = new HashMap<String, Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String, List<KGramPostingsEntry>> index = new HashMap<String, List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 2;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }

    /**
     * Get intersection of two postings lists
     */
    public List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {

        List<KGramPostingsEntry> result = new ArrayList<>();
        int index1 = 0, index2 = 0;

        if (p1.isEmpty() || p2.isEmpty())
            return result;

        KGramPostingsEntry entry1 = p1.get(index1), entry2 = p2.get(index2);

        while (true) {
            int id1 = entry1.tokenID, id2 = entry2.tokenID;
            if (id1 < id2) {
                // Advance index1 if not at the end
                if (index1 + 1 < p1.size())
                    entry1 = p1.get(++index1);
                else
                    break;
            } else if (id1 == id2) {
                result.add(entry1);
                // Advance both indexes if not at the end
                if (index1 + 1 < p1.size() && index2 + 1 < p2.size()) {
                    entry1 = p1.get(++index1);
                    entry2 = p2.get(++index2);
                } else
                    break;
            } else {
                // Advance index2 if not at the end
                if (index2 + 1 < p2.size())
                    entry2 = p2.get(++index2);
                else
                    break;
            }
        }
        return result;

    }

    /** Inserts all k-grams from a token into the index. */
    public void insert(String token) {

        //
        // YOUR CODE HERE
        //
        if (getIDByTerm(token) != null) {
            return;
        }
        int newid = generateTermID();
        term2id.put(token, newid);
        id2term.put(newid, token);
        int kgramNum = token.length() + 3 - getK();
        KGramPostingsEntry a = new KGramPostingsEntry(newid);

        String kgrams;
        String newToken = "^" + token + "$";

        for (int i = 0; i < kgramNum; i++) {
            kgrams = newToken.substring(i, i + getK());

            if (!index.containsKey(kgrams)) {
                index.put(kgrams, new ArrayList<KGramPostingsEntry>());
            }
            if (!index.get(kgrams).contains(a)) {
                index.get(kgrams).add(a);
            }

        }

    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        //
        // YOUR CODE HERE
        //
        if (index.containsKey(kgram)) {
            return index.get(kgram);
        } else {
            return null;
        }
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    private static HashMap<String, String> decodeArgs(String[] args) {
        HashMap<String, String> decodedArgs = new HashMap<String, String>();
        int i = 0, j = 0;
        while (i < args.length) {
            if ("-p".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ("-f".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ("-k".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ("-kg".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println("Unknown option: " + args[i]);
                break;
            }
        }
        return decodedArgs;
    }

    public int searchK(String str) {
        String[] kgrams = str.split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != getK()) {
                System.err.println(
                        "Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + getK()
                                + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = getPostings(kgram);
            } else {
                postings = intersect(postings, getPostings(kgram));
            }
        }
        return postings.size();
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String, String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
        Tokenizer tok = new Tokenizer(reader, true, false, true, args.get("patterns_file"));
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println(
                        "Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }
}
