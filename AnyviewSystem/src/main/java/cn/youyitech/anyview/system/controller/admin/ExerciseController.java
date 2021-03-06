package cn.youyitech.anyview.system.controller.admin;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.framework.loippi.mybatis.paginator.domain.Order.Direction;
import com.framework.loippi.support.Page;
import com.framework.loippi.support.Pageable;
import cn.youyitech.anyview.system.entity.ClassAndStudent;
import cn.youyitech.anyview.system.entity.ClassEntity;
import cn.youyitech.anyview.system.entity.Course;
import cn.youyitech.anyview.system.entity.CourseArrange;
import cn.youyitech.anyview.system.entity.CourseArrangeAndWorkingTable;
import cn.youyitech.anyview.system.entity.Exercise;
import cn.youyitech.anyview.system.entity.Question;
import cn.youyitech.anyview.system.entity.QuestionContent;
import cn.youyitech.anyview.system.entity.SchemeContentTable;
import cn.youyitech.anyview.system.entity.Student;
import cn.youyitech.anyview.system.entity.Teacher;
import cn.youyitech.anyview.system.entity.WorkingTable;
import cn.youyitech.anyview.system.service.ClassAndStudentService;
import cn.youyitech.anyview.system.service.ClassService;
import cn.youyitech.anyview.system.service.CourseArrangeAndWorkingTableService;
import cn.youyitech.anyview.system.service.CourseArrangeService;
import cn.youyitech.anyview.system.service.CourseService;
import cn.youyitech.anyview.system.service.ExerciseService;
import cn.youyitech.anyview.system.service.QuestionService;
import cn.youyitech.anyview.system.service.RedisService;
import cn.youyitech.anyview.system.service.SchemeContentTableService;
import cn.youyitech.anyview.system.service.ScoreTableService;
import cn.youyitech.anyview.system.service.StudentService;
import cn.youyitech.anyview.system.service.SystemUserService;
import cn.youyitech.anyview.system.service.TeacherService;
import cn.youyitech.anyview.system.service.WorkingTableService;
import cn.youyitech.anyview.system.support.Message;
import cn.youyitech.anyview.system.utils.FileUtils;
import cn.youyitech.anyview.system.utils.PullParserXmlUtils;
import cn.youyitech.anyview.system.utils.ZipFileWrite;


/**
 * Controller - ????????????
 * 
 * @author zzq
 * @version 1.0
 */

@Controller("adminExerciseController")
@RequestMapping("/admin/exercise")
public class ExerciseController extends GenericController {

	@Resource
	private SystemUserService systemUserService;

	@Resource
	private TeacherService teacherService;

	@Resource
	private ExerciseService exerciseService;

	@Resource
	private CourseArrangeService courseArrangeService;

	@Autowired
	private WorkingTableService workingTableService;

	@Resource
	private QuestionService questionService;

	@Resource
	private SchemeContentTableService schemeContentTableService;

	@Resource
	private RedisService redisService;
	
	@Autowired
	private ClassAndStudentService classAndStudentService;
	
	@Autowired
	private ClassService classService;
	
	@Autowired
	private CourseArrangeAndWorkingTableService courseArrangeAndWorkingTableService;
	
	@Autowired
	private CourseService courseService;
	
	@Resource
	private StudentService studentService;

	@Resource
	private PullParserXmlUtils parserUtils;
	
	@Resource
	private ScoreTableService scoreTableService;
	
	@Autowired
	private FileUtils fileUtils;

	private Long cID;//????????????????????????ID
	private List<CourseArrange> courseArrangelist;//???????????????????????????
	
	private String Name_flag=null;
	private float process=0;
	/**
	 * ????????????
	 */
	@RequiresPermissions("admin:system:exercise")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String list(HttpServletRequest request, Pageable pageable, ModelMap model) {

		redisService.delete(systemUserService.getCurrentUserName() + "Exercise" + "workingTableId");
		redisService.delete(systemUserService.getCurrentUserName() + "ExerciseController" + "exerciseList");
		
		//???????????????
		//???????????????id?????????????????????????????????
		Teacher teacher = teacherService.find("username", systemUserService.getCurrentUserName());
		List<CourseArrange> courseArrangeList = courseArrangeService.findList("teacher_id", teacher.getId());
		
		//????????????flag
		if(request.getParameter("Name_flag")!=null) {
			Name_flag = request.getParameter("Name_flag");
		}
	
		//??????id

		String classId = request.getParameter("filter_cId");
		String courseId = request.getParameter("filter_courseId");
		String workingTableId = request.getParameter("filter_vId");
		
		List<WorkingTable> workingTableList = new ArrayList<>();
		List<Course> courseList = new ArrayList<>();
		List<ClassEntity> classSystemList = new ArrayList<>();
		
		for (int i = 0; i < courseArrangeList.size(); i++) {
			//???????????????
			classSystemList.add(courseArrangeList.get(i).getClassSystem());

			if (classId!=null && Integer.valueOf(classId) == courseArrangeList.get(i).getClassSystem().getId()) {
				//????????????????????????
				courseList.add(courseArrangeList.get(i).getCourse());
				
				//????????????????????????????????????????????????
				List<CourseArrangeAndWorkingTable> courseArrangeAndWorkingTableList = courseArrangeAndWorkingTableService.findList("courseArrangeId",
						courseArrangeList.get(i).getId());
				for (int j = 0; j < courseArrangeAndWorkingTableList.size(); j++) {
					//???????????????
					if(courseId!=null && Integer.valueOf(courseId) ==courseArrangeAndWorkingTableList.get(j).getWorkingTable().getCourseId()) {
						workingTableList.add(courseArrangeAndWorkingTableList.get(j).getWorkingTable());
					}
				}			
			}		
		}
		// ??????????????????????????????
		List<WorkingTable> workingTableByCreaterList = workingTableService.findList("tableCreater",
				systemUserService.getCurrentUser().getName());
		for (int i = 0; i < workingTableByCreaterList.size(); i++) {
			if(courseId!=null && Integer.valueOf(courseId) ==workingTableByCreaterList.get(i).getCourseId()) {
				workingTableList.add(workingTableByCreaterList.get(i));
			}
			
		}		
		
		//??????
		String className=null;
		if(classId!=null)
			className = classService.find("id",String.valueOf(classId)).getClassName();

		String courseName=null;
		if(courseId!=null) {
			int cid = Integer.valueOf(courseId);
			Course c = courseService.find((long)cid);
			courseName = c.getCourseName();
		}
			
		String workingTableName=null;
		if(workingTableId!=null) {
			int vid = Integer.valueOf(workingTableId);
			WorkingTable w = workingTableService.find((long)vid);
			workingTableName = w.getTableName();			
		}	
		

		//?????????????????????????????????????????????????????????
		int flag=0;
		
		//??????map
		Map parametermap=null;
		
		//????????????????????????
		List<SchemeContentTable> schemeContentlist=schemeContentTableService.findList("VID",workingTableId);
		int total=schemeContentlist.size();	
		
		//????????????
		if ("desc".equals(Name_flag)) {	
			model.addAttribute("Name_flag", "desc");
			pageable.setOrderProperty("SID");
			pageable.setOrderDirection(Direction.DESC);
		}
		else {
			model.addAttribute("Name_flag", "asc");
		}
		
		
		//Page<ClassAndStudent> cslist = classAndStudentService.findByPage(pageable);
		Page<ClassAndStudent> cslist = null;
		

		//????????????????????????
		if(classId!=null&&!classId.equals("")||courseId!=null&&!courseId.equals("")||workingTableId!=null&&!workingTableId.equals("")) {
			flag=1;
			//???????????????????????????request???????????????pageable??????paramter??????
			processQueryConditions(pageable, request);	
			parametermap = (Map) pageable.getParameter();
			parametermap.remove("cId");
			parametermap.put("student_class_id", classId);
			pageable.setParameter(parametermap);
			
			cslist = classAndStudentService.findByPage(pageable);
			//System.out.println(cslist.getContent().size());
			Map<String,String>[]maps = new Map[cslist.getContent().size()];
			
			for(int i = 0; i<cslist.getContent().size(); i++) {
				Student tempStudent = cslist.getContent().get(i).getStudent();
				//System.out.println(tempStudent.getId());
				maps[i] = new HashMap<String, String>();
				maps[i].put("sno", tempStudent.getUsername());
				maps[i].put("sname", tempStudent.getName());
				maps[i].put("sid",Integer.toString(tempStudent.getId()));
				
				//?????????????????????
				int pass = exerciseService.countRightNumber
						(Integer.valueOf(classId), Integer.valueOf(courseId), Integer.valueOf(workingTableId), tempStudent.getId());
				
				maps[i].put("pass", String.valueOf(pass));
			}
			
			model.addAttribute("maps", maps);
		}
		

		
		model.addAttribute("page", cslist);
		model.addAttribute("params", parametermap);	
		model.addAttribute("flag", flag);
		model.addAttribute("total",total);
		model.addAttribute("classId", classId);
		model.addAttribute("courseId", courseId);
		model.addAttribute("workingTableId", workingTableId);
		model.addAttribute("className", className);
		model.addAttribute("courseName", courseName);
		model.addAttribute("workingTableName", workingTableName);
		
		//?????????????????????????????????
		if(courseId!=null) {
			model.addAttribute("courseList", courseList);
			model.addAttribute("workingTableList", workingTableList);
		}

		
		return "/admin/exercise/list";
	}

	/**
	 * ?????????????????????
	 */
	@RequestMapping(value = "/correct", method = RequestMethod.GET)
	public String correct(Long workingTableId,Long classId, Pageable pageable, ModelMap model) {
		if (classId!=null)	cID=classId;
		// ??????????????????????????????id??????redis
		if (workingTableId != null) {
			redisService.save(systemUserService.getCurrentUserName() + "Exercise" + "workingTableId", String.valueOf(workingTableId));
		} 
		else {
			workingTableId = Long.parseLong(redisService.get(systemUserService.getCurrentUserName() + "Exercise" + "workingTableId"));
		}
		// ????????????????????????id??????????????????????????????
		List<SchemeContentTable> sctList = schemeContentTableService.findList("VID", workingTableId);
		
		//????????????id??????question???
		List<Question> questionList = new ArrayList<>();	
		for (int i = 0; i < sctList.size(); i++) {
			SchemeContentTable schemeContentTable = sctList.get(i);				
			questionList.add(questionService.find(schemeContentTable.getPID()));
		}
		
		//?????????????????????
		int total=classAndStudentService.howMany(cID.intValue());

		List<Integer> list=new ArrayList<>();
		//??????????????????,???????????????count???????????????
		for (int i=0;i<questionList.size();i++){
			List<Exercise> exerciseList = exerciseService.getExerciseAnswer(cID.intValue(),questionList.get(i).getId());
			int pass=0;
			for (int t=0;t<exerciseList.size();t++){
				if (exerciseList.get(t).getRunResult()==1) {
					pass++;
				}
			}
			list.add(pass);
		}
		model.addAttribute("total",total);
		model.addAttribute("pass",list);
		model.addAttribute("page", questionService.pageMethod(pageable,questionList));

		// ????????????????????????????????????QuestionContent??????
		List<QuestionContent> questionContentList = new ArrayList<>();
		for (int i = 0; i < questionList.size(); i++) {
			questionContentList.add(parserUtils.getPullParserQuestionList(questionList.get(i).getContent()).getQuestionContent());
		}
		model.addAttribute("questionContentList", questionContentList);
		model.addAttribute("classId", cID);
		model.addAttribute("vId", workingTableId);

		return "/admin/exercise/correctlist";
	}
	
	/**
         * ???????????????????????????
     */
    @RequestMapping(value = "/judgeQuestion", method = RequestMethod.GET)
    @ResponseBody
    public String judgeQuestion(Long id, int flag, ModelMap model) {
        return "";
    }

	/**
	  *  ?????????????????????,flag???0
	 */
	@RequestMapping(value = "/correctQuestionStudent", method = RequestMethod.GET)
	public String correctQuestion(int cid, int flag, int sid, Long index, int eindex, ModelMap model) {
		//eindex??????????????????
		
		//???redis?????????questionList,???????????????????????????
    	List<Question> tempList = new ArrayList<>();
		String stringFromRedis= redisService.get(systemUserService.getCurrentUserName() + "ExerciseController" + "questionList");
		tempList = JSON.parseObject(stringFromRedis,new TypeReference<List<Question>>() {});	
		Question q =tempList.get(eindex);
		
		Map map = new HashMap();
		
		map.put("index", eindex);
		
		Student tempStudent = studentService.find((long)sid);
		map.put("name", tempStudent.getName());
		map.put("username", tempStudent.getUsername());
		
		map.put("description", parserUtils.getPullParserQuestionList(q.getContent()).getQuestionContent().getQuestion_description());
		map.put("standard", parserUtils.getPullParserQuestionList(q.getContent()).getQuestionContent().getStandard_answer());
		
		Exercise tempExercise = exerciseService.findByPSC((long)q.getId(), sid, (long)cid);
		if(tempExercise!=null) {
			map.put("studentAnswer", parserUtils.getStudentAnswer(tempExercise.getEContent()));
			map.put("pass", tempExercise.getRunResult());
			map.put("comment", tempExercise.getEComment());
		}
			
		else {
			map.put("studentAnswer", "????????????");
			map.put("pass", "???");
		}
			
		
		
		model.addAttribute("map",map);
		model.addAttribute("eindex",eindex);
		model.addAttribute("nindex",eindex==tempList.size()-1?eindex:eindex+1);
		model.addAttribute("pindex",eindex==0?0:eindex-1);
		model.addAttribute("sid",sid);
		model.addAttribute("cid",cid);
		model.addAttribute("pid",q.getId());
		model.addAttribute("eindex",eindex);
		model.addAttribute("flag", flag);
		model.addAttribute("index",index);

		
		return "/admin/exercise/correctQuestionStudent";
	}
	
	/**
	  * ?????????????????????
	 */
	@RequestMapping(value = "/correctQuestionByProblem", method = RequestMethod.GET)
	public String correctQuestionByProblem(int flag, int sindex, Long index, ModelMap model) {
		//??????redis?????????exerciseList?????????index???????????????exercise
		String temp = redisService.get(systemUserService.getCurrentUserName() + "ExerciseController" + "exerciseList");
		List<Exercise> templist = JSON.parseObject(temp,new TypeReference<List<Exercise>>() {});
		
		Exercise exerciseList =templist.get(sindex);
		List<QuestionContent> questionContentList = new ArrayList<>();
		List<String> studentAnswerList = new ArrayList<>();
		questionContentList.add(parserUtils.getPullParserQuestionList(exerciseList.getQuestion().getContent()).getQuestionContent());
		studentAnswerList.add(parserUtils.getStudentAnswer(exerciseList.getEContent()));
		model.addAttribute("exerciseList", exerciseList);
		redisService.save(systemUserService.getCurrentUserName() + "ExerciseController" + "exerciseList",JSON.toJSONString(templist));
		
		
		model.addAttribute("nsindex",sindex+1==templist.size()?sindex:sindex+1);
		model.addAttribute("psindex",sindex==0?0:sindex-1);
		model.addAttribute("sindex",sindex);
		model.addAttribute("pid",exerciseList.getPId());
		model.addAttribute("sid",exerciseList.getSId());
		model.addAttribute("cid",cID);
		model.addAttribute("questionContentList",questionContentList);
		model.addAttribute("studentAnswerList",studentAnswerList);
		model.addAttribute("flag", flag);
		model.addAttribute("index",index);		
		return "/admin/exercise/correctQuestionProblem";
		
	}
	
	//??????????????????????????????????????????
	@RequestMapping(value = "/selectlist",method = RequestMethod.GET)
	public String selectlist(HttpServletRequest request,int sid,ModelMap model) {
		String cId=request.getParameter("cId");
		String courseId=request.getParameter("courseId");
		String vId=request.getParameter("vId");
		int studentId = sid;
		Student tempStudent = studentService.find((long)studentId);
		String studentName = tempStudent.getName();
		
		List<Question> redisQustionList = new ArrayList<>();
		List<SchemeContentTable>questionList = schemeContentTableService.findList("VID", vId);
		for (SchemeContentTable sct : questionList) {
			redisQustionList.add(sct.getQuestion());
		}
		redisService.save(systemUserService.getCurrentUserName() + "ExerciseController" + "questionList",JSON.toJSONString(redisQustionList));		
		
		Map[]maps = new Map[questionList.size()];
		for (int i=0; i<questionList.size();i++) {
			SchemeContentTable q = questionList.get(i);
			maps[i] = new HashMap<String, String>();
			maps[i].put("chapter", q.getVChapName());
			maps[i].put("questionName", q.getQuestion().getQuestion_name());
			maps[i].put("description", parserUtils.getPullParserQuestionList(q.getQuestion().getContent()).getQuestionContent().getQuestion_description());
			
			Exercise e = exerciseService.findByPSC((long)q.getQuestion().getId(), studentId, (long)Integer.valueOf(cId));
			if(e==null) {
				maps[i].put("pass", "?????????");
				maps[i].put("score","0");
				maps[i].put("time", "?????????");

			}
			else {
				maps[i].put("pass", e.getRunResult()==1?"???":"???");
				maps[i].put("score",e.getScore());
				maps[i].put("time", e.getFirstPastTime());
			}
			
		}
		
		model.addAttribute("cId", cId);
		model.addAttribute("courseId", courseId);
		model.addAttribute("vId", vId);
		model.addAttribute("sId",studentId);
		model.addAttribute("maps", maps);
		model.addAttribute("name", studentName);
		
		return "/admin/exercise/selectlist";
	}
	

	//??????????????????????????????
	@RequestMapping(value = "/stulist",method = RequestMethod.GET)
	public String stulist(Long id, ModelMap model) {
		//?????????????????????????????????
		List<Student> allstudent = new ArrayList<>();
		
		List<ClassAndStudent> templist = classAndStudentService.findByClassID(cID.intValue());
		for (ClassAndStudent classAndStudent : templist) {
			allstudent.add(classAndStudent.getStudent());
		}
		
		//????????????exercise???????????????
		List<Exercise> exerciseList = new ArrayList<Exercise>();
		Iterator it=allstudent.iterator();
		while(it.hasNext()) {
			Student s= (Student)it.next();
			Exercise exercise= exerciseService.findByPSC(id, s.getId(), cID);
			if(exercise!=null) {
				exerciseList.add(exercise);
				it.remove();
			}
		}
		redisService.save(systemUserService.getCurrentUserName() + "ExerciseController" + "exerciseList",JSON.toJSONString(exerciseList));
		model.addAttribute("exerciseList", exerciseList);
		model.addAttribute("studentList", allstudent);
		
		return "/admin/exercise/stulist";
	}
	
	//????????????????????????
	@SuppressWarnings("unchecked")
	@ResponseBody
    @RequestMapping("jump")
    public Map<String,Object> jump(int eindex,int sid, int cid){
		//???redis?????????questionList,???????????????????????????
    	List<Question> tempList = new ArrayList<>();
		String stringFromRedis= redisService.get(systemUserService.getCurrentUserName() + "ExerciseController" + "questionList");
		tempList = JSON.parseObject(stringFromRedis,new TypeReference<List<Question>>() {});	
		Question q =tempList.get(eindex);
		
		Map map = new HashMap();
		
		map.put("index", eindex);
		
		Student tempStudent = studentService.find((long)sid);
		map.put("name", tempStudent.getName());
		map.put("username", tempStudent.getUsername());
		
		map.put("description", parserUtils.getPullParserQuestionList(q.getContent()).getQuestionContent().getQuestion_description());
		map.put("standard", parserUtils.getPullParserQuestionList(q.getContent()).getQuestionContent().getStandard_answer());
		
		Exercise tempExercise = exerciseService.findByPSC((long)q.getId(), sid, (long)cid);
		if(tempExercise!=null) {
			map.put("answer", parserUtils.getStudentAnswer(tempExercise.getEContent()));
			map.put("pass", tempExercise.getRunResult()==1?"???":"???");
			map.put("comment", tempExercise.getEComment());
		}
			
		else {
			map.put("studentAnswer", "????????????");
			map.put("pass", "/");
		}
		map.put("eindex", eindex);
		map.put("nindex", eindex==tempList.size()-1?eindex:eindex+1);
		map.put("pindex", eindex==0?0:eindex-1);
		
		return map;		
	}
	
	//???????????????????????????
	@ResponseBody
    @RequestMapping("save0")
	public void save0(int eindex, int sid, int cid, String comment){
    	List<Question> tempList = new ArrayList<>();
		String stringFromRedis= redisService.get(systemUserService.getCurrentUserName() + "ExerciseController" + "questionList");
		tempList = JSON.parseObject(stringFromRedis,new TypeReference<List<Question>>() {});
		Question q =tempList.get(eindex);

		Exercise e = exerciseService.findByPSC((long)q.getId(), sid, (long)cid);
		e.setEComment(comment);
		exerciseService.update(e);	
	}
	
	//???????????????????????????
	@ResponseBody
    @RequestMapping("save1")
	public void save1(int sindex, String comment){
    	//???redis?????????exercise????????????????????????exercise
    	List<Exercise> tempList = new ArrayList<>();
		String stringFromRedis= redisService.get(systemUserService.getCurrentUserName() + "ExerciseController" + "exerciseList");
		tempList = JSON.parseObject(stringFromRedis,new TypeReference<List<Exercise>>() {});	
		
		
		tempList.get(sindex).setEComment(comment);
		exerciseService.update(tempList.get(sindex));	
		
		//????????????redis????????????????????????????????????????????????
		redisService.save(systemUserService.getCurrentUserName() + "ExerciseController" + "exerciseList",JSON.toJSONString(tempList));
	}
	/**
	 * ???????????????
	 */
	@ModelAttribute
	public void init(Model model) {
		// ?????????????????????????????????
		Teacher temp = new Teacher();
		temp.setUsername(systemUserService.getCurrentUserName());
		temp.setSchoolId(systemUserService.getCurrentUser().getSchoolId());
		Teacher currentTeacher = teacherService.findByUserName(temp);

		List<CourseArrange> courseArrangeList = courseArrangeService.findByTeacher(currentTeacher.getId());

		List<ClassEntity> classList = new ArrayList<>();
		List<Course> courseList = new ArrayList<>();
		List<Long> tableIdList = new ArrayList<>();
		WorkingTable workingTable = new WorkingTable();
		List<WorkingTable> workList = new ArrayList<>();

		boolean classFlag = true;
		boolean courseFlag = true;

		for (int i = 0; i < courseArrangeList.size(); i++) {
			CourseArrange courseArrange = courseArrangeList.get(i);
			// ??????????????????
			for (int j = 0; j < classList.size(); j++) {
				if (classList.get(j).getId() == courseArrange.getClassSystem().getId()) {
					classFlag = false;
				}
			}
			if (classFlag) {
				classList.add(courseArrangeList.get(i).getClassSystem());
			} else {
				classFlag = true;
			}
			// ??????????????????
			for (int j = 0; j < courseList.size(); j++) {
				if (courseList.get(j).getId() == courseArrange.getCourse().getId()) {
					courseFlag = false;
				}
			}
			if (courseFlag) {
				courseList.add(courseArrangeList.get(i).getCourse());
			} else {
				courseFlag = true;
			}
		}
		// ????????????????????????
		for (int i = 0; i < courseArrangeList.size(); i++) {
			List<CourseArrangeAndWorkingTable> courseArrangeAndWorkingTableList = courseArrangeList.get(i)
					.getCourseArrangeAndWorkingTable();
			for (int j = 0; j < courseArrangeAndWorkingTableList.size(); j++) {
				tableIdList.add((long) courseArrangeAndWorkingTableList.get(j).getWorkingTableId());
			}
		}
		List<WorkingTable> workingTableByCreaterList = workingTableService.findList("tableCreater",
				systemUserService.getCurrentUser().getName());
		for (int i = 0; i < workingTableByCreaterList.size(); i++) {
			tableIdList.add(workingTableByCreaterList.get(i).getId());
		}
		// ???????????????id
		if (tableIdList.size() > 1) {
			List<Long> tableIdListNew = new ArrayList<>();
			for (Long id : tableIdList) {
				if (!tableIdListNew.contains(id)) {
					tableIdListNew.add(id);
				}
			}
			tableIdList = tableIdListNew;
		}
		if (tableIdList.size() > 0) {
			workingTable.setTableIdList(tableIdList);
			workList = workingTableService.findContentList(workingTable);
		}

		model.addAttribute("classList", classList);
		//model.addAttribute("courseList", courseList);
		//model.addAttribute("workingTableList", workList);

	}
	
	
	//?????????????????????ajax
	@ResponseBody
	@RequestMapping("getChangeajax")
	public Object getChangeList( String parentId, String flag){
		Map<String,Object> returnMap = new HashMap<String,Object>();
		Teacher temp = new Teacher();
		temp.setUsername(systemUserService.getCurrentUserName());
		temp.setSchoolId(systemUserService.getCurrentUser().getSchoolId());
		Teacher currentTeacher = teacherService.findByUserName(temp);
		List<CourseArrange> courseArrangeList = courseArrangeService.findByTeacher(currentTeacher.getId());
		List<CourseArrange> courseArranges=new ArrayList<>();
		if (flag.equals("C")){

			List<Course> courseList=new ArrayList<>();
			for (int i=0;i<courseArrangeList.size();i++) {
				if (Integer.parseInt(parentId) == courseArrangeList.get(i).getClassSystem().getId()) {
					courseArranges.add(courseArrangeList.get(i));
				}
			}
			boolean courseflag=true;
			for (int i = 0; i < courseArranges.size(); i++) {
				CourseArrange courseArrange = courseArranges.get(i);
				// ??????????????????
				for (int j = 0; j < courseList.size(); j++) {
					if (courseList.get(j).getId() == courseArrange.getClassSystem().getId()) {
						courseflag= false;
					}
				}
				if (courseflag){
					courseList.add(courseArranges.get(i).getCourse());
				}
				else courseflag=true;
			}
			courseArrangelist=courseArranges;
			returnMap.put("result","success");
			returnMap.put("courselist",courseList);
		}
		else {
			//parentId???courseId
			List<WorkingTable> workList = new ArrayList<>();
			List<Long> tableIdList = new ArrayList<>();
			WorkingTable workingTable = new WorkingTable();
			
			for (int i = 0; i <courseArrangelist.size(); i++) {
				List<CourseArrangeAndWorkingTable> courseArrangeAndWorkingTableList=null;
				if(Integer.valueOf(parentId)==courseArrangelist.get(i).getCourse_id()) {
					courseArrangeAndWorkingTableList = courseArrangelist.get(i).getCourseArrangeAndWorkingTable();
					for (int j = 0; j < courseArrangeAndWorkingTableList.size(); j++) {
						tableIdList.add((long) courseArrangeAndWorkingTableList.get(j).getWorkingTableId());	
					}	
				}
			}
			List<WorkingTable> workingTableByCreaterList = workingTableService.findList("tableCreater",
					systemUserService.getCurrentUser().getName());
			for (int i = 0; i < workingTableByCreaterList.size(); i++) {
				if(workingTableByCreaterList.get(i).getCourseId()==Integer.valueOf(parentId)) {
					tableIdList.add(workingTableByCreaterList.get(i).getId());
				}
			}
			// ???????????????id
			if (tableIdList.size() > 1) {
				List<Long> tableIdListNew = new ArrayList<>();
				for (Long id : tableIdList) {
					if (!tableIdListNew.contains(id)) {
						tableIdListNew.add(id);
					}
				}
				tableIdList = tableIdListNew;
			}
			if (tableIdList.size() > 0) {
				workingTable.setTableIdList(tableIdList);
				workList = workingTableService.findContentList(workingTable);
			}

			returnMap.put("result","success");
			returnMap.put("workingtablelist",workList);

		}
		return returnMap;
	}
	
	@RequestMapping(value = { "/exportByStudentOne" })
	public @ResponseBody Message exportByStudentOne(HttpServletRequest request,HttpServletResponse response) {
		int classId = Integer.valueOf(request.getParameter("cid"));
		int courseId = Integer.valueOf(request.getParameter("coid"));
		int vId = Integer.valueOf(request.getParameter("vid"));
		
		List<ClassAndStudent> tempList = classAndStudentService.findByClassID(classId);
		List<Student> studentList = new ArrayList<Student>();
		for (ClassAndStudent classAndStudent : tempList) {
			studentList.add(classAndStudent.getStudent());
		}
				
		File file = new File(systemUserService.getCurrentUserName());
		if(!file.exists()) {
			file.mkdir();
		}
		else {
			//?????????
			fileUtils.deleteDir(file);
			file.mkdir();
		}
		//???????????????????????????excel
		File output = new File(file.getPath()+File.separator+"????????????");
		Workbook wb = new HSSFWorkbook();
		
		float number = 0;
		for (Student student : studentList) {
			Sheet sheet = wb.createSheet(student.getName());
			Row row = sheet.createRow((int) 0);
			
			Font font = wb.createFont();
			CellStyle style = wb.createCellStyle();
			style.setWrapText(true); 	//????????????		
			font.setFontHeightInPoints((short) 0);	// ??????????????????		
			font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);	// ????????????
			
			Cell cell = row.createCell((short) 0);
			cell.setCellStyle(style);
			cell.setCellValue("??????");
			
			cell = row.createCell((short) 1);
			cell.setCellStyle(style);
			cell.setCellValue("??????");
			
			cell = row.createCell((short) 2);
			cell.setCellStyle(style);
			cell.setCellValue("????????????");
			
			cell = row.createCell((short) 3);
			cell.setCellStyle(style);
			cell.setCellValue("????????????");
			
			cell = row.createCell((short) 4);
			cell.setCellStyle(style);
			cell.setCellValue("????????????");
			
			String name = student.getName();
			String num = student.getUsername();
			
			// ????????????????????????id??????????????????????????????
			List<SchemeContentTable> sctList = schemeContentTableService.findList("VID", vId);	
			//????????????id??????question???
			List<Question> questionList = new ArrayList<>();	
			for (int i = 0; i < sctList.size(); i++) {
				SchemeContentTable schemeContentTable = sctList.get(i);				
				questionList.add(questionService.find(schemeContentTable.getPID()));
			}
			
			int i = 1;
			for (Question q : questionList) {
				row = sheet.createRow(i);
				
				String standardAnswer = parserUtils.getPullParserQuestionList(q.getContent()).getQuestionContent().getStandard_answer();
				//String questionDescription = parserUtils.getPullParserQuestionList(q.getContent()).getQuestionContent().getQuestion_description();
				String answer;
				String pass;
				
				Exercise e = exerciseService.findByPSC((long)q.getId(), student.getId(), (long)classId);
				if(e==null) {
					answer = "????????????";
					pass = "/";
				}
				else {
					answer = parserUtils.getStudentAnswer(e.getEContent());
					pass = e.getRunResult()==1?"???":"???";
				}
				
				row = sheet.createRow(i);
				
				cell = row.createCell((short) 0);
				cell.setCellStyle(style);
				cell.setCellValue(name);
				
				cell = row.createCell((short) 1);
				cell.setCellStyle(style);
				cell.setCellValue(num);
				
				cell = row.createCell((short) 2);
				cell.setCellStyle(style);
				cell.setCellValue(pass);
				
				cell = row.createCell((short) 3);
				cell.setCellStyle(style);
				cell.setCellValue(answer);
				
				cell = row.createCell((short) 4);
				cell.setCellStyle(style);
				cell.setCellValue(standardAnswer);
				
				i++;
			}	
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(2);
			sheet.autoSizeColumn(3);
			sheet.autoSizeColumn(4);
			
			number++;
			process = (number/studentList.size())*100;
		}
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(output);
			wb.write(fout);
			fout.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/x-download");
		response.setHeader("Content-Disposition", "attachment;filename=studentAnswer.xls");
		OutputStream out;
		InputStream in;
		try {
			out = response.getOutputStream();
			in = new FileInputStream(output);
			 
			int len = 0;
			byte[] buffer = new byte[1024];
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}	 
			in.close();	
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		return SUCCESS_MESSAGE;
	}
	
	@RequestMapping(value = { "/exportByProblemOne" })
	public @ResponseBody Message exportByProblemOne(HttpServletRequest request,HttpServletResponse response) {
		
		int classId = Integer.valueOf(request.getParameter("cid"));
		int vId = Integer.valueOf(request.getParameter("vid"));
		
		File file = new File(systemUserService.getCurrentUserName());
		if(!file.exists()) {
			file.mkdir();
		}
		else {
			//?????????
			fileUtils.deleteDir(file);
			file.mkdir();
		}
		
		//???????????????????????????excel
		File output = new File(file.getPath()+File.separator+"????????????");
		Workbook wb = new HSSFWorkbook();
		
		List<SchemeContentTable> questionList = schemeContentTableService.findList("VID", vId);
		List<ClassAndStudent> casList = classAndStudentService.findByClassID(classId);
		
		float haveDone = 0;
		for (SchemeContentTable schemeContentTable : questionList) {
			Long pid = schemeContentTable.getPID();
			Question q = questionService.find(pid);
			Sheet sheet = wb.createSheet(pid.toString());
			
			Row row = sheet.createRow((int) 0);
			Font font = wb.createFont();
			CellStyle style = wb.createCellStyle();

			style.setWrapText(true); 	//????????????		
			font.setFontHeightInPoints((short) 0);	// ??????????????????		
			font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);	// ????????????
			
			Cell cell = row.createCell((short) 0);
			cell.setCellStyle(style);
			cell.setCellValue("??????");
			
			cell = row.createCell((short) 1);
			cell.setCellStyle(style);
			cell.setCellValue("??????");
			
			cell = row.createCell((short) 2);
			cell.setCellStyle(style);
			cell.setCellValue("??????");
			
			cell = row.createCell((short) 3);
			cell.setCellStyle(style);
			cell.setCellValue("????????????");
			
			cell = row.createCell((short) 4);
			cell.setCellStyle(style);
			cell.setCellValue("????????????");
			

			//???????????????????????????????????????????????????
			row = sheet.createRow(1);
			String standardAnswer = parserUtils.getPullParserQuestionList(q.getContent()).getQuestionContent().getStandard_answer();
			String questionDescription = parserUtils.getPullParserQuestionList(q.getContent()).getQuestionContent().getQuestion_description();
				
			cell = row.createCell((short) 4);
			cell.setCellStyle(style);
			cell.setCellValue(standardAnswer);
			
			int i = 2;	
			for (ClassAndStudent classAndStudent : casList) {
				Student tempStudent = classAndStudent.getStudent();
				Exercise e = exerciseService.findByPSC(pid, tempStudent.getId(), (long)classId);
				String name = tempStudent.getName();
				String number = tempStudent.getUsername();
				String answer;
				String pass;
				if(e==null) {
					answer = "????????????";
					pass = "/";
				}
				else {
					answer = parserUtils.getStudentAnswer(e.getEContent());
					pass = e.getRunResult()==1?"???":"???";
				}
				
				row = sheet.createRow(i);
				
				cell = row.createCell((short) 0);
				cell.setCellStyle(style);
				cell.setCellValue(name);
				
				cell = row.createCell((short) 1);
				cell.setCellStyle(style);
				cell.setCellValue(number);
				
				cell = row.createCell((short) 2);
				cell.setCellStyle(style);
				cell.setCellValue(answer);
				
				cell = row.createCell((short) 3);
				cell.setCellStyle(style);
				cell.setCellValue(pass);
				
			
				i++;
			}
			sheet.autoSizeColumn(0);
			sheet.autoSizeColumn(1);
			sheet.autoSizeColumn(2);
			sheet.autoSizeColumn(3);
			sheet.autoSizeColumn(4);
			
			haveDone++;
			process = haveDone/questionList.size()*100;
		}
		
		FileOutputStream fout;
		try {
			fout = new FileOutputStream(output);
			wb.write(fout);
			fout.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		response.setCharacterEncoding("UTF-8");
		response.setContentType("application/x-download");
		response.setHeader("Content-Disposition", "attachment;filename=studentAnswer.xls");
		OutputStream out;
		InputStream in;
		try {
			out = response.getOutputStream();
			in = new FileInputStream(output);
			 
			int len = 0;
			byte[] buffer = new byte[1024];
			while ((len = in.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}	 
			in.close();	
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		return SUCCESS_MESSAGE;
	}
	
	@ResponseBody
	@RequestMapping("getProcess")
	public Object getProcess( String parentId, String flag){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("process", Math.round(process));
		return map;
	}

}
