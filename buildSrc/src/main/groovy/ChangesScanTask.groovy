import org.eclipse.jgit.api.Git
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import org.gradle.api.DefaultTask

class ChangesScanTask extends DefaultTask {

    def scanChanges() {

        Git git = Git.open(new File(".git"))

        Repository repository = new FileRepository(".git")

        ObjectId branchId = repository.resolve("refs/heads/master")

        def taggedCommitId = null

        for (Ref ref : git.tagList().call()) {
            println ">>> ${ref.getName()}"
            if (ref.getName() == "refs/tags/water-mark") {
                taggedCommitId = ref.getObjectId()
            }
        }

        for (RevCommit commit : git.log().add(branchId).call()) {
            if (commit.getId() == taggedCommitId) {
                println commit.getFullMessage().trim()
            }
        }

        TreeWalk treeWalk = new TreeWalk( repository )
        treeWalk.reset( commit.getId() )
        while( treeWalk.next() ) {
            String path = treeWalk.getPathString()
            // ...
        }
        treeWalk.close()

    }

}
