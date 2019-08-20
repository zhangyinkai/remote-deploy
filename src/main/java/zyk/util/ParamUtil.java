package zyk.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ParamUtil {
    public static Properties param = new Properties();

    public ParamUtil() {
    }

    public static void init(String path){
        try {
            if(path!=null&&!path.trim().isEmpty()&& new File(path).exists()){
                param.load(new FileInputStream(path));
            }else{
                param.load(ParamUtil.class.getClassLoader().getResourceAsStream("content.properties"));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}