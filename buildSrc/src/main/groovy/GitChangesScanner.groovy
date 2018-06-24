import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RawTextComparator
import org.eclipse.jgit.internal.storage.file.FileRepository
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.util.io.DisabledOutputStream

class GitChangesScanner {

    private final Git git
    private final Repository repository;

    GitChangesScanner() {
        git = Git.open(new File(".git"))
        repository = new FileRepository(".git")
    }

    List<File> scan() {
        ObjectId waterMarkCommitId = findWaterMarkCommit()
        List<RevCommit> commits = collectCommitsToProcess(waterMarkCommitId)
        return collectChangedPropertyFiles(commits)
    }

    private List<File> collectChangedPropertyFiles(List<RevCommit> commits) {

        List<File> propertyFiles = []

        RevWalk walk = new RevWalk(repository)

        for (RevCommit commit : commits) {

            RevCommit parentCommit = walk.parseCommit(commit.parents[0].id)

            DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)
            df.repository = repository
            df.diffComparator = RawTextComparator.DEFAULT
            df.detectRenames = true

            for (DiffEntry diff : df.scan(parentCommit.getTree(), commit.getTree())) {
                String path = diff.newPath
                if (path.startsWith('configuration/')) {
                    propertyFiles.add(new File(path))
                }
            }
        }
        return propertyFiles
    }

    private ObjectId findWaterMarkCommit() {
        for (Ref ref : git.tagList().call()) {
            if (ref.name == "refs/tags/water-mark") {
                return ref.objectId
            }
        }
        throw new RuntimeException("No water mark commit found")
    }

    private List<RevCommit> collectCommitsToProcess(ObjectId waterMarkCommitId) {
        ObjectId branchId = repository.resolve("refs/heads/master")
        List<RevCommit> commits = []
        for (RevCommit commit : git.log().add(branchId).call()) {
            if (commit.id == waterMarkCommitId) {
                break
            }
            commits << commit
        }
        return commits
    }

}
