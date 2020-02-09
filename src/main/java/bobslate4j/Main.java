package bobslate4j;

import bobthebuildtool.pojos.buildfile.Project;

import java.util.Map;

public enum Main {;

    public static void installPlugin(final Project project) {
        project.addCommand("slate4j", "Generates documentation in slatedocs format", Main::generateSlateDocs);
    }

    private static int generateSlateDocs(final Project project, final Map<String, String> env, final String[] args) {
        return 0;
    }
}
