package zone.rong.bansoukou.relauncher.processchecker;

public class J9ProcessChecker implements ProcessChecker {

    @Override
    public boolean checkExistence(String pid) {
        return ProcessHandle.of(Long.parseLong(pid)).map(ProcessHandle::isAlive).orElse(false);
    }

}
