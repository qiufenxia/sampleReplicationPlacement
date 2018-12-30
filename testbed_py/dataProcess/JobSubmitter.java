package com.mapred;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class JobSubmitter {
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		
		Configuration conf = new Configuration();
		
		Job protraitJob = Job.getInstance(conf);
		
		//重要：指定本job所在的jar包
		protraitJob.setJarByClass(JobSubmitter.class);
		
		//设置protraitJob所用的mapper逻辑类为哪个类
		protraitJob.setMapperClass(MyMapper.class);
		//设置protraitJob所用的reducer逻辑类为哪个类
		protraitJob.setReducerClass(MyReducer.class);
		
		//设置map阶段输出的kv数据类型
		protraitJob.setMapOutputKeyClass(Text.class);
		protraitJob.setMapOutputValueClass(Text.class);
		
		//设置reduce阶段输出的kv数据类型
		protraitJob.setOutputKeyClass(Text.class);
		protraitJob.setOutputValueClass(Text.class);
		
		//判断output目录是否已存在
		Path path = new Path(args[1]);
		FileSystem fileSystem = path.getFileSystem(conf);
		if (fileSystem.exists(path)) {
			fileSystem.delete(path, true);// true的意思是，就算output有东西，也一带删除
		}
		
		//设置要处理的文本数据所存放的路径
		FileInputFormat.addInputPath(protraitJob, new Path(args[0]));
		FileOutputFormat.setOutputPath(protraitJob, new Path(args[1]));
		
		//提交job给hadoop集群
		System.exit(protraitJob.waitForCompletion(true)?0:1);
	}
}