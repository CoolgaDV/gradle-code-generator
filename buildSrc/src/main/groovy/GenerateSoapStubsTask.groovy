import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.GradleBuild
import org.gradle.api.tasks.TaskAction

class GenerateSoapStubsTask extends DefaultTask {

    private final Logger log = Logging.getLogger(GenerateSoapStubsTask.class)

    private final ObjectMapper jsonMapper = new ObjectMapper()

    void configure() {

        List<File> propertyFiles = new GitChangesScanner().scan()

        configureTasks(propertyFiles)
    }

    @TaskAction
    void complete() {
        log.quiet('SOAP stubs generation completed')
    }

    private void configureTasks(List<File> propertyFiles) {

        def cleanTask = createCleanTask()

        for (File file : propertyFiles) {

            StubConfiguration stubConfiguration = loadStubConfiguration(file)

            def projectPath = makeProjectPath(stubConfiguration)
            def taskPostfix = makeTaskPostfix(stubConfiguration)

            def generateSourcesTask = createSourceGenerationTask(
                    stubConfiguration,
                    projectPath,
                    taskPostfix)
            def preProcessTask = createPreProcessTask(
                    stubConfiguration,
                    taskPostfix)
            def gradleBuildTask = createGradleBuildTask(
                    projectPath,
                    taskPostfix)

            generateSourcesTask.dependsOn(cleanTask)
            preProcessTask.dependsOn(generateSourcesTask)
            gradleBuildTask.dependsOn(preProcessTask)
            this.dependsOn(gradleBuildTask)
        }
    }

    private Task createCleanTask() {
        return createTask("cleanPreviousResults", Delete) {
            delete 'result'
        }
    }

    private Task createSourceGenerationTask(StubConfiguration configuration,
                                            String projectPath,
                                            String taskPostfix) {
        Map<String, String> properties = [
                groupId : configuration.groupId,
                artifactId : configuration.artifactId,
                version : configuration.version
        ]

        List<String> filesWithSubstitutions = [
                'settings.gradle',
                'build.gradle'
        ]
        String templateDirectory = 'template'

        return createTask("generate${taskPostfix}", Copy) {
            from(templateDirectory) {
                for (String file : filesWithSubstitutions) {
                    include file
                }
                expand(properties)
            }
            from(templateDirectory) {
                for (String file : filesWithSubstitutions) {
                    exclude file
                }
            }
            into projectPath
        }
    }

    private Task createPreProcessTask(StubConfiguration stubConfiguration, String taskPostfix) {

        return createTask("preProcess${taskPostfix}", PreProcessingTask) {
            configuration = stubConfiguration
        }
    }

    private Task createGradleBuildTask(String projectPath, String taskPostfix) {

        return createTask("build${taskPostfix}", GradleBuild) {
            buildFile = "${projectPath}/build.gradle"
            tasks = ['clean', 'build']
        }
    }

    private StubConfiguration loadStubConfiguration(File file) {

        return jsonMapper.readValue(file, StubConfiguration.class)
    }

    private Task createTask(String name, Class type, Closure closure) {

        return project.tasks.create(name: name, type: type, closure)
    }

    private static String makeTaskPostfix(StubConfiguration configuration) {

        Closure transformer = { Character character, StringBuilder result ->
            result.append(character.toUpperCase())
        }

        return substitute(configuration.groupId, transformer, true) +
                substitute(configuration.artifactId, transformer, true)
    }

    private static String makeProjectPath(StubConfiguration configuration) {

        Closure transformer = { Character character, StringBuilder result ->
            result.append('-').append(character)
        }

        return 'result/' +
                substitute(configuration.groupId, transformer, false) + '-' +
                substitute(configuration.artifactId, transformer, false)
    }

    private static String substitute(String source,
                                     Closure transformer,
                                     boolean transformFirst) {

        boolean needTransformation = transformFirst
        StringBuilder result = new StringBuilder()

        for (Character character : source.toCharArray()) {
            if (!character.isLetterOrDigit()) {
                needTransformation = true
                continue
            }
            if (needTransformation) {
                transformer.call(character, result)
                needTransformation = false
            } else {
                result.append(character)
            }
        }
        return result.toString()
    }

}
