package zone.rong.bansoukou.processcheck;

public class J9ProcessMaintainer implements ProcessMaintainer {

    private long pid = -1;

    @Override
    public boolean checkExistence(String pid) {
        if (this.pid == -1) {
            this.pid = Long.parseLong(pid);
        }
        return ProcessHandle.allProcesses().anyMatch(h -> h.pid() == this.pid);
    }

}
