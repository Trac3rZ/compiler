package boa.datagen.validation;

import java.util.HashSet;


public class Commit {
	private String id;
	private HashSet<ChangedFile> changedFiles;
	
	public Commit(String id, HashSet<ChangedFile> changedFiles) {
		this.id = id;
		this.changedFiles = new HashSet<ChangedFile>(changedFiles);
	}
	
	public String getID() {
		return id;
	}
	
	public HashSet<ChangedFile> getChangedFiles() {
		return changedFiles;
	}
	
	public int hashCode() {
		return id.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		} else {
			Commit f = (Commit) obj;
			return this.getChangedFiles().equals(f.getChangedFiles());
		}
	}
	
	public String toString() {
		String res = "Commit: " + id + "\n";
		for (ChangedFile file : changedFiles) {
			res += file.toString() + "\n";
		}
		res += "=========================================";
		return res;
	}
	
	public static class ChangedFile {
		private String fullName;
		private ChangedType type;
			
		public ChangedFile(String fullName, ChangedType type) {
			this.fullName = fullName;
			this.type = type;
		}
		
		public String getFullName() {
			return fullName;
		}
		
		public ChangedType getChangedType() {
			return type;
		}
		
		public int hashCode() {
			return fullName.hashCode();
		}
		
		public String toString() {
			return type + " " + fullName;
		}
		
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj == null || obj.getClass() != this.getClass()) {
				return false;
			} else {
				ChangedFile f = (ChangedFile) obj;
				return f.getChangedType() == this.getChangedType() && f.getFullName().equals(this.getFullName());
			}
		}
	}	
}
