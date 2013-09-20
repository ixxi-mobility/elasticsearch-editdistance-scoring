package biz.ixxi.script;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;
import org.elasticsearch.script.AbstractDoubleSearchScript;
import org.elasticsearch.common.xcontent.support.XContentMapValues;
import org.elasticsearch.ElasticSearchIllegalArgumentException;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.fielddata.ScriptDocValues;

import org.apache.lucene.search.spell.LevensteinDistance;

import java.util.Map;
import java.lang.Math;

public class EditDistanceScript extends AbstractDoubleSearchScript {

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
    public double runAsDouble() {
        ESLogger logger = Loggers.getLogger(EditDistanceScript.class);
        logger.info("************** pouet ****************");
        logger.info(searchString);
        ScriptDocValues.Strings fieldData = (ScriptDocValues.Strings) doc().get(fieldName);
        logger.info(fieldData.getValues().toString());
        String target = fieldData.getValue();
        logger.info(target);
        if (target == null || searchString == null) {
            return 0;
        }
        LevensteinDistance builder = new LevensteinDistance();
        float distance = builder.getDistance(target, searchString);
        if (distance == 0) {
            return 1;
        } else {
            return 1 / distance;
        }
    }
}
