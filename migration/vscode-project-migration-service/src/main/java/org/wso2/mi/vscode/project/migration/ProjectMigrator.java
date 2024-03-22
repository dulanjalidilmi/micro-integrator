package org.wso2.mi.vscode.project.migration;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.wso2.mi.vscode.project.migration.models.ConfigProject;
import org.wso2.mi.vscode.project.migration.models.IntegrationProject;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProjectMigrator {
    private static final Logger LOGGER = Logger.getLogger(ProjectMigrator.class.getName());
    private static String newWorkspaceLocation;
    private static final Map<String, String> oldFolderStructure = new HashMap<>(Map.of(
            "synapseConfig", File.separator + "src" + File.separator + "main" +
                    File.separator + "synapse-config",
            "metadata",  File.separator + "src" + File.separator + "main" + File.separator + "resources" +
                    File.separator + "metadata",
            "dataservices", File.separator + "dataservice",
            "datasources", File.separator + "datasource"
    ));
    private static final Map<String, String> newFolderStructure = new HashMap<>(Map.of(
            "artifacts", File.separator + "src" + File.separator + "main" + File.separator + "wso2mi" +
                    File.separator + "artifacts",
            "metadata",  File.separator + "src" + File.separator + "main" + File.separator + "wso2mi" +
                    File.separator + "resources" + File.separator + "metadata",
            "dataservices", File.separator + "src" + File.separator + "main" + File.separator + "wso2mi" +
                    File.separator + "artifacts" + File.separator + "dataservices",
            "datasources", File.separator + "src" + File.separator + "main" + File.separator + "wso2mi" +
                    File.separator + "artifacts" + File.separator + "datasources",
            "connectors", File.separator + "src" + File.separator + "main" + File.separator + "wso2mi" +
                    File.separator + "resources" + File.separator + "connectors"
    ));

    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "Initiating the project migration");
        Scanner scanner = new Scanner(System.in);
        System.out.println("Please enter a file path of the old workspace :");
        String originalWorkspace = scanner.nextLine();

        System.out.println("Please enter a file path of new workspace :");
        newWorkspaceLocation = scanner.nextLine();

        // Close the scanner
        scanner.close();

        System.out.println("originalWorkspace: " + originalWorkspace);
        System.out.println("newWorkspaceLocation: " + newWorkspaceLocation);

//        String originalWorkspace = "/Users/dulanjali/IntegrationStudio/8.2.0/workspace/tmp/";
//        newWorkspaceLocation = "/Users/dulanjali/Desktop/migrator/migratedWorkspace/";

        originalWorkspace = verifyFolderPath(originalWorkspace);
        newWorkspaceLocation = verifyFolderPath(newWorkspaceLocation);

        List<IntegrationProject> integrationProjectList = getIntegrationProjectList(originalWorkspace);
        migrate(integrationProjectList);
    }

    private static void migrate(List<IntegrationProject> integrationProjectList) {
        //create new project structure for each project
        for (IntegrationProject project: integrationProjectList) {
            createNewProjectFolders(project);
            processNewProject(project, project.getName());
        }
    }

    private static void processNewProject(IntegrationProject project, String projectName) {
        // todo: update this
        LOGGER.log(Level.INFO, "Initiating artifact folder processing");
        switch (project.getType()) {
            case MULTIMODULE:
                // if this a maven multimodule we need to check modules in the moduleList
                for (IntegrationProject module :project.getModuleList()) {
                    processNewProject(module, projectName);
                };
                break;
            case ESB:
                processSynapseConfig(project, projectName);
                break;
            case DS:
                processDataServices(project, projectName);
                break;
            case DATASOURCE:
                processDatasources(project, projectName);
                break;
            case CONNECTOR:
                processConnectors(project, projectName);
                break;
        }

    }

    private static void processConnectors(IntegrationProject project, String projectName) {
        Path destination = new File(newWorkspaceLocation + File.separator + projectName
                + newFolderStructure.get("connectors")).toPath();

        File dir = new File(project.getPath());
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".zip"));
        if (files != null && files.length > 0) {
            for (File file : files) {
               copy(file.toPath(), destination.resolve(file.toPath().getFileName()));
            }
        }
    }

    private static void processDataServices(IntegrationProject project, String projectName) {
        Path source = new File(project.getPath() + oldFolderStructure.get("dataservices")).toPath();
        Path destination = new File(newWorkspaceLocation + File.separator + projectName
                + newFolderStructure.get("dataservices")).toPath();
        copyAllFiles(source, destination);
    }

    private static void processDatasources(IntegrationProject project, String projectName) {
        Path source = new File(project.getPath() + oldFolderStructure.get("datasources")).toPath();
        Path destination = new File(newWorkspaceLocation + File.separator + projectName +
                newFolderStructure.get("datasources")).toPath();
        copyAllFiles(source, destination);
    }

    private static void processSynapseConfig(IntegrationProject project, String projectName) {
        Path source = new File(project.getPath() + oldFolderStructure.get("synapseConfig")).toPath();
        Path destination = new File(newWorkspaceLocation + File.separator + projectName +
                newFolderStructure.get("artifacts")).toPath();
        copyAllFiles(source, destination);
        // finally we have updated api folder to apis. hence move the api folder to apis
        if (new File(destination + File.separator + "api").renameTo(new File(destination + File.separator + "apis"))) {
            LOGGER.log(Level.INFO, "Renamed api folder to apis");
        } else {
            exitWithError("Error occurred while renaming api folder to apis");
        }

        // copy metadata folder
        processResourcesMetaDataFolder(project, projectName);
    }

    private static void processResourcesMetaDataFolder(IntegrationProject project, String projectName) {
        Path source = new File(project.getPath() + oldFolderStructure.get("metadata")).toPath();
        Path destination = new File(newWorkspaceLocation + File.separator + projectName +
                newFolderStructure.get("metadata")).toPath();
        copyAllFiles(source, destination);
    }


    private static void copyAllFiles(Path source, Path dest) {
        try (Stream<Path> stream = Files.walk(source)) {
            stream.forEach(file -> copy(file, dest.resolve(source.relativize(file))));
        } catch (IOException e) {
            // todo handle this properly
            throw new RuntimeException(e);
        }
    }

    private static void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (DirectoryNotEmptyException e) {
            //this is ignored since we have already created the folder structure
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createNewProjectFolders(IntegrationProject project) {
        LOGGER.log(Level.INFO, "Initiating the creation of new project folders");
        String projectName = project.getName();
        ArrayList<String> artifacts = new ArrayList<>();
        artifacts.add("apis");
        artifacts.add("endpoints");
        artifacts.add("inbound-endpoints");
        artifacts.add("local-entries");
        artifacts.add("message-processors");
        artifacts.add("message-stores");
        artifacts.add("proxy-services");
        artifacts.add("sequences");
        artifacts.add("tasks");
        artifacts.add("templates");
        artifacts.add("datasources");
        artifacts.add("dataservices");

        // Create artifacts folder
        List<String> folders = new ArrayList<>();
        String srcFolder = newWorkspaceLocation + File.separator + projectName + File.separator + "src";
        String mainFolder = srcFolder + File.separator + "main";
        String artifactsFolder = mainFolder + File.separator + "wso2mi" + File.separator + "artifacts";
        String resourcesFolder = mainFolder + File.separator + "wso2mi" + File.separator + "resources";

        artifacts.forEach(folderName -> folders.add(artifactsFolder + File.separator + folderName));

        // registry folder
        String registryFolder = resourcesFolder + File.separator + "registry";
        String govRegistryFolder = registryFolder + File.separator + "gov";
        String confRegistryFolder = registryFolder + File.separator + "conf";
        folders.add(govRegistryFolder);
        folders.add(confRegistryFolder);

        // connectors
        String connectorsFolder = resourcesFolder + File.separator + "connectors";
        folders.add(connectorsFolder);

        // metadata
        String metaDataFolder = resourcesFolder + File.separator + "metadata";
        folders.add(metaDataFolder);

        // class mediators
        String classMediatorsFolder = mainFolder + File.separator + "java";
        folders.add(classMediatorsFolder);

        // tests folders
        String testsFolder = srcFolder + File.separator + "tests" + File.separator + "wso2mi";
        String testsJavaFolder = srcFolder + File.separator + "tests" + File.separator + "java";
        folders.add(testsFolder);
        folders.add(testsJavaFolder);

        for (String folder: folders) {
            if (!new File(folder).mkdirs()) {
                exitWithError("Error occurred while creating folder :" + folder);
            }
        }

        createPomFile(project);
    }

    private static void createPomFile(IntegrationProject project) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try (
                InputStream inputStream = classLoader.getResourceAsStream("pom-template.xml");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String templateContent = reader.lines().collect(Collectors.joining(System.lineSeparator()));

            Map<String, String> projectData = getProjectData(project);

            for (Map.Entry<String, String> entry : projectData.entrySet()) {
                templateContent = templateContent.replace("${" + entry.getKey() + "}", entry.getValue());
            }

            Files.write(Paths.get(newWorkspaceLocation + File.separator + project.getName() +
                    File.separator + "pom.xml"), templateContent.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> getProjectData(IntegrationProject project) {
        String rootPomLocation = project.getPath() + File.separator + "pom.xml";
        File rootPom = new File(rootPomLocation);
        Map<String, String> projectData = new HashMap<>();
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(rootPom);
            doc.getDocumentElement().normalize();

            // Read the "groupId" element
            NodeList groupIdList = doc.getElementsByTagName("groupId");
            String groupId = groupIdList.item(0).getTextContent();
            projectData.put("groupId", groupId);

            // Read the "artifactId" element
            NodeList artifactIdList = doc.getElementsByTagName("artifactId");
            String artifactId = artifactIdList.item(0).getTextContent();
            projectData.put("artifactId", artifactId);

            // Read the "version" element
            NodeList versionList = doc.getElementsByTagName("version");
            String version = versionList.item(0).getTextContent();
            projectData.put("version", version);

            // Read the "name" element
            NodeList nameList = doc.getElementsByTagName("name");
            String name = nameList.item(0).getTextContent();
            projectData.put("name", name);

            // Read the "description" element
            NodeList descriptionList = doc.getElementsByTagName("description");
            if (descriptionList.getLength() > 0) {
                String description = descriptionList.item(0).getTextContent();
                projectData.put("description", description);
            } else {
                projectData.put("description", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return projectData;
    }

    // 1
    private static List<IntegrationProject> getIntegrationProjectList(String directory) {
        // check all the folders
        List<IntegrationProject> projectList = new ArrayList<>();
        File[] projectDirs = new File(directory).listFiles(File::isDirectory);

        // Filter to list only directories
        if (projectDirs != null) {
            for (File projectDir : projectDirs) {
                // Look for .project file in each directory
                File projectFile = new File(projectDir, ".project");
                if (projectFile.exists()) {
                    IntegrationProject integrationProject = createProjectList(projectFile);
                    if (integrationProject != null) {
                        projectList.add(integrationProject);
                    }
                }
            }
        }
        return projectList;
    }

    // 2
    private static IntegrationProject createProjectList(File projectFile) {
        String name;
        ConfigProject.ConfigType type;

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(projectFile);
            doc.getDocumentElement().normalize();

            NodeList nameList = doc.getElementsByTagName("name");
            if (nameList.getLength() > 0) {
                name = nameList.item(0).getTextContent();
                if (!name.equals(".tmp")) {
                    NodeList natureList = doc.getElementsByTagName("nature");
                    if (natureList.getLength() > 0) {
                        String nature = natureList.item(0).getTextContent();
                        switch (nature) {
                            case "org.wso2.developerstudio.eclipse.mavenmultimodule.project.nature":
                                List<IntegrationProject> moduleList = getIntegrationProjectList(projectFile.getParent());
                                return new IntegrationProject(name, projectFile.getParent(),
                                        ConfigProject.ConfigType.MULTIMODULE, moduleList);
                            case "org.wso2.developerstudio.eclipse.esb.project.nature":
                                return new IntegrationProject(name, projectFile.getParent(),
                                        ConfigProject.ConfigType.ESB);
                            case "org.wso2.developerstudio.eclipse.ds.project.nature":
                                return new IntegrationProject(name, projectFile.getParent(),
                                        ConfigProject.ConfigType.DS);
                            case "org.wso2.developerstudio.eclipse.datasource.project.nature":
                                return new IntegrationProject(name, projectFile.getParent(),
                                        ConfigProject.ConfigType.DATASOURCE);
                            case "org.wso2.developerstudio.eclipse.artifact.connector.project.nature":
                                return new IntegrationProject(name, projectFile.getParent(),
                                        ConfigProject.ConfigType.CONNECTOR);
                        }
                    }
                }
            }
        } catch (Exception e) {
            //todo handle properly
            throw new RuntimeException(e);
        }
        return null;
    }

    private static String verifyFolderPath(String folderPath) {
        String location = folderPath.endsWith(File.separator) ?
                folderPath.substring(0, folderPath.lastIndexOf(File.separator)) : folderPath;
        File file = new File(location);
        if (!(file.exists() && file.isDirectory())) {
            exitWithError("Path provided : " + location + " is incorrect. Please enter a valid folder path");
        }
        return location;
    }


    /**
     * Log and terminate the program
     * @param msg the error log message
     */
    private static void exitWithError(String msg) {
        LOGGER.severe(msg);
        LOGGER.info("Migration failed.");
        System.exit(1);
    }
}
