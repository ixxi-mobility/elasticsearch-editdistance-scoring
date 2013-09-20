package biz.ixxi.script;

import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.script.ScriptModule;

public class IxxiScriptPlugin extends AbstractPlugin {

    @Override
    public String name() {
        return "ixxi-script";
    }

    @Override
    public String description() {
        return "Ixxi scripts";
    }

    public void onModule(ScriptModule module) {
        module.registerScript("editdistance", EditDistanceScript.Factory.class);
    }

}