package com.mapred;

import java.io.*;
import java.util.HashMap;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class MyMapper extends Mapper<LongWritable, Text, Text, Text> {
	//指定appTab的保存位置
	public static final Path APPTAB_PATH = new Path("hdfs://deepin:9000/protrait/input/appTab.txt");
	//public static final String APPTAB_PATH = "/home/arym/Documents/protrait/input/appTab.txt";
	
	//建立Hash map保存appTab的内容
	private HashMap<Integer, String> AppTab = new HashMap<Integer, String>();
	
	//setup在Task执行之前，只执行一次
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		//读取appTab.txt文件，将表格存到Hash Map中
		FileSystem fileSystem = FileSystem.get(new Configuration()); 
		BufferedReader br = new BufferedReader(new InputStreamReader(fileSystem.open(APPTAB_PATH))); 
		//BufferedReader br = new BufferedReader(new FileReader(APPTAB_PATH));
		String line = null;
		while(null != (line=br.readLine())) {
			String[] attrs = line.split("\\|");
			//分离属性
			String AppID = attrs[0]; 	//应用的ID
			String flag = attrs[0];
			String RestAttrs = ""; 	//应用的名称+男女权重+五段年龄段权重（8个属性）
			for(String attr : attrs) {
				if(attr == flag)continue;
				RestAttrs = RestAttrs + "-" + attr;
			}
			AppTab.put(Integer.parseInt(AppID), RestAttrs.substring(1));
		}
		br.close();
	}
	
	@Override
	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		//切分一条数据得到手机号（作为用户ID）和用户数据（AppID	使用日期	使用时长）
		String line = value.toString();
		String[] attrs = line.split("\\|");
		//根据AppID从Hash Map中取出应用名和权重信息
		String[] tabs = AppTab.get(Integer.parseInt(attrs[1])).split("-");
		/*
		 * attrs[0]:手机号（输出的key）
		 * attrs[1]:appID
		 * attrs[3]:使用时长
		 * tabs[0]:app名称
		 * tabs[1]:男性权重
		 * tabs[2]:女星权重
		 * tabs[3]:年龄段一权重
		 * tabs[4]:年龄段二权重
		 * tabs[5]:年龄段三权重
		 * tabs[6]:年龄段四权重
		 * tabs[7]:年龄段五权重
		 * */
		//拼接整理数据（共8项）
		String CombinedAttrs = attrs[3]+"-"+tabs[1]+"-"+tabs[2]+"-"+tabs[3]+"-"+tabs[4]+"-"+tabs[5]+"-"+tabs[6]+"-"+tabs[7];
		//以手机号为key，应用信息（1）+各项权重（7）为value输出
		System.out.println(CombinedAttrs);
		context.write(new Text(attrs[0]), new Text(CombinedAttrs));
	}
}