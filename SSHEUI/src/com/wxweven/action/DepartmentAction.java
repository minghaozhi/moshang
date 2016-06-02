package com.wxweven.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JsonConfig;
import net.sf.json.util.CycleDetectionStrategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.opensymphony.xwork2.ActionContext;
import com.wxweven.base.BaseAction;
import com.wxweven.domain.Department;
import com.wxweven.utils.JsonLibDeptProcessor;

@Controller
// 声明为Spring管理的Controller，默认的名字是类名的第一个字母小写
// @Controller 与 @Controller("departmentAction") 等价
@Scope("prototype")
// 声明作用域为 prototype，所有的action 作用域都是 prototype
public class DepartmentAction extends BaseAction<Department> {

	private static final long serialVersionUID = 2894678137860541838L;
	protected  Log logger = LogFactory.getLog(this.getClass());
	
	/** 分页 排序参数 */
	private String page;// 当前页
	private String rows;// 每页记录数
	private String sort;// 排序的字段
	private String order;// ASC 或者 desc
	
	//----------------------------------
	private Integer parentId;
	private Integer subId;

	//---------------------------------
	private Integer[] deletIds;// 要被删除的记录的id
	
	
	//==============部门下拉框获取========================

	public String getDepartmentTree() throws Exception {
		String deptTree = departmentService.getDepartmentMTree();

		out.print(deptTree);
		out.flush();
		out.close();

		return null;
	}

	
	//=================部门管理================	
		//默认显示list.jsp页面 
		public String toList() throws Exception {
			ActionContext.getContext().put("jspGridTitle", "部门列表");
			return "list";
		}
		
	   // 获取部门列表数据 
		public String list() throws Exception {
			// 1. 根据分页，排序，查询条件等来获取用户列表
			List<Department> departmentList = departmentService.findAll(getPage(), getRows(), getSort(), getOrder(), null);
	
			// 2. 获得符合条件的用户的总数(不带分页)
			int totalCount = departmentService.totalCount();
			logger.debug("departmentList = " + departmentList + ", count =" + totalCount);
		
//			因为bean里有Date字段，且从数据库里读出来的是java.sql.Date赋值给了java.util.Date,转化成JSONArray时出错；可以:
//			new java.util.Date(rs.getDate("date").getTime);
			
			// 3. setCycleDetectionStrategy 防止出现死循环异常, 并且排除 usergourp 的影响
			JsonConfig config = new JsonConfig();
			config.setExcludes(new String[] {"userGroup"});
			config.setCycleDetectionStrategy(CycleDetectionStrategy.LENIENT);
			
			// 对从数据库中取回的字段过滤
			config.registerJsonValueProcessor(String.class, JsonLibDeptProcessor.instance);
//			config.registerJsonValueProcessor(Date.class, JsonLibDateProcessor.instance);
//			config.registerJsonValueProcessor(String.class, JsonLibUserProcessor.instance);

			// 4. 根据list得到json字符串
			String resultStr = JSONArray.fromObject(departmentList, config).toString();

			// 5. 包装json字符串，符合easyui要求
			resultStr = wrapReturnJsonStr(resultStr, totalCount);

			logger.debug("departmentList--->" + resultStr);

			// 6. 返回数据给前台
			out.print(resultStr);
			out.flush();
			out.close();

			// 7. 由于直接返回数据给前台，而不需要跳转页面，这里直接返回null
			return null;// 返回 null 表示不用跳转页面
		}

		// 添加页面 
		public String addUI() throws Exception {
			return "addUI";
		}

		//添加 
		public String add() throws Exception {
			// 封装到对象中（当model是实体类型时，也可以使用model，但要设置未封装的属性）
			// >> 设置所属部门
			model.setParent(departmentService.getById(new Integer(parentId)));
			// id 生成策略
			List<Integer> subIds = new ArrayList<Integer>(); 
			Integer sub_Id = null;
			Set<Department> childrenDeptSet =  departmentService.getById(new Integer(parentId)).getChildren();

			if(childrenDeptSet.size() == 0)
				sub_Id = new Integer(parentId + "01");
			else{
				int i = 0;
				for(Department dept : childrenDeptSet){
					subIds.add(dept.getId());
				}
				// 取出队列中最后一个
				sub_Id = new Integer("" + subIds.toArray()[subIds.toArray().length-1]) + 1;
			}	
						
			model.setId(sub_Id);
			// >> 保存到数据库
			logger.debug("department add model---->" + model);
			departmentService.save(model);

			out.print("添加部门成功！");
			out.flush();
			out.close();

			return null;
		}

	
		//删除 
		public String delete() throws Exception {
			 logger.debug("删除的id是----》"+Arrays.asList(deletIds));
			// 循环删除对应id的记录
			for (int i = 0; i < deletIds.length; i++) {
				 departmentService.delete(deletIds[i]);
			}

			out.print("删除记录成功！");
			out.flush();
			out.close();

			return null;
		}


		//修改页面 
		public String editUI() throws Exception {
			// 准备回显的数据
			Department dept = departmentService.getById(model.getId());
			ActionContext.getContext().getValueStack().push(dept);
			if (dept.getParent() != null) {
				parentId = dept.getParent().getId();
				ActionContext.getContext().getValueStack().push(parentId);
			}

			return "editUI";
		}

		//修改
		public String edit() throws Exception {
			// 1. 从数据库中取出原对象
			Department dept = departmentService.getById(model.getId());

			// 2. 设置要修改的属性,但是有些属性，如loginName,realName，密码等不能直接修改
			
			dept.setName(model.getName());
			dept.setAddress(model.getAddress());
			dept.setPhoneNumber(model.getPhoneNumber());
			dept.setEmail(model.getEmail());
			dept.setDescription(model.getDescription());
			// >> 设置所属部门
			dept.setParent(departmentService.getById(parentId));

			// 3. 更新到数据库
			departmentService.update(dept);

			out.print("修改信息成功！");
			out.flush();
			out.close();

			return null;
		}
/*
		//检查loginName是否存在
		public String isExist() throws Exception {
			String retMsg = "false";
			boolean exist = userService.isExist(model.getLoginName());
			if (exist) {
				retMsg = "true";
			}

			out.print(retMsg);
			out.flush();
			out.close();

			return null;
		}
		
		//导出Excel
		public String export() throws Exception {
			logger.debug("111111111111");
			String filename = ServletActionContext.getServletContext().getRealPath(".");//当前类路径
			//1. 生成Excel
			userService.excelWriter();
			return null;
		}
		
*/	
	
		//================== getter() & setter() ========================
		public String getPage() {
			return page;
		}

		public void setPage(String page) {
			this.page = page;
		}

		public String getRows() {
			return rows;
		}

		public void setRows(String rows) {
			this.rows = rows;
		}

		public String getSort() {
			return sort;
		}

		public void setSort(String sort) {
			this.sort = sort;
		}

		public String getOrder() {
			return order;
		}

		public void setOrder(String order) {
			this.order = order;
		}
		
		public Integer getParentId() {
			return parentId;
		}


		public void setParentId(Integer parentId) {
			this.parentId = parentId;
		}


		public Integer[] getDeletIds() {
			return deletIds;
		}


		public void setDeletIds(Integer[] deletIds) {
			this.deletIds = deletIds;
		}

		public Integer getSubtId() {
			return subId;
		}


		public void setSubtId(Integer subtId) {
			this.subId = subtId;
		}
		


}