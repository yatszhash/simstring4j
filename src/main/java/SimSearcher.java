import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.util.Pair;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Created by yatszhash on 2016/11/30.
 */
public class SimSearcher {
    private int ngram;

    public static enum COEF_FUNC_TYPE {
        cosine,
        dise,
        jaccard,
        simpson,
    }

    private double lowerThreshold;
    private COEF_FUNC_TYPE coefType;
    // TODO remove
    private Map<String, List<Pair<String, Integer>>> inverseIndices;
    //TODO replace with lucene index. its temporal implementation
    private PseudoInverseIndices pseudoIndices;

    SimSearcher(double lowerThreshold, COEF_FUNC_TYPE coefType) {
        //TODO check threshold is not 0
        this(lowerThreshold, coefType, 3);
    }

    SimSearcher(double lowerThreshold, COEF_FUNC_TYPE coefType, int ngram) {
        this.ngram = ngram;
        this.lowerThreshold = lowerThreshold;
        this.coefType = coefType;
        this.pseudoIndices = new PseudoInverseIndices(ngram);
    }

    public List<String> search(String target) throws Exception {
        List<String> features = extractFeatures(target);

        int min_n = searchRangeMin(features);
        int max_n = searchRangeMax(features);

        return IntStream.rangeClosed(min_n, max_n).parallel()
                .mapToObj(l -> {
                    int num = minOverlapNumFeatures(features, l);
                    return slowOverlapJoin(features, l, num);
                })
                .flatMap(List::stream).collect(Collectors.toList());
    }

    public int searchRangeMin(List<String> features) {
        return (int) Math.floor(Math.pow(lowerThreshold, 2.0)
                * features.size());
    }

    public int searchRangeMax(List<String> features) {
        return (int) Math.floor(features.size() / Math.pow(lowerThreshold, 2.0));
    }

    public int minOverlapNumFeatures(List<String> features, int l) {
        return (int) Math.floor((lowerThreshold * l));
    }

    public List<String> overlapJoin(List<String> features,
                                    int searchFeatureSize, int overlapMinSize) {
        Map<String, List<String>> indices
                = pseudoIndices.getWithFeatureSize(
                        searchFeatureSize);

        sortWithCountInIndices(features, indices);

        LinkedHashMap<String, Integer> possibleWords =
                new LinkedHashMap<>();

        for (int k = 0; k < features.size() - overlapMinSize + 1; k++) {
            List<String> words = indices.get(features.get(k));
            if (words == null) {
                continue;
            }

            for (String word : words) {
                Integer count = possibleWords.get(word);
                if (count != null) {
                    count += 1;
                } else {
                    possibleWords.put(word, 1);
                }
            }
        }

        List<String> searchedList = new ArrayList<>();
        for (int k = features.size() - overlapMinSize + 1;
             k < features.size(); k++) {
            List<String> words = indices.get(features.get(k));
            if (words == null) {
                continue;
            }

            for (String wordStored : possibleWords.keySet()) {
                if (Collections.binarySearch(words, wordStored) >= 0) {
                    possibleWords.merge(wordStored, 1, Integer::sum);
                }

                if (overlapMinSize <= possibleWords.get(wordStored)) {
                    searchedList.add(wordStored);
                    possibleWords.remove(wordStored);
                } else if (possibleWords.get(wordStored)
                        + features.size() - k - 1 < overlapMinSize) {
                    //cannot surpass min overlap size even if the all of the rest equals this word
                    possibleWords.remove(wordStored);
                }
            }
        }

        return searchedList;
    }

    public void sortWithCountInIndices(List<String> features,
                                       Map<String, List<String>> indices) {
        features.sort(Comparator.comparing(
                f -> countInIndices(f, indices)));
    }

    public int countInIndices(String feature,
                              Map<String, List<String>> indices) {
        List<String> queryResult = indices.get(feature);
        return queryResult != null ? queryResult.size() : 0;
    }

    public static void main(String[] args) throws IOException {
        StringReader reader = new StringReader("This is a test String");
        Tokenizer tokenizer = new NGramTokenizer(3, 3);
        tokenizer.setReader(reader);
        //StopFilter newTokenizer = new StopFilter(tokenizer, CharArraySet.EMPTY_SET);

        tokenizer.reset();
        CharTermAttribute termAttribute = tokenizer.getAttribute(
                CharTermAttribute.class);

        while (tokenizer.incrementToken()) {
            String token = termAttribute.toString();
            System.out.println(token);
        }
    }

    public void addWordsToIndices(String[] words) throws IOException
    {
        pseudoIndices.addAllWords(words);
    }

    /**
     * This method is slow.
     * Its only  for comparing with the fast overlap join method.
     * @param features
     * @param featureSize
     * @param overlapMinSize
     * @return
     */
    public List<String> slowOverlapJoin(
            List<String> features, int featureSize, int overlapMinSize) {
        Map<String, Long> searched = features.stream()
                .map(inverseIndices::get)
                //.peek(s -> System.out.println(s.toString()))
                .filter(p -> p != null)
                .flatMap(List::stream)
                //.peek(System.out::println) //for debug
                .filter(p -> p.getValue() == featureSize)
                .map(Pair::getKey)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        return searched.entrySet().stream()
                .filter(e -> e.getValue() >= overlapMinSize)
                .map(Map.Entry::getKey)
                .sorted().collect(Collectors.toList());
    }

    //TODO more flexible choose for feature extraction
    public List<String> extractFeatures(String target) throws Exception {
        //TODO replace with indices' feature extraction method
        StringReader reader = new StringReader(target);
        Tokenizer tokenizer = new NGramTokenizer(this.ngram, this.ngram);
        tokenizer.setReader(reader);
        StopFilter newTokenizer = new StopFilter(tokenizer,
                CharArraySet.EMPTY_SET);
        newTokenizer.reset();
        CharTermAttribute termAttribute = newTokenizer.getAttribute(
                CharTermAttribute.class);

        List<String> result = new ArrayList<>();

        while (newTokenizer.incrementToken()) {
            result.add(termAttribute.toString());
        }

        return result;
    }

}
