# 介绍
针对tomcat应用远程部署支持增量，全量，集成在Idea的插件中也可以使用，单独使用需要写bat
## 实现功能
1:备份文件,2:停服务器3:清除文件4:上传文件5:启动服务器,6:增量升级,7:打增量包（svn）
## 环境要求
centos,java,tomcat,svn
## 命令集合
cd,mkdir,tar,lsof,grep,awk,ps,kill,rm,unzip

# 使用
## 集成Idea插件（External tools）
配置例子：java -jar remote-deploy.jar svnpath=$ModuleFileDir$ classpath=$OutputPath$
## bat运行
配置例子：java -cp .;ganymed-ssh2-262.jar;remote-deploy.jar zyk.RemoteDeployApplication
