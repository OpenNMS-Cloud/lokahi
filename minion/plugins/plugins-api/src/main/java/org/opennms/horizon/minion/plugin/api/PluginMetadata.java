package org.opennms.horizon.minion.plugin.api;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.opennms.taskset.contract.TaskType;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class PluginMetadata {

    private String pluginName;
    private TaskType pluginType;
    // private List<FieldConfigMeta> fieldConfigs;

}
