/**
 *
 */
package evaluation;

import client.Dashboard;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import SoundexGR.SoundexGRExtra;
import stemmerWrapper.StemmerWrapper;
import evaluation.BulkCheck.*;

import static evaluation.BulkCheck.DocNames;
import static evaluation.BulkCheck.length_per_DocName;

/**
 * @author Yannis Tzitzikas (yannistzitzik@gmail.com)
 * <p>
 * It performs measurements over a dictionary, uses the dictionary for producing synthetic datasets for
 * evaluating the performance of matching algorithms
 */
public class DictionaryBasedMeasurements {
    public static long DatasetSize = 0; // the size of the dataset
    static private String placeDict = null;    // the path for locating the dictionary in the Resources
    static private BufferedReader readerDict = null;    // it is defined in this way for locating the dictionary within the IDE and in a packaged jar
    static Map<String, HashSet<String>> codesToWords = null;// map Codes->Words
    private static Set<String> wordsSet = null;    // the set of words of the dictionary if loaded

    public static long words_read = 0;

    public static void add_current_datasetSize(long counter) {
        DatasetSize += counter;
    }

    // todo: to complete the letters with διαλυτικά
    static char[] vowels = {'α', 'ε', 'η', 'υ', 'ι', 'ο', 'ω',
            'ά', 'έ', 'ή', 'ύ', 'ί', 'ό', 'ώ',
            'ϋ'
    };


    /**
     * Sets the location of the dictionary in a way that ensures readability (within an IDE and in a packaged jar)
     *
     * @param resourcePlace the resource place
     */
    public static void setDictionaryLocation(String resourcePlace) {
        placeDict = resourcePlace;
        try {
            FileInputStream inDict = new FileInputStream(placeDict);
            readerDict = new BufferedReader(new InputStreamReader(inDict, "UTF-8"));
        } catch (FileNotFoundException e) {
            System.err.println("Error: Dictionary file not found at " + placeDict);
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }


    /**
     * Checks if a char is a vowel of the Greek language
     *
     * @param c
     * @return
     */
    static boolean isVowel(char c) {
        for (char v : vowels) {
            if (c == v)
                return true;
        }
        return false;
    }


    /**
     * Prints the contents of a map String->Integer
     *
     * @param map
     */
    private static void printMap(Map<String, Integer> map) {
        System.out.println("Code: Frequency");
        for (String code : map.keySet()) {
            System.out.printf("%s: %d\n", code, map.get(code));
        }
    }

    /**
     * It prints an inverted index (a map of words to sets of words)
     *
     * @param ii      : the map
     * @param verbose : if false only the size of the posting list of each term is shown, if true all elements are printed too
     */
    private static void printInvertedIndex(Map<String, HashSet<String>> ii, boolean verbose) {
        for (String code : ii.keySet()) {
            System.out.printf("%s,%d\n", code, ii.get(code).size());
            if (verbose) {
                for (String w : ii.get(code)) {
                    System.out.println("\t" + w);
                }
            }
        }
    }

    /**
     * It performs a few statistical measurements over the dictionary (about the distribution of codes).
     *
     * @param bfr the BuffferedReader  of the dictionary
     */
    public static void performMeasurements(BufferedReader bfr) {
        int counter = 0;        // word counter
        int totalCharNum = 0;
        int wordMinSize = Integer.MAX_VALUE;
        int wordMaxSize = 0;
        int curWordSize = 0;
        String line;
        int codesCharSize = 0;
        long startTime = 0, endTime = 0, totalTime = 0;
        String wordEncoded = "";

        int option2test = 1; // 1: CoundexGRExtra, 2: stemmer

        //for codes' analytics (it measures the number of words assigned the same code)
        Map<String, Integer> codesAndCounts = new HashMap<>(); // for code analytics
        StemmerWrapper stemmer = new StemmerWrapper(); // for the case we want to perform analogous measurements using a stemmer

        // map keeping  for each code the set of words that have that code
        Map<String, HashSet<String>> codesAndWords = new HashMap<>(); // for code analytics

        try {
            //FileReader fl = new FileReader(path);  BufferedReader bfr = new BufferedReader(fl);	// if we wanted to read by path

            startTime = System.nanoTime();
            while ((line = bfr.readLine()) != null) {
                counter++;                        // counting words
                curWordSize = line.length();    // size of current word
                totalCharNum += curWordSize;    // adding chars of current word
                add_current_datasetSize(1);
                if (curWordSize < wordMinSize) wordMinSize = curWordSize;  // for min/max word sizes
                if (curWordSize > wordMaxSize) wordMaxSize = curWordSize;

                if (option2test == 1) {
                    wordEncoded = SoundexGRExtra.encode(line); // for testing SoundexGRExtra
                } else if (option2test == 2) {
                    wordEncoded = stemmer.getStemOf(line);        // for testing a stemmer
                }

                codesCharSize += wordEncoded.length();

                //System.out.printf("%10s --> %s\n", line, wordInSoundex); // for debugging only

                //extra for analytics (counting how many words go to the same code/stem
                Integer codefreq = codesAndCounts.get(wordEncoded);
                codesAndCounts.put(wordEncoded, (codefreq == null) ? 1 : codefreq + 1);

                //DETAILED ANALYSIS
                HashSet wordsWithThatCode = codesAndWords.get(wordEncoded);
                if (wordsWithThatCode == null) { // the code is not in the map
                    wordsWithThatCode = new HashSet();
                    wordsWithThatCode.add(line);
                    codesAndWords.put(wordEncoded, wordsWithThatCode);
                } else {
                    wordsWithThatCode.add(line);
                }
            }
            System.out.println("Words read: " + counter);
            add_current_datasetSize(counter);
        } catch (Exception e) {
            System.out.println(e);
        }

        endTime = System.nanoTime();
        totalTime = endTime - startTime;

        System.out.println("\tTEST OPTION      : " + ((option2test == 1) ? "SoundexGRExtra" : "Stemmer"));
        System.out.println("\tElapsed time     : " + TimeUnit.SECONDS.convert(totalTime, TimeUnit.NANOSECONDS) + " secs");
        System.out.println("\tElapsed time     : " + TimeUnit.MILLISECONDS.convert(totalTime, TimeUnit.NANOSECONDS) + " msecs");
        System.out.println("Number of words read      : " + counter);
        System.out.println("Total number of chars read: " + totalCharNum);
        System.out.println("Avg word size             : " + (totalCharNum + 0.0) / counter);
        System.out.println("Min word size             : " + wordMinSize);
        System.out.println("Max word size             : " + wordMaxSize);
        System.out.println("Average code size         : " + (codesCharSize + 0.0) / counter);

        // Analytics for the codes
        System.out.println("Number of Distinct codes  : " + codesAndCounts.keySet().size());
        int minCount = Integer.MAX_VALUE, maxCount = 0;
        for (String code : codesAndCounts.keySet()) {
            int codeCount = codesAndCounts.get(code);
            if (codeCount < minCount) {
                minCount = codeCount;
            }
            if (codeCount > maxCount) {
                maxCount = codeCount;
            }
        }

        System.out.println("Min number of words of a code: " + minCount);
        System.out.println("Max number of words of a code: " + maxCount);
        //printMap(codesAndCounts);
        //Detailed:
        printInvertedIndex(codesAndWords, false);
    }


    /**
     * Returns the set of words of the dictionary (assuming it has been loaded/refreshed)
     *
     * @return
     */
    public static Set<String> getWords() {
        return wordsSet;
    }

    /**
     * It looks up a word in the dictionary. If the dictionary is not loaded, it loads it.
     *
     * @param word
     * @return
     */
    public static boolean lookup(String word) {
        wordsSet = new HashSet<>(); // for keeping the words of the dictionary
        String line;

        try {
            // Use FileInputStream for local file system path
            FileInputStream inDict = new FileInputStream(placeDict); // This is for local file paths
            BufferedReader bfr = new BufferedReader(new InputStreamReader(inDict, "UTF-8"));

            while ((line = bfr.readLine()) != null) {
                wordsSet.add(line);
            }

            // Closing the BufferedReader
            bfr.close();

        } catch (FileNotFoundException e) {
            System.out.println("File not found at path: " + placeDict);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error reading file: " + e);
            e.printStackTrace();
        }

        return wordsSet.contains(word);
    }


    public static int calculateSuggestedCodeLen(String SelectedMethod) {
        int File_index = -1;

        // Search for the selected dataset file in the datasetFileList
        for (int i = 0; i < BulkCheck.DatasetFiles.length; i++) {
            if (BulkCheck.DatasetFiles[i].endsWith(Dashboard.getSelectedDatasetFile() + ".txt")) {
                File_index = i;
                break;
            }
        }

        switch (SelectedMethod) {
            // If the file index was found, calculate the length
            case ("Real-time length calculation"):
                if (File_index != -1) {
                    //System.out.println("File index: " + File_index);
                    //System.out.println("Doc name: " + Dashboard.getSelectedDatasetFile());
                    //System.out.println("Length: " + length_per_DocName.get(Dashboard.getSelectedDatasetFile()));
                    int length = length_per_DocName.get(Dashboard.getSelectedDatasetFile());
                    Dashboard.appSoundexCodeLen = length; // Set length
                    return length;
                } else {
                    // Handle the case where the selected file was not found
                    System.err.println("File not found: " + Dashboard.getSelectedDatasetFile());
                    return -1; // or some default value
                }
            case ("Predefined length"):
                int numWords = Dashboard.getNumberOfWords_of_SelectedDatasetFile();
                if (numWords <= 0) {
                    throw new RuntimeException("Number of words should be greater than 0");
                } else if (numWords <= 100) {
                    Dashboard.appSoundexCodeLen = 4;
                } else if (numWords <= 1000) {
                    Dashboard.appSoundexCodeLen = 7;
                } else if (numWords <= 2000) {
                    Dashboard.appSoundexCodeLen = 8;
                } else if (numWords <= 3000) {
                    Dashboard.appSoundexCodeLen = 11;
                } else {
                    Dashboard.appSoundexCodeLen = 12;
                }
                return Dashboard.appSoundexCodeLen;
            case ("Hybrid method i-ii"):
                return SoundexGRExtra.LengthEncoding;
            case ("Hybrid method ii-iii"):
                return SoundexGRExtra.LengthEncoding;
            default:
                System.out.println("Error: No method selected");
                return -1;
        }
    }


    /**
     * Returns all words having the same code
     *
     * @param code
     * @return
     */
    public static Set<String> returnWordsHavingTheSameCode(String code, String path) {
        //if (codesToWords == null) {
        codesToWords = new HashMap<>(); // the map
        String line;

        //System.out.println("Starting encoding the dictionary with code length " + SoundexGRExtra.LengthEncoding);

        try {
            String dictResourcePlace;

            // Use FileInputStream instead of getResourceAsStream
            String currentDir = System.getProperty("user.dir");
            if (path == null) {
                dictResourcePlace = currentDir + "\\Resources\\collection_words\\" + Dashboard.getSelectedDatasetFile() + "_words.txt";
            } else {
                dictResourcePlace = currentDir + path;
            }
            setDictionaryLocation(dictResourcePlace);
            //System.out.printf("Reading dictionary from %s\n", placeDict);
            FileInputStream inDict = new FileInputStream(placeDict);
            BufferedReader bfr = new BufferedReader(new InputStreamReader(inDict, "UTF-8"));
            while ((line = bfr.readLine()) != null) {
                String wordEncoded = SoundexGRExtra.encode(line);
                HashSet<String> wordsWithThatCode = codesToWords.get(wordEncoded);
                if (wordsWithThatCode == null) { // the code is not in the map
                    wordsWithThatCode = new HashSet<>();
                    wordsWithThatCode.add(line);
                    codesToWords.put(wordEncoded, wordsWithThatCode);
                } else {
                    wordsWithThatCode.add(line);
                }
            }
            bfr.close(); // Close BufferedReader after reading the file
        } catch (Exception e) {
            System.out.println(e);
        }

        //System.out.println("Dictionary was read, number of phonetic keys = " + codesToWords.keySet().size());
        //}
        return codesToWords.get(code);
    }


    /**
     * Reads a dictionary and produces a file containing for each word
     * of the dictionary a few variations that contain various kinds of errors.
     * Various parameters (STEP, etc) are inside the body
     *
     * @param path
     * @param outputPath
     */
    public static void createEvaluationDataset(String path, String outputPath) {
        int STEP = 100000; // to pick rows every STEP number of lines
        String line;
        int counter = 0;                // for counting the step
        int counterOutputlines = 0;    // for counting the lines written
        int wordsWritten = 0;        // for counting the words written
        try {
            FileReader fl = new FileReader(path);
            BufferedReader bfr = new BufferedReader(fl);
            FileWriter fo = new FileWriter(outputPath, false); // overwrite file if it exists
            while ((line = bfr.readLine()) != null) {
                if (counter % STEP == 0) {

                    fo.write(line); // write the word in its initial form
                    wordsWritten++;
                    for (String v : returnVariations(line)) { // word variations with mistakes
                        fo.write("," + v);
                        wordsWritten++;
                    }
                    fo.write("\n");
                    counterOutputlines++;
                }
                counter++; // counting lines read
            }
            fl.close();
            fo.close();
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Done. Lines read " + counter + ", lines created " + counterOutputlines + " (step = " + STEP + "), words written " + wordsWritten + ", avg words per line " + wordsWritten / (counterOutputlines + 0.0));
    }


    /**
     * Creates and returns erroneous variations of the param word
     *
     * @param word
     * @return
     */
    static public String[] returnVariations(String word) {
        return returnVariationsNew(word);
    }


    static String[] returnVariationsNew(String word) {
        // internal class for the rules:
        class Rule { // internal class
            String type = "Replacement"; // default value
            char[] notBeforeChars = null;
            String pattern = null;
            String replacement = null;
            char[] notAfterChars = null;

            Random rand = new Random(); //instance of random class

            Rule(String type) {
                this.type = type;
            }

            Rule(char[] notBeforeChars, String pattern, String replacement, char[] notAfterChars) {
                this.notBeforeChars = notBeforeChars;
                this.pattern = pattern;
                this.replacement = replacement;
                this.notAfterChars = notAfterChars;
            }

            /**
             * TODO: Applies the rule on a word. It takes as argument a string builder for cases where we want >1 rules to be applied
             * @param word
             * @param oword
             * @return
             */

            String apply(String word, StringBuilder oword) { // deletion of one char
                int wordLen = word.length();

                int randPos;

                switch (type) {
                    case "DelRandom": // Delete one random character apart from first and last
                        if (wordLen - 2 > 0) {
                            randPos = rand.nextInt(wordLen - 2);    // random num in  0.. wordLen-2
                            oword.delete(1 + randPos, 1 + randPos + 1);        // deletion of one char
                            return oword.toString();
                        } else
                            return null;
                        //break;
                    case "RepeatOne": //Replace one random character with the next character (apart from first): usually in typing
                        if (wordLen - 2 > 0) {
                            randPos = rand.nextInt(wordLen - 2);    // random num in  0.. wordLen-1
                            oword.setCharAt(1 + randPos, word.charAt(1 + randPos + 1));
                            return oword.toString();
                        } else return null;
                        //break;
                    case "Swap":  //Swap two consecutive middle letters (at a random place) if they are different
                        if (wordLen - 2 > 0) {
                            randPos = rand.nextInt(wordLen - 2);    // random num in  0.. wordLen-2
                            char tmp = word.charAt(1 + randPos);
                            if (tmp != word.charAt(1 + randPos + 1)) { // they are different letters
                                oword.setCharAt(1 + randPos, word.charAt(1 + randPos + 1));
                                oword.setCharAt(1 + randPos + 1, tmp);
                                return oword.toString();
                            }
                        } else return null;
                        break;
                    case "DeleteSame": //If it contains two consequent same letters delete one
                        for (int i = 0; i < wordLen - 1; i++) {
                            if (word.charAt(i) == word.charAt(i + 1)) {
                                oword.delete(i, i + 1);        // deletion of one char
                                return oword.toString(); //addition of the word to the array
                            }
                        }
                        break;
                    case "DoubleConsonant": // It doubles a consonant in the middle
                        //lala
                        for (int i = 1; i < wordLen - 1; i++) {
                            if (!isVowel(word.charAt(i))) { // if consonant
                                oword.insert(i, word.charAt(i));    //doubles it
                                return oword.toString(); //addition of the word to the array
                            }
                        }
                        break;
                    default:  // a general replacement rule
                        int pos = -1;
                        if ((pos = word.indexOf(pattern)) != -1) { // if it contains the pattern
                            boolean okBefore = true;
                            if (notBeforeChars != null) { // if there is a restriction on the previous chars
                                for (char c : notBeforeChars) {
                                    if ((pos > 0) && (c == word.charAt(pos - 1))) {
                                        okBefore = false;
                                    }
                                }
                            }
                            boolean okAfter = true;
                            if (notAfterChars != null) { // if there is a restriction on the next chars
                                for (char c : notAfterChars) {
                                    if ((pos < wordLen - 1) && (c == word.charAt(pos + 1))) {
                                        okAfter = false;
                                    }
                                }
                            }

                            if (okBefore && okAfter) {
                                oword.delete(pos, pos + pattern.length());    // deletion
                                oword.insert(pos, replacement);            // insertion
                            }
                            return (okBefore && okAfter) ? oword.toString() : null;
                        } // if contains the pattern
                } // switch
                return null;
            } // apply
        } // end of inner class


        // A. INITIALIZATION
        int K = 100; // max number of word variations to create
        String[] retStrings = new String[K]; //  will hold the variations
        int variationCounter = 0;

        // B. RULES CREATION
        Rule[] rules = {
                // Orthographic-i
                new Rule(null, "η", "υ", null),
                new Rule(null, "ή", "ύ", null),
                new Rule(new char[]{'ο', 'α', 'ε'}, "υ", "η", null), // checked
                new Rule(new char[]{'ο', 'α', 'ε'}, "ύ", "ή", null), // checked
                new Rule(null, "η", "ει", null),
                new Rule(null, "η", "οι", null),
                new Rule(new char[]{'ο', 'α', 'ε'}, "ι", "υ", null), //checked
                new Rule(new char[]{'ο', 'α', 'ε'}, "ί", "ύ", null), //checked
                new Rule(new char[]{'ο', 'α', 'ε'}, "ι", "η", null), //checked
                new Rule(new char[]{'ο', 'α', 'ε'}, "ί", "ή", null), //checked

                // ι->οι, ι->ει, ί->οι, ί->ει,
                new Rule(new char[]{'ο', 'α', 'ε'}, "ι", "ει", null),
                new Rule(new char[]{'ο', 'α', 'ε'}, "ί", "εί", null),
                new Rule(new char[]{'ο', 'α', 'ε'}, "ι", "οι", null),
                new Rule(new char[]{'ο', 'α', 'ε'}, "ί", "οί", null),

                new Rule(null, "οι", "ει", null),
                new Rule(null, "οί", "εί", null),
                new Rule(null, "ει", "οι", null),
                new Rule(null, "εί", "οί", null),
                // Orthographic-e
                new Rule(null, "ε", "αι", new char[]{'ι', 'ί'}),
                new Rule(null, "έ", "αί", new char[]{'ι', 'ί'}),
                new Rule(null, "αι", "ε", null),
                new Rule(null, "αί", "έ", null),
                // Orthographic-au,eu
                new Rule(null, "αυ", "αφ", null),
                new Rule(null, "αυ", "αβ", null),
                new Rule(null, "αφ", "αυ", null),
                new Rule(null, "αβ", "αυ", null),
                new Rule(null, "ευ", "εβ", null),
                new Rule(null, "εύ", "έβ", null),
                new Rule(null, "εβ", "ευ", null),
                new Rule(null, "έβ", "εύ", null),
                new Rule(null, "ευ", "εφ", null),
                new Rule(null, "εύ", "έφ", null),
                new Rule(null, "εφ", "ευ", null),
                new Rule(null, "έφ", "εύ", null),
                // Orthographic-g
                new Rule(null, "γκ", "γγ", null),
                new Rule(null, "γγ", "γκ", null),
                // Orthographic-o
                new Rule(null, "ο", "ω", new char[]{'υ', 'ύ', 'ι', 'ί'}), //checked
                new Rule(null, "ό", "ώ", new char[]{'υ', 'ύ', 'ι', 'ί'}), //checked
                new Rule(null, "ω", "ο", null),
                new Rule(null, "ώ", "ό", null),
                // Orthographic-ks
                new Rule(null, "ξ", "κσ", null),
                new Rule(null, "ψ", "πσ", null),


                /*
                // Rules for Typing Problems (currently inactive)
                new Rule("DelRandom"), 			// Deletion of random letter in the middle
                new Rule("RepeatOne"), 			// Repetition of one character: overwrites the next
                new Rule("Swap"), 				// Swapping of two consecutive chars
                */
                new Rule("DeleteSame"),        // Delete two consecutive same chars
                //new Rule("DoubleConsonant")		// Doubles a consonant in the middle
        };


        // C. APPLICATION OF SINGLE MISTAKE RULES
		/*
		for (Rule rcur: rules) {
			StringBuilder oword = new StringBuilder(word); // new builder for each rule
			retStrings[variationCounter++]= rcur.apply(word, oword);  // storing the result of applying  the rule
		}
		*/

        //D. APPLICATION OF MANY RULES: MAX (ONGOING) + 1 error rule
        StringBuilder oword = new StringBuilder(word); // new builder for each rule
        for (Rule rcur : rules) {
            retStrings[variationCounter++] = rcur.apply(word, oword);  // storing the result of applying  the rule
            if (word != null)  // updating the current word (for applying the next typo on that
                word = oword.toString();
        }
        Rule rRepeat = new Rule("DoubleConsonant");
        retStrings[variationCounter++] = rRepeat.apply(word, oword);
		
		
		/*
		// E. APPLICATION OF MORE THAN ONE MISTAKE BY PICKING A RANDOM RULE
		int NumOfVariationsWithXMistakesToMake = 10; // how many variations of the words to try to create  // 
		int NumMistakesToMake =3; // num of mistakes to try create over the same  word
		Random rand = new Random(); //instance of random class
		for (int i=0; i<NumOfVariationsWithXMistakesToMake; i++) { // for each variation to be created
			StringBuilder oword = new StringBuilder(word); // new builder
			for (int j=0; j<NumMistakesToMake; j++) { // 
				int ruleIndex = rand.nextInt(rules.length); // index of a random rule 
				if (word!=null) // the previous application did not return null 
					retStrings[variationCounter++]= rules[ruleIndex].apply(word, oword); 
				if (word!=null)  // updating the current word (for applying the next typo on that
					word=oword.toString();
			}
		}
		*/

        // E.  Return of the variations
        // E1. Eliminate null values
        retStrings = Arrays.stream(retStrings).filter(Objects::nonNull).toArray(String[]::new);
        // E2. Eliminate duplicates
        retStrings = Arrays.stream(retStrings).distinct().toArray(String[]::new);
        // E3. Delete the correct word
        String correctWord = word;
        retStrings = Arrays.stream(retStrings).filter(e -> e.compareTo(correctWord) != 0).toArray(String[]::new);
        return retStrings;
    }


    public static void main(String[] args) {
        System.out.println("[DictionaryBasedMeasurements]-start");

        // Setting the place of the dictionary (for being accessible from the IDE and in a package jar)
        String dictResourcePlace = "/dictionaries/EN-winedt/gr.dic";
        setDictionaryLocation(dictResourcePlace);

        performMeasurements(readerDict);   // for general measurements over the dictionary
        // performMeasurements("Resources/dictionaries/EN-winedt/gr.dic");  // for general measurements over the dictionary (itwork only within the IDE)

        // produces a dataset with synthetic errors
        createEvaluationDataset("Resources/dictionaries/EN-winedt/gr.dic", "Resources/names/dictionaryBased.txt");

        System.out.println("[DictionaryBasedMeasurements]-complete");
    }


    /**
     *
     */
    public static void invalidateMap() {
        codesToWords = null;  // TODO to check

    }


}
