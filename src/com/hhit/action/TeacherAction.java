package com.hhit.action;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.transaction.Synchronization;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.hhit.base.BaseAction;
import com.hhit.entity.Chapter;
import com.hhit.entity.ClassSelectCourse;
import com.hhit.entity.Class_;
import com.hhit.entity.Course;
import com.hhit.entity.Department;
import com.hhit.entity.Judgement;
import com.hhit.entity.LogFile;
import com.hhit.entity.Role;
import com.hhit.entity.SingleChoice;
import com.hhit.entity.Teacher;
import com.hhit.entity.TestPaper;
import com.hhit.entity.TestQuestion;
import com.hhit.entity.User;
import com.hhit.util.ClassPropertyFilter;
import com.hhit.util.DepartmentUtils;
import com.hhit.util.JsonUtil;
import com.hhit.util.QueryHelper;
import com.opensymphony.xwork2.ActionContext;

@SuppressWarnings("serial")
@Controller
@Scope("prototype")
public class TeacherAction extends BaseAction<Teacher>{
	
	private Integer departmentId;
	
	private Integer[] roleIds;
	
	private Integer classId;
	
	private int viewType;// 0姓名；1账号
	private String inputTerm = "";// 输入的词条
	
	// ajax json返回
	private String result;
	
	private String teacherNum;
	
	//老师自己添加课程对应的班级需要
	private Integer courseId;
	//班级
	private Integer[] classIds;
	//
	private Integer classSelectCourseId;
	
	//doInfo:1-->课前预习2-->课堂测试3-->课后复习
	private Integer doInfo;
	private Integer judgementCount;
	private Integer singleChoiceCount;
	private Timestamp endTime;
	private Integer chapterId;
	
	private Integer testPaperId;
	
	private Integer singleChoiceId;
	private Integer judgementId;
	
	/** 列表 */
	public String list() throws Exception {
		// 准备数据, departmentList
		List<Department> topList = departmentService.findTopList();
		List<Department> departmentList = DepartmentUtils
				.getAllDepartments(topList);
		ActionContext.getContext().put("departmentList", departmentList);
		
		new QueryHelper(Teacher.class, "t")//
		.addCondition((departmentId != null), "t.department.id=?",departmentId)//
				.addCondition((viewType == 0) && (inputTerm.trim().length() > 0),"t.teaName LIKE ?", "%" + inputTerm + "%")//
				.addCondition((viewType == 1) && (inputTerm.trim().length() > 0),"t.teaNum LIKE ?", "%" + inputTerm + "%")//
				.preparePageBean(teacherService, pageNum, pageSize);

		return "list";
	}
	/** 删除 */
	public String delete() throws Exception{
		teacherService.delete(model.getId());
		logFileService.save(new LogFile(getCurrentUser().getUserNum(), getCurrentUser().getUserType(),
				ServletActionContext.getRequest().getRemoteAddr(), new Timestamp(new Date().getTime()),
				"删除【老师id="+model.getId()+"】成功"));
		return "toList";
	}
	/** 批量删除 */
	public String bulkDelete() throws Exception{
		Map<String, Object> map = new HashMap<String, Object>();
		// 直接根据id删除
		teacherService.delete(model.getId());
		result = "ok";
		map.put("name", result);
		JsonUtil.toJson(ServletActionContext.getResponse(), map);

		return null;
	}
	
	/** 添加界面 */
	public String addUI() throws Exception{
		// 准备数据, departmentList
		List<Department> topList = departmentService.findTopList();
		List<Department> departmentList = DepartmentUtils.getAllDepartments(topList);
		ActionContext.getContext().put("departmentList", departmentList);
		// 准备数据, roleList
		List<Role> roleList = roleService.findAll();
		ActionContext.getContext().put("roleList", roleList);
		
		return "saveUI";
	}
	/** 添加 */
	public String add() throws Exception {
		/** 保存用户相应信息 */
		// 封装到对象中（当model是实体类型时，也可以使用model，但要设置未封装的属性）
		// >> 设置所属部门和角色
		model.setDepartment(departmentService.findById(departmentId));
		List<Role> roleList=roleService.findByIds(roleIds);
		model.setRoles(new HashSet<Role>(roleList));
		// 保存数据库
		teacherService.save(model);
		// ** 保存用户登陆信息 */
		// >> 设置默认密码为账号（要使用MD5摘要）
		User userModel = new User();
		String md5Digest = DigestUtils.md5Hex(model.getTeaNum());
		userModel.setPassword(md5Digest);
		userModel.setTeacher(model);
		userModel.setUserNum(model.getTeaNum());
		userModel.setUserType("老师");
		// 保存到数据库
		userService.save(userModel);

		return "toList";
	}
	/** 修改页面 */
	public String editUI() throws Exception {
		// 准备数据, departmentList
		List<Department> topList = departmentService.findTopList();
		List<Department> departmentList = DepartmentUtils
				.getAllDepartments(topList);
		ActionContext.getContext().put("departmentList", departmentList);
		
		// 准备回显的数据
		Teacher teaFind=teacherService.findById(model.getId());
		ActionContext.getContext().getValueStack().push(teaFind);
		
		// 准备数据, roleList
		List<Role> roleList = roleService.findAll();
		ActionContext.getContext().put("roleList", roleList);
		
		//回显数据--部门
		if (teaFind.getDepartment() != null) {
			departmentId = teaFind.getDepartment().getId();
		}
		//回显数据--角色
		if (teaFind.getRoles() != null) {
			roleIds = new Integer[teaFind.getRoles().size()];
			int index = 0;
			for (Role role : teaFind.getRoles()) {
				roleIds[index++] = role.getId();
			}
		}

		return "saveUI";
	}
	/** 修改 */
	public String edit() throws Exception {
		// 1，从数据库中取出原对象
		Teacher teaFind=teacherService.findById(model.getId());

		// 2，设置要修改的属性
		teaFind.setBirthday(model.getBirthday());
		teaFind.setFinalEdu(model.getFinalEdu());
		teaFind.setInTime(model.getInTime());
		teaFind.setSex(model.getSex());
		teaFind.setTeaName(model.getTeaName());
		teaFind.setTeaNum(model.getTeaNum());
		teaFind.setTitle(model.getTitle());
		// >> 设置所属部门和角色
		teaFind.setDepartment(departmentService.findById(departmentId));
		List<Role> roleList = roleService.findByIds(roleIds);
		teaFind.setRoles(new HashSet<Role>(roleList));
		
		// 3，更新到数据库
		teacherService.update(teaFind);

		return "toList";
	}
	/** 初始化密码 */
	public String initPassword() throws Exception{
		Teacher teaFind=teacherService.findById(model.getId());
		User userFind=userService.findByTeacher(teaFind);
		
		userFind.setPassword(DigestUtils.md5Hex("123456"));
		//更新用户
		userService.update(userFind);
		return "toList";
	}
	/** 老师对应的课程(老师可以自己添加课程) */
	public String findTeacherCourse() throws Exception{
		Map<String, Object> map=new HashMap<>();
		//根据工号查找
		Teacher teaFind=teacherService.findByTeacherNum(teacherNum);
		if(teaFind==null){
			map.put("name", "noTeacher");
		}
		else{
			if(teaFind.getCourses().size()>0){
				List<Course> courseList=new ArrayList<>(teaFind.getCourses());
				//过滤
				ClassPropertyFilter.ListCourseFilter(map, courseList);
				map.put("name", "success");
			}
			else{
				map.put("name","noCourse");
			}
		}
		JsonUtil.toJson(ServletActionContext.getResponse(), map);
		return null;
	}
	/** 添加课程对应的班级列表界面 */
	public String listCourseClass() throws Exception{
		Teacher teaFind=getCurrentUser().getTeacher();
		if(teaFind.getCourses().size()<1){
			return "noCourseError";
		}
		else{
			//准备数据--老师对应课程
			List<Course> couseList=new ArrayList<>(teaFind.getCourses());
			ActionContext.getContext().put("couseList", couseList);

		}
		if(courseId==null){
			//准备数据--第一门课程的教学进程
			List<Course> couseList=new ArrayList<>(teaFind.getCourses());
			Course courseFind=couseList.get(0);
			//准备数据--课程名
			ActionContext.getContext().getValueStack().push(courseFind);

			List<ClassSelectCourse> classSelectCourseList=classSelectCourseService.findByTeacherNumAndCourse(teaFind.getTeaNum(),courseFind);
			ActionContext.getContext().put("classSelectCourseList", classSelectCourseList);
			courseId=courseFind.getId();
		}
		else{
			//准备数据--课程名
			ActionContext.getContext().getValueStack().push(courseService.findById(courseId));
			//准备数据--课程id
			ActionContext.getContext().put("courseId", courseId);
			//准备数据--对应课程的教学进程
			List<ClassSelectCourse> classSelectCourseList=classSelectCourseService.findByTeacherNumAndCourse(teaFind.getTeaNum(),courseService.findById(courseId));
			ActionContext.getContext().put("classSelectCourseList", classSelectCourseList);
		}
		
		return "listCourseClass";
	}
	/** 添加课程班级界面 */
	@SuppressWarnings("unchecked")
	public String addCourseClassUI() throws Exception{
		//准备数据--课程名
		Course courseFind=courseService.findById(courseId);
		ActionContext.getContext().getValueStack().push(courseFind);
		//准备数据--部门
		List<Department> topList=departmentService.findTopList();
		List<Department> departmentList=DepartmentUtils.getAllDepartments(topList);
		ActionContext.getContext().put("departmentList", departmentList);
		//准备数据--班级--空集合
		List<Class_> classList=Collections.EMPTY_LIST;
		ActionContext.getContext().put("classList", classList);
		return "saveCourseClassUI";
	}
	/** 添加课程班级 */
	public String addCourseClass() throws Exception{
		Teacher teaFind=getCurrentUser().getTeacher();
		//找到班级
		List<Class_> classList= classService.findByIds(classIds);
		//找到课程
		Course courseFind=courseService.findById(courseId);
		for(int i=0;i<classList.size();i++){
			//保存选课表
			classSelectCourseService.save(new ClassSelectCourse(""+classList.get(i).getId()+"-"+teaFind.getTeaNum()+"-"+courseId,
					teaFind.getTeaNum(), classList.get(i), courseFind));
		}
		//准备数据--courseId
		ActionContext.getContext().put("courseId", courseId);
		return "toListCourseClass";
	}
	/** 删除课程班级 */
	public String deleteCourseClass() throws Exception{
		classSelectCourseService.delete(classSelectCourseId);
		
		//courseId携带过去
		ActionContext.getContext().put("courseId", courseId);
		return "toListCourseClass";
	}
	
	/** 布置课前预习、课堂测试、课后复习界面 */
	public String myCourseUI() throws Exception{
		Teacher teaFind=getCurrentUser().getTeacher();
		if(teaFind.getCourses().size()<1){
			return "noCourseError";
		}
		else{
			//准备数据--老师对应课程
			List<Course> couseList=new ArrayList<>(teaFind.getCourses());
			ActionContext.getContext().put("couseList", couseList);
		}
		if(courseId!=null){
			
			Course courseFind=courseService.findById(courseId);
			ActionContext.getContext().getValueStack().push(courseFind);
			//准备数据--章节
			List<Chapter> chapterList=chapterService.findByCourse(courseFind);
			ActionContext.getContext().put("chapterList", chapterList);
			//准备数据--老师教的班级
			List<ClassSelectCourse> classSelectList=classSelectCourseService.findByTeacherNumAndCourse(teaFind.getTeaNum(), courseFind);
			List<Class_> classList=new ArrayList<>();
			for(int i=0;i<classSelectList.size();i++){
				classList.add(classSelectList.get(i).getClass_());
			}
			ActionContext.getContext().put("classList", classList);
			//准备数据--doInfo
			ActionContext.getContext().put("doInfo", doInfo);
			//准备分页数据--测试paper
			//-->处理doInfo
			String testType="";
			switch (doInfo) {
			case 1:
				testType="课前预习";
				break;
			case 2:
				testType="课堂测试";
				break;
			case 3:
				testType="课后复习";
				break;
			default:
				break;
			}
			new QueryHelper(TestPaper.class, "t")//
			.addCondition("t.course=?", courseFind)//
			.addCondition("t.teaNum=?", getCurrentUser().getTeacher().getTeaNum())//
			.addCondition(testType!="","t.testType=?", testType)//
			.preparePageBean(testPaperService, pageNum, pageSize);
			
		}
		else{
			//准备章节空集合
			List<Chapter> chapterList=Collections.EMPTY_LIST;
			ActionContext.getContext().put("chapterList", chapterList);
			//准备班级空集合
			List<Class_> classList=Collections.EMPTY_LIST;
			ActionContext.getContext().put("classList", classList);
			//准备数据--所有测试
			new QueryHelper(TestPaper.class, "t")//
			.addCondition("t.teaNum=?", getCurrentUser().getTeacher().getTeaNum())//
			.preparePageBean(testPaperService, pageNum, pageSize);
		}
			
		return "myCourseUI";
	}
	
	/** 自动生成测试提库 */
	public String autoMakeQuestion() throws Exception{
		Teacher teaFind=getCurrentUser().getTeacher();
		//测试题目总数
		Integer questionCount=judgementCount+singleChoiceCount;
		//设置测试题目类型
		String testType="";
		switch (doInfo) {
		case 1:
			testType="课前预习";
			break;
		case 2:
			testType="课堂测试";
			break;
		case 3:
			testType="课后复习";
			break;
		default:
			break;
		}
		//保存测试卷
		TestPaper testPaperModel=new TestPaper(testType, questionCount, 0, endTime, teaFind.getTeaNum(), classService.findById(classId),
				courseService.findById(courseId), chapterService.findById(chapterId));
		testPaperService.save(testPaperModel);
		
		/*
		 * 生成判断题
		 */
		//最后一条记录Id
		int maxId=judgementService.findMaxRecord().getId();
		//第一条记录Id
		int minId=judgementService.findMinRecord().getId();
		int random,i=0;
		List<Integer> intList=new ArrayList<>();
		//生成minId--maxId之间的judgementCount个随机数
		while(i<judgementCount){
			random=minId+(int)(Math.random()*(maxId-minId));
			if(!intList.contains(random)){
				intList.add(random);
				i++;
			}
		}
		//保存判断题
		for(i=0;i<judgementCount;i++){
			int recordId=intList.get(i);
			Judgement judgementFind=judgementService.findById(recordId);
			//没有找到记录
			if(judgementFind==null){
				//距离小的Id近
				if((maxId-recordId)>(recordId-minId)){
					while(judgementFind==null){
						recordId++;
						if(!intList.contains(recordId)){
							judgementFind=judgementService.findById(recordId);
							TestQuestion questionModel=new TestQuestion(judgementFind, null, testPaperModel);
							testQuestionService.save(questionModel);
						}
					}
				}
				//距离大的Id近
				else{
					while(judgementFind==null){
						recordId--;
						//集合中没有这个记录
						if(!intList.contains(recordId)){
							judgementFind=judgementService.findById(recordId);
							TestQuestion questionModel=new TestQuestion(judgementFind, null, testPaperModel);
							testQuestionService.save(questionModel);
						}
					}
				}
			}
			else{
				//保存
				TestQuestion questionModel=new TestQuestion(judgementFind, null, testPaperModel);
				testQuestionService.save(questionModel);
			}
		}
		/**
		 * 生成单选题
		 */
		/*
		 * 生成判断题
		 */
		//最后一条记录Id
		int maxSingleId=singleChoiceService.findMaxRecord().getId();
		//第一条记录Id
		int minSingleId=singleChoiceService.findMinRecord().getId();
		int j=0;
		List<Integer> singleList=new ArrayList<>();
		//生成minId--maxId之间的judgementCount个随机数
		while(j<judgementCount){
			random=maxSingleId+(int)(Math.random()*(minSingleId-maxSingleId));
			if(!singleList.contains(random)){
				singleList.add(random);
				j++;
			}
		}
		//保存判断题
		for(j=0;j<singleChoiceCount;j++){
			int recordId=intList.get(j);
			SingleChoice singleFind=singleChoiceService.findById(recordId);
			//没有找到记录
			if(singleFind==null){
				//距离小的Id近
				if((maxSingleId-recordId)>(recordId-minSingleId)){
					while(singleFind==null){
						recordId++;
						if(!intList.contains(recordId)){
							singleFind=singleChoiceService.findById(recordId);
							TestQuestion questionModel=new TestQuestion(null, singleFind, testPaperModel);
							testQuestionService.save(questionModel);
						}
					}
				}
				//距离大的Id近
				else{
					while(singleFind==null){
						recordId--;
						//集合中没有这个记录
						if(!intList.contains(recordId)){
							singleFind=singleChoiceService.findById(recordId);
							TestQuestion questionModel=new TestQuestion(null, singleFind, testPaperModel);
							testQuestionService.save(questionModel);
						}
					}
				}
			}
			else{
				//保存
				TestQuestion questionModel=new TestQuestion(null, singleFind, testPaperModel);
				testQuestionService.save(questionModel);
			}
		}
		//准备数据--courseId
		ActionContext.getContext().put("courseId", courseId);
		
		return "autoMakeQuestion";
	}
	/** 删除测试paper */
	public String deleteTestPaper() throws Exception{
		testPaperService.delete(testPaperId);
		//携带courseId
		ActionContext.getContext().put("courseId", courseId);
		return "toMyCourseUI";
	}
	/** 查看习题 */
	public String seeTestPaperUI() throws Exception{
		//准备数据--单选列表
		TestPaper testPaperFind=testPaperService.findById(testPaperId);
		List<TestQuestion> testQuestionList=new ArrayList<>(testPaperFind.getTestQuestions());
		List<Judgement> judgementList=new ArrayList<>();
		for(int i=0;i<testQuestionList.size();i++){
			if(testQuestionList.get(i).getJudgement()!=null){
				judgementList.add(testQuestionList.get(i).getJudgement());
			}
		}
		ActionContext.getContext().put("judgementList", judgementList);
		//准备数据--单选
		List<SingleChoice> singleChoiceList=new ArrayList<SingleChoice>();
		for(int i=0;i<testQuestionList.size();i++){
			if(testQuestionList.get(i).getSingleChoice()!=null){
				singleChoiceList.add(testQuestionList.get(i).getSingleChoice());
			}
		}
		ActionContext.getContext().put("singleChoiceList", singleChoiceList);
		//准备--testPaperId
		ActionContext.getContext().put("testPaperId", testPaperId);
		return "seeTestPaperUI";
	}
	/** 删除单选题目 */
	public String deleteSingleChoice() throws Exception{
		//删除
		SingleChoice singleFind=singleChoiceService.findById(singleChoiceId);
		testQuestionService.deleteBySingleChoice(singleFind);
		//修改paper题目数量---同步操作
		synchronized (this) {
			TestPaper testPaper=testPaperService.findById(testPaperId);
			testPaper.setQuestionCount(testPaper.getQuestionCount()-1);
			testPaperService.update(testPaper);
		}
		//准备--testPaperId
		ActionContext.getContext().put("testPaperId", testPaperId);
		return "toSeeTestPaperUI";
	}
	/**  */
	public String deleteJudgement() throws Exception{
		//删除
		Judgement judgeFind=judgementService.findById(judgementId);
		testQuestionService.deleteByJudgement(judgeFind);
		//修改paper题目数量---同步操作
		synchronized (this) {
			TestPaper testPaper=testPaperService.findById(testPaperId);
			testPaper.setQuestionCount(testPaper.getQuestionCount()-1);
			testPaperService.update(testPaper);
		}
		//准备--testPaperId
		ActionContext.getContext().put("testPaperId", testPaperId);
		return "toSeeTestPaperUI";
	}
	
//==============================	
	public Integer getDepartmentId() {
		return departmentId;
	}
	public void setDepartmentId(Integer departmentId) {
		this.departmentId = departmentId;
	}
	public Integer getClassId() {
		return classId;
	}
	public void setClassId(Integer classId) {
		this.classId = classId;
	}
	public int getViewType() {
		return viewType;
	}
	public void setViewType(int viewType) {
		this.viewType = viewType;
	}
	public String getInputTerm() {
		return inputTerm;
	}
	public void setInputTerm(String inputTerm) {
		this.inputTerm = inputTerm;
	}
	public Integer[] getRoleIds() {
		return roleIds;
	}
	public void setRoleIds(Integer[] roleIds) {
		this.roleIds = roleIds;
	}
	public String getTeacherNum() {
		return teacherNum;
	}
	public void setTeacherNum(String teacherNum) {
		this.teacherNum = teacherNum;
	}
	public Integer getCourseId() {
		return courseId;
	}
	public void setCourseId(Integer courseId) {
		this.courseId = courseId;
	}
	public Integer[] getClassIds() {
		return classIds;
	}
	public void setClassIds(Integer[] classIds) {
		this.classIds = classIds;
	}
	public Integer getClassSelectCourseId() {
		return classSelectCourseId;
	}
	public void setClassSelectCourseId(Integer classSelectCourseId) {
		this.classSelectCourseId = classSelectCourseId;
	}
	public Integer getDoInfo() {
		return doInfo;
	}
	public void setDoInfo(Integer doInfo) {
		this.doInfo = doInfo;
	}
	public Integer getJudgementCount() {
		return judgementCount;
	}
	public void setJudgementCount(Integer judgementCount) {
		this.judgementCount = judgementCount;
	}
	public Integer getSingleChoiceCount() {
		return singleChoiceCount;
	}
	public void setSingleChoiceCount(Integer singleChoiceCount) {
		this.singleChoiceCount = singleChoiceCount;
	}
	public Timestamp getEndTime() {
		return endTime;
	}
	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}
	public Integer getChapterId() {
		return chapterId;
	}
	public void setChapterId(Integer chapterId) {
		this.chapterId = chapterId;
	}
	public Integer getTestPaperId() {
		return testPaperId;
	}
	public void setTestPaperId(Integer testPaperId) {
		this.testPaperId = testPaperId;
	}
	public Integer getSingleChoiceId() {
		return singleChoiceId;
	}
	public void setSingleChoiceId(Integer singleChoiceId) {
		this.singleChoiceId = singleChoiceId;
	}
	public Integer getJudgementId() {
		return judgementId;
	}
	public void setJudgementId(Integer judgementId) {
		this.judgementId = judgementId;
	}
}
