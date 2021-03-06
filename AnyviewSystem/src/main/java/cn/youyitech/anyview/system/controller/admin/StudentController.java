package cn.youyitech.anyview.system.controller.admin;

import cn.youyitech.anyview.system.dto.StudentAndUser;
import cn.youyitech.anyview.system.dto.user.User;
import cn.youyitech.anyview.system.entity.*;
import cn.youyitech.anyview.system.service.*;
import cn.youyitech.anyview.system.support.AdminEnum;
import cn.youyitech.anyview.system.support.EnumConstants;
import cn.youyitech.anyview.system.support.FileInfo;
import cn.youyitech.anyview.system.support.Message;
import cn.youyitech.anyview.system.utils.IdsUtils;
import cn.youyitech.anyview.system.utils.PoiReadExcel;
import cn.youyitech.anyview.system.utils.SortUtil;
import com.alibaba.fastjson.JSON;
import com.framework.loippi.support.Page;
import com.framework.loippi.support.Pageable;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Controller - StudentController
 * 
 * @author zzq
 * @version 1.0
 */

@Controller("adminStudentController")
@RequestMapping("/admin/student")
public class StudentController extends GenericController {

	@Resource
	private StudentService studentService;

	@Resource
	private SystemUserService systemUserService;

	@Resource
	private TeacherService teacherService;

	@Resource
	private ClassAndStudentService classAndStudentService;
	@Resource
	private ExerciseService exerciseService;

	@Value("${system.init_password}")
	private String init_password;

	@Resource(name = "fileServiceImpl")
	private FileService fileService;

	@Autowired
	private SchoolService schoolService;

	@Autowired
	private CollegeService collegeService;

	@Autowired
	private MajorService majorService;

	@Autowired
	private ClassService classService;

	@Resource
	private RedisService redisService;

	@Autowired
	PoiReadExcel read;

	@Autowired
	private IdsUtils idsUtils;
    private String className;
	private String filter_user_name;
	private String filter_name;
	/**
	 * ?????????????????????????????????
	 */
	@RequestMapping(value = "/check_username", method = RequestMethod.GET)
	public @ResponseBody boolean checkUsername(String studentId, String username, int schoolId) {
		// ???????????????????????????
		if (StringUtils.isEmpty(username)) {
			return false;
		}
		// ????????????????????????????????????????????????????????????????????????????????????????????????
		if (studentId != null && !studentId.equals("")) {
			Student currentStudent = studentService.find("id", studentId);
			String username_string = username;
			if (currentStudent != null) {
				if (currentStudent.getUsername().equals(username_string)) {
					return true;
				}
			}
		}

		// ????????????????????????????????????????????????????????????
		Student student = new Student();
		student.setUsername(username);
		student.setSchoolId(schoolId);
		List<Student> studentList = studentService.findByAttribute(student);
		if (studentList.size() > 0) {
			return false;
		}
		return true;
	}

	/**
	 * ??????
	 */
	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public String add(ModelMap model) {
		// ???????????????????????????????????????????????????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "collegeList");
		redisService.delete(systemUserService.getCurrentUserName() + "majorList");
		redisService.delete(systemUserService.getCurrentUserName() + "classList");

		if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.teacher.getValue()) {
			ClassEntity classSystem = classService.find("id",
					redisService.get(systemUserService.getCurrentUserName() + "ttclassName"));
			model.addAttribute("ttclassSystem", classSystem);
		}

		return "/admin/student/add";
	}

	/**
	 * ????????????
	 */
	@Transactional
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String save(HttpServletRequest request, RedirectAttributes redirectAttributes) throws Exception {

		String schoolId = request.getParameter("schoolId");
		// ?????????
		String username = request.getParameter("number");
		// ??????
		String name = request.getParameter("name");
		String sex = request.getParameter("sex");
		String state = request.getParameter("state");
		String classString = request.getParameter("className");
		String email = request.getParameter("email");

		// ?????????????????????
		User currentUser = systemUserService.getCurrentUser();

		// ??????????????????
		Student student = getStudent("", sex, state, username, name, schoolId, email);
		student.setEnabled(1);
		// ???????????????????????????????????????123456???????????????????????????6???????????????
		if (request.getParameter("email") != null && !request.getParameter("email").equals("")) {
			student.setEmail(email);
		}
		student.setRoleId(EnumConstants.authorityEnum.student.getValue());
		student.setIsLocked(false);
		student.setCreatedDate(new Date());
		student.setCreatedBy(currentUser.getName());
		studentService.saveOne(student);

		Student temp = new Student();
		temp.setRoleId(EnumConstants.authorityEnum.student.getValue());
		temp.setSchoolId(Integer.parseInt(schoolId));
		temp.setUsername(username);
		student = studentService.findByAttribute(temp).get(0);

		// ????????????????????????
		// ??????????????????????????????????????????????????????????????????
		if (classString.contains(",")) {
			String[] classArray = classString.split(",");
			// ?????????????????????????????????????????????
			for (int i = 0; i < classArray.length; i++) {
				ClassAndStudent classAndStudent = new ClassAndStudent();
				classAndStudent.setStudent_id(student.getId());
				classAndStudent.setStudent_class_id(Integer.parseInt(classArray[i]));
				classAndStudent.setEnabled(1);
				classAndStudentService.save(classAndStudent);
			}
		} else {
			ClassAndStudent classAndStudent = new ClassAndStudent();
			classAndStudent.setStudent_id(student.getId());
			classAndStudent.setStudent_class_id(Integer.parseInt(classString));
			classAndStudent.setEnabled(1);
			classAndStudentService.save(classAndStudent);
		}
		if (request.getParameter("email") != null && !request.getParameter("email").equals("")) {
			addFlashMessage(redirectAttributes, Message.success("????????????????????????????????????????????????????????????????????????????????????"));
		} else {
			addFlashMessage(redirectAttributes, SUCCESS_MESSAGE);

		}

		return "redirect:list.jhtml";
	}

	/**
	 * ????????????
	 */
	@RequestMapping(value = "/createTemplate", method = RequestMethod.GET)
	public String createTemplate(ModelMap model, HttpServletResponse response) {

		return "redirect:list.jhtml";
	}

	/**
	 * ????????????
	 */
	@RequestMapping(value = "/importStudent", method = RequestMethod.GET)
	public String importStudent(ModelMap model) {

		return "/admin/student/importStudent";
	}

	/**
	 * ??????????????????
	 */
	@Transactional
	@RequestMapping(value = "/saveStudent", method = RequestMethod.POST)
	public String saveStudent(MultipartFile excelFile, RedirectAttributes redirectAttributes) {
		// ??????????????????????????????
		String url = fileService.uploadLocal(FileInfo.FileType.file, excelFile);
		if (url.contains(".xls") || url.contains(".xlsx")) {
			User currentUser = systemUserService.getCurrentUser();
			// ???????????????????????????????????????
			StudentAndUser studentAndUser = this.read.readStudentExcel(url);
			List<Student> lists_student = studentAndUser.getLists_student();
			List<ClassAndStudent> lists_classAndStudent = studentAndUser.getLists_classAndStudent();
			List<Integer> lists_number = studentAndUser.getLists_number();
			// ?????????????????????
			int space_number = 0;
			if (lists_student != null) {
				for (int i = 0; i < lists_student.size(); i++) {
					// ????????????
					Student student = lists_student.get(i);

					Student temp1 = new Student();
					temp1.setUsername(student.getUsername());
					temp1.setSchoolId(student.getSchoolId());
					Student record = studentService.findByUserNameAndSchoolId(temp1);
					if( record != null){
						record.setEnabled(1);
						record.setRoleId(EnumConstants.authorityEnum.student.getValue());
						record.setIsLocked(false);
						record.setLastUpdatedDate(new Date());
						record.setLastUpdatedBy(currentUser.getName());
						studentService.update(record);
					}else {
						student.setEnabled(1);
						student.setRoleId(EnumConstants.authorityEnum.student.getValue());
						student.setIsLocked(false);
						student.setCreatedDate(new Date());
						student.setCreatedBy(currentUser.getName());
						student.setLastUpdatedDate(new Date());
						student.setLastUpdatedBy(currentUser.getName());
						try {
							studentService.saveOne(student);
						} catch (MessagingException | IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							addFlashMessage(redirectAttributes, Message.error("" + e));
						}

					}

					Student temp = new Student();
					temp.setUsername(student.getUsername());
					temp.setSchoolId(student.getSchoolId());
					student = studentService.findByUserNameAndSchoolId(student);

					// ???????????????????????????????????????????????????????????????
					if (lists_number.size() != 0) {
						int number = lists_number.get(i);
						for (int j = 0; j < number; j++) {
							ClassAndStudent classAndStudent = lists_classAndStudent.get(i + j + space_number);
							classAndStudent.setStudent_id(student.getId());
							classAndStudent.setEnabled(1);
							classAndStudentService.save(classAndStudent);
							if (j == number - 1) {
								space_number = space_number + (number - 1);
							}
						}
					}

				}
			}
			// ?????????????????????????????????????????????????????????redis
			if (lists_student.size() != 0) {
				redisService.save(systemUserService.getCurrentUserName() + "importStudent_number",
						String.valueOf(lists_student.size()));
			} else {
				redisService.save(systemUserService.getCurrentUserName() + "importStudent_number", String.valueOf(-1));
			}
		}
		return "redirect:list.jhtml";
	}

	/**
	 * ????????????
	 */
	@RequiresPermissions("admin:system:student")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String list(HttpServletRequest request, Pageable pageable, ModelMap model) {
		// ??????id
		className = request.getParameter("filter_cName");
		filter_user_name = request.getParameter("filter_user_name");
		filter_name = request.getParameter("filter_name");
		String usernameflag=request.getParameter("usernameflag");
		if (systemUserService.getCurrentUser().getRoleId() == AdminEnum.authorityEnum.teacher.getValue()
				&& className != null) {
			redisService.save(systemUserService.getCurrentUserName() + "ttclassName", className);
		}
		// ????????????????????????????????????redis????????????
		redisService.delete(systemUserService.getCurrentUserName() + "collegeFlag");
		redisService.delete(systemUserService.getCurrentUserName() + "majorFlag");
		redisService.delete(systemUserService.getCurrentUserName() + "classFlag");
		redisService.delete(systemUserService.getCurrentUserName() + "editstudent");

		processQueryConditions(pageable, request);
		Map map = (Map) pageable.getParameter();
		pageable.setParameter(map);
		if (systemUserService.getCurrentUser().getRoleId() == AdminEnum.authorityEnum.teacher.getValue()) {
			List<ClassAndStudent> classAndStudentList = classAndStudentService.findList("student_class_id",
					Integer.valueOf(redisService.get(systemUserService.getCurrentUserName() + "ttclassName")));
			List<Integer> studentIdList = new ArrayList<>();
			for (ClassAndStudent classAndStudent : classAndStudentList) {
				studentIdList.add(classAndStudent.getStudent_id());
			}
			Student student = new Student();
			if (!"".equals(filter_user_name) && filter_user_name != null) {
				student.setUsername(filter_user_name);
			}
			if (!"".equals(filter_name) && filter_name != null) {
				student.setName(filter_name);
			}
			if (studentIdList.size() > 0) {
				student.setStudentIdList(studentIdList);
				List<Student> studentList = studentService.findEntityList(student);
				if (usernameflag!=null){
				if (usernameflag.equals("asc"))SortUtil.sort(studentList,true,"username");
				else  SortUtil.sort(studentList,false,"username");
                    model.addAttribute("usernameflag",usernameflag);
				}

				model.addAttribute("page", studentService.pageMethod(pageable, studentList));
			} else {
				List<Student> studentList = new ArrayList<>();
				model.addAttribute("page", studentList);
			}
		} else {
            Page<Student> page=this.studentService.findByPage(pageable);
            if (usernameflag!=null){
                if (usernameflag.equals("asc"))SortUtil.sort(page.getContent(),true,"username");
                else  SortUtil.sort(page.getContent(),false,"username");
                model.addAttribute("usernameflag",usernameflag);
            }
			model.addAttribute("page", page);
		}
		model.addAttribute("params", map);
		// ??????????????????????????????????????????????????????????????????????????????
		if (redisService.get(systemUserService.getCurrentUserName() + "importStudent_number") != null) {
			model.addAttribute("number",
					redisService.get(systemUserService.getCurrentUserName() + "importStudent_number"));
			redisService.delete(systemUserService.getCurrentUserName() + "importStudent_number");
		}
		return "/admin/student/list";
	}

	/**
	 * ????????????
	 */
	@RequestMapping(value = { "/delete" }, method = { RequestMethod.POST })
	public @ResponseBody Message delete(HttpServletRequest request, Long[] ids) {

		List<String> recordStudentId = new ArrayList<>();
		for (long id : ids) {
			recordStudentId.add(String.valueOf(id));
		}
		if (recordStudentId.size() != 0) {
			try {
				for (int i=0;i<recordStudentId.size();i++){
					Long temp=Long.parseLong(recordStudentId.get(i));
					List<Exercise> exercises=exerciseService.findbySid(temp);
					for(int j=0;j<exercises.size();j++){
						exerciseService.delete(exercises.get(j).getEId());
					}
				}
				studentService.deleteOne(recordStudentId);
			} catch (Exception e) {
				System.out.println("??????"+e.getMessage());
				return ERROR_MESSAGE;
			}
			return SUCCESS_MESSAGE;
		} else {
			return ERROR_MESSAGE;
		}

	}

	/**
	 * ?????????????????????
	 */
	@RequestMapping(value = "/initialize", method = RequestMethod.GET)
	public String initialize(Long[] ids, RedirectAttributes redirectAttributes) {

		if (ids != null) {
			for (long id : ids) {
				Student student = studentService.find(id);
				try {
					studentService.updateInitPass(student);
				} catch (MessagingException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					addFlashMessage(redirectAttributes, Message.error("" + e));
				}
			}
			addFlashMessage(redirectAttributes, Message.success("????????????????????????????????????123456???????????????????????????????????????????????????"));
		} else {
			addFlashMessage(redirectAttributes, ERROR_MESSAGE);
		}

		return "redirect:list.jhtml";
	}

	/**
	 * ????????????
	 */
	@Transactional
	@RequestMapping(value = "/editSave", method = RequestMethod.POST)
	public String editSave(HttpServletRequest request, RedirectAttributes redirectAttributes) {

		String id = request.getParameter("id");
		String schoolId = request.getParameter("schoolId");
		// ?????????
		String username = request.getParameter("number");
		// ??????
		String name = request.getParameter("name");
		String sex = request.getParameter("sex");
		String state = request.getParameter("state");
		// ????????????
		String classString = request.getParameter("className");
		String email = request.getParameter("email");
		// ??????????????????
		Student student = getStudent(id, sex, state, username, name, schoolId, email);
		studentService.update(student);

		// ????????????????????????
		if (!classString.equals("") && classString != null && !classString.equals("className")) {
			if (classString.contains(",")) {
				String[] classArray = classString.split(",");
				// ???????????????????????????????????????
				List<ClassAndStudent> classAndStudentList = classAndStudentService.findList("student_id",
						Long.parseLong(request.getParameter("id")));
				// ??????????????????????????????
				for (int i = 0; i < classAndStudentList.size(); i++) {
					ClassAndStudent classAndStudent = classAndStudentList.get(i);
					classAndStudent.setStudent_id(Integer.parseInt(id));
					classAndStudent.setStudent_class_id(Integer.parseInt(classArray[i]));
					classAndStudentService.update(classAndStudent);
				}
				// ??????????????????????????????????????????????????????????????????????????????
				if (classArray.length > classAndStudentList.size()) {
					for (int i = classAndStudentList.size(); i < classArray.length; i++) {
						ClassAndStudent classAndstudent = new ClassAndStudent();
						classAndstudent.setStudent_id(Integer.parseInt(request.getParameter("id")));
						classAndstudent.setStudent_class_id(Integer.parseInt(classArray[i]));
						classAndstudent.setEnabled(1);
						classAndStudentService.save(classAndstudent);
					}
				}
			} else {
				// ????????????????????????????????????????????????????????????????????????????????????????????????????????????
				List<ClassAndStudent> casList = classAndStudentService.findList("student_id", Long.parseLong(id));
				for (int i = 0; i < casList.size(); i++) {
					if (i == 0) {
						casList.get(i).setStudent_id(Integer.parseInt(id));
						casList.get(i).setStudent_class_id(Integer.parseInt(classString));
						classAndStudentService.update(casList.get(i));
					} else {
						classAndStudentService.delete(casList.get(i).getId());
					}
				}

			}
		}

		addFlashMessage(redirectAttributes, SUCCESS_MESSAGE);

		return "redirect:list.jhtml";
	}

	/**
	 * ??????
	 */
	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public String edit(Long id, ModelMap model) {
		// ???redis??????????????????????????????????????????????????????????????????????????????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "collegeList");
		redisService.delete(systemUserService.getCurrentUserName() + "majorList");
		redisService.delete(systemUserService.getCurrentUserName() + "classList");

		redisService.save(systemUserService.getCurrentUserName() + "collegeFlag", "0");
		redisService.save(systemUserService.getCurrentUserName() + "majorFlag", "0");
		redisService.save(systemUserService.getCurrentUserName() + "classFlag", "0");
		// ???????????????????????????redis
		redisService.save(systemUserService.getCurrentUserName() + "editstudent",
				JSON.toJSONString(studentService.find(id)));
		Student temp_student = JSON
				.parseObject(redisService.get(systemUserService.getCurrentUserName() + "editstudent"), Student.class);
		model.addAttribute("student", temp_student);
		if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.teacher.getValue()) {
			ClassEntity classSystem = classService.find("id",
					redisService.get(systemUserService.getCurrentUserName() + "ttclassName"));
			model.addAttribute("ttclassSystem", classSystem);
		}

		return "/admin/student/edit";
	}

	/**
	 * ???????????????
	 */
	@ModelAttribute
	public void init(Model model) {
		// ?????????????????????????????????
		List<School> schoolList = schoolService.findAll();
		Teacher teacher = teacherService.find("username", systemUserService.getCurrentUserName());
		model.addAttribute("teacherInit", teacher);
		model.addAttribute("schoolList", schoolList);
		model.addAttribute("systemUser", systemUserService.getCurrentUser());

	}

	/**
	 * ????????????id???????????????????????????
	 */
	@ResponseBody
	@RequestMapping("/CollegeAjax")
	public List<College> collegeAjax(String schoolId) {

		// ???redis??????????????????????????????????????????????????????????????????????????????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "collegeList");
		redisService.delete(systemUserService.getCurrentUserName() + "majorList");
		redisService.delete(systemUserService.getCurrentUserName() + "classList");
		List<College> collegeList = schoolService.find(Long.parseLong(schoolId)).getSchoolCollegeList();
		SortUtil.sort(collegeList,true,"collegeName");
		// ??????????????????????????????redis
		redisService.save(systemUserService.getCurrentUserName() + "collegeList", JSON.toJSONString(collegeList));
		if (collegeList.size() == 0) {
			redisService.save(systemUserService.getCurrentUserName() + "collegeFlag", "1");
		}

		return collegeList;
	}

	/**
	 * ?????????????????????
	 */
	@RequestMapping(value = { "/listDialogCollege" }, method = { RequestMethod.GET })
	public String listDialogCollege(HttpServletRequest request, Pageable pageable, ModelMap model) {
		// ???redis????????????????????????
		List<College> collegeList = null;
		if (redisService.get(systemUserService.getCurrentUserName() + "collegeList") != null) {
			collegeList = idsUtils.collegejsonToList(
					redisService.get(systemUserService.getCurrentUserName() + "collegeList"), College.class);
		} else {
			collegeList = new ArrayList<>();
		}
		// ?????????????????????????????????
		if (redisService.get(systemUserService.getCurrentUserName() + "editstudent") != null && collegeList.size() == 0
				&& redisService.get((systemUserService.getCurrentUserName() + "collegeFlag")).equals("0")) {
			Student temp_student = JSON.parseObject(
					redisService.get(systemUserService.getCurrentUserName() + "editstudent"), Student.class);
			long schoolId = temp_student.getSchoolId();
			// ?????????????????????????????????id???????????????????????????
			collegeList = schoolService.find(schoolId).getSchoolCollegeList();
			SortUtil.sort(collegeList,true,"collegeName");
			redisService.save(systemUserService.getCurrentUserName() + "collegeList", JSON.toJSONString(collegeList));
			model.addAttribute("page",
					collegeService.pageMethod(pageable, schoolService.find(schoolId).getSchoolCollegeList()));

		} else if (redisService.get(systemUserService.getCurrentUserName() + "editstudent") != null
				&& collegeList.size() == 0
				&& redisService.get((systemUserService.getCurrentUserName() + "collegeFlag")).equals("1")) {
			List<College> blank_college = new ArrayList<>();
			model.addAttribute("page", collegeService.pageMethod(pageable, blank_college));
		} else {
			// ????????????????????????????????????????????????????????????????????????????????????
			if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.manager.getValue()) {
				collegeList = schoolService.find((long) systemUserService.getCurrentUser().getSchoolId())
						.getSchoolCollegeList();
				SortUtil.sort(collegeList,true,"collegeName");
				redisService.save(systemUserService.getCurrentUserName() + "collegeList",
						JSON.toJSONString(collegeList));
			}
			model.addAttribute("page", collegeService.pageMethod(pageable, collegeList));
		}

		return "/admin/student/listDialogCollege";
	}

	/**
	 * ????????????id???????????????????????????
	 */
	@ResponseBody
	@RequestMapping("/MajorAjax")
	public List<Major> majorAjax(String selectIds) {

		// ???redis?????????????????????????????????????????????????????????????????????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "majorList");
		redisService.delete(systemUserService.getCurrentUserName() + "classList");

		College college = null;
		if (selectIds.contains(",")) {
			// ?????????????????????
			List<Major> majorList = new ArrayList<>();
			String[] ids = selectIds.split(",");
			for (int i = 0; i < ids.length; i++) {
				college = collegeService.find(Long.parseLong(ids[i]));
				List<Major> list = college.getCollegeMajorList();
				SortUtil.sort(list,true,"majorName");
				majorList.addAll(list);
			}
			redisService.save(systemUserService.getCurrentUserName() + "majorList", JSON.toJSONString(majorList));
		} else {
			// ??????????????????
			college = collegeService.find(Long.parseLong(selectIds));
			redisService.save(systemUserService.getCurrentUserName() + "majorList",
					JSON.toJSONString(college.getCollegeMajorList()));
		}
		if (idsUtils
				.majorjsonToList(redisService.get(systemUserService.getCurrentUserName() + "majorList"), Major.class)
				.size() == 0) {
			redisService.save(systemUserService.getCurrentUserName() + "majorFlag", "1");
		}

		return idsUtils.majorjsonToList(redisService.get(systemUserService.getCurrentUserName() + "majorList"),
				Major.class);
	}

	/**
	 * ?????????????????????
	 */
	@RequestMapping(value = { "/listDialogMajor" }, method = { RequestMethod.GET })
	public String listDialogMajor(HttpServletRequest request, Pageable pageable, ModelMap model) {
		// ???redis????????????????????????
		List<Major> majorList = null;
		if (redisService.get(systemUserService.getCurrentUserName() + "majorList") != null) {
			majorList = idsUtils.majorjsonToList(redisService.get(systemUserService.getCurrentUserName() + "majorList"),
					Major.class);
			SortUtil.sort(majorList,true,"majorName");
		} else {
			majorList = new ArrayList<>();
		}
		// ?????????????????????????????????
		if (redisService.get(systemUserService.getCurrentUserName() + "editstudent") != null && majorList.size() == 0
				&& redisService.get(systemUserService.getCurrentUserName() + "majorFlag").equals("0")) {
			List<Major> newmajorList = new ArrayList<>();
			Student temp_student = JSON.parseObject(
					redisService.get(systemUserService.getCurrentUserName() + "editstudent"), Student.class);
			List<ClassAndStudent> classAndStudentList = temp_student.getClassList();
			for (int i = 0; i < classAndStudentList.size(); i++) {
				ClassAndStudent classAndStudent = classAndStudentList.get(i);
				// ??????????????????????????????????????????????????????????????????????????????????????????
				long collegeId = classAndStudent.getClassSystem().getMajor().getCollege().getId();
				newmajorList.addAll(collegeService.find(collegeId).getCollegeMajorList());
			}
			majorList = newmajorList;
			if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.teacher.getValue()) {
				Teacher teacher = teacherService.find("username", systemUserService.getCurrentUserName());
				newmajorList = collegeService.find((long) teacher.getCollege_id()).getCollegeMajorList();
			}
			redisService.save(systemUserService.getCurrentUserName() + "majorList", JSON.toJSONString(newmajorList));
			// ???????????????major
			if (newmajorList.size() > 1) {
				List<Major> majorListNew = new ArrayList<>();
				for (Major major : newmajorList) {
					if (!majorListNew.contains(major)) {
						majorListNew.add(major);
					}
				}
				newmajorList = majorListNew;
			}

			model.addAttribute("page", majorService.pageMethod(pageable, newmajorList));
		} else if (redisService.get(systemUserService.getCurrentUserName() + "editstudent") != null
				&& majorList.size() == 0
				&& redisService.get(systemUserService.getCurrentUserName() + "majorFlag").equals("1")) {
			List<Major> blank_major = new ArrayList<>();
			model.addAttribute("page", majorService.pageMethod(pageable, blank_major));
		} else {
			// ??????????????????????????????????????????????????????????????????????????????????????????
			if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.teacher.getValue()) {
				Teacher teacher = teacherService.find("username", systemUserService.getCurrentUserName());
				majorList = collegeService.find((long) teacher.getCollege_id()).getCollegeMajorList();
			}
			redisService.save(systemUserService.getCurrentUserName() + "majorList", JSON.toJSONString(majorList));
			// ???????????????major
			if (majorList.size() > 1) {
				List<Major> majorListNew = new ArrayList<>();
				for (Major major : majorList) {
					if (!majorListNew.contains(major)) {
						majorListNew.add(major);
					}
				}
				majorList = majorListNew;
			}
			model.addAttribute("page", majorService.pageMethod(pageable, majorList));
		}

		return "/admin/student/listDialogMajor";
	}

	/**
	 * ????????????id???????????????????????????
	 */
	@ResponseBody
	@RequestMapping("/ClassAjax")
	public List<ClassEntity> classAjax(String selectIds) {
		// ???redis????????????????????????????????????????????????????????????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "classList");

		if (selectIds.contains(",")) {
			// ?????????????????????
			List<ClassEntity> classList = new ArrayList<>();
			String[] ids = selectIds.split(",");
			for (int i = 0; i < ids.length; i++) {
				List<ClassEntity> tempList = classService.findList("major_id", Long.parseLong(ids[i]));
				for (int j = 0; j < tempList.size(); j++) {
					if (tempList.get(j).getEnabled() == 1) {
						classList.add(tempList.get(j));
					}
				}
			}
			redisService.save(systemUserService.getCurrentUserName() + "classList", JSON.toJSONString(classList));
		} else {
			// ??????????????????
			List<ClassEntity> classList = new ArrayList<>();
			List<ClassEntity> tempList = classService.findList("major_id", selectIds);
			for (int i = 0; i < tempList.size(); i++) {
				if (tempList.get(i).getEnabled() == 1) {
					classList.add(tempList.get(i));
				}
			}
			redisService.save(systemUserService.getCurrentUserName() + "classList", JSON.toJSONString(classList));
		}

		if (idsUtils.classjsonToList(redisService.get(systemUserService.getCurrentUserName() + "classList"),
				ClassEntity.class).size() == 0) {
			redisService.save(systemUserService.getCurrentUserName() + "classFlag", "1");
		}

		return idsUtils.classjsonToList(redisService.get(systemUserService.getCurrentUserName() + "classList"),
				ClassEntity.class);
	}

	/**
	 * ?????????????????????
	 */
	@RequestMapping(value = { "/listDialogClass" }, method = { RequestMethod.GET })
	public String listDialogClass(HttpServletRequest request, Pageable pageable, ModelMap model) {
		// ???redis???????????????id?????????????????????
		List<ClassEntity> classList = null;
		if (redisService.get(systemUserService.getCurrentUserName() + "classList") != null) {
			classList = idsUtils.classjsonToList(redisService.get(systemUserService.getCurrentUserName() + "classList"),
					ClassEntity.class);
			SortUtil.sort(classList,true,"className");
		} else {
			classList = new ArrayList<>();
		}
		// ???????????????????????????
		String className = request.getParameter("className");
		// ??????????????????????????????
		if (redisService.get(systemUserService.getCurrentUserName() + "editstudent") != null && classList.size() == 0
				&& (className == null || className.equals(""))
				&& redisService.get(systemUserService.getCurrentUserName() + "classFlag").equals("0")) {
			List<ClassEntity> classLists = new ArrayList<>();
			Student temp_student = JSON.parseObject(
					redisService.get(systemUserService.getCurrentUserName() + "editstudent"), Student.class);
			List<ClassAndStudent> classAndStudentList = temp_student.getClassList();

			for (int i = 0; i < classAndStudentList.size(); i++) {
				ClassAndStudent classAndStudent = classAndStudentList.get(i);
				// ??????????????????????????????????????????????????????????????????????????????????????????
				long majorId = classAndStudent.getClassSystem().getMajor().getId();
				List<ClassEntity> temp = classService.findList("major_id", majorId);
				for (int j = 0; j < temp.size(); j++) {
					if (temp.get(j).getEnabled() == 1) {
						classLists.add(temp.get(j));
					}
				}
			}

			classList = classLists;
			redisService.save(systemUserService.getCurrentUserName() + "classList", JSON.toJSONString(classLists));
			// ???????????????class
			if (classLists.size() > 1) {
				List<ClassEntity> classListNew = new ArrayList<>();
				for (ClassEntity classEntity : classLists) {
					if (!classListNew.contains(classEntity)) {
						classListNew.add(classEntity);
					}
				}
				classLists = classListNew;
			}
			model.addAttribute("page", classService.pageMethod(pageable, classLists));

		} else if (redisService.get(systemUserService.getCurrentUserName() + "editstudent") != null
				&& classList.size() == 0 && (className == null || className.equals(""))
				&& redisService.get(systemUserService.getCurrentUserName() + "classFlag").equals("1")) {

			List<ClassEntity> blank_class = new ArrayList<>();
			model.addAttribute("page", classService.pageMethod(pageable, blank_class));

		} else if (className == null || className.equals("")) {
			redisService.save(systemUserService.getCurrentUserName() + "classList", JSON.toJSONString(classList));
			// ???????????????class
			if (classList.size() > 1) {
				List<ClassEntity> classListNew = new ArrayList<>();
				for (ClassEntity classEntity : classList) {
					if (!classListNew.contains(classEntity)) {
						classListNew.add(classEntity);
					}
				}
				classList = classListNew;
			}
			model.addAttribute("page", classService.pageMethod(pageable, classList));

		}
		// ?????????????????????????????????????????????
		if (redisService.get(systemUserService.getCurrentUserName() + "editstudent") != null && classList.size() == 0
				&& className != null) {
			List<ClassEntity> classLists = new ArrayList<>();
			Student temp_student = JSON.parseObject(
					redisService.get(systemUserService.getCurrentUserName() + "editstudent"), Student.class);
			List<ClassAndStudent> classAndStudentList = temp_student.getClassList();

			for (int i = 0; i < classAndStudentList.size(); i++) {
				ClassAndStudent classAndStudent = classAndStudentList.get(i);
				// ??????????????????????????????????????????????????????????????????????????????????????????
				long majorId = classAndStudent.getClassSystem().getMajor().getId();
				List<ClassEntity> temp = classService.findList("major_id", majorId);
				for (int j = 0; j < temp.size(); j++) {
					if (temp.get(j).getEnabled() == 1 && temp.get(j).getClassName().contains(className)) {
						classLists.add(temp.get(j));
					}
				}
			}

			classList = classLists;
			// ???????????????class
			if (classList.size() > 1) {
				List<ClassEntity> classListNew = new ArrayList<>();
				for (ClassEntity classEntity : classList) {
					if (!classListNew.contains(classEntity)) {
						classListNew.add(classEntity);
					}
				}
				classList = classListNew;
			}
			model.addAttribute("page", classService.pageMethod(pageable, classList));

		} else if (className != null) {
			List<ClassEntity> temp_classLists = new ArrayList<>();
			for (int i = 0; i < classList.size(); i++) {
				if (classList.get(i).getClassName().contains(className)) {
					temp_classLists.add(classList.get(i));
				}
			}
			model.addAttribute("page", classService.pageMethod(pageable, temp_classLists));
		}

		return "/admin/student/listDialogClass";
	}

	/**
	 * ???????????????????????????
	 */
	public Student getStudent(String id, String sex, String state, String username, String name, String schoolId,
			String email) {

		Student student = new Student();
		if (id != null && !id.equals("")) {
			student.setId(Integer.parseInt(id));
		}
		student.setSex(sex);
		student.setState(state);
		student.setUsername(username);
		student.setName(name);
		student.setSchoolId(Integer.parseInt(schoolId));
		student.setEmail(email);
		student.setLastUpdatedDate(new Date());
		student.setLastUpdatedBy(systemUserService.getCurrentUser().getName());

		return student;

	}

}
