package zyk.ssh2;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import zyk.util.LogType;
import zyk.util.LogUtil;


import java.io.*;

public class SshTemplate {
    private Connection conn;
    final int timeout = 500;

    public SshTemplate(String hostname, String username, String password, int port) {
        try {
            LogUtil.print(LogType.INFO, "==> 建立SSH连接;host = "+hostname+";username = "+username+";port = "+port);
            this.conn = (new Authenticate(hostname, username, password, port)).getConne();
            LogUtil.print(LogType.INFO, "<== 连接成功！");
        } catch (Exception var6) {
            LogUtil.print(LogType.ERROR, "<==连接服务器失败");
            var6.printStackTrace();
        }

    }

    public Result execCommand(String cmd) throws Exception {
       return execCommand(cmd,false);
    }

    private boolean resultPrint(Result result, Session sess, BufferedReader br, StringBuilder sb, String line) throws IOException {
        if (line == null) {
            br.close();
            result.sysout = sb.toString();
            LogUtil.print(LogType.INFO,"<== 控台打印: " + result.sysout);
            long c = System.currentTimeMillis();
            boolean b = true;
            while (sess.getExitStatus() == null){
                if(b){
                    System.out.print("<================================== 等待执行结果:");
                    b=false;
                }
                System.out.print(".");
                if(System.currentTimeMillis()-c>=timeout){
                    System.out.println();
                    LogUtil.print(LogType.ERROR,"<== 获取执行结果超时，请重新尝试执行！");
                    return false;
                }
            };
            if(!b)System.out.println();
            result.rc = sess.getExitStatus();
            result.isSuccess = result.rc == 0;
            LogUtil.print(LogType.INFO,"<== 执行结果: " + result.rc + " | " + result.isSuccess);
            return true;
        }
        return false;
    }

    public Result execCommand(String cmd, boolean keep) throws Exception {
        Result result = new Result();
        result.rc = 1;
        result.isSuccess = false;
        Session sess = null;
        PrintWriter out = null;
        try {
            LogUtil.print(LogType.INFO,"==> 执行命令: " + cmd);
            sess = this.conn.openSession();
            if(keep){
                sess.requestPTY("bash");
                sess.startShell();
                out = new PrintWriter(sess.getStdin());
                out.println(cmd);
                out.flush();
            }else{
                sess.execCommand(cmd);
            }

            InputStream stdout = new StreamGobbler(sess.getStdout());
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout));
            StringBuilder sb = new StringBuilder();

            while(true) {
                String line = br.readLine();
                if (resultPrint(result, sess, br, sb, line)) return result;

                if (!line.endsWith("\n")) {
                    line = line + "\n";
                }

                if (keep) {
                    System.out.println(line);
                }

                sb.append(line);
            }
        } catch (Exception var12) {
            LogUtil.print(LogType.ERROR, "<== "+ var12.getMessage());
            throw var12;
        } finally {
            if(out!=null){
                out.close();
            }
            if (sess != null) {
                sess.close();
            }

        }
    }

    public void close() {
        if (this.conn != null) {
            this.conn.close();
        }

    }

    public SCPClient createSCPClient() throws Exception {
        return this.conn != null ? this.conn.createSCPClient() : null;
    }
}
