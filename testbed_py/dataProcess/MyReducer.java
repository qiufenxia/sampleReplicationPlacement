package com.mapred;

import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class MyReducer extends Reducer<Text, Text, Text, Text> {
	//性别类
	public class genderProportion {
		double male;
		double female;
		
		//构造函数
		public genderProportion() {
			this.male = 0.5;
			this.male = 0.5;
		}
		public genderProportion(double male, double female) {
			this.male = male;
			this.female = male;
		}
		//get函数
		public String getMale() {
			return String.format("%.3f", male);
		}
		public String getFemale() {
			return String.format("%.3f", female);
		}
		//计算性别比重
		public void renewProportion(double male2, double female2, int time) {
			double sum = (male+female + (male2+female2)*time);
			if(sum != 0) {
				this.male = (this.male + male2*time) / sum;
				this.female = (this.female + female2*time) / sum;
			}
		}
	}
	
	//年龄类
	public class ageProportion {
		double age1;
		double age2;
		double age3;
		double age4;
		double age5;
		
		//构造函数
		ageProportion() {
			age1 = age2 = age3 = age4 = age5 = 0.2;
		}
		ageProportion(double age1, double age2, double age3, double age4, double age5) {
			this.age1 = age1;
			this.age2 = age2;
			this.age3 = age3;
			this.age4 = age4;
			this.age5 = age5;
		}
		//get函数
		public String getAge1() {
			return String.format("%.3f", age1);
		}
		public String getAge2() {
			return String.format("%.3f", age2);
		}
		public String getAge3() {
			return String.format("%.3f", age3);
		}
		public String getAge4() {
			return String.format("%.3f", age4);
		}
		public String getAge5() {
			return String.format("%.3f", age5);
		}
		//计算年龄比重
		public void renewProportion(double pAge1, double pAge2, double pAge3, double pAge4, double pAge5, int time) {
			double sum = (age1+age2+age3+age4+age5 + (pAge1+pAge2+pAge3+pAge4+pAge4)*time);
			if(sum != 0) {
				this.age1 = (age1 + pAge1*time) / sum;
				this.age2 = (age2 + pAge2*time) / sum;
				this.age3 = (age3 + pAge3*time) / sum;
				this.age4 = (age4 + pAge4*time) / sum;
				this.age5 = (age5 + pAge5*time) / sum;
			}
		}
	}
	
	@Override
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		//初始化性别和年龄对象
		genderProportion gender = new genderProportion();
		ageProportion age = new ageProportion();
		int time;
		double male, female;
		double age1, age2, age3, age4, age5;
		//遍历values列表求最终的性别和年龄比重
		for(Text value : values) {
			String[] attrs = value.toString().split("-");
			/*
			 * key:手机号（输出的key）
			 * attrs[0]:使用时长
			 * attrs[1]:男性权重
			 * attrs[2]:女星权重
			 * attrs[3]:年龄段一权重
			 * attrs[4]:年龄段二权重
			 * attrs[5]:年龄段三权重
			 * attrs[6]:年龄段四权重
			 * attrs[7]:年龄段五权重
			 * */
			//数据类型转换
			time = Integer.parseInt(attrs[0]);
			try {
				male = Double.parseDouble(attrs[1]);
			}catch(NumberFormatException e) {
				male = 1-Double.parseDouble(attrs[2]);
			}
			female = Double.parseDouble(attrs[2]);
			age1 = Double.parseDouble(attrs[3]);
			age2 = Double.parseDouble(attrs[4]);
			age3 = Double.parseDouble(attrs[5]);
			age4 = Double.parseDouble(attrs[6]);
			age5 = Double.parseDouble(attrs[7]);
			//迭代计算最终的性别和年龄比重
			gender.renewProportion(male, female, time);
			age.renewProportion(age1, age2, age3, age4, age5, time);
		}
		//整理拼接数据
		String protrait = gender.getMale() +"-"+ gender.getFemale() +"-"+ 
				age.getAge1() +"-"+ age.getAge2() +"-"+ age.getAge3() +"-"+ age.getAge4() +"-"+ age.getAge5();
		//输出结果
		System.out.println(key.toString()+": "+protrait);
		context.write(key, new Text(protrait));
	}
}