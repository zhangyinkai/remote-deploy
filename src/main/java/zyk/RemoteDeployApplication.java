package zyk;

import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SCPOutputStream;
import zyk.ssh2.Result;
import zyk.ssh2.SshTemplate;
import zyk.util.LogType;
import zyk.util.LogUtil;
import zyk.util.PackUtil;
import zyk.util.ParamUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


 
public class RemoteDeployApplication {
    private static HashMap<String, String> decompressionMap = new HashMap();
    private static ArrayList<String> execList = new ArrayList(7);

    public RemoteDeployApplication() {
    }

    public static void main(String[] args) throws Exception{
        init(args);
        run();
    }

    private static SshTemplate getTemplate(){
        String hostname = ParamUtil.param.getProperty("hostname", "192.168.199.252");
        String username = ParamUtil.param.getProperty("username", "root");
        String password = ParamUtil.param.getProperty("password", "");
        int port = Integer.valueOf(ParamUtil.param.getProperty("port", "22"));
        return  new SshTemplate(hostname, username, password, port);
    }


    public static void run() {
        SshTemplate sshTemplate = null;
        try {
            boolean flag = !ParamUtil.param.getProperty("exec_break_flag", "true").equals("false");
            String cases = ParamUtil.param.getProperty("exec_case", "1,2,3,4,5").replaceAll("\\s", "");
            if (cases.endsWith(",")) {
                cases = cases.substring(0, cases.length() - 1);
            }else if("7".equals(cases)){
                //只有打包
                PackUtil.packIncrement();
                return;
            }
            sshTemplate = getTemplate();
            String[] caseSet = cases.split(",");
            ArrayList<Integer> list = new ArrayList(execList.size());
            String[] arr$ = caseSet;
            int len$ = caseSet.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                String exec_case = arr$[i$];
                try {
                    int i = Integer.valueOf(exec_case);
                    if (i <= 0 || i > execList.size()) {
                        throw new Exception("exec case set fail . must be an integer of 1-5");
                    }

                    list.add(i - 1);
                } catch (Exception var20) {
                    throw new Exception("exec case set fail . Illegal character " + exec_case + ", must be an integer of 1-5");
                }
            }

            Class clazz = RemoteDeployApplication.class;
            Object obj = clazz.newInstance();
            Iterator i$ = list.iterator();

            while(i$.hasNext()) {
                int i = (Integer)i$.next();
                try {
                    Method method = clazz.getMethod((String)execList.get(i), sshTemplate.getClass());
                    method.invoke(obj, sshTemplate);
                } catch (Exception var21) {
                    var21.printStackTrace();
                    if (flag) {
                        throw new Exception("exception breaking ..");
                    }
                }
            }
        } catch (Exception var22) {
            var22.printStackTrace();
        } finally {
            if (sshTemplate != null) {
                sshTemplate.close();
            }

        }

    }

    /**
     *  增量打包
     * @return
     * @throws Exception
     */
    public void packIncrement(SshTemplate sshTemplate) throws Exception{
        String increment_path = PackUtil.packIncrement();
        if(increment_path!=null&&!increment_path.trim().isEmpty())ParamUtil.param.setProperty("increment_path",increment_path);
    }


    /**
     * 备份数据
     * @param sshTemplate
     * @throws Exception
     */
    public void bakSource(SshTemplate sshTemplate) throws Exception {
        LogUtil.print(LogType.INFO,"--------------------begin backup file ----------------------");
        String tomcatDir = ParamUtil.param.getProperty("tomcat_dir");
        if (!tomcatDir.endsWith("/")) {
            tomcatDir = tomcatDir + "/";
        }

        boolean b = this.checkDir(sshTemplate, tomcatDir);
        if (!b) {
            throw new Exception("exec check dir error : " + tomcatDir);
        } else {
            String testDir = tomcatDir + "backup";
            Result checkDir = sshTemplate.execCommand("cd " + testDir);
            if (!checkDir.isSuccess && !sshTemplate.execCommand("mkdir " + testDir).isSuccess) {
                throw new Exception("exec mkdir fail : " + testDir);
            } else {
                String appName = ParamUtil.param.getProperty("app_name");
                String tar_dir = tomcatDir + "webapps" + "/";
                String bak_cmd = "cd " + testDir + " ; backtime=`date +%Y%m%d%H%M%S`;" + "tar -czvf " + appName + "$backtime.tar.gz " + tar_dir + appName;
                if (sshTemplate.execCommand(bak_cmd).isSuccess) {
                    LogUtil.print(LogType.INFO,"exec backup success.");
                    LogUtil.print(LogType.INFO,"--------------------backup file end----------------------");
                } else {
                    throw new Exception("exec backup fail.");
                }
            }
        }
    }

    /**
     *  停服
     * @param sshTemplate
     * @throws Exception
     */
    public void killService(SshTemplate sshTemplate) throws Exception {
        LogUtil.print(LogType.INFO,"--------------------begin kill service ----------------------");
        String tomcatDir = ParamUtil.param.getProperty("tomcat_dir");
        if (!tomcatDir.endsWith("/")) {
            tomcatDir = tomcatDir + "/";
        }

        String app_port = ParamUtil.param.getProperty("app_port");
        boolean b = this.checkDir(sshTemplate, tomcatDir);
        if (!b) {
            throw new Exception("exec check dir error : " + tomcatDir);
        } else {
            String pid_cmd = "/usr/sbin/lsof -i:" + app_port + " | grep -v 'grep' | grep 'LISTEN' |awk 'NR==1{print $2}'";
            String pid = "";
            Result pidRes = sshTemplate.execCommand(pid_cmd);
            if (pidRes.isSuccess && pidRes.sysout != null && pidRes.sysout.replaceAll("\\s", "").length() > 0) {
                pid = pidRes.sysout.replaceAll("\\s", "");
                String repCheckCmd = "ps -ef | grep -v 'grep' | grep '" + tomcatDir + "'";
                Result repCheckRes = sshTemplate.execCommand(repCheckCmd);
                if (repCheckRes.isSuccess) {
                    if (!repCheckRes.sysout.contains(tomcatDir)) {
                        throw new Exception(" app_port and tomcat_dir not match .");
                    } else {
                        String killCmd = "kill -9 " + pid;
                        Result result = sshTemplate.execCommand(killCmd);
                        if (result.isSuccess) {
                            LogUtil.print(LogType.INFO,"exec kill success.");
                            LogUtil.print(LogType.INFO,"--------------------kill service end----------------------");
                        } else {
                            throw new Exception("exec kill fail : " + app_port);
                        }
                    }
                } else {
                    throw new Exception("exec find app port error : " + app_port);
                }
            } else {
                throw new Exception("exec find app port error : " + app_port);
            }
        }
    }

    /**
     * 清理缓存文件
     * @param sshTemplate
     * @throws Exception
     */
    public void cleanFile(SshTemplate sshTemplate) throws Exception {
        LogUtil.print(LogType.INFO,"--------------------begin clean----------------------");
        String tomcatDir = ParamUtil.param.getProperty("tomcat_dir");
        if (!tomcatDir.endsWith("/")) {
            tomcatDir = tomcatDir + "/";
        }

        //String tar_dir = tomcatDir + "webapps" + "/";
        String clean_cache = tomcatDir + "work";
        boolean b = this.checkDir(sshTemplate, clean_cache);
        if (!b) {
            throw new Exception("exec check dir error : " + clean_cache);
        } else {
            String clean_cache_cmd = "rm -rf " + clean_cache + "/*";
            Result clean_cache_cmd_result = sshTemplate.execCommand(clean_cache_cmd);
            if (clean_cache_cmd_result.isSuccess) {
                LogUtil.print(LogType.INFO,"exec clean cache file success.");
            }

         /*   String clean_cmd = "cd " + tar_dir + ";rm -rf " + appName + " " + appName + ".war";
            Result result = sshTemplate.execCommand(clean_cmd);
            if (result.isSuccess) {
                LogUtil.print(LogType.INFO,"exec clean file success.");
                LogUtil.print(LogType.INFO,"--------------------clean end----------------------");
            } else {
                throw new Exception("exec clean file fail : " + tar_dir);
            }*/
        }
    }

    /**
     * 上传文件
     * @param sshTemplate
     * @throws Exception
     */
    public void scpPut(SshTemplate sshTemplate) throws Exception {
        LogUtil.print(LogType.INFO,"--------------------begin upload----------------------");
        String tomcatDir = ParamUtil.param.getProperty("tomcat_dir");
        if (!tomcatDir.endsWith("/")) {
            tomcatDir = tomcatDir + "/";
        }

        String tar_dir = tomcatDir + "webapps" + "/";
        boolean b = this.checkDir(sshTemplate, tar_dir);
        if (!b) {
            throw new Exception("exec check dir error : " + tar_dir);
        } else {
            SCPClient scpClient = sshTemplate.createSCPClient();
            String war_path = ParamUtil.param.getProperty("war_path");
            File file = new File(war_path);
            if (!file.isFile()) {
                throw new Exception("war path fail : " + war_path);
            } else {
                LogUtil.print(LogType.INFO,"executing upload file please wait success ...");
                scpPutFile(tar_dir, scpClient, file);
                LogUtil.print(LogType.INFO,"exec upload file success.");
                LogUtil.print(LogType.INFO,"--------------------upload end----------------------");
            }
        }
    }

    private void scpPutFile(String tar_dir, SCPClient scpClient, File file) throws IOException {
        SCPOutputStream scpOutputStream = scpClient.put(file.getName(),file.length(), tar_dir,null);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] bytes = null;
        if(file.length()<=0){
            return;
            //小于1M的文件
        }else if(file.length()<=1024*1024){
            bytes = new byte[(int)file.length()];
        }else{
            bytes = new byte[1024];
        }
        while (fileInputStream.read(bytes)!=-1){
            scpOutputStream.write(bytes);
        }
        scpOutputStream.flush();
        scpOutputStream.close();
        return;
    }

    /**
     * 启动服务器
     * @param sshTemplate
     * @throws Exception
     */
    public void start(SshTemplate sshTemplate) throws Exception {
        LogUtil.print(LogType.INFO,"--------------------begin start service----------------------");
        String tomcatDir = ParamUtil.param.getProperty("tomcat_dir");
        if (!tomcatDir.endsWith("/")) {
            tomcatDir = tomcatDir + "/";
        }

        String tar_dir = tomcatDir + "bin" + "/";
        boolean b = this.checkDir(sshTemplate, tar_dir);
        if (!b) {
            throw new Exception("exec check dir error : " + tar_dir);
        } else {
            String start_cmd = "cd " + tar_dir + ";./startup.sh;tail -f ../" + "logs/catalina.out";

            Result result = sshTemplate.execCommand(start_cmd, true);
            if (result.isSuccess) {
                LogUtil.print(LogType.INFO,"exec start  success.");
                LogUtil.print(LogType.INFO,"--------------------start service end----------------------");
            } else {
                throw new Exception("exec start  fail : " + tar_dir);
            }
        }
    }

    /**
     * 增量打包
     * @param sshTemplate
     * @throws Exception
     */
    public void decompression(SshTemplate sshTemplate) throws Exception {
        LogUtil.print(LogType.INFO,"--------------------increment start----------------------");
        String tomcatDir = ParamUtil.param.getProperty("tomcat_dir");
        if (!tomcatDir.endsWith("/")) {
            tomcatDir = tomcatDir + "/";
        }

        String tar_dir = tomcatDir + "webapps" + "/";
        boolean b = this.checkDir(sshTemplate, tar_dir);
        if (!b) {
            throw new Exception("exec check dir error : " + tar_dir);
        } else {
            String appName = ParamUtil.param.getProperty("app_name");
            //防止更新文件更多存留   Add*.zip
            String cleanOldFile = "rm -rf "+tar_dir+appName+"Add*.*";
            sshTemplate.execCommand(cleanOldFile);

            SCPClient scpClient = sshTemplate.createSCPClient();
            String increment_path = ParamUtil.param.getProperty("increment_path");
            File file = new File(increment_path);
            if (!file.isFile()) {
                throw new Exception("increment  path fail : " + increment_path);
            } else {
                String extFileName = getFileExtName(increment_path);
                if (!decompressionMap.containsKey(extFileName)) {
                    throw new Exception("increment  path fail  : Must  tar,tar.gz,tar.bz2,tar.bz,tar.Z,zip ");
                } else {
                    LogUtil.print(LogType.INFO,"executing upload file please wait success ...");
                    scpPutFile(tar_dir, scpClient, file);
                    LogUtil.print(LogType.INFO,"exec upload file success.");
                    LogUtil.print(LogType.INFO,"exec decompression  start.");
                    String cmd = "cd " + tar_dir + ";yes|" + (String)decompressionMap.get(extFileName) + " " + file.getName();
                    Result result = sshTemplate.execCommand(cmd);
                    if (result.isSuccess) {
                        LogUtil.print(LogType.INFO,"exec decompression  success.");
                        LogUtil.print(LogType.INFO,"--------------------increment end----------------------");
                    } else {
                        throw new Exception("exec decompression  fail : " + increment_path);
                    }
                }
            }
        }
    }

    public static void init(String[] args) throws Exception{
        PackUtil.init(args);

        execList.add("bakSource"); //备份
        execList.add("killService"); //停服
        execList.add("cleanFile"); //清理缓存
        execList.add("scpPut"); //上传war包
        execList.add("start");//启动服务
        execList.add("decompression"); //解压（增量）
        execList.add("packIncrement"); //增量打包
        decompressionMap.put(".tar", "tar -xvf");
        decompressionMap.put(".tar.gz", "tar -zxvf");
        decompressionMap.put(".tar.bz2", "tar jxvf");
        decompressionMap.put(".tar.bz", "tar jxvf");
        decompressionMap.put(".tar.Z", "tar Zxvf");
        decompressionMap.put(".zip", "unzip");

        String logLevelStr = ParamUtil.param.getProperty("logLevel","INFO");
        try {
            LogUtil.setLogLevel(LogType.valueOf(logLevelStr));
        }catch (Exception e){

        }
    }

    /**
     * 检查目录是否存在
     * @param sshTemplate
     * @param testDir
     * @return
     */
    private boolean checkDir(SshTemplate sshTemplate, String testDir) {
        boolean flag = false;

        try {
            Result checkDir = sshTemplate.execCommand("cd " + testDir);
            if (checkDir.isSuccess) {
                flag = true;
            }
        } catch (Exception var5) {
            LogUtil.print(LogType.INFO,"exec check dir error : " + testDir);
            var5.printStackTrace();
        }

        return flag;
    }

    private static String getFileExtName(String filePath) {
        return filePath.substring(filePath.indexOf("."));
    }
}