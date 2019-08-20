package zyk.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtil {

    private static LogType logLevel = LogType.INFO;

    public static void setLogLevel(LogType logLevel) {
        LogUtil.logLevel = logLevel;
    }

    /**
     * 日志打印 IDEA 会有颜色输出
     * @param logType
     * @param message
     */
    public static void print(LogType logType,String message){
        if(logLevel.ordinal()<=logType.ordinal()){
            String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
            switch (logType) {
                case DEBUG:
                    System.out.println(date+" \033[30;4m" + "[DEBUG]" + "\033[0m  "+message);
                    break;
                case INFO:
                    System.out.println(date+" \033[30;4m" + "[INFO]" + "\033[0m  "+message);
                    break;
                case ERROR:
                    System.out.println(date+" \033[31;4m" + "[ERROR]" + "\033[0m  "+message);
                    break;
                case WARN:
                    System.out.println(date+" \033[33;4m" + "[WARN]" + "\033[0m  "+message);
                    break;
            }

        }
    }
}
