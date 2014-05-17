package biz.ixxi.script;

import org.apache.lucene.search.spell.*;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.script.AbstractFloatSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import java.util.Map;

import static org.elasticsearch.common.xcontent.support.XContentMapValues.nodeStringValue;

public class EditDistanceScript extends AbstractFloatSearchScript {

    public enum Algorithm {
        LEVENSTEIN, NGRAM3, JAROWINKLER, LUCENE, DEFAULT;

        public Algorithm parse(String str) {
            try {
                return valueOf(str.toUpperCase());
            } catch (IllegalArgumentException e) {
                return DEFAULT;
            }
        }
    }

    public static class Factory implements NativeScriptFactory {
        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            if (params == null) throw new ElasticsearchIllegalArgumentException("Missing parameters");
            String fieldName = nodeStringValue(params.get("field"), null);
            if (fieldName == null) throw new ElasticsearchIllegalArgumentException("Missing the field parameter");
            String searchString = nodeStringValue(params.get("search"), null);
            if (fieldName == null) throw new ElasticsearchIllegalArgumentException("Missing the search parameter");
            String str = nodeStringValue(params.get("editdistance"), "NGRAM3").toUpperCase();
            Algorithm algorithm = Algorithm.valueOf(str);
            return new EditDistanceScript(fieldName, searchString, algorithm);
        }
    }

    private static final ESLogger LOG = Loggers.getLogger(EditDistanceScript.class);
    private final String fieldName;
    private final String searchString;
    private Algorithm algorithm;

    public EditDistanceScript(String fieldName, String searchString, Algorithm algorithm) {
        this.fieldName = fieldName;
        this.searchString = searchString;
        this.algorithm = algorithm;
    }

    @Override
    public float runAsFloat() {
        ScriptDocValues.Strings name = (ScriptDocValues.Strings) doc().get(fieldName);
        String candidate = name.getValues().get(0);
        if (candidate == null) return 0.0f;
        Float finalScore = getDistance(searchString, candidate);
        finalScore = finalScore + (score() / 100);
        LOG.debug("distance[{}](searchString={},candidate={})={}", algorithm, searchString, candidate, finalScore);
        return finalScore;
    }

    private float getDistance(String target, String other) {
        StringDistance builder;
        switch (algorithm) {
            case LEVENSTEIN:
                builder = new LevensteinDistance();
                break;
            case NGRAM3:
                builder = new NGramDistance(3);
                break;
            case JAROWINKLER:
                builder = new JaroWinklerDistance();
                break;
            case LUCENE:
                builder = new LuceneLevenshteinDistance();
                break;
            default:
                builder = new NGramDistance(); // default to NGRAM size 2
        }
        return builder.getDistance(target, other);
    }

}
