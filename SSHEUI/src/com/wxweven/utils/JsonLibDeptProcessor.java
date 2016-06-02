package com.wxweven.utils;

import net.sf.json.JsonConfig;
import net.sf.json.processors.JsonValueProcessor;

public class JsonLibDeptProcessor implements JsonValueProcessor {
	/** 供调用的 static 实例 */
	public static final JsonLibDeptProcessor instance = new JsonLibDeptProcessor();
	
	public Object processObjectValue(String key, Object value, JsonConfig jc) {
		
		if (value == null) {
			return "";
		} else if(key.equals("deptState")){//对 deptState 属性过滤
			
			if(value.equals("enable"))
				return 1; 
			else
				return 0;
		}
		
		return value.toString();
	}

	public Object processArrayValue(Object value, JsonConfig arg1) {
		return null;
	}

}


