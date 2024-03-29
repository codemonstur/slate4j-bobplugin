package slate4j;

import bobthebuildtool.pojos.buildfile.Project;
import bobthebuildtool.pojos.error.VersionTooOld;
import com.google.gson.Gson;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import jcli.errors.InvalidCommandLine;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;
import slate4j.model.SlateFile;
import slate4j.model.SlateHeading;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static bobthebuildtool.services.Functions.isNullOrEmpty;
import static bobthebuildtool.services.Update.requireBobVersion;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.readAllBytes;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static jcli.CliParserBuilder.newCliParser;
import static slate4j.IO.*;
import static slate4j.model.SlateFile.toSlateFile;

public enum BobPlugin {;

    public static void installPlugin(final Project project) throws VersionTooOld {
        requireBobVersion("7");
        project.addCommand("slate4j", "Generates documentation in slatedocs format", BobPlugin::generateSlateDocs);
    }

    private static int generateSlateDocs(final Project project, final Map<String, String> env, final String[] args)
            throws InvalidCommandLine, IOException {
        final CliDocs arguments = newCliParser(CliDocs::new).parse(args);

        final Path inputFile = project.parentDir.resolve(arguments.indexFile);
        if (!exists(inputFile)) return 0;

        final Path logoFile = isNullOrEmpty(arguments.logoFile) ? null : project.parentDir.resolve(arguments.logoFile);
        final Path outputPath = project.getBuildTarget().resolve(arguments.outputFile);

        final String html = compileSlateDocument(inputFile, logoFile);
        outputPath.getParent().toFile().mkdirs();
        Files.writeString(outputPath, html, CREATE, TRUNCATE_EXISTING);

        return 0;
    }

    public static String compileSlateDocument(final Path indexFile, final Path logoFile) throws IOException {
        final Gson gson = new Gson();
        final SlateFile slateFile = toSlateFile(indexFile);

        final Document wrapper = Jsoup.parse(resourceToString("/webbin/wrapper.html"));
        wrapper.selectFirst("title").text(slateFile.header.title);
        wrapper.selectFirst("body").attr("data-languages", gson.toJson(slateFile.header.languages));
        for (final var langSelector : wrapper.select("div.lang-selector")) {
            for (final var lang : slateFile.header.languages) {
                langSelector.appendChild(
                    wrapper.createElement("a")
                        .attr("href", "#")
                        .attr("data-language-name", lang)
                        .text(lang));
            }
        }

        final String js = resourceToString("/webbin/custom.min.js");
        final String css = resourceToString("/webbin/custom.min.css");
        wrapper.selectFirst("head")
                .appendChild( wrapper.createElement("script").appendChild(new DataNode(js)) )
                .appendChild( wrapper.createElement("style").appendChild(new DataNode(css)) );

        final String logo = existsFile(logoFile)
                ? encodeBase64(readAllBytes(logoFile))
                : resourceToString("/static/img/logo.base64.txt");
        wrapper.selectFirst("img[class=logo]").attr("src", "data:image/png;base64,"+logo);

        final Element body = Jsoup.parseBodyFragment(markdownToHtml(slateFile.content)).body();
        insertTableOfContents(wrapper, body);

        final Element element = wrapper.selectFirst("div.content");
        for (final var child : body.children()) {
            element.appendChild(child);
        }

        for (final var codeTags : wrapper.select("code")) {
            final String language = toLanguageClass(codeTags.attr("class"));
            if (language == null) continue;

            codeTags.parent().addClass("highlight").addClass(language).addClass("tab-"+language);
        }

        for (final var notice : wrapper.select("aside[class=notice]")) {
            notice.prependChild( wrapper.createElement("li").addClass("fas").addClass("fa-info-circle") );
        }
        for (final var success : wrapper.select("aside[class=success]")) {
            success.prependChild( wrapper.createElement("li").addClass("fas").addClass("fa-check-circle") );
        }
        for (final var warning : wrapper.select("aside[class=warning]")) {
            warning.prependChild( wrapper.createElement("li").addClass("fas").addClass("fa-exclamation-circle") );
        }

        return wrapper.html();
    }

    private static String toLanguageClass(final String classAttr) {
        for (final String clas : classAttr.split(" ")) {
            if (clas.startsWith("language-"))
                return clas.substring(9);
        }
        return null;
    }

    private static void insertTableOfContents(final Document wrapper, final Element body) {
        final List<SlateHeading> toc = discoverH1AndH2(body);

        final Element tocElement = wrapper.selectFirst("#toc");
        for (final SlateHeading heading : toc) {
            final Element topLi = wrapper.createElement("li").appendChild(
                wrapper.createElement("a")
                    .attr("href", "#" + heading.id)
                    .attr("class", "toc-h1 toc-link")
                    .attr("data-title", heading.name)
                    .text(heading.name));
            if (!heading.subHeadings.isEmpty()) {
                final Element ul = wrapper.createElement("ul").attr("class", "toc-list-h2");
                for (final SlateHeading subHeading : heading.subHeadings) {
                    ul.appendChild(
                        wrapper.createElement("li").appendChild(
                            wrapper.createElement("a")
                                .attr("href", "#" + subHeading.id)
                                .attr("class", "toc-h2 toc-link")
                                .attr("data-title", subHeading.name)
                                .text(subHeading.name)));
                }
                topLi.appendChild(ul);
            }
            tocElement.appendChild(topLi);
        }
    }

    private static List<SlateHeading> discoverH1AndH2(Element body) {
        final List<SlateHeading> toc = new ArrayList<>();

        body.traverse(new NodeVisitor() {
            private int index = 0;
            public void head(Node node, int depth) {
                if (node instanceof Element) {
                    final Element element = (Element) node;
                    final String name = element.tagName();
                    if (name.equals("h1") || name.equals("h2")) {
                        final var sh = new SlateHeading(toLevel(name), toUniqueId(index++, element), element.text(), new ArrayList<>());
                        element.attr("id", sh.id);
                        if (sh.level == 1) toc.add(sh);
                        else toc.get(toc.size()-1).subHeadings.add(sh);
                    }
                }
            }

            private int toLevel(final String name) {
                return Integer.parseInt(name.substring(1, 2));
            }

            private String toUniqueId(final int index, final Element element) {
                return element.text().replaceAll(" ", "-").toLowerCase();
            }

            public void tail(Node node, int depth) {}
        });

        return toc;
    }

    public static String markdownToHtml(final String markdown) {
        final MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(TablesExtension.create()));
        final Parser parser = Parser.builder(options).build();
        final HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        return renderer.render(parser.parse(markdown));
    }

}
