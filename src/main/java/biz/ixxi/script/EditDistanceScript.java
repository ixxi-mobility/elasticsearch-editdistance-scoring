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

import java.util.Map;
import java.lang.Math;

public class EditDistanceScript extends AbstractFloatSearchScript {

    public static class Factory implements NativeScriptFactory {

        @Override
        public ExecutableScript newScript(@Nullable Map<String, Object> params) {
            String fieldName = params == null ? null : XContentMapValues.nodeStringValue(params.get("field"), null);
            String searchString = params == null ? "" : XContentMapValues.nodeStringValue(params.get("search"), "");
            if (fieldName == null) {
                throw new ElasticSearchIllegalArgumentException("Missing the field parameter");
            }
            return new EditDistanceScript(fieldName, searchString);
        }
    }


    private final String fieldName;
    private final String searchString;
    private Float finalScore;
    ESLogger logger;

    public EditDistanceScript(String fieldName, String searchString){
        this.fieldName = fieldName;
        this.searchString = searchString;
        this.logger = Loggers.getLogger(EditDistanceScript.class);
    }

    @Override
    public float runAsFloat() {
        logger.info("************** runAsFloat ****************");
        finalScore = score();
        logger.info("finalScore at runAsFloat init " + finalScore);
        // logger.info(doc().toString());
        // logger.info(name.getValues().toString());
        // String candidate = (String)source().get(fieldName);
        ScriptDocValues.Strings name = (ScriptDocValues.Strings) doc().get(fieldName);
        String candidate = name.getValues().get(0);
        if (candidate == null || searchString == null) {
            return 0.0f;
        }
        String[] partials = searchString.split(" ");
        for (String partial: partials) {
            partialRun(partial, candidate);
        }
        // logger.info(searchString + " " + candidate + " " + r.toString() + " " + score() + " " + f);
        logger.info("finalScore " + finalScore.toString());
        return finalScore;
    }

    public void partialRun(String partial, String candidate) {
        Float r = Float.NaN;
        if (candidate.contains(partial)) {
            r = 1.0f;
        } else {
            logger.info("Comparing " + partial + " and " + candidate);
            LevensteinDistance builder = new LevensteinDistance();
            Integer index = guessBestPart(partial, candidate);
            if (index != -1) {
                Integer endIndex = index + partial.length() > candidate.length() -1 ? candidate.length() - 1 : index + partial.length();
                String candidatePartial = candidate.substring(index, endIndex);
                r = builder.getDistance(candidatePartial, partial);
                logger.info("r for " + partial + " and " + candidatePartial + " => " + r.toString());
            }
        }
        logger.info("r " + r.toString() + " " + finalScore);
        if (!Float.isNaN(r)) {
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

}
