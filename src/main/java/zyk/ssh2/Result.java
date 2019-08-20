package zyk.ssh2;

public class Result {
    public int rc;
    public String sysout;
    public String error_msg;
    public boolean isSuccess;

    public Result() {
    }

    void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }
}