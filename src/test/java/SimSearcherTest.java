import javafx.util.Pair;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Created by yatszhash on 2016/12/02.
 */
public class SimSearcherTest {
    @Test
    public void search() throws Exception {

    }

    @Test
    public void testExtractFeatures() throws Exception {
        String target = "somatosensory";

        List<String> expected = new ArrayList<String>(){{
            add("som");
            add("oma");
            add("mat");
            add("ato");
            add("tos");
            add("ose");
            add("sen");
            add("ens");
            add("nso");
            add("sor");
            add("ory");
        }};

        SimSearcher sut = new SimSearcher(0.7, SimSearcher.COEF_FUNC_TYPE.cosine);
        List<String> actual = sut.extractFeatures(target);

        assertThat(actual, is(expected));
    }

    @Test
    public void testSearchRangeMin() throws Exception {
        List<String> features = new ArrayList<String>(){{add("ab"); add("bc");
        add("cd"); add("de"); add("ef"); add("fg");}};
        Double lowerThreshold = 0.7;

        int expected = 2;

        SimSearcher sut = new SimSearcher(lowerThreshold, SimSearcher.COEF_FUNC_TYPE.cosine);
        int actual = sut.searchRangeMin(features);

        assertThat(actual, is(expected));
    }

    @Test
    public void testSearchRangeMax() throws Exception {
        List<String> features = new ArrayList<String>(){{add("ab"); add("bc");
            add("cd"); add("de"); add("ef"); add("fg");}};
        Double lowerThreshold = 0.7;

        int expected = 12;

        SimSearcher sut = new SimSearcher(lowerThreshold, SimSearcher.COEF_FUNC_TYPE.cosine);
        int actual = sut.searchRangeMax(features);

        assertThat(actual, is(expected));
    }

    @Test
    public void testMinOverlapNumFeatures() throws Exception {
        List<String> features = new ArrayList<String>(){{add("ab"); add("bc");
            add("cd"); add("de"); add("ef"); add("fg");}};
        Double lowerThreshold = 0.7;

        int expected = 2;
        int l = 4;

        SimSearcher sut = new SimSearcher(lowerThreshold, SimSearcher.COEF_FUNC_TYPE.cosine);
        int actual = sut.minOverlapNumFeatures(features, 4);

        assertThat(actual, is(expected));
    }

    @Test
    public void testOverlapJoin() throws Exception {
        int overlapMInsize = 3;
        int featureSize = 4;
        List<String> features = new ArrayList<String>() {{
            add("ab");
            add("bc");
            add("cd");
            add("de");
            add("ef");
            add("fg");
            add("gh");
            add("hi");
        }}; //"abcdefghi"

        Map<String, List<Pair<String, Integer>>> inverseIndices = new HashMap<>();

        //common with input
        addToIndex(inverseIndices, "ab", "abdd", 3); //smaller feature size and overlap
        addToIndex(inverseIndices, "ab", "abdzx", 4); //enough feature size and smaller overlap
        addToIndex(inverseIndices, "ab", "abcdd", 4); //enough feature size and overlap
        addToIndex(inverseIndices, "ab", "abcdef", 5); //larger feature size and enough overlap

        addToIndex(inverseIndices, "bc", "abdzx", 4); //enough feature size and smaller overlap
        addToIndex(inverseIndices, "bc", "abcdd", 4); //enough feature size and overlap
        addToIndex(inverseIndices, "bc", "abcdef", 5); //larger feature size and enough overlap

        addToIndex(inverseIndices, "cd", "abcdd", 4); //enough feature size and overlap
        addToIndex(inverseIndices, "cd", "abcdef", 5); //larger feature size and enough overlap


        addToIndex(inverseIndices, "de", "abcdef", 5); //larger feature size and enough overlap

        addToIndex(inverseIndices, "ef", "abcdef", 5); //larger feature size and enough overlap

        //difference with input
        addToIndex(inverseIndices, "bd", "abdd", 3); //smaller feature size and overlap
        addToIndex(inverseIndices, "bd", "abdzx", 4); //enough feature size and smaller overlap

        addToIndex(inverseIndices, "dd", "abdd", 3); //smaller feature size and overlap
        addToIndex(inverseIndices, "ab", "abcdd", 4); //enough feature size and overlap

        addToIndex(inverseIndices, "dz", "abdzx", 4); //enough feature size and smaller overlap

        addToIndex(inverseIndices, "zx", "abdzx", 4); //enough feature size and smaller overlap

        SimSearcher sut = new SimSearcher(0.7,
                SimSearcher.COEF_FUNC_TYPE.cosine, 2);

        setPrivateField(sut, "inverseIndices", inverseIndices);

        List<String> actual = sut.slowOverlapJoin(features, featureSize, overlapMInsize);

        assertThat(actual, is(containsInAnyOrder("abcdd")));
    }

    @Test
    public void testNewOverLapJoin() throws IOException
    {
        int overlapMInsize = 3;
        int featureSize = 4;
        List<String> features = new ArrayList<String>() {{
            add("ab");
            add("bc");
            add("cd");
            add("de");
            add("ef");
            add("fg");
            add("gh");
            add("hi");
        }}; //"abcdefghi"

        String[] indiceWords = {"abdd", "abdzx", "abcdd", "abcdef"};

        SimSearcher sut = new SimSearcher(0.7,
                SimSearcher.COEF_FUNC_TYPE.cosine, 2);

        sut.addWordsToIndices(indiceWords);

        List<String> actual = sut.overlapJoin(features, featureSize,
                overlapMInsize);

        assertThat(actual, is(containsInAnyOrder("abcdd")));

    }

    public static void addToIndex(
            Map<String, List<Pair<String, Integer>>> inverseIndices,
            String feature, String word, int featureSize){

            List<Pair<String, Integer>> words;
            if (inverseIndices.get(feature) == null) {
                words = new ArrayList<>();
                inverseIndices.put(feature, words);
            } else {
                words = inverseIndices.get(feature);
            }

            words.add(new Pair<>(word, featureSize));
    }

    public static void setPrivateField(Object targetObj, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException
    {
        Class c = targetObj.getClass();
        Field field = c.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(targetObj, value);
    }

}