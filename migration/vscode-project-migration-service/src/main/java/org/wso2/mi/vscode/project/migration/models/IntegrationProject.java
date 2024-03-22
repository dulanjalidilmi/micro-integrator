package org.wso2.mi.vscode.project.migration.models;

import java.util.ArrayList;
import java.util.List;

public class IntegrationProject {
    public String name;
    public String path;
    public ConfigProject.ConfigType type;
    private List<IntegrationProject> moduleList = new ArrayList<>();

    public IntegrationProject(String name, String path, ConfigProject.ConfigType type,
                              List<IntegrationProject> modules) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.moduleList = modules;
    }

    public IntegrationProject(String name, String path, ConfigProject.ConfigType type) {
        this.name = name;
        this.type = type;
        this.path = path;
    }

//    public IntegrationProject(String name, ConfigProject.ConfigType type, List<IntegrationProject> modules) {
//        this.name = name;
//        this.type = type;
//        this.moduleList = modules;
//    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ConfigProject.ConfigType getType() {
        return type;
    }

    public void setType(ConfigProject.ConfigType type) {
        this.type = type;
    }

    public List<IntegrationProject> getModuleList() {
        return moduleList;
    }

    public void setModuleList(List<IntegrationProject> moduleList) {
        this.moduleList = moduleList;
    }
}
