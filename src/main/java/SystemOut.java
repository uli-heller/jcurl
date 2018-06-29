import lombok.Getter;
import lombok.Setter;

public class SystemOut {
    @Getter @Setter
    private boolean quiet = false;
    private SystemOut() {}

    private static class LazyHolder {
        static final SystemOut INSTANCE = new SystemOut();
    }

    public static SystemOut getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void println(String message) {
        SystemOut.println(quiet, message);
    }

    static public void println(boolean suppressOutput, String message) {
        if (! suppressOutput) {
            System.out.println(message);
        }
    }
}
