package com.plugin.autoflox.invarscope.aji.executiontracer;

public class AnonymousFunctionEntity {
	private String fileName;
	private int scriptTagNo;
	private int relativeLineNoInTag;
	private int id;

	public AnonymousFunctionEntity(String fileName, int scriptTagNo, int relativeLineNoInTag, int id){
		this.fileName = fileName;
		this.scriptTagNo = scriptTagNo;
		this.relativeLineNoInTag = relativeLineNoInTag;
		this.id = id;
	}
	
	public int getId(){
		return this.id;
	}
	
	public String getFileName(){
		return this.fileName;
	}
	
	public int getScriptTagNo(){
		return this.scriptTagNo;
	}
	
	public int getRelativeLineNoInTag(){
		return this.relativeLineNoInTag;
	}
}
