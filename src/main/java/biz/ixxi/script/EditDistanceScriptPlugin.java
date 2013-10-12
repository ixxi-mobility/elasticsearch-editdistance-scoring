package biz.ixxi.script;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.script.ScriptModule;

public class EditDistanceScriptPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "editdistance-scoring";
    }

    @Override
    public String description() {
        return "Editdistance based script for custom scoring";
    }

    public void onModule(ScriptModule module) {
        module.registerScript("editdistance", EditDistanceScript.Factory.class);
    }

}