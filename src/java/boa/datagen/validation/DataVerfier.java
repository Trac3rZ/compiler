package boa.datagen.validation;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;

import boa.datagen.validation.Commit.ChangedFile;

public class DataVerfier {
	public static void main(String[] args) throws IOException, GitAPIException {
		if (args.length < 1) {
			System.out.println("args[0] is path to .git repository");
		}
		String path = args[0];

		Repository repository = new FileRepository(path);
		
		System.out.println("Getting commits map using Boa");
		HashMap<String, HashSet<ChangedFile>> boaCommits = Utility.getCommitsBoa(repository);
		System.out.println("Boa: " + boaCommits.size());
		
		System.out.println("Getting commits map using JGit");
		HashMap<String, HashSet<ChangedFile>> jgitCommits = Utility.getCommitsJGit(repository);
		System.out.println("JGit: " + jgitCommits.size());
		
		System.out.println("Finding differences");
		HashSet<String> diffChangedFiles = new HashSet<>();
		HashSet<String> diffCommits = new HashSet<>();
		
		System.out.println("First pass running");
		for (String commitID : jgitCommits.keySet()) {
			HashSet<ChangedFile> jgitCF = jgitCommits.get(commitID);
			HashSet<ChangedFile> boaCF = boaCommits.get(commitID);
			
			if (boaCF != null) {
				if (!jgitCF.equals(boaCF)) {
					diffChangedFiles.add(commitID);
				}
			} else {
				diffCommits.add(commitID);
			}
		}
		
		System.out.println("Seconds pass running");
		for (String commitID : boaCommits.keySet()) {
			HashSet<ChangedFile> jgitCF = jgitCommits.get(commitID);
			if (jgitCF == null) {
				diffCommits.add(commitID);
			}
		}
		System.out.println("Done");
		System.out.println("===============================================");
		if (diffChangedFiles.size() == 0 && diffCommits.size() == 0) {
			System.out.println("There are no differences!");
		} else {
			if (diffChangedFiles.size() > 0) {
				System.out.println("These are commits exist in commit data retrieved from Boa and JGit, but differ in changed files:");
				for (String commit : diffChangedFiles) {
					System.out.println("Commit ID: " + commit);
					System.out.println("Changed files using Boa:");
					for (ChangedFile cf : boaCommits.get(commit)) {
						System.out.println(cf);
					}
					System.out.println("Changed files using JGit:");
					for (ChangedFile cf : jgitCommits.get(commit)) {
						System.out.println(cf);
					}
				}
			}
			if (diffCommits.size() > 0) {
				System.out.println("These are " + diffCommits.size() + " commits exists in only one of two commit data");
				for (String commit : diffCommits) {
					if (boaCommits.get(commit) != null) {
						System.out.println(commit + " only exists in Boa commit data");
					} else {
						System.out.println(commit + " only exists in JGit commit data");
					}
				}
			}
 		}
	}

}
