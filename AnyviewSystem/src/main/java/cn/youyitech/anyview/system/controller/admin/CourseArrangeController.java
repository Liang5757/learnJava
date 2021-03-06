package cn.youyitech.anyview.system.controller.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import cn.youyitech.anyview.system.utils.SortUtil;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alibaba.fastjson.JSON;
import com.framework.loippi.support.Pageable;

import cn.youyitech.anyview.system.dto.user.User;
import cn.youyitech.anyview.system.entity.ClassEntity;
import cn.youyitech.anyview.system.entity.College;
import cn.youyitech.anyview.system.entity.Course;
import cn.youyitech.anyview.system.entity.CourseArrange;
import cn.youyitech.anyview.system.entity.CourseArrangeAndWorkingTable;
import cn.youyitech.anyview.system.entity.Major;
import cn.youyitech.anyview.system.entity.School;
import cn.youyitech.anyview.system.entity.Teacher;
import cn.youyitech.anyview.system.entity.WorkingTable;
import cn.youyitech.anyview.system.service.ClassService;
import cn.youyitech.anyview.system.service.CollegeService;
import cn.youyitech.anyview.system.service.CourseArrangeAndWorkingTableService;
import cn.youyitech.anyview.system.service.CourseArrangeService;
import cn.youyitech.anyview.system.service.CourseService;
import cn.youyitech.anyview.system.service.RedisService;
import cn.youyitech.anyview.system.service.SchoolService;
import cn.youyitech.anyview.system.service.SystemUserService;
import cn.youyitech.anyview.system.service.TeacherService;
import cn.youyitech.anyview.system.service.WorkingTableService;
import cn.youyitech.anyview.system.support.EnumConstants;
import cn.youyitech.anyview.system.support.Message;
import cn.youyitech.anyview.system.support.WorktableEnum;
import cn.youyitech.anyview.system.utils.IdsUtils;

/**
 * Controller - ????????????
 * 
 * @author zzq
 * @version 1.0
 */

@Controller("adminCourseArrangeController")
@RequestMapping("/admin/course_arrange")
public class CourseArrangeController extends GenericController {

	@Resource
	private CourseArrangeService courseArrangeService;

	@Resource
	private SystemUserService systemUserService;

	@Autowired
	private SchoolService schoolService;

	@Autowired
	private CollegeService collegeService;

	@Autowired
	private ClassService classService;

	@Autowired
	private CourseService courseService;

	@Autowired
	private TeacherService teacherService;

	@Autowired
	private WorkingTableService workingTableService;

	@Autowired
	private CourseArrangeAndWorkingTableService courseArrangeAndWorkingTableService;

	@Resource
	private RedisService redisService;

	@Autowired
	private IdsUtils idsUtils;

	// ????????????????????????
	@RequestMapping(value = "/checkAll", method = RequestMethod.GET)
	public @ResponseBody boolean checkAll(String id, String class_id, int course_id, int teacher_id) {

		// ????????????????????????????????????
		CourseArrange courseArrange = null;
		if (id != null && !id.equals("")) {
			courseArrange = courseArrangeService.find(Long.parseLong(id));
		}
		if (class_id.contains(",")) {
			String[] classId = class_id.split(",");
			boolean[] bArray = new boolean[classId.length];
			for (int j = 0; j < classId.length; j++) {
				// ????????????????????????
				if (courseArrange != null) {
					if ((courseArrange.getClass_id() == Long.parseLong(classId[j]))
							&& courseArrange.getTeacher_id() == teacher_id) {
						bArray[j] = true;
					}else{
						CourseArrange temp = new CourseArrange();
						temp.setClass_id(Integer.parseInt(classId[j]));
						temp.setCourse_id(course_id);
						temp.setTeacher_id(teacher_id);
						List<CourseArrange> courseArrangeList = courseArrangeService.findByAttribute(temp);
						if (courseArrangeList.size() > 0) {
							bArray[j] = false;
						}else{
							bArray[j] = true;
						}
					}
				}else{
					//??????
					CourseArrange temp = new CourseArrange();
					temp.setClass_id(Integer.parseInt(classId[j]));
					temp.setCourse_id(course_id);
					temp.setTeacher_id(teacher_id);
					List<CourseArrange> courseArrangeList = courseArrangeService.findByAttribute(temp);
					if (courseArrangeList.size() > 0) {
						bArray[j] = false;
					}else{
						bArray[j] = true;
					}
				}
			}
			for(int k=0;k<bArray.length;k++){
				if(!bArray[k]){
					return false;
				}
			}
			return true;
		} else {
			// ????????????????????????
			if (courseArrange != null) {
				if ((courseArrange.getClass_id() == Long.parseLong(class_id))
						&& courseArrange.getTeacher_id() == teacher_id) {
					return true;
				}
			}
			CourseArrange temp = new CourseArrange();
			temp.setClass_id(Integer.parseInt(class_id));
			temp.setCourse_id(course_id);
			temp.setTeacher_id(teacher_id);
			List<CourseArrange> courseArrangeList = courseArrangeService.findByAttribute(temp);
			if (courseArrangeList.size() > 0) {
				return false;
			}
		}

		return true;

	}

	/**
	 * ??????
	 */
	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public String add(ModelMap model) {
		// ???????????????redis???????????????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "class");

		return "/admin/course_arrange/add";
	}

	/**
	 * ????????????
	 */
	@Transactional
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String save(HttpServletRequest request, RedirectAttributes redirectAttributes) {
		// ??????id
		String className = request.getParameter("class_name");
		// ??????id
		String courseName = request.getParameter("courseName");
		// ??????id
		String teacherName = request.getParameter("teacherName");
		// ?????????id
		String workingTableId = request.getParameter("working_table");
		// ?????????????????????
		User user = systemUserService.getCurrentUser();
		// ????????????
		if (className.contains(",")) {
			String[] classArray = className.split(",");
			for (int i = 0; i < classArray.length; i++) {
				// ??????????????????
				CourseArrange courseArrange = getCourseArrange("", classArray[i], courseName, teacherName);
				courseArrange.setCreated_person(user.getName());
				courseArrange.setCreated_date(new Date());
				courseArrange.setEnabled(1);

				courseArrangeService.save(courseArrange);

				// ??????????????????????????????
				long courseArrangeId = courseArrangeService.findTotal().get(courseArrangeService.findTotal().size() - 1)
						.getId();
				//addcAndw(courseArrange.getClass_id(),courseArrangeId, workingTableId);
				addcAndw(courseArrange.getClass_id(),courseArrangeId, workingTableId, courseName, teacherName);

			}
		} else {
			// ????????????
			CourseArrange courseArrange = getCourseArrange("", className, courseName, teacherName);
			courseArrange.setCreated_person(user.getName());
			courseArrange.setCreated_date(new Date());
			courseArrange.setEnabled(1);
			courseArrangeService.save(courseArrange);

			// ??????????????????????????????
			long courseArrangeId = courseArrangeService.findTotal().get(courseArrangeService.findTotal().size() - 1)
					.getId();
			//addcAndw(courseArrange.getClass_id(),courseArrangeId, workingTableId);
			addcAndw(courseArrange.getClass_id(),courseArrangeId, workingTableId, courseName, teacherName);

		}
		addFlashMessage(redirectAttributes, SUCCESS_MESSAGE);
		return "redirect:list.jhtml";
	}

	/**
	 * ????????????
	 */
	@RequestMapping(value = { "/delete" }, method = { RequestMethod.POST })
	public @ResponseBody Message delete(Long[] ids) {

		List<String> recordCourseArrange = new ArrayList<>();
		for (long id : ids) {
			recordCourseArrange.add(String.valueOf(id));
		}
		if (recordCourseArrange.size() != 0) {
			try {
				courseArrangeService.deleteOne(recordCourseArrange);
			} catch (Exception e) {
				return ERROR_MESSAGE;
			}
			return SUCCESS_MESSAGE;
		} else {
			return ERROR_MESSAGE;
		}
	}

	/**
	 * ????????????
	 */
	@RequiresPermissions("admin:system:coursearrange")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String list(HttpServletRequest request, Pageable pageable, ModelMap model) {
		// ?????????????????????redis??????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "edit_course_arrange");
		processQueryConditions(pageable, request);
		Map map = (Map) pageable.getParameter();
		pageable.setParameter(map);
		model.addAttribute("params", map);
		// ????????????????????????????????????????????????????????????????????????????????????????????????
		model.addAttribute("page", this.courseArrangeService.findByPage(pageable));

		return "/admin/course_arrange/list";
	}

	/**
	 * ??????
	 */
	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public String edit(Long id, ModelMap model) {
		// ??????redis????????????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "class");
		// ??????id???????????????????????????
		CourseArrange edit_course_arrange = courseArrangeService.find(id);
		redisService.save(systemUserService.getCurrentUserName() + "edit_course_arrange",
				JSON.toJSONString(edit_course_arrange));

		// ????????????id???????????????????????????
		long schoolId = edit_course_arrange.getClassSystem().getMajor().getCollege().getSchool().getId();
		School school = schoolService.find(schoolId);
		List<College> collegeList = school.getSchoolCollegeList();
		SortUtil.sort(collegeList,true,"collegeName");
		// ????????????id???????????????????????????
		long collegeId = edit_course_arrange.getClassSystem().getMajor().getCollege().getId();
		College college = collegeService.find(collegeId);
		List<Major> majorList = college.getCollegeMajorList();
		SortUtil.sort(majorList,true,"majorName");
		// ????????????id???????????????????????????
		List<Course> courseList = courseService.findByInMany((int) collegeId);

		// ????????????id?????????????????????
		List<Teacher> teacherList = teacherService.findList("college_id", collegeId);
		SortUtil.sort(teacherList,true,"name");
		model.addAttribute("course_arrange", edit_course_arrange);
		model.addAttribute("collegeList", collegeList);
		model.addAttribute("majorList", majorList);
		model.addAttribute("courseList", courseList);
		model.addAttribute("teacherList", teacherList);

		return "/admin/course_arrange/edit";
	}

	/**
	 * ????????????
	 */
	@Transactional
	@RequestMapping(value = "/editSave", method = RequestMethod.POST)
	public String editSave(HttpServletRequest request, RedirectAttributes redirectAttributes) {

		String id = request.getParameter("id");
		// ??????id
		String className = request.getParameter("class_name");
		// ??????id
		String courseName = request.getParameter("courseName");
		// ??????id
		String teacherName = request.getParameter("teacherName");
		// ?????????id
		String workingTableId = request.getParameter("working_table");
		// ??????????????????
		User user = systemUserService.getCurrentUser();

		if (className.contains(",")) {
			// ?????????????????????????????????
			String[] classArray = className.split(",");
			// ??????????????????????????????
			CourseArrange courseArrange = getCourseArrange(id, classArray[0], courseName, teacherName);
			courseArrangeService.update(courseArrange);

			// ??????????????????????????????
			// ????????????????????????????????????????????????
			courseArrangeAndWorkingTableService.deleteByCourseArrangeId(courseArrange.getId());
			// ???????????????????????????????????????
			//addcAndw(courseArrange.getClass_id(),courseArrange.getId(), workingTableId);
			addcAndw(courseArrange.getClass_id(),courseArrange.getId(), workingTableId, courseName, teacherName);

			for (int i = 1; i < classArray.length; i++) {
				// ?????????????????????????????????
				CourseArrange courseA = getCourseArrange("", classArray[i], courseName, teacherName);
				courseA.setCreated_person(user.getName());
				courseA.setCreated_date(new Date());
				courseA.setEnabled(1);
				courseArrangeService.save(courseA);

				// ??????????????????????????????
				//addcAndw(courseA.getClass_id(),courseArrangeService.findTotal().get(courseArrangeService.findTotal().size() - 1).getId(),
				//		workingTableId);
				addcAndw(courseA.getClass_id(),courseArrangeService.findTotal().get(courseArrangeService.findTotal().size() - 1).getId(),
						workingTableId,courseName, teacherName);

			}
		} else {
			// ?????????????????????????????????
			CourseArrange courseArrange = getCourseArrange(id, className, courseName, teacherName);
			courseArrangeService.update(courseArrange);

			// ??????????????????????????????
			// ????????????????????????????????????????????????
			courseArrangeAndWorkingTableService.deleteByCourseArrangeId(courseArrange.getId());
			// ???????????????????????????????????????
			//addcAndw(courseArrange.getClass_id(),courseArrange.getId(), workingTableId);
			addcAndw(courseArrange.getClass_id(),courseArrange.getId(), workingTableId, courseName,teacherName);

		}

		addFlashMessage(redirectAttributes, SUCCESS_MESSAGE);

		return "redirect:list.jhtml";
	}

	/**
	 * ???????????????
	 */
	@ModelAttribute
	public void init(Model model) {

		// ?????????????????????????????????
		List<School> schoolList = schoolService.findAll();

		model.addAttribute("schoolList", schoolList);
		model.addAttribute("systemUser", systemUserService.getCurrentUser());
		// ?????????????????????????????????????????????????????????????????????????????????????????????
		if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.manager.getValue()) {
			List<College>collegeList=schoolService.find((long) systemUserService.getCurrentUser().getSchoolId()).getSchoolCollegeList();
			SortUtil.sort(collegeList,true,"collegeName");
			model.addAttribute("collegeList",collegeList);
		}
	}

	/**
	 * ????????????????????????id?????????????????????
	 */
	@ResponseBody
	@RequestMapping("/ClassAjax")
	public List<ClassEntity> classAjax(@RequestBody Major major) {

		// ??????redis????????????????????????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "class");
		// ????????????????????????id?????????????????????
		List<ClassEntity> classList = classService.findList("major_id", major.getId());
		SortUtil.sort(classList,true,"className");
		// ???????????????????????????redis???
		redisService.save(systemUserService.getCurrentUserName() + "class", JSON.toJSONString(classList));

		return classList;
	}

	/**
	 * ?????????????????????
	 */
	@RequestMapping(value = { "/listDialogClass" }, method = { RequestMethod.GET })
	public String listDialogClass(HttpServletRequest request, Pageable pageable, ModelMap model) {
		// ???redis?????????????????????
		List<ClassEntity> classList = null;
		if (redisService.get(systemUserService.getCurrentUserName() + "class") != null) {
			classList = idsUtils.classjsonToList(redisService.get(systemUserService.getCurrentUserName() + "class"),
					ClassEntity.class);
			SortUtil.sort(classList,true,"className");
		} else {
			classList = new ArrayList<>();
		}
		// ?????????????????????????????????
		if (redisService.get(systemUserService.getCurrentUserName() + "edit_course_arrange") != null
				&& classList.size() == 0) {
			// ??????????????????????????????
			CourseArrange edit_course_arrange = JSON.parseObject(
					redisService.get(systemUserService.getCurrentUserName() + "edit_course_arrange"),
					CourseArrange.class);
			long majorId = edit_course_arrange.getClassSystem().getMajor().getId();
			List<ClassEntity> newclassList = classService.findList("major_id", majorId);
			classList = newclassList;
			redisService.save(systemUserService.getCurrentUserName() + "class", JSON.toJSONString(newclassList));
			model.addAttribute("page", classService.pageMethod(pageable, newclassList));

		} else {
			redisService.save(systemUserService.getCurrentUserName() + "class", JSON.toJSONString(classList));
			model.addAttribute("page", classService.pageMethod(pageable, classList));
		}
		return "/admin/course_arrange/listDialogClass";
	}

	/**
	 * ????????????????????????id?????????????????????
	 */
	@ResponseBody
	@RequestMapping("/CourseAjax")
	public List<Course> courseAjax(String collegeId) {

		List<Course> courseList = courseService.findByInMany(Integer.parseInt(collegeId));

		return courseList;
	}

	/**
	 * ????????????????????????id?????????????????????
	 */
	@ResponseBody
	@RequestMapping("/TeacherAjax")
	public List<Teacher> teacherAjax(String collegeId) {

		List<Teacher> teacherList = teacherService.findList("college_id", Long.parseLong(collegeId));
		SortUtil.sort(teacherList,true,"name");
		return teacherList;
	}

	/**
	 * ????????????????????????
	 */
	@RequestMapping(value = { "/listDialogWorkingTable" }, method = { RequestMethod.GET })
	public String listDialogWorkingTable(String courseId, HttpServletRequest request, Pageable pageable,
			ModelMap model) {
		// ????????????id????????????????????????
		List<WorkingTable> workingTableList = new ArrayList<>();
		if (courseId != null && !courseId.equals("")) {
			List<WorkingTable> temp_workingTableList = workingTableService.findList("courseId", courseId);
			for (int i = 0; i < temp_workingTableList.size(); i++) {
				WorkingTable workingTable = temp_workingTableList.get(i);
				// ?????????????????????????????????????????????
				if (workingTable.getTableCreater().equals(systemUserService.getCurrentUser().getName())) {
					workingTableList.add(workingTable);
				}
				// ???????????????????????????????????????
				else if (workingTable.getTableLevel() == WorktableEnum.levelEnum.schoolOpen.getValue()
						&& workingTable.getCourse().getCollege().getSchool().getId() == systemUserService
								.getCurrentUser().getSchoolId()) {
					workingTableList.add(workingTable);
				}
				// ???????????????????????????????????????
				else if (workingTable.getTableLevel() == WorktableEnum.levelEnum.openAll.getValue()) {
					workingTableList.add(workingTable);
				}
			}

			redisService.save(systemUserService.getCurrentUserName() + "CourseArrange" + "workingTableList",
					JSON.toJSONString(workingTableList));
		}
		model.addAttribute("page", workingTableService.pageMethod(pageable, workingTableList));
		return "/admin/course_arrange/listDialogWorkingTable";
	}

	/**
	 * ???????????????????????????????????????
	 * ????????????id ??????id ??????id????????? 2019.03.27 cjs
	 */
	private void addcAndw(int classId,long courseArrangeId, String workingTableId, String courseId, String teacherId) {

		if (workingTableId.contains(",")) {
			// ???????????????id
			String[] wtIdArray = workingTableId.split(",");
			for (int k = 0; k < wtIdArray.length; k++) {
				CourseArrangeAndWorkingTable cAndw = new CourseArrangeAndWorkingTable();
				cAndw.setCourseArrangeId((int) courseArrangeId);
				cAndw.setClassId(classId);
				System.out.println("?????????"+cAndw.getClassId());
				cAndw.setWorkingTableId(Integer.parseInt(wtIdArray[k]));
				cAndw.setCourseId(Integer.parseInt(courseId));
				cAndw.setTeacherId(Integer.parseInt(teacherId));
				cAndw.setEnabled(1);

				//courseArrangeAndWorkingTableService.save(cAndw);
				courseArrangeAndWorkingTableService.saveOne(cAndw);
			}
		} else if (workingTableId != null && !workingTableId.equals("")) {
			// ???????????????id
			CourseArrangeAndWorkingTable cAndw = new CourseArrangeAndWorkingTable();
			cAndw.setCourseArrangeId((int) courseArrangeId);
			cAndw.setWorkingTableId(Integer.parseInt(workingTableId));
			cAndw.setClassId(classId);
			cAndw.setEnabled(1);
			System.out.println("?????????"+cAndw.getClassId());
			cAndw.setCourseId(Integer.parseInt(courseId));
			cAndw.setTeacherId(Integer.parseInt(teacherId));
			//courseArrangeAndWorkingTableService.save(cAndw);
			courseArrangeAndWorkingTableService.saveOne(cAndw);
		}

	}

	/**
	 * ???????????????????????????????????????
	 */
	public CourseArrange getCourseArrange(String id, String className, String courseName, String teacherName) {

		CourseArrange courseArrange = new CourseArrange();
		if (id != null && !id.equals("")) {
			courseArrange.setId(Long.parseLong(id));
		}
		courseArrange.setClass_id(Integer.parseInt(className));
		courseArrange.setCourse_id(Integer.parseInt(courseName));
		courseArrange.setTeacher_id(Integer.parseInt(teacherName));
		courseArrange.setUpdate_person(systemUserService.getCurrentUser().getName());
		courseArrange.setUpdate_date(new Date());
		return courseArrange;
	}

}
