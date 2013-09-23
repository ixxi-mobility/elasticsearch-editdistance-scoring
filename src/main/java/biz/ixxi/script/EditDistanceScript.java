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

    public EditDistanceScript(String fieldName, String searchString){
        this.fieldName = fieldName;
        this.searchString = searchString;
    }

    @Override
    public float runAsFloat() {
        Float r;
        // ESLogger logger = Loggers.getLogger(EditDistanceScript.class);
        // logger.info("************** pouet ****************");
        String target = (String)source().get(fieldName);
        if (target == null || searchString == null) {
            r = 0.0f;
        } else {
            LevensteinDistance builder = new LevensteinDistance();
            r = builder.getDistance(target, searchString);
        }
        Float f = score() * r;
        // logger.info(searchString + " " + target + " " + r.toString() + " " + score() + " " + f);
        return f;
    }
}
