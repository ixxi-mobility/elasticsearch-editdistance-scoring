package biz.ixxi.script;

import org.apache.lucene.search.spell.*;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.script.AbstractFloatSearchScript;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import java.util.Map;

public class EditDistanceScript extends AbstractFloatSearchScript {

    public static class Factory implements NativeScriptFactory {

        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            String fieldName = params == null ? null : XContentMapValues.nodeStringValue(params.get("field"), null);
            String searchString = params == null ? "" : XContentMapValues.nodeStringValue(params.get("search"), "");
            String algo = params == null ? "" : XContentMapValues.nodeStringValue(params.get("editdistance"), "ngram");
            if (fieldName == null) {
                throw new ElasticsearchIllegalArgumentException("Missing the field parameter");
            }
            return new EditDistanceScript(fieldName, searchString, algo);
        }
    }


    private final String fieldName;
    private final String searchString;
    private Integer previousEndIndex;
    private String algo;
    // ESLogger logger;

    public EditDistanceScript(String fieldName, String searchString, String algo) {
        this.fieldName = fieldName;
        this.searchString = searchString;
        this.algo = algo;
    }

    @Override
    public float runAsFloat() {
        // logger.info("************** runAsFloat ****************");
        // logger = Loggers.getLogger(EditDistanceScript.class);
        // logger.info(doc().toString());
        // logger.info(name.getValues().toString());
        // String candidate = (String)source().get(fieldName);
        ScriptDocValues.Strings name = (ScriptDocValues.Strings) doc().get(fieldName);
        String candidate = name.getValues().get(0);
        // logger.info(candidate);
        if (candidate == null || searchString == null) {
            return 0.0f;
        }
        // logger.info("finalScore before for " + candidate + " and " + searchString + " => " + finalScore);
        Float finalScore = getDistance(searchString, candidate);
        finalScore = finalScore + (score() / 100);
        // logger.info(searchString + " | " + candidate + " | " + score() + " / " + finalScore.toString());
        return finalScore;
    }

    private float getDistance(String target, String other) {
        StringDistance builder;
        if ("levenstein".equals(algo)) {
            builder = (LevensteinDistance) new LevensteinDistance();
        } else if ("ngram3".equals(algo)) {
            builder = (NGramDistance) new NGramDistance(3);
        } else if ("jarowinkler".equals(algo)) {
            builder = (JaroWinklerDistance) new JaroWinklerDistance();
        } else if ("lucene".equals(algo)) {
            builder = (LuceneLevenshteinDistance) new LuceneLevenshteinDistance();
        } else {
            builder = (NGramDistance) new NGramDistance();  // default size: 2
        }
        // logger.info("Algo " + builder.toString() + " " + target + " / " + other + " => " + builder.getDistance(target, other));
        return builder.getDistance(target, other);
    }

}
