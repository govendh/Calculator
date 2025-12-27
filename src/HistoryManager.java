import java.io.*;
import java.nio.file.*;

public class HistoryManager {

    private static final Path FILE =
            Paths.get(System.getProperty("user.home"), "AdvancedCalculator", "history.txt");

    static {
        try {
            Files.createDirectories(FILE.getParent());
            if (!Files.exists(FILE)) {
                Files.createFile(FILE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save(String entry) {
        try (FileWriter fw = new FileWriter(FILE.toFile(), true)) {
            fw.write(entry + System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String load() {
        try {
            return Files.readString(FILE);
        } catch (IOException e) {
            return "";
        }
    }

    public static void clear() {
        try {
            Files.writeString(FILE, "");
        } catch (IOException ignored) {}
    }
}
