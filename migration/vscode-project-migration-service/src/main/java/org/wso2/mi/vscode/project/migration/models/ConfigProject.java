package org.wso2.mi.vscode.project.migration.models;

public class ConfigProject {
    private String name;
    private ConfigType type;

    public ConfigProject(String name, ConfigType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ConfigType getType() {
        return type;
    }

    public void setType(ConfigType type) {
        this.type = type;
    }

    public enum ConfigType {
        MULTIMODULE, // Represents "org.wso2.developerstudio.eclipse.mavenmultimodule.project.nature"
        ESB, // Represents "org.wso2.developerstudio.eclipse.esb.project.nature"
        DS,   // Represents "org.wso2.developerstudio.eclipse.ds.project.nature"
        DATASOURCE, // Represents "org.wso2.developerstudio.eclipse.datasource.project.nature"
        CONNECTOR // Represents "org.wso2.developerstudio.eclipse.artifact.connector.project.nature"
    }
}
