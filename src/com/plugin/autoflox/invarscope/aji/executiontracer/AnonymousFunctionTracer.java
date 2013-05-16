package com.plugin.autoflox.invarscope.aji.executiontracer;

import java.util.Vector;

public class AnonymousFunctionTracer {

	public static int anonymousFunctionCouter = 0;

	public static String fileName;
	public static int scriptTagNo;
	public static int relativeLineNoInTag;
	public static Vector<AnonymousFunctionEntity> AnonymousFunctionList = new Vector<AnonymousFunctionEntity>();

	public static void addToAnonymousFunctionList(
			AnonymousFunctionEntity newEntity) {
		AnonymousFunctionTracer.AnonymousFunctionList.add(newEntity);
	}

	public static AnonymousFunctionEntity getAnonymousFunctionEntity(String fileName,
			int scriptTagNo, int relativeLineNoInTag) {
		for (int i = 0; i < AnonymousFunctionTracer.AnonymousFunctionList
				.size(); i++) {
			if (AnonymousFunctionTracer.AnonymousFunctionList.get(i)
					.getFileName() == fileName
					&& AnonymousFunctionTracer.AnonymousFunctionList.get(i)
							.getScriptTagNo() == scriptTagNo
					&& AnonymousFunctionTracer.AnonymousFunctionList.get(i)
							.getRelativeLineNoInTag() == relativeLineNoInTag) {
				return AnonymousFunctionTracer.AnonymousFunctionList.get(i);
			}
		}
		return null;
	}

	public static boolean isAnonymousFunctionAdded(String fileName,
			int scriptTagNo, int relativeLineNoInTag) {
		for (int i = 0; i < AnonymousFunctionTracer.AnonymousFunctionList
				.size(); i++) {
			if (AnonymousFunctionTracer.AnonymousFunctionList.get(i)
					.getFileName() == fileName
					&& AnonymousFunctionTracer.AnonymousFunctionList.get(i)
							.getScriptTagNo() == scriptTagNo
					&& AnonymousFunctionTracer.AnonymousFunctionList.get(i)
							.getRelativeLineNoInTag() == relativeLineNoInTag) {
				return true;
			}
		}
		return false;
	}
}
