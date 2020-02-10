package slate4j;

import bobthebuildtool.pojos.buildfile.Project;

import java.util.Map;

public enum BobPlugin {;

    public static void installPlugin(final Project project) {
        project.addCommand("slate4j", "Generates documentation in slatedocs format", BobPlugin::generateSlateDocs);
    }

    private static int generateSlateDocs(final Project project, final Map<String, String> env, final String[] args) {
        return 0;
    }

}
