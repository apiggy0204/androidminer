package edu.ntu.arbor.sbchao.androidlogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

public class LogFileReader {
	Scanner scanner = null;
	DataManager mgr = null;
	
	public LogFileReader(File file, DataManager mgr) throws IOException{
		getScanner(file);
		this.mgr = mgr;
	}
	
	void getScanner(File file) throws IOException, FileNotFoundException{
		scanner = new Scanner(new BufferedReader(new FileReader(file)));
	}
	
	public ArrayList<ArrayList<NameValuePair>> all(){
		ArrayList<ArrayList<NameValuePair>> list = new ArrayList<ArrayList<NameValuePair>>();
		while (hasNext()){
			ArrayList<NameValuePair> params = next();
			list.add(params);
		}
		return list;
	}
	
	public ArrayList<NameValuePair> next(){
		
		ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
		if( scanner.hasNextLine()){
			String line = scanner.nextLine();
			String [] tokens = line.split("\t");
			assert(tokens.length == mgr.getColumnCount());
			for(int i=0; i<tokens.length; i++){
				if(!tokens[i].equals("")){
					params.add(new BasicNameValuePair(mgr.getColumnName(i), tokens[i]));
				}
			}						
			return params;
		}
		return null;
	}
	
	public boolean hasNext(){
		return scanner.hasNextLine();		
	}	
}



