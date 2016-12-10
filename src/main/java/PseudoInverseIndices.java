import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  inverce indice data structure for test and experiment
 *  This is very slow and it consumes large memory.
 *  Don't use for actual use.
 * Created by yatszhash on 2016/12/10.
 */
public class PseudoInverseIndices {
    //classified with the length of words
    List<Map<String, List<String>>> indices;
    int ngram;

    public PseudoInverseIndices(int ngram){
        this.ngram = ngram;
        indices = new ArrayList<>();
    }

    public Map<String, List<String>> getWithFeatureSize(
            int featureSize){
        if (featureSize > indices.size()){
            throw new IndexOutOfBoundsException();
        }

        return indices.get(featureSize - 1);
    }

    public void addWord(String word) throws IOException {
        int indicesMaxLength = indices.size();
        List<String> features = extractFeatures(word);
        int featureSize = features.size();

        if (featureSize > indicesMaxLength){
            for (int i=indicesMaxLength + 1; i <= featureSize; i++){
                indices.add(new HashMap<>());
            }
        }

        features.forEach(f -> {
            addFeature(featureSize, f, word);
        });
    }

    public void addAllWords(String[] words) throws IOException {
        for (String word : words){
            addWord(word);
        }
    }

    private void addFeature(int featureSize, String feature, String word){
        Map<String, List<String>> sameLengthIndices
                = indices.get(featureSize - 1);
        List<String> wordsInIndices = sameLengthIndices.get(feature);

        if (wordsInIndices == null){
            List<String> newFeatureWords = new ArrayList<>();
            newFeatureWords.add(word);
            sameLengthIndices.put(feature, newFeatureWords);
            return;
        }

        wordsInIndices.add(word);
        //TODO sort at once
        wordsInIndices.sort(null);
    }

    public List<String> extractFeatures(String target)
            throws IOException {
        //TODO reuse
        StringReader reader = new StringReader(target);
        Tokenizer newTokenizer = new NGramTokenizer(this.ngram, this.ngram);
        newTokenizer.setReader(reader);
        //StopFilter newTokenizer = new StopFilter(tokenizer,
        //        CharArraySet.EMPTY_SET);
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
