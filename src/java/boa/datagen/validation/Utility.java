package boa.datagen.validation;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import boa.datagen.scm.AbstractCommit;
import boa.datagen.scm.GitConnector;
import boa.datagen.validation.Commit.ChangedFile;
import boa.types.Diff.ChangedFile.Builder;

public class Utility {

	/**
	 * Get all commits using JGit
	 * 
	 * @param repository
	 * @return
	 * @throws IOException
	 * @throws GitAPIException
	 */
	public static HashMap<String, HashSet<ChangedFile>> getCommitsJGit(Repository repository) throws IOException, GitAPIException {
		HashMap<String, HashSet<ChangedFile>> commitMap = new HashMap<>();
		Collection<Ref> allRefs = repository.getRefDatabase().getRefs();
		Git git = new Git(repository);

		// a RevWalk allows to walk over commits based on some filtering that is defined
		try (RevWalk revWalk = new RevWalk(repository)) {
			for (Ref ref : allRefs) {
				revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
			}
			for (RevCommit commit : revWalk) {
				commitMap.put(commit.getName(), getChangedFiles(repository, git, commit.getName()));
			}
		}
		
		return commitMap;
	}

	/**
	 * Get all commits using Boa
	 * 
	 * @param repository
	 * @return
	 */
	public static HashMap<String, HashSet<ChangedFile>> getCommitsBoa(Repository repository) {
		HashMap<String, HashSet<ChangedFile>> commitList = new HashMap<>();

		String path = repository.getDirectory().getAbsolutePath();
		GitConnector gc = new GitConnector(path, new String());
		gc.setRevisions(true);

		for (AbstractCommit commit : gc.getRevisions()) {
			HashSet<ChangedFile> changedFiles = new HashSet<>();
			for (Builder changedFile : commit.getChangedFilesList()) {
				ChangedType type;
				switch (changedFile.getChange()) {
				case ADDED:
					type = ChangedType.ADD;
					break;
				case MODIFIED:
					type = ChangedType.MODIFY;
					break;
				case COPIED:
					type = ChangedType.COPY;
					break;
				case RENAMED:
					type = ChangedType.RENAMED;
					break;
				case DELETED:
					type = ChangedType.DELETE;
					break;
				default:
					type = ChangedType.OTHER;
				}
				changedFiles.add(new Commit.ChangedFile(changedFile.getName(), type));
			}
			commitList.put(commit.getId(), changedFiles);
		}
		gc.close();
		return commitList;
	}

	public static HashSet<Commit.ChangedFile> getChangedFiles(Repository repository, Git git, String newCommit)
			throws GitAPIException, IOException {
		HashSet<Commit.ChangedFile> changedFiles = new HashSet<>();
		List<DiffEntry> diffs = git.diff().setOldTree(prepareTreeParser(repository, newCommit + "^"))
				.setNewTree(prepareTreeParser(repository, newCommit)).call();
		RenameDetector rd = new RenameDetector(repository);
		rd.addAll(diffs);
		diffs = rd.compute();	
		
		for (DiffEntry diff : diffs) {
			ChangedType type;
			switch (diff.getChangeType()) {
			case ADD:
				type = ChangedType.ADD;
				break;
			case MODIFY:
				type = ChangedType.MODIFY;
				break;
			case COPY:
				type = ChangedType.COPY;
				break;
			case RENAME:
				type = ChangedType.RENAMED;
				break;
			case DELETE:
				type = ChangedType.DELETE;
				break;
			default:
				type = ChangedType.OTHER;
			}
			changedFiles.add(new Commit.ChangedFile(type == ChangedType.DELETE ? diff.getOldPath() : diff.getNewPath(), type));
		}
		return changedFiles;
	}
	

	private static AbstractTreeIterator prepareTreeParser(Repository repository, String objectId) throws IOException {
		try (RevWalk walk = new RevWalk(repository)) {
//			System.out.println("Resolving " + objectId);
			CanonicalTreeParser treeParser = new CanonicalTreeParser();
			try {
				RevCommit commit = walk.parseCommit(repository.resolve(objectId));
				RevTree tree = walk.parseTree(commit.getTree().getId());
				try (ObjectReader reader = repository.newObjectReader()) {
					treeParser.reset(reader, tree.getId());
				}
			} catch (NullPointerException e) {
				// only first commit get NPE
			} finally {
				walk.dispose();
			}
			
			return treeParser;
		}
	}

}
