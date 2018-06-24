import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction

class PreProcessingTask extends DefaultTask {

    private final Logger log = Logging.getLogger(PreProcessingTask.class)

    StubConfiguration configuration

    @TaskAction
    void preProcess() {

        log.quiet("Called task with name: ${name}")

        if (!configuration.deprecate) {
            return
        }


    }

}
