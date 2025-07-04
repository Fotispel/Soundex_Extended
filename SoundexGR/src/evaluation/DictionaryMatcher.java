/**
 *
 */
package evaluation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import client.Dashboard;
import utils.*;

import SoundexGR.SoundexGRExtra;

/**
 * @author Yannis Tzitzikas (yannistzitzik@gmail.com)
 */
public class DictionaryMatcher {
    public static Set<String> rankedWords = new HashSet<>();
    public static String FirstMatch = "";
    private static boolean FirstMatchFound = false;


    /**
     * Lookups a work in the dictionary
     *
     * @param word
     * @return
     */
    static boolean lookup(String word) {
        return DictionaryBasedMeasurements.lookup(word);
    }


    /**
     * finds those words with edit distance less than k
     *
     * @param word
     * @param K
     * @return
     */
    public static Set<String> getDicWordByEditDist(String word, int K) {
        Set<String> res = new HashSet<>();
        Set<String> dwords = DictionaryBasedMeasurements.getWords();
        //System.out.println("******"+ dwords.size());
        for (String dword : dwords) {
            if (EditDistance.EditDistDP(word, dword) <= K) {
                res.add(dword);
                //System.out.print("+");
            }
        }
        return res;
    }

    /**
     * Ranks a set of word by their edit distance withe one word
     *
     * @param word
     * @param wordsToRank
     * @return
     */
    public static Map<String, Integer> RankByEditDistance(String word, Set<String> wordsToRank) {
        Map<String, Integer> wordsAndDists = new HashMap<>();
        for (String candidateword : wordsToRank) {
            wordsAndDists.put(candidateword, EditDistance.EditDistDP(word, candidateword));
        }

        // sort the map wrt to the value in increasing order
        Map<String, Integer> sorted = wordsAndDists
                .entrySet()
                .stream()
                //.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e2, LinkedHashMap::new));


        if (!sorted.isEmpty() && !FirstMatchFound) {
            FirstMatch = sorted.keySet().iterator().next();
            FirstMatchFound = true;
        } else if (!FirstMatchFound) // if the map is empty
            FirstMatch = "";

        return sorted;

    }


    /**
     * Returns a string with the matching of a word, by using code length as defined by the parameter
     *
     * @param word
     * @param codeLength the code length to be used
     * @return a verbose string with the results of the matchings
     */

    public static String getMatchings(String word, int codeLength, boolean isRunningDemo) {
        FirstMatchFound = false;

        String currentDir = System.getProperty("user.dir");
        String dictResourcePlace;
        if (!isRunningDemo)
            dictResourcePlace = currentDir + "\\Resources\\collection_words\\" + Dashboard.getSelectedDatasetFile() + "_words.txt";
        else {
            dictResourcePlace = currentDir + "\\Resources\\dictionaries\\EN-winedt\\gr.dic";
            //SoundexGRExtra.LengthEncoding = 8;
            //System.out.println("Default code length for demo: " + SoundexGRExtra.LengthEncoding);
        }

        DictionaryBasedMeasurements.setDictionaryLocation(dictResourcePlace);

        String output = "";
        //A. Check if word exists in the dictionary
        if (lookup(word)) { // if it exists
            FirstMatch = word;
            FirstMatchFound = true;
            return "The word \"" + word + "\" exists in the dictionary.";
        } else {
            output = "APPROXIMATE MATCHES FOR " + word + "\n";
        }

        //B1. Code length
        //TODO: An den brei kamia na meiwnei to len na kanei null to map kai na ksanaprospathei ews otou exei estw mia

        //int nwordsFound = 0;
        //for (int codeLen=12;  codeLen>=4;  codeLen--)  {

        codeLength = DictionaryBasedMeasurements.calculateSuggestedCodeLen(Dashboard.getSelectedMethod());
        SoundexGRExtra.LengthEncoding = codeLength;
        String wcode = SoundexGRExtra.encode(word);
        //System.out.println("Code length: " + codeLength + " for word: " + word + " with code: " + wcode);

        String path_to_selected_dataset = "\\Resources\\collection_words\\" + Dashboard.getSelectedDatasetFile() + "_words.txt";
        Set<String> wordsHavingTheSameCode = DictionaryBasedMeasurements.returnWordsHavingTheSameCode(wcode, path_to_selected_dataset);
        ArrayList<String> matches = new ArrayList<>();
        if (wordsHavingTheSameCode != null) {
            for (String m : wordsHavingTheSameCode) {
                matches.add(m);
            }

            output += "* Approximate Matches (words having the same SoundexGR codes with \"" + word + "\" with code length = " + codeLength + "):";
            output += matches.size() + " matches" + "\n";
            output += matches.toString() + "\n";
            // edit distance-base ranking
            output += "\n* Ranking of the above " + matches.size() + " words wrt Edit distance (showing the Edit Dist of each word):\n";

            output += RankByEditDistance(word, wordsHavingTheSameCode).toString();
            output += "\n\n";
            // by edit dist directly from the dictionary

        } else {
            output += "No word with the same SoundexGR code was found for this word. Try reducing the code length :(\n";
        }

        // Edit Distance based:
        int K = 3;
        output += "* Approximate Matches directly from the Dictionary ordered by Edit distance (less than " + K + "):";
        Set<String> matchesByED = getDicWordByEditDist(word, K);
        output += matchesByED.size() + " matches\n";
        //System.out.println("\n>>>"+getDicWordByEditDist(word,K));
        String ranked = RankByEditDistance(word, matchesByED).toString();
        output += ranked;
        rankedWords = new HashSet<>(RankByEditDistance(word, matchesByED).keySet().stream().limit(10).collect(Collectors.toList()));
        output += "\n";


        return output;

		/*
		if (wordsHavingTheSameCode!=null) {
			for (String w: wordsHavingTheSameCode) {
				output+= " " + w;
				//System.out.println(output);
			}
			output+="\n";
		} else output +="No word with tha code was found.\n";

		retun lala
		*/
    }

    public static void main(String[] lala) {

        String[] exampleWords = {"Γιάννης", "μύνημα", "διάλιμα"};

        for (int i = 0; i < exampleWords.length; i++) {
            String ex = exampleWords[i];
            String m = getMatchings(ex, 12, false);
            System.out.println(ex + "\t: " + m);
        }
    }

}
