from jpype import *


libpath ="-Djava.class.path="lib";
# 多个jar包用:分开，项目也需要导出jar包
# print(libpath)
jdkpath ="/usr/lib/jvm/java-8-oracle/jre/lib/amd64/server/libjvm.so"
# 这里注意版本路径
startJVM(jdkpath,"-ea",libpath )
# 启动jvm
JDClass = JClass("目标类的路径")
result = JDClass.fun()
# print(result.result)

# 将结果储存到文件中，并用socket返回

shutdownJVM()
#停止jvm