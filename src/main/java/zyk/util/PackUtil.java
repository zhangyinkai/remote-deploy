package zyk.util;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * idea 适配SVN 打增量包
 */
public class PackUtil {

    //#上个版本号
    private static String upperVersion ;
    //#当前版本号
    private static String currentVersion = "HEAD";
    //#svn地址包括项目名
    private static String svnPath;
    //#项目编译后地址包括项目名
    private static String classPath;
    //应用名称
    private static String appName;

    //生成对应目录
    private static String rootPath;

    public static String packIncrement() throws Exception{
        return execCmd(buildSVNCmd());
    }

    /**
     * 复制文件
     * @param originPath
     * @param targetPath
     * @throws Exception
     */
    private static void copyFile(String originPath,String targetPath){
        File originFile = new File(originPath);
        if(!originFile.exists()) {
            LogUtil.print(LogType.ERROR,"源文件不存在！"+originPath);
            return;
        };
        if(originFile.isDirectory()) return;//文件夹不处理
        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;
        try{
            File targetFile = new File(targetPath);
            //目录不存在创建
            if(!targetFile.exists()){
                File dic = targetFile.getParentFile();
                if(!dic.exists()){
                    dic.mkdirs();
                }
                targetFile.createNewFile();
            }
            fileInputStream = new FileInputStream(originFile);
            fileOutputStream = new FileOutputStream(targetPath,false);
            byte[] bytes = null;
            if(originFile.length()<=0){
                LogUtil.print(LogType.WARN,"源文件大小为0！"+originPath);
                return;
                //小于1M的文件
            }else if(originFile.length()<=1024*1024){
                bytes = new byte[(int)originFile.length()];
            }else{
                bytes = new byte[1024];
            }
            while (fileInputStream.read(bytes)!=-1){
                fileOutputStream.write(bytes);
            }
            fileOutputStream.flush();
            LogUtil.print(LogType.INFO,"copy done ==> "+targetPath);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(fileOutputStream!=null){
                try {
                    fileOutputStream.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            if(fileInputStream!=null){
                try {
                    fileInputStream.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 获取对应的文件
     * @param arr
     */
    private static void findFile(String[] arr){
        // 创建root目录
        String originPath = arr[1];
        if(arr[0].equalsIgnoreCase("D")){
            LogUtil.print(LogType.INFO,"<== skip D");
        }else if(originPath.toLowerCase().indexOf("webroot\\")!=-1){
            //查找到webroot 下的静态资源以及jar包
            String filePath = originPath.substring(originPath.indexOf("WebRoot")+7);
            copyFile(originPath,rootPath+filePath);
        }else if(originPath.toLowerCase().indexOf("webapp\\")!=-1){
            //查找到webapp 下的静态资源以及jar包
            String filePath = originPath.substring(originPath.indexOf("webapp")+6);
            copyFile(originPath,rootPath+filePath);
        }else if(originPath.indexOf("config\\")!=-1){
            //配置文件
            String filePath = originPath.substring(originPath.indexOf("config")+6);
            copyFile(originPath,rootPath+"\\WEB-INF\\classes"+filePath);
        }else if(originPath.indexOf("src\\")!=-1&&originPath.indexOf(".java")!=-1){
            if(originPath.indexOf("src\\main\\java\\")!=-1&&originPath.indexOf(".java")!=-1){
                originPath = originPath.replace("\\main\\java","");
            }
            //class文件
            String fileTemp = originPath.substring(originPath.indexOf("src")+3);

            String fileOriginPath = classPath+(fileTemp.replaceAll("\\.java",".class"));
            LogUtil.print(LogType.DEBUG,"fileOriginPath = "+fileOriginPath);
            String fileOriginDic = fileOriginPath.substring(0,fileOriginPath.lastIndexOf("\\"));
            LogUtil.print(LogType.DEBUG,"fileOriginDic = "+fileOriginDic);

            String filePath = rootPath+"\\WEB-INF\\classes"+(fileTemp.replaceAll(".java",".class"));
            LogUtil.print(LogType.DEBUG,"filePath = "+filePath);
            final String fileDic = filePath.substring(0,filePath.lastIndexOf("\\"));
            LogUtil.print(LogType.DEBUG,"fileDic = "+fileDic);
            final String fileName = filePath.substring(filePath.lastIndexOf("\\")+1).replace(".class","");
            LogUtil.print(LogType.DEBUG,"fileName = "+fileName);

            copyFile(fileOriginPath,filePath);
            //获取内部类 或者 匿名类
            File dic = new File(fileOriginDic);
            if(dic.exists()){
               dic.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        if(file.getPath().indexOf(fileName+"$")!=-1&&file.getPath().indexOf(".class")!=-1){
                            copyFile(file.getPath(),fileDic+"\\"+file.getName());
                        };
                        return false;
                    }
                });
            }

        }else{
            LogUtil.print(LogType.WARN,"<== skip");
        }

    }


    /**
     * 执行CMD
     * @param cmd
     */
    private static String execCmd(String cmd) throws Exception{
        Runtime run = Runtime.getRuntime();
        LogUtil.print(LogType.INFO,"================执行获取修改列表 开始================");
        LogUtil.print(LogType.INFO,"执行命令："+cmd);
        BufferedReader br = null;
        boolean b = true;
        try {
            Process process = run.exec(cmd);
            InputStream in = process.getInputStream();
            InputStreamReader reader = new InputStreamReader(in);
             br = new BufferedReader(reader);
            String message;
            rootPath = classPath.substring(0,classPath.length()-1)+"-temp\\"+(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()))+"\\"+appName;
            while((message = br.readLine()) != null) {
                LogUtil.print(LogType.INFO,"==>"+message);
                if(message.length()==0||message.indexOf("---")!=-1) continue;
                b=false;
                String[] arr = message.trim().replaceAll("\\s{1,}", " ").split(" ");
                findFile(arr);
            }
            if(b) LogUtil.print(LogType.WARN,"没有获取到任何内容");
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(br!=null){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        LogUtil.print(LogType.INFO,"================执行获取修改列表 结束================");
        if(!b) return toZip();
        return null;
    }

    /**
     * 构造svn执行命令
     * @return
     * @throws Exception
     * --summarize 显示结果的概要
     */
    private static String  buildSVNCmd() throws Exception{
        LogUtil.print(LogType.DEBUG,"================构造执行SVN命令 开始================");
        if(classPath==null||classPath.trim().isEmpty()) throw new Exception("classPath 是必须配置的~");
        String changeList =  ParamUtil.param.getProperty("changeList");
        StringBuilder sb;
        if(changeList!=null&&changeList.trim().length()>0){
            sb = new StringBuilder("svn st");
            if(svnPath!=null&&!svnPath.trim().isEmpty()){
                sb.append(" "+svnPath.trim());
            }
            sb.append(" --cl "+changeList);
        }else{
            sb = new StringBuilder("svn diff");
            if(upperVersion!=null&&!upperVersion.trim().isEmpty()){
                sb.append(" -r ").append(currentVersion).append(":").append(upperVersion.trim());
            }
            if(svnPath!=null&&!svnPath.trim().isEmpty()){
                sb.append(" "+svnPath.trim());
            }
            sb.append(" --summarize");
        }

        LogUtil.print(LogType.DEBUG,"构造svn命令："+sb.toString());
        LogUtil.print(LogType.DEBUG,"================构造执行SVN命令 结束================");
        return sb.toString();
    }

    /**
     * 初始化参数
     * @param args
     * @throws Exception
     */
    public static void init(String[] args) throws Exception{
        LogUtil.print(LogType.DEBUG,"================初始化参数 开始================");
        for (String str : args){
            LogUtil.print(LogType.DEBUG,str);
            if(str.indexOf("=")!=-1){
                if(str.toLowerCase().indexOf("svnpath")!=-1){
                    svnPath = str.split("=")[1];
                    if(!svnPath.endsWith("\\")){
                        svnPath+="\\";
                    }
                }
                if(str.toLowerCase().indexOf("classpath")!=-1){
                    classPath = str.split("=")[1];
                    if(!classPath.endsWith("\\")){
                        classPath+="\\";
                    }
                }
            }
        }

        if(svnPath!=null){
            String configPath = svnPath+"remote-deploy\\content.properties";
            LogUtil.print(LogType.INFO,"开始初始化配置文件 ==> "+configPath);
            ParamUtil.init(configPath);
        }
        upperVersion = ParamUtil.param.getProperty("upperVersion");
        currentVersion = ParamUtil.param.getProperty("currentVersion");
        appName = ParamUtil.param.getProperty("app_name");
        LogUtil.print(LogType.DEBUG,"================初始化参数 完成================");
    }



    /**
     * copy to https://www.cnblogs.com/zeng1994/p/7862288.html
     * 打压缩包
     */
    public static String toZip() throws Exception {
        String zipPath = rootPath+"Add"+(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()))+".zip";
        LogUtil.print(LogType.INFO,"开始进行压缩，压缩文件："+zipPath);
        File zipFile  = new File(zipPath);
        if(!zipFile.exists()){
            zipFile.createNewFile();
        }
        long start = System.currentTimeMillis();
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            File sourceFile = new File(rootPath);
            compress(sourceFile, zos, sourceFile.getName(), true);
            long end = System.currentTimeMillis();
            LogUtil.print(LogType.INFO,"压缩完成，耗时：" + (end - start) + " ms");
            return zipPath;
        } catch (Exception e) {
            throw new RuntimeException("zip error from ZipUtils", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void compress(File sourceFile, ZipOutputStream zos, String name, boolean KeepDirStructure) throws Exception {
        int BUFFER_SIZE = 2 * 1024;
        byte[] buf = new byte[BUFFER_SIZE];
        if (sourceFile.isFile()) {
            zos.putNextEntry(new ZipEntry(name));
            int len;
            FileInputStream in = new FileInputStream(sourceFile);
            while ((len = in.read(buf)) != -1) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
            in.close();
        } else {
            File[] listFiles = sourceFile.listFiles();
            if (listFiles == null || listFiles.length == 0) {
                if (KeepDirStructure) {
                    zos.putNextEntry(new ZipEntry(name + "/"));
                    zos.closeEntry();
                }
            } else {
                for (File file : listFiles) {
                    if (KeepDirStructure) {
                        compress(file, zos, name + "/" + file.getName(), KeepDirStructure);
                    } else {
                        compress(file, zos, file.getName(), KeepDirStructure);
                    }
                }
            }
        }
    }

}
