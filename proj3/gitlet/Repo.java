package gitlet;


import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;


/** Repo of gitlet.
 * @author Kevin Ren
 */

public class Repo implements Serializable {
    /** Tree of commits. */
    private HashMap<String, Commit> _commits;
    /** Map of branches. */
    private HashMap<String, String> _branches;
    /** Head pointer. */
    private String _head;
    /** All removed files. */
    private ArrayList<String> _removed;
    /** Parent ID. */
    private String _pid;
    /** Staging area. */
    private HashMap<String, Blob> _staging;
    /** Current snapshot. */
    private HashMap<String, Blob> _snapshot;
    /** Given branch's modified files. */
    private ArrayList<String> _givenMod;
    /** Current branch's modified files. */
    private ArrayList<String> _currentMod;
    /** Split. */
    private Commit _split;

    /** Constructor for a Repo. */
    public Repo() {
        Commit initial = new Commit("initial commit", "master");
        File gitlet = new File(".gitlet");
        gitlet.mkdir();
        File commits = new File(".gitlet/commits");
        commits.mkdir();
        File staging = new File(".gitlet/staging");
        staging.mkdir();
        String id = initial.id();
        File initFile = new File(".gitlet/commits/" + id);
        Utils.writeContents(initFile, Utils.serialize(initial));
        _head = "master";
        _branches = new HashMap<>();
        _branches.put("master", initial.id());
        _commits = new HashMap<>();
        _commits.put(initial.id(), initial);
        _pid = initial.id();
        _staging = new HashMap<>();
        _snapshot = new HashMap<>();
        _removed = new ArrayList<>();
    }

    /** Takes in NAME, adds it to staging sometimes. */
    public void add(String name) {
        File file = new File(name);
        if (!file.exists()) {
            Utils.message("File does not exist.");
            throw new GitletException();
        }
        File commitFile = new File(".gitlet/commits/" + _pid);
        Commit lastCommit = Utils.readObject(commitFile, Commit.class);
        Blob blob = new Blob(name);
        if (lastCommit.map() == null) {
            File newFile = new File(".gitlet/staging/" + blob.id());
            _staging.put(name, blob);
            Utils.writeObject(newFile, blob);
        } else if (lastCommit.map().containsKey(name)) {
            File newFile = new File(".gitlet/staging/" + blob.id());
            if (blob.id().
                    equals(lastCommit.map().get(name).id())) {
                if (_removed.contains(name)) {
                    _removed.remove(name);
                }
                if (newFile.exists()) {
                    _staging.remove(name);
                    newFile.delete();
                }
            } else {
                _staging.put(name, blob);
                Utils.writeObject(newFile, blob);
            }
        } else {
            File newFile = new File(".gitlet/staging/" + blob.id());
            _staging.put(name, blob);
            Utils.writeObject(newFile, blob);
        }
    }

    /** Takes in MESSAGE, and makes a commit of the current snapshot. */
    public void commit(String message) {
        File parentFile = new File(".gitlet/commits/" + _branches.get(_head));
        Commit parent = Utils.readObject(parentFile, Commit.class);
        String pid = parent.id();
        Commit commit = new Commit(message, pid, _snapshot, _head);
        if (_staging.isEmpty() && _removed.isEmpty()) {
            Utils.message("No changes added to the commit.");
            throw new GitletException();
        }
        HashMap<String, Blob> tracked = commit.map();
        if (_removed != null) {
            for (String fileName : _removed) {
                tracked.remove(fileName);
            }
        }
        for (String fileName : _staging.keySet()) {
            tracked.put(fileName, _staging.get(fileName));
            _snapshot.put(fileName, _staging.get(fileName));
        }
        File newFile = new File(".gitlet/commits/" + commit.id());
        Utils.writeObject(newFile, commit);

        if (_removed != null) {
            _removed.clear();
        }
        if (_staging != null) {
            _staging.clear();
        }

        _pid = commit.id();
        _branches.put(_head, commit.id());
    }

    /** Unstages the FILENAME if currently staged. */
    public void rm(String fileName) {
        File file = new File(fileName);
        boolean error = true;
        if (!_staging.isEmpty() && _staging.containsKey(fileName)) {
            File removed = new File(".gitlet/staging"
                + _staging.get(fileName).id());
            removed.delete();
            _staging.remove(fileName);
            if (_snapshot != null
                    && _snapshot.containsKey(fileName)) {
                _removed.add(fileName);
                _snapshot.remove(fileName);
            }
            error = false;
        }

        File commitFile = new File(".gitlet/commits/" + _pid);
        Commit lastCommit = Utils.readObject(commitFile, Commit.class);
        HashMap<String, Blob> tracked = lastCommit.map();
        if (tracked != null && lastCommit.map().containsKey(fileName)
                && tracked.containsKey(fileName)) {
            _snapshot.remove(fileName);
            _removed.add(fileName);
            File removed = new File(fileName);
            removed.delete();
            error = false;
        }

        if (error) {
            Utils.message("No reason to remove the file.");
            throw new GitletException();
        }
    }

    /** Prints out a log of the current repo. */
    public void log() {
        List<String> entries = new ArrayList<>();
        String temp = _branches.get(_head);
        while (temp != null) {
            File commitFile = new File(".gitlet/commits/" + temp);
            Commit commit = Utils.readObject(commitFile, Commit.class);
            logHelper(commit.id());
            temp = commit.pid();
        }
    }

    /** Takes in ID. */
    public void logHelper(String id) {
        File commitFile = new File(".gitlet/commits/" + id);
        Commit commit = Utils.readObject(commitFile, Commit.class);
        System.out.println("===");
        System.out.println("commit " + commit.id());
        System.out.println("Date: " + commit.timeStamp());
        System.out.println(commit.log());
        System.out.println();
    }

    /** Returns a global log. */
    public void globalLog() {
        File allCommits = new File(".gitlet/commits");
        File[] commitArr = allCommits.listFiles();
        for (File f : commitArr) {
            logHelper(f.getName());
        }
    }

    /** Finds the MESSAGE from the commits. */
    public void find(String message) {
        boolean exists = false;
        File allCommits = new File(".gitlet/commits");
        File[] commitArr = allCommits.listFiles();
        for (File f : commitArr) {
            String name = f.getName();
            File commitFile = new File(".gitlet/commits/" + name);
            Commit commit = Utils.readObject(commitFile, Commit.class);
            if (commit.log().equals(message)) {
                System.out.println(commit.id());
                exists = true;
            }
        }

        if (!exists) {
            Utils.message("Found no commit with that message.");
            throw new GitletException();
        }
    }

    /** Returns the current status of the repo. */
    public void status() {
        System.out.println("=== Branches ===");
        for (String branch : _branches.keySet()) {
            if (branch.equals(_head)) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for (Map.Entry file : _staging.entrySet()) {
            Blob b = (Blob) file.getValue();
            System.out.println(b.name());
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        if (!_removed.isEmpty()) {
            for (String s : _removed) {
                System.out.println(s);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not "
                + "Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files === ");
    }

    /** Checks for modified files, returns MODIFIED.*/
    private List<String> checkModified() {
        List<String> allFiles = Utils.plainFilenamesIn("user.dir");
        List<String> modified = new ArrayList<>();
        for (String s : allFiles) {
            File commitFile = new File(".gitlet/commits/" + _pid);
            Commit lastCommit = Utils.readObject(commitFile, Commit.class);
            HashMap<String, Blob> tracked = lastCommit.map();
            for (Map.Entry entry: tracked.entrySet()) {
                Blob last = (Blob) entry.getValue();
                Blob now = new Blob(s);
                if (!last.id().equals(now.id())) {
                    modified.add(now.name());
                }
            }
        }
        return modified;
    }

    /** Checkout function taking in OPERANDS. */
    public void checkout(List<String> operands) {
        String commitID = "";
        String fileName = "";
        if (operands.size() == 2 && operands.get(0).equals("--")) {
            fileName = operands.get(1);
            commitID = _branches.get(_head);
        } else if (operands.size() == 3 && operands.get(1).equals("--")) {
            commitID = operands.get(0);
            fileName = operands.get(2);
        } else if (operands.size() == 1) {
            checkoutBranch(operands.get(0));
            return;
        } else {
            Utils.message("Incorrect operands");
            throw new GitletException();
        }

        File commitFile = new File(".gitlet/commits/" + commitID);
        if (!commitFile.exists()) {
            Utils.message("No commit with that id exists.");
            throw new GitletException();
        }
        Commit commit = Utils.readObject(commitFile, Commit.class);
        HashMap<String, Blob> tracked = commit.map();
        if (tracked.containsKey(fileName)) {
            File file = new File(fileName);
            Blob blob = tracked.get(fileName);
            Utils.writeContents(file, blob.string());
        } else {
            Utils.message("File does not exist in that commit.");
            throw new GitletException();
        }
    }

    /** Helper function for checking out a BRANCH. */
    public void checkoutBranch(String branch) {
        if (branch.equals(_head)) {
            System.out.println("No need to checkout the current branch.");
            throw new GitletException();
        }
        if (!_branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            throw new GitletException();
        }

        File commitFile = new File(".gitlet/commits/" + _branches.get(branch));
        Commit commit = Utils.readObject(commitFile, Commit.class);
        HashMap<String, Blob> files = commit.map();
        checkUntracked(commit);
        HashMap<String, Blob> copy = new HashMap<>();

        HashSet<String> keys = new HashSet<>(_snapshot.keySet());
        for (String s : keys) {
            if (files == null) {
                Utils.restrictedDelete(new File(s));
                rm(s);
            } else {
                if (!files.containsKey(s)) {
                    Utils.restrictedDelete(new File(s));
                    rm(s);
                }
            }
        }

        if (files != null) {
            for (Map.Entry entry : files.entrySet()) {
                Blob blob = (Blob) entry.getValue();
                String name = blob.name();
                File newFile = new File(name);
                Utils.writeContents(newFile, blob.string());
                copy.put(name, blob);
            }
        }

        _snapshot.clear();
        _snapshot.putAll(copy);
        _pid = _branches.get(branch);
        _head = branch;

        File staging = new File(".gitlet/staging");
        for (File file : staging.listFiles()) {
            if (!file.isDirectory()) {
                file.delete();
            }
        }
        _staging.clear();
    }

    /** Checks for untracked files compared to COMMIT. */
    private void checkUntracked(Commit commit) {
        HashMap<String, Blob> map = commit.map();
        List<String> allFiles = Utils.plainFilenamesIn(
                System.getProperty("user.dir"));
        if (_snapshot == null) {
            if (allFiles.size() > 0) {
                Utils.message("There is an untracked file in "
                        + "the way; delete it or add it first.");
                throw new GitletException();
            }
        }
        for (String s : allFiles) {
            if (!_snapshot.containsKey(s) && map != null
                    && map.containsKey(s)) {
                Utils.message("There is an untracked file in "
                        + "the way; delete it or add it first.");
                throw new GitletException();
            }
        }
    }

    /** Removes a BRANCH. */
    public void rmBranch(String branch) {
        if (!_branches.keySet().contains(branch)) {
            Utils.message("A branch with that name does not exist.");
            throw new GitletException();
        } else if (_head.equals(branch)) {
            Utils.message("Cannot remove the current branch.");
            throw new GitletException();
        } else {
            _branches.remove(branch);
        }
    }

    /** Resets to COMMITID. */
    public void reset(String commitID) {
        if (!new File(".gitlet/commits/" + commitID).exists()) {
            System.out.println("No commit with that id exists.");
            throw new GitletException();
        }

        File commitFile = new File(".gitlet/commits/" + commitID);
        Commit commit = Utils.readObject(commitFile, Commit.class);
        HashMap<String, Blob> files = commit.map();

        checkUntracked(commit);

        if (_snapshot != null) {
            HashSet<String> keys = new HashSet<>(_snapshot.keySet());
            for (String s : keys) {
                if (files == null) {
                    Utils.restrictedDelete(new File(s));
                    rm(s);
                } else if (!files.containsKey(s)) {
                    Utils.restrictedDelete(new File(s));
                    rm(s);
                }
            }
        }

        if (files != null) {
            for (String s : files.keySet()) {
                String branchName = _branches.get(_head);
                ArrayList<String> operands = new ArrayList<>();
                operands.add(branchName);
                operands.add("--");
                operands.add(s);
                checkout(operands);
                add(s);
            }
        }

        if (_staging != null) {
            _staging.clear();
        }
        _branches.put(_head, commitID);

    }

    /** Creates a branch with NAME. */
    public void branch(String name) {
        if (_branches.containsKey(name)) {
            Utils.message("A branch with that name already exists.");
        } else {
            _branches.put(name, _pid);
            File commitFile = new File(".gitlet/commits/" + _pid);
            Commit commit = Utils.readObject(commitFile, Commit.class);
        }
    }

    /** Returns latest common ANCESTOR of BRANCH1 and BRANCH2. */
    private Commit lca(String branch1, String branch2) {
        ArrayList<String> branch1Commits = new ArrayList<String>();
        ArrayList<String> branch2Commits = new ArrayList<String>();

        String pid1 = _branches.get(branch1);
        String pid2 = _branches.get(branch2);

        while (pid1 != null) {
            branch1Commits.add(pid1);
            File commitFile = new File(".gitlet/commits/" + pid1);
            Commit commit = Utils.readObject(commitFile, Commit.class);
            pid1 = commit.pid();
        }
        while (pid2 != null) {
            branch2Commits.add(pid2);
            File commitFile = new File(".gitlet/commits/" + pid2);
            Commit commit = Utils.readObject(commitFile, Commit.class);
            pid2 = commit.pid();
        }
        for (String s : branch1Commits) {
            if (branch2Commits.contains(s)) {
                File commitFile = new File(".gitlet/commits/" + s);
                Commit commit = Utils.readObject(commitFile, Commit.class);
                return commit;
            }
        }
        return null;
    }

    /** The merge function takes in GIVENBRANCH, good luck have fun. */
    public void merge(String givenBranch) {
        mergeErrors(givenBranch);
        String branchID = _branches.get(givenBranch);
        File givenBranchHeadFile = new File(".gitlet/commits/" + branchID);
        Commit givenHead = Utils.readObject(givenBranchHeadFile, Commit.class);
        _split = lca(givenBranch, _head);
        File currentBranchHeadFile = new File(".gitlet/commits/" + _pid);
        Commit currentHead =
                Utils.readObject(currentBranchHeadFile, Commit.class);
        if (_split.id().equals
                (_branches.get(givenBranch))) {
            Utils.message(" Given branch is an"
                    + "ancestor of the current branch.");
            throw new GitletException();
        } else if (_split.id().equals(_pid)) {
            Utils.message("Current branch fast-forwarded.");
            throw new GitletException();
        }

        checkUntracked(givenHead);

        _givenMod = checkMod(givenHead, _split);
        _currentMod = checkMod(currentHead, _split);

        mergeHelper(givenBranch);

        HashMap<String, Blob> currentMap = currentHead.map();
        HashMap<String, Blob> givenMap = givenHead.map();
        for (String s : givenMap.keySet()) {
            if (_split.map() == null || !_split.map().containsKey(s)) {
                if (_currentMod.contains(s)) {
                    if (_givenMod.contains(s)) {
                        mergeConflict(givenBranch, s);
                    }
                } else {
                    String branchName = _branches.get(givenBranch);
                    ArrayList<String> operands = new ArrayList<>();
                    operands.add(branchName);
                    operands.add("--");
                    operands.add(s);
                    checkout(operands);
                    add(s);
                }
            } else if (!currentMap.containsKey(s)) {
                if (!givenMap.get(s).id().equals
                        (_split.map().get(s).id())) {
                    String branchName = _branches.get(givenBranch);
                    ArrayList<String> operands = new ArrayList<>();
                    operands.add(branchName);
                    operands.add("--");
                    operands.add(s);
                    checkout(operands);
                    add(s);
                }
            }
        }
        commit("Merged " + givenBranch + " into " + _head + ".");
    }

    /** Helper, takes in BRANCH. */
    private void mergeHelper(String branch) {
        String branchID = _branches.get(branch);
        File givenBranchHeadFile = new File(".gitlet/commits/" + branchID);
        Commit givenHead =
                Utils.readObject(givenBranchHeadFile, Commit.class);
        File currentBranchHeadFile = new File(".gitlet/commits/" + _pid);
        Commit currentHead =
                Utils.readObject(currentBranchHeadFile, Commit.class);

        HashMap<String, Blob> splitFiles = _split.map();
        HashMap<String, Blob> givenMap = givenHead.map();
        HashMap<String, Blob> currentMap = currentHead.map();


        if (splitFiles != null) {
            for (String s : splitFiles.keySet()) {
                if (!currentHead.map().containsKey(s)
                        && !givenHead.map().containsKey(s)) {
                    Utils.restrictedDelete(new File(s));
                    rm(s);
                } else if (_currentMod.contains(s) && _givenMod.contains(s)) {
                    if (!givenMap.get(s).id().equals(currentMap.get(s).id())) {
                        mergeConflict(branch, s);
                    }
                } else if (_givenMod.contains(s)) {
                    String branchName = _branches.get(branch);
                    ArrayList<String> operands = new ArrayList<>();
                    operands.add(branchName);
                    operands.add("--");
                    operands.add(s);
                    checkout(operands);
                    add(s);
                } else if (!givenMap.containsKey(s)
                        && !_currentMod.contains(s)) {
                    _snapshot.remove(s);
                    Utils.restrictedDelete(new File(s));
                }
            }
        }
    }

    /** Handles merge conflicts, takes in BRANCH, FILENAME. */
    private void mergeConflict(String branch, String fileName) {
        String branchID = _branches.get(branch);
        File givenBranchHeadFile = new File(".gitlet/commits/" + branchID);
        Commit givenHead =
                Utils.readObject(givenBranchHeadFile, Commit.class);
        File currentBranchHeadFile = new File(".gitlet/commits/" + _pid);
        Commit currentHead =
                Utils.readObject(currentBranchHeadFile, Commit.class);
        HashMap<String, Blob> currentMap = currentHead.map();
        HashMap<String, Blob> givenMap = givenHead.map();

        Blob c;
        String currentContents;
        if (currentMap.containsKey(fileName)) {
            c = currentMap.get(fileName);
            currentContents = c.string();
        } else {
            c = null;
            currentContents = "";
        }

        Blob g;
        String givenContents;
        if (givenMap.containsKey(fileName)) {
            g = givenMap.get(fileName);
            givenContents = g.string();
        } else {
            g = null;
            givenContents = "";
        }

        String contents = "<<<<<<< HEAD\n";
        contents += currentContents;
        contents += "=======\n" + givenContents;
        contents += ">>>>>>>\n";
        Utils.writeContents(new File(fileName), contents);
        add(fileName);
        Utils.message("Encountered a merge conflict.");
    }

    /** Checks for modified files compared to a commit.
     * Takes in CURRENT, COMPARETO. Returns MODIFIED */
    private ArrayList<String> checkMod(Commit current, Commit compareTo) {
        List<String> allFiles = Utils.plainFilenamesIn(
                System.getProperty("user.dir"));
        ArrayList<String> modified = new ArrayList<>();
        if (allFiles != null && !allFiles.isEmpty()) {
            for (String s : allFiles) {
                HashMap<String, Blob> thisMap = current.map();
                HashMap<String, Blob> thatMap = compareTo.map();
                if (thisMap != null && thatMap != null) {
                    if (thisMap.containsKey(s)
                            && thatMap.containsKey(s)) {
                        Blob thisBlob = thisMap.get(s);
                        Blob thatBlob = thatMap.get(s);
                        if (!thisBlob.id().equals(thatBlob.id())) {
                            modified.add(s);
                        }
                    }
                }
            }
        }
        return modified;
    }

    /** Checks for errors in the merge command. Takes in BRANCH. */
    private void mergeErrors(String branch) {
        if (!_staging.isEmpty()) {
            Utils.message("You have uncommitted changes.");
            throw new GitletException();
        } else if (!_branches.containsKey(branch)) {
            Utils.message("A branch with that name does not exist.");
            throw new GitletException();
        } else if (_head.equals(branch)) {
            Utils.message("Cannot merge a branch with itself.");
            throw new GitletException();
        }
    }

    /** Returns commit map. */
    public HashMap<String, Commit> commitHashMap() {
        return _commits;
    }

    /** Returns the branches. */
    public HashMap<String, String> branches() {
        return _branches;
    }

    /** Returns head pointer. */
    public String head() {
        return _head;
    }

}
