package bobslate4j;

import bobthebuildtool.pojos.buildfile.Project;
import bobthebuildtool.pojos.internal.DescriptionAndInterface;

public enum Main {;

    public static void installPlugin(final Project project) {
        project.commands.put("slate4j", new DescriptionAndInterface<>("Generates documentation in slatedocs format"
                , (project1, environment, args) -> {
            slate4j.MavenDocs
            return 0;
        }));
    }

}
