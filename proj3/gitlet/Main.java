package gitlet;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Kevin Ren
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            if (args.length == 0) {
                Utils.message("Please enter a command.");
                throw new GitletException();
            }
            String command = args[0];
            if (isValid(command)) {
                String[] opArr = Arrays.copyOfRange(args, 1, args.length);
                ArrayList<String> operands = new ArrayList<>();
                operands.addAll(Arrays.asList(opArr));
                if (initialized()) {
                    File repoFile = new File(path);
                    repo = Utils.readObject(repoFile, Repo.class);
                    if (commands1.contains(command)) {
                        execute(command, operands);
                    } else if (commands2.contains(command)) {
                        execute2(command, operands);
                    } else if (commands3.contains(command)) {
                        execute3(command, operands);
                    }
                    File newFile = new File(path);
                    Utils.writeObject(newFile, repo);
                } else if (command.equals("init")) {
                    repo = new Repo();
                    File repoFile = new File(path);
                    Utils.writeObject(repoFile, repo);
                } else {
                    Utils.message("Not in an initialized Gitlet directory.");
                    throw new GitletException();
                }
            } else {
                Utils.message("No command with that name exists.");
                throw new GitletException();
            }
        } catch (GitletException e) {
            System.exit(0);
        }
    }

    /** Returns true if repo has been initialized, false otherwise. */
    private static boolean initialized() {
        return Files.exists(Paths.get(".gitlet"));
    }

    /** Takes in COMMAND, returns validity of the command. */
    private static boolean isValid(String command) {
        return commands.contains(command);
    }

    /** Takes in COMMAND and OPERANDS, executes the command. */
    private static void execute(String command,
                                ArrayList<String> operands) {
        switch (command) {
        case "init" :
            Utils.message("A Gitlet version-control system "
                + "already exists in the current directory.");
            throw new GitletException();
        case "add" :
            if (operands.size() != 1) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            repo.add(operands.get(0));
            break;
        case "commit" :
            if (operands.size() == 1) {
                if (operands.get(0).equals("")) {
                    Utils.message("Please enter a commit message.");
                    throw new GitletException();
                }
                repo.commit(operands.get(0));
            } else if (operands.size() == 0) {
                Utils.message("Please enter a commit message.");
                throw new GitletException();
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            break;
        default:
            Utils.message("something wrong");
        }
    }

    /** Takes in COMMAND and OPERANDS, executes the command. */
    private static void execute2(String command, ArrayList<String> operands) {
        switch (command) {
        case "find" :
            if (operands.size() == 1) {
                repo.find(operands.get(0));
                break;
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
        case "status":
            if (operands.size() == 0) {
                repo.status();
                break;
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
        case "branch":
            if (operands.size() == 1) {
                repo.branch(operands.get(0));
                break;
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
        case "rm-branch":
            if (operands.size() == 1) {
                repo.rmBranch(operands.get(0));
                break;
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
        case "reset":
            if (operands.size() == 1) {
                repo.reset(operands.get(0));
                break;
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
        case "merge":
            if (operands.size() == 1) {
                repo.merge(operands.get(0));
                break;
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
        default:
            Utils.message("something wrong");
        }
    }

    /** Takes in COMMAND and OPERANDS, executes the command. */
    private static void execute3(String command,
                                ArrayList<String> operands) {
        switch (command) {
        case "rm" :
            if (operands.size() == 1) {
                repo.rm(operands.get(0));
            } else {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            break;
        case "log" :
            if (operands.size() == 0) {
                repo.log();
                break;
            } else {
                Utils.message("Incorrect Argument");
                throw new GitletException();
            }
        case "checkout" :
            if (operands.size() == 0 || operands.size() > 3) {
                Utils.message("Incorrect operands.");
                throw new GitletException();
            }
            repo.checkout(operands);
            break;
        case "global-log" :
            if (operands.size() == 0) {
                repo.globalLog();
                break;
            } else {
                Utils.message("Incorrect Argument");
                throw new GitletException();
            }
        default:
            Utils.message("something wrong");
        }
    }

    /** The big Repo. */
    private static Repo repo;
    /** Repo path. */
    private static String path = ".gitlet/repo";
    /** Lists all valid commands. */
    private static List<String> commands = new ArrayList<>(
            Arrays.asList("init", "add",
            "commit", "rm", "log", "global-log",
            "find", "status", "checkout",
            "branch", "rm-branch", "reset", "merge"));
    /** First commands. */
    private static List<String> commands1 = new ArrayList<>(
            Arrays.asList("init", "add",
                    "commit"));
    /** Second commands. */
    private static List<String> commands2 = new ArrayList<>(
            Arrays.asList("find", "status",
                    "branch", "rm-branch", "reset",
                    "merge"));
    /** Third commands. */
    private static List<String> commands3 = new ArrayList<>(
            Arrays.asList("rm", "log", "global-log",
                    "checkout"));
}
