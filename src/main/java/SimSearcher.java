import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javafx.util.Pair;
import org.apache.lucene.analysis.CharArrayMap;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Created by yatszhash on 2016/11/30.
 */
public class SimSearcher
{
    private int ngram;
    public static enum COEF_FUNC_TYPE{
        cosine,
        dise,
        jaccard,
        simpson,
    };

    private double lowerThreshold;
    private COEF_FUNC_TYPE coefType;
    //TODO replace with lucene index
    private Map<String, List<Pair<String, Integer>>> inverseIndices;

    SimSearcher(double lowerThreshold, COEF_FUNC_TYPE coefType){
        //TODO check threshold is not 0
       this(lowerThreshold, coefType, 3);
    }

    SimSearcher(double lowerThreshold, COEF_FUNC_TYPE coefType, int ngram){
        this.ngram = ngram;
        this.lowerThreshold = lowerThreshold;
        this.coefType = coefType;
    }

    public List<String> search(String target) throws Exception
    {
        List<String> features = extractFeatures(target);

        int min_n = searchRangeMin(features);
        int max_n = searchRangeMax(features);

        return IntStream.rangeClosed(min_n, max_n).parallel()
                .mapToObj(l -> {
                     int num = minOverlapNumFeatures(features, l);
                    return overlapJoin(features, l, num);
                })
                .flatMap(List::stream).collect(Collectors.toList());
    }

    //TODO more flexible choose for feature extraction
    public List<String> extractFeatures(String target) throws Exception
    {
        StringReader reader = new StringReader(target);
        Tokenizer tokenizer = new NGramTokenizer(this.ngram, this.ngram);
        tokenizer.setReader(reader);
        StopFilter newTokenizer = new StopFilter(tokenizer, CharArraySet.EMPTY_SET);
        newTokenizer.reset();
        CharTermAttribute termAttribute = newTokenizer.getAttribute(
                CharTermAttribute.class);

        List<String> result = new ArrayList<>();

        while(newTokenizer.incrementToken()){
            result.add(termAttribute.toString());
        }

        return result;
    }

    public int searchRangeMin(List<String> features){
        return (int)Math.floor(Math.pow(lowerThreshold, 2.0) * features.size());
    }

    public int searchRangeMax(List<String> features){
        return (int)Math.floor(features.size() / Math.pow(lowerThreshold, 2.0));
    }

    public int minOverlapNumFeatures(List<String> features, int l)
    {
        return (int)Math.floor((lowerThreshold * l));
    }

    public List<String> overlapJoin(List<String> features, int featureSize, int overlapMinSize)
    {
        //Map<String, Long> searched = features.stream().map(inverseIndices::get)
        Map<String, Long> searched = features.stream()
                .map(inverseIndices::get)
                //.peek(s -> System.out.println(s.toString()))
                .filter(p -> p != null)
                .flatMap(List::stream)
                //.peek(System.out::println) //for debug
                .filter(p -> p.getValue() == featureSize)
                .map(Pair::getKey)
                .collect(Collectors.groupingBy(s-> s, Collectors.counting()));

        return searched.entrySet().stream()
                .filter(e-> e.getValue() >= overlapMinSize)
                .map(Map.Entry::getKey)
                .sorted().collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        StringReader reader = new StringReader("This is a test String");
        Tokenizer tokenizer = new NGramTokenizer(3, 3);
        tokenizer.setReader(reader);
        //StopFilter newTokenizer = new StopFilter(tokenizer, CharArraySet.EMPTY_SET);

        tokenizer.reset();
        CharTermAttribute termAttribute = tokenizer.getAttribute(
                CharTermAttribute.class);

        while(tokenizer.incrementToken()){
            String token = termAttribute.toString();
            System.out.println(token);
        }


    }


}
