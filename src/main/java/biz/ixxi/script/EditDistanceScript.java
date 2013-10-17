package biz.ixxi.script;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.script.AbstractFloatSearchScript;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.fielddata.ScriptDocValues;

import org.apache.lucene.search.spell.LevensteinDistance;
import org.apache.lucene.search.spell.NGramDistance;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.LuceneLevenshteinDistance;
import org.apache.lucene.search.spell.StringDistance;

import java.util.Map;
import java.lang.Math;

public class EditDistanceScript extends AbstractFloatSearchScript {

    public static class Factory implements NativeScriptFactory {

        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            String fieldName = params == null ? null : XContentMapValues.nodeStringValue(params.get("field"), null);
            String searchString = params == null ? "" : XContentMapValues.nodeStringValue(params.get("search"), "");
            String algo = params == null ? "" : XContentMapValues.nodeStringValue(params.get("editdistance"), "levenstein");
            if (fieldName == null) {
                throw new ElasticSearchIllegalArgumentException("Missing the field parameter");
            }
            return new EditDistanceScript(fieldName, searchString, algo);
        }
    }


    private final String fieldName;
    private final String searchString;
    private Float finalScore;
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
        finalScore = 1.0f;
        previousEndIndex = 0;
        // logger = Loggers.getLogger(EditDistanceScript.class);
        // logger.info(doc().toString());
        // logger.info(name.getValues().toString());
        // String candidate = (String)source().get(fieldName);
        ScriptDocValues.Strings name = (ScriptDocValues.Strings) doc().get(fieldName);
        String candidate = name.getValues().get(0);
        if (candidate == null || searchString == null) {
            return 0.0f;
        }
        // logger.info("finalScore before for " + candidate + " and " + searchString + " => " + finalScore);
        String[] partials = searchString.split(" ");
        for (String partial: partials) {
            partialRun(partial, candidate);
        }
        finalScore = finalScore + (score() / 100);
        // logger.info(searchString + " " + candidate + " " + score() + " / " + finalScore.toString());
        return finalScore;
    }

    public void partialRun(String partial, String candidate) {
        Float r = Float.NaN;
        Integer endIndex = -1;
        Integer index = -1;
        if (candidate.contains(partial)) {
            r = 1.1f;
            index = candidate.indexOf(partial);
            endIndex = index + partial.length();
        } else {
            // logger.info("Comparing " + partial + " and " + candidate);
            index = guessBestPart(partial, candidate);
            if (index != -1) {
                Integer nextSpace = candidate.indexOf(" ", index);
                endIndex = nextSpace != -1 ? nextSpace : candidate.length() - 1;
                String candidatePartial = candidate.substring(index, endIndex);
                LevensteinDistance builder = new LevensteinDistance();
                r = getDistance(candidatePartial, partial);
                // logger.info("r for " + partial + " and " + candidatePartial + " => " + r.toString());
            }
        }
        // logger.info("r " + r.toString() + " " + finalScore);
        if (!Float.isNaN(r)) {
            if (index - previousEndIndex < 3 && index - previousEndIndex > 0) {
                r = r * 1.4f;
            }
            previousEndIndex = endIndex;
            finalScore = finalScore * r;
        }
    }

    private Integer guessBestPart(String partial, String candidate) {
        Integer index = -1;
        while (partial.length() > 2) {
            partial = partial.substring(0, partial.length() - 1);
            index = candidate.indexOf(partial);
            if (index != -1) {
                break;
            }
        }
        return index;
    }

    private float getDistance(String target, String other) {
        StringDistance builder;
        if ("ngram".equals(algo)) {
            builder = (NGramDistance) new NGramDistance();
        } else if ("jarowinkler".equals(algo)) {
            builder = (JaroWinklerDistance) new JaroWinklerDistance();
        } else if ("lucene".equals(algo)) {
            builder = (LuceneLevenshteinDistance) new LuceneLevenshteinDistance();
        } else {
            builder = (LevensteinDistance) new LevensteinDistance();
        }
        // logger.info("Algo " + builder.toString() + " " + target + " / " + other + " => " + builder.getDistance(target, other));
        return builder.getDistance(target, other);
    }

}
