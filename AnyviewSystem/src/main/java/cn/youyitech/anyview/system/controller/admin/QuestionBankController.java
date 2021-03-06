package cn.youyitech.anyview.system.controller.admin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import cn.youyitech.anyview.system.utils.*;
import com.github.abel533.echarts.code.Sort;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
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

import cn.youyitech.anyview.system.dto.ImportQuestionContent;
import cn.youyitech.anyview.system.dto.QuestionTemp;
import cn.youyitech.anyview.system.dto.user.User;
import cn.youyitech.anyview.system.entity.College;
import cn.youyitech.anyview.system.entity.Course;
import cn.youyitech.anyview.system.entity.Question;
import cn.youyitech.anyview.system.entity.QuestionBank;
import cn.youyitech.anyview.system.entity.QuestionContent;
import cn.youyitech.anyview.system.entity.QuestionHeaderFile;
import cn.youyitech.anyview.system.entity.School;
import cn.youyitech.anyview.system.entity.Teacher;
import cn.youyitech.anyview.system.service.CollegeService;
import cn.youyitech.anyview.system.service.CourseService;
import cn.youyitech.anyview.system.service.FileService;
import cn.youyitech.anyview.system.service.QuestionBankService;
import cn.youyitech.anyview.system.service.QuestionService;
import cn.youyitech.anyview.system.service.RedisService;
import cn.youyitech.anyview.system.service.SchoolService;
import cn.youyitech.anyview.system.service.SystemUserService;
import cn.youyitech.anyview.system.service.TeacherService;
import cn.youyitech.anyview.system.support.AdminEnum;
import cn.youyitech.anyview.system.support.EnumConstants;
import cn.youyitech.anyview.system.support.FileInfo;
import cn.youyitech.anyview.system.support.Message;
import cn.youyitech.anyview.system.support.Setting;

import com.alibaba.fastjson.JSON;
import com.framework.loippi.support.Pageable;

/**
 * Controller - ????????????
 * 
 * @author zzq
 * @version 1.0
 */

@Controller("adminQuestionBankController")
@RequestMapping("/admin/question_bank")
public class QuestionBankController extends GenericController {

	@Resource
	private QuestionBankService questionBankService;

	@Resource
	private QuestionService questionService;

	@Resource
	private SystemUserService systemUserService;

	@Resource
	private SchoolService schoolService;

	@Resource
	private CollegeService collegeService;

	@Resource
	private CourseService courseService;

	@Resource(name = "fileServiceImpl")
	private FileService fileService;

	@Resource
	private RedisService redisService;

	@Autowired
	private IdsUtils idsUtils;

	@Resource
	TeacherService teacherService;

	@Autowired
	ZipFileRead zipFileRead;

	@Autowired
	ZipFileWrite zipFileWrite;

	@Autowired
	FileUtils fileUtils;

	@Resource
	private PullParserXmlUtils parserUtils;

	@RequestMapping(value = "/checkBank", method = RequestMethod.GET)
	public @ResponseBody boolean checkBank(String id, String bankName, String courseName) {
		// ?????????????????????id????????????
		if (StringUtils.isEmpty(bankName) || StringUtils.isEmpty(courseName)) {
			return false;
		}
		// ???????????????????????????????????????????????????????????????????????????
		QuestionBank questionBank = null;
		if (id != null && !id.equals("")) {
			questionBank = questionBankService.find(Long.parseLong(id));
		}
		if (questionBank != null) {
			if (questionBank.getQuestion_bank().equals(bankName) && questionBank.getCourse_name().equals(courseName)) {
				return true;
			}
		}
		// ????????????????????????????????????????????????
		QuestionBank temp = new QuestionBank();
		temp.setQuestion_bank(bankName);
		temp.setCourse_name(courseName);
		List<QuestionBank> questionBankList = questionBankService.findByAttribute(temp);
		if (questionBankList.size() > 0) {
			return false;
		}

		return true;

	}

	/**
	 * ??????
	 */
	@RequestMapping(value = "/add", method = RequestMethod.GET)
	public String add(ModelMap model) {

		initCourse(model);

		return "/admin/question_bank/add";
	}

	/**
	 * ????????????
	 */
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	public String save(QuestionBank questionBank, RedirectAttributes redirectAttributes) {
		// ????????????????????????
		User user = systemUserService.getCurrentUser();
		questionBank.setEnabled(1);
		questionBank.setCreated_person(user.getName());
		questionBank.setCreated_date(new Date());
		questionBank.setUpdate_person(user.getName());
		questionBank.setUpdate_date(new Date());
		// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
		if ((systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.manager.getValue()
				|| systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.teacher.getValue())
				&& questionBank.getPublic_level() == EnumConstants.publiclevelEnum.localSchoolOpen.getValue()) {
			questionBank.setSpecific_school(String.valueOf(systemUserService.getCurrentUser().getSchoolId()));
		}
		questionBankService.save(questionBank);
		addFlashMessage(redirectAttributes, SUCCESS_MESSAGE);
		return "redirect:list.jhtml";
	}

	/**
	 * ????????????
	 */
	@RequiresPermissions("admin:system:questionbank")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public String list(HttpServletRequest request, Pageable pageable, ModelMap model) {
		redisService.delete(systemUserService.getCurrentUserName() + "recordSchoolId");
		redisService.delete(systemUserService.getCurrentUserName() + "recordSchoolName");

		processQueryConditions(pageable, request);
		Map map = (Map) pageable.getParameter();
		pageable.setParameter(map);
		initList(model, pageable);
		model.addAttribute("params", map);
		// ??????????????????????????????????????????????????????
		if (redisService.get(systemUserService.getCurrentUserName() + "importBank_number") != null) {
			model.addAttribute("number",
					redisService.get(systemUserService.getCurrentUserName() + "importBank_number"));
			redisService.delete(systemUserService.getCurrentUserName() + "importBank_number");
		}
		return "/admin/question_bank/list";
	}

	/**
	 * ????????????
	 */
	@RequestMapping(value = { "/delete" }, method = { RequestMethod.POST })
	public @ResponseBody Message delete(Long[] ids) {
		List<String> recordQuestionBank = new ArrayList<>();
		for (long id : ids) {
			recordQuestionBank.add(String.valueOf(id));
		}
		if (recordQuestionBank.size() > 0) {
			try {
				questionBankService.deleteOne(recordQuestionBank);
			} catch (Exception e) {
				return Message.error(e.getMessage());
			}
			return SUCCESS_MESSAGE;
		} else {
			return ERROR_MESSAGE;
		}

	}

	/**
	 * ??????
	 */
	@RequestMapping(value = "/edit", method = RequestMethod.GET)
	public String edit(Long id, ModelMap model) {
		// ??????redis????????????????????????????????????
		redisService.delete(systemUserService.getCurrentUserName() + "recordSchoolId");
		redisService.delete(systemUserService.getCurrentUserName() + "recordSchoolName");
		// ??????id?????????????????????
		QuestionBank questionbank = questionBankService.find(id);
		model.addAttribute("questionbank", questionbank);

		// ??????????????????????????????id???????????????????????????
		if (questionbank.getSpecific_school() != null && !questionbank.getSpecific_school().equals("")) {
			List<String> schoolNameList = new ArrayList<>();
			String specificschoolId = questionbank.getSpecific_school();
			if (specificschoolId.contains(",")) {
				String[] specificschoolIdArray = specificschoolId.split(",");
				for (int i = 0; i < specificschoolIdArray.length; i++) {
					schoolNameList.add(schoolService.find(Long.parseLong(specificschoolIdArray[i])).getSchoolName());
				}
			} else {
				schoolNameList.add(schoolService.find(Long.parseLong(specificschoolId)).getSchoolName());
			}
			model.addAttribute("schoolNameList", schoolNameList);
		}

		initCourse(model);

		return "/admin/question_bank/edit";
	}

	/**
	 * ????????????
	 */
	@RequestMapping(value = "/editSave", method = RequestMethod.POST)
	public String editSave(QuestionBank questionBank, RedirectAttributes redirectAttributes) {
		// ??????????????????????????????
		User user = systemUserService.getCurrentUser();
		// ????????????????????????????????????????????????????????????????????????
		if (questionBank.getSpecific_school() == null) {
			questionBank.setSpecific_school("");
		}
		// ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
		if ((systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.manager.getValue()
				|| systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.teacher.getValue())
				&& questionBank.getPublic_level() == EnumConstants.publiclevelEnum.localSchoolOpen.getValue()) {
			questionBank.setSpecific_school(String.valueOf(systemUserService.getCurrentUser().getSchoolId()));
		}
		questionBank.setUpdate_person(user.getUsername());
		questionBank.setUpdate_date(new Date());
		questionBankService.update(questionBank);

		addFlashMessage(redirectAttributes, SUCCESS_MESSAGE);

		return "redirect:list.jhtml";
	}

	/**
	 * ?????????????????????
	 */
	@RequestMapping(value = { "/listDialogSchool" }, method = { RequestMethod.GET })
	public String listDialogSchool(HttpServletRequest request, Pageable pageable, ModelMap model) {
		// ??????????????????????????????????????????redis???????????????????????????
		model.addAttribute("page", schoolService.findByPage(pageable));
		model.addAttribute("recordSchoolId",
				idsUtils.stringjsonToList(systemUserService.getCurrentUserName() + "recordSchoolId"));
		model.addAttribute("recordSchoolName",
				idsUtils.stringjsonToList(systemUserService.getCurrentUserName() + "recordSchoolName"));

		return "/admin/question_bank/listDialogSchool";
	}

	/**
	 * ?????????????????????(0:?????????????????????1????????????????????????2??????????????????3???????????????)
	 */
	@ResponseBody
	@RequestMapping(value = { "/recordSchool" }, method = { RequestMethod.POST })
	public List<String> recordSchool(String schoolID, String flag) throws IOException {
		// ??????redis??????????????????????????????
		List<String> recordSchoolId = null;
		List<String> recordSchoolName = null;
		if (redisService.get(systemUserService.getCurrentUserName() + "recordSchoolId") != null) {
			recordSchoolId = idsUtils.stringjsonToList(systemUserService.getCurrentUserName() + "recordSchoolId");
		} else {
			recordSchoolId = new ArrayList<>();
		}
		if (redisService.get(systemUserService.getCurrentUserName() + "recordSchoolName") != null) {
			recordSchoolName = idsUtils.stringjsonToList(systemUserService.getCurrentUserName() + "recordSchoolName");
		} else {
			recordSchoolName = new ArrayList<>();
		}

		if (flag.equals("1")) {
			String schoolName = schoolService.find(Long.parseLong(schoolID)).getSchoolName();
			recordSchoolName.add(schoolName);
			recordSchoolId.add(schoolID);
		} else if (flag.equals("0")) {
			String schoolName = schoolService.find(Long.parseLong(schoolID)).getSchoolName();
			recordSchoolName.remove(schoolName);
			recordSchoolId.remove(schoolID);
		} else if (flag.equals("2")) {
			recordSchoolId.clear();
			recordSchoolName.clear();
			List<School> schoolList = schoolService.findAll();
			for (int i = 0; i < schoolList.size(); i++) {
				recordSchoolId.add(String.valueOf(schoolList.get(i).getId()));
				recordSchoolName.add(schoolList.get(i).getSchoolName());
			}
		} else if (flag.equals("3")) {
			recordSchoolId.clear();
			recordSchoolName.clear();
		}
		// ???redis?????????????????????????????????
		redisService.save(systemUserService.getCurrentUserName() + "recordSchoolId", JSON.toJSONString(recordSchoolId));
		redisService.save(systemUserService.getCurrentUserName() + "recordSchoolName",
				JSON.toJSONString(recordSchoolName));
		return recordSchoolId;
	}

	/**
	 * ????????????
	 */
	@RequestMapping(value = "/importQuestionBank", method = RequestMethod.GET)
	public String importQuestionBank(ModelMap model) {

		return "/admin/question_bank/importQuestionBank";
	}

	/**
	 * ????????????????????????
	 */
	@Transactional
	@RequestMapping(value = "/saveimportQuestionBank", method = RequestMethod.POST)
	public String saveimportQuestionBank(MultipartFile qbfile, ModelMap model, RedirectAttributes redirectAttributes) {
		// ????????????????????????
		String url = fileService.uploadLocal(FileInfo.FileType.file, qbfile);
		ImportQuestionContent importqc = null;
		if (url.contains(".zip")) {
			importqc = zipFileRead.readQuestionBankZip(url);
		} else if (url.contains(".rar")) {
			importqc = zipFileRead.readQuestionBankRar(url);
		}
		if (importqc != null) {

			User user = systemUserService.getCurrentUser();
			// ??????????????????
			List<String> chapterNameList = importqc.getChapterNameList();
			// ???????????????????????????????????????
			List<Integer> questionNumberList = importqc.getQuestionNumberList();
			// ??????????????????
			List<String> questionNameList = importqc.getQuestionNameList();
			// ??????????????????
			List<QuestionContent> questionContentList = importqc.getQuestionContentList();
			// ???????????????????????????
			List<QuestionHeaderFile> headerFileList = importqc.getHeaderFileList();
			//????????????????????????????????????
            List<Integer> headerNameList = importqc.getHeaderNameList();
          //?????????????????????
            int allHeaderCount = 0;
			// ???????????????????????????
			int number = 0;
			String fileName = qbfile.getOriginalFilename();
			String courseName = fileName.substring(0, fileName.indexOf("_"));
			String questionBankName = fileName.substring(fileName.indexOf("_") + 1, fileName.indexOf("."));
			List<Course> courseList = courseService.findList("courseName", courseName);

			if (courseList.size() > 0) {
				if (checkBank("", questionBankName, courseName)) {
					// ????????????
					number++;
					QuestionBank qb = new QuestionBank();
					// ???????????????????????????????????????
					qb.setQuestion_bank(questionBankName);
					qb.setCourse_name(courseName);
					qb.setPublic_level(EnumConstants.publiclevelEnum.secrecy.getValue());
					qb.setCreated_person(user.getName());
					qb.setCreated_date(new Date());
					qb.setUpdate_person(user.getName());
					qb.setUpdate_date(new Date());
					qb.setEnabled(1);
					questionBankService.save(qb);

					// ????????????????????????
					int allnumber = 0;
					// ???????????????list?????????????????????1??????
					int flag = 0;
					// ?????????????????????????????????
					int questionNumber = 0;
					// ???????????????id
					long questionbank_id = questionBankService.findTotal()
							.get((questionBankService.findTotal().size() - 1)).getId();
					// ????????????
					String chapterName = "";

					List<QuestionHeaderFile> tempList = new ArrayList<>();
					if (questionNameList != null) {
						for (int i = 0; i < questionNameList.size(); i++) {
							// ????????????
							if (questionNumberList.size() > flag) {
								questionNumber = questionNumberList.get(flag);
							}
							// ???????????????????????????
							if ((questionNumber + allnumber) == i) {
								allnumber = allnumber + questionNumber;
								chapterName = chapterNameList.get(flag);
								flag++;
							}

							if (!chapterName.equals("") && chapterName != null) {
								Question question = new Question();
								question.setQuestion_name(questionNameList.get(i));
								question.setChapter(getChapterName(chapterName));
								question.setPublic_level(EnumConstants.publiclevelEnum.secrecy.getValue());
								question.setDifficulty(EnumConstants.difficultyEnum.one.getValue());
								question.setState(EnumConstants.statusEnum.start.getText());

								QuestionContent qc = questionContentList.get(i);
								int headerCount = headerNameList.get(i);
		                        for(int j = allHeaderCount; j<allHeaderCount + headerCount; j++){
		                              QuestionHeaderFile qhf = headerFileList.get(j);
		                              
		                              tempList.add(qhf);
		                        }
								// ????????????????????????????????????????????????xml?????????????????????
								question.setContent(parserUtils.createQuestionXml(qc, tempList));
								tempList.clear();
								allHeaderCount = allHeaderCount + headerCount;
								question.setQuestion_bank_id((int) questionbank_id);
								question.setCreated_person(user.getName());
								question.setCreated_date(new Date());
								question.setUpdate_person(user.getName());
								question.setUpdate_date(new Date());
								question.setEnabled(1);
								questionService.save(question);
							}

						}
					}
				} else {
					number = -2;
				}
			}
			// ??????????????????????????????redis???
			if (number >= 0 || number == -2) {
				redisService.save(systemUserService.getCurrentUserName() + "importBank_number", String.valueOf(number));
			} else {
				redisService.save(systemUserService.getCurrentUserName() + "importBank_number", String.valueOf(-1));
			}
		}

		return "redirect:list.jhtml";
	}

	/**
	 * ????????????
	 */
	@ResponseBody
	@RequestMapping(value = "/exportQuestionBank", method = RequestMethod.GET)
	public List<String> exportQuestionBank(Long[] ids, ModelMap model, RedirectAttributes redirectAttributes) {

		SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

		// ??????redis?????????????????????????????????
		List<QuestionBank> questionBankList = new ArrayList<>();
		// ????????????
		List<String> pathList = new ArrayList<>();
		for (long id : ids) {
			questionBankList.add(questionBankService.find(id));
		}
		// ??????????????????
		Setting setting = SettingUtils.get();
		String newpath = setting.getUploadPath() + "/upload/tempfile";
		// ????????????????????????????????????????????????
		for (int a = 0; a < questionBankList.size(); a++) {

			QuestionBank questionBank = questionBankList.get(a);
			List<Question> questionList = questionService.findList("question_bank_id", questionBank.getId());
			String questionbankName = fileUtils.createFileDir(newpath, questionBank.getQuestion_bank());
			for (int i = 0; i < questionList.size(); i++) {
				Question question = questionList.get(i);

				String chapterFileName = fileUtils.createFileDir(questionbankName,
						setChapterName(question.getChapter()));
				String questionFileName = fileUtils.createFileDir(chapterFileName, question.getQuestion_name());
				// ??????????????????xml?????????????????????????????????????????????????????????
				QuestionTemp questionTemp = parserUtils.getPullParserQuestionList(question.getContent());
				QuestionContent questionContent = questionTemp.getQuestionContent();
				List<QuestionHeaderFile> qhfList = questionTemp.getHeaderFileList();
				// ??????????????????
				fileUtils.createFile(questionFileName,
						setChapterName(question.getChapter()) + question.getQuestion_name(),
						questionContent.getStudent_answer(), ".c");
				fileUtils.createFile(questionFileName, "document", questionContent.getQuestion_description(), "");
				fileUtils.createFile(questionFileName, "dx", questionContent.getHeaderfile_content(), ".c");
				fileUtils.createFile(questionFileName, "stds", questionContent.getStandard_answer(), ".c");
				for (int j = 0; j < qhfList.size(); j++) {
					fileUtils.createFile(questionFileName, "head" + (j + 1), qhfList.get(j).getHeader_file_content(),
							"");
				}

			}
			// ???????????????????????????zip?????????
			zipFileWrite.createZip(questionbankName, questionbankName + "_" + format.format(new Date()) + ".zip");
			fileUtils.deleteDirectory(questionbankName);
			pathList.add(questionbankName + "_" + format.format(new Date()) + ".zip");
		}
		zipFileWrite.createZip(newpath, newpath + ".zip");
		addFlashMessage(redirectAttributes, SUCCESS_MESSAGE);
		return pathList;
	}

	/**
	 * ????????????????????????
	 */
	@ResponseBody
	@RequestMapping(value = "/deleteFile", method = RequestMethod.GET)
	public String deleteFile(HttpServletRequest request, ModelMap model) {

		String filepath = request.getParameter("filepath");
		if (filepath.contains(",")) {

			String[] paths = filepath.split(",");
			// ????????????????????????
			for (int i = 0; i < paths.length; i++) {
				// ????????????????????????
				File zipfile = new File(paths[i]);
				if (zipfile.exists()) {
					zipfile.delete();
				}
			}
		}
		// ????????????????????????
		else {
			// ????????????????????????
			File zipfile = new File(filepath);
			if (zipfile.exists()) {
				zipfile.delete();
			}
		}

		return "success";
	}

	/**
	 * ???????????????
	 */
	@ModelAttribute
	public void init(Model model) {
		model.addAttribute("systemUser", systemUserService.getCurrentUser());
	}

	/**
	 * ?????????????????????
	 */
	public void initList(ModelMap model, Pageable pageable) {
		// ????????????????????????-1?????????????????????1??????????????????0
		if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.admin.getValue()) {
			model.addAttribute("page", this.questionBankService.findByPage(pageable));
			getSpecificSchool(model, this.questionBankService.findByPage(pageable).getContent());
		} else {
			List<QuestionBank> allqbList = this.questionBankService.findAll();
			List<QuestionBank> qbList = new ArrayList<>();
			// ??????????????????????????????
			List<College> collegeList = collegeService.findByIdMany(systemUserService.getCurrentUser().getSchoolId());
			List<Course> allcourse = new ArrayList<>();
			// ???????????????
			boolean flag = false;
			for (int i = 0; i < collegeList.size(); i++) {
				List<Course> tempcourse = courseService.findByInMany(collegeList.get(i).getId().intValue());
				for (int j = 0; j < tempcourse.size(); j++) {
					for (int k = 0; k < allcourse.size(); k++) {
						if (allcourse.get(k).getCourseName().equals(tempcourse.get(j).getCourseName())) {
							flag = true;
						}
					}
					if (!flag) {
						allcourse.add(tempcourse.get(j));
					} else {
						flag = false;
					}
				}
			}

			// ???????????????????????????????????????????????????????????????
			for (int i = 0; i < allqbList.size(); i++) {

				for (int j = 0; j < allcourse.size(); j++) {
					if (allcourse.get(j).getCourseName().equals(allqbList.get(i).getCourse_name())) {
						if (allqbList.get(i).getPublic_level() == EnumConstants.publiclevelEnum.schoolOpen.getValue()
								|| allqbList.get(i).getPublic_level() == EnumConstants.publiclevelEnum.localSchoolOpen
										.getValue()) {
							String[] specific_schoolId = allqbList.get(i).getSpecific_school().split(",");
							for (int k = 0; k < specific_schoolId.length; k++) {
								if (specific_schoolId[k]
										.equals(String.valueOf(systemUserService.getCurrentUser().getSchoolId()))) {
									qbList.add(allqbList.get(i));
								}
							}
						} else if (allqbList.get(i).getPublic_level() == EnumConstants.publiclevelEnum.secrecy
								.getValue()) {
							if (allqbList.get(i).getCreated_person()
									.equals(systemUserService.getCurrentUser().getName())) {
								qbList.add(allqbList.get(i));
							}
						} else {
							qbList.add(allqbList.get(i));
						}

					}
				}

			}
			if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.manager.getValue()) {
				model.addAttribute("page", this.questionBankService.pageMethod(pageable, qbList));
				getSpecificSchool(model, qbList);
			} else if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.teacher
					.getValue()) {
				Teacher temp = new Teacher();
				temp.setUsername(systemUserService.getCurrentUserName());
				temp.setSchoolId(systemUserService.getCurrentUser().getSchoolId());
				Teacher teacher = teacherService.findByUserName(temp);
				College c = collegeService.find((long) teacher.getCollege_id());
				// ?????????????????????????????????
				List<Course> courseList = c.getCollegeCourseList();
				List<QuestionBank> temp_qbList = new ArrayList<>();
				for (int i = 0; i < qbList.size(); i++) {
					for (int j = 0; j < courseList.size(); j++) {
						if (courseList.get(j).getCourseName().equals(qbList.get(i).getCourse_name())) {
							temp_qbList.add(qbList.get(i));
						}
					}
				}
				model.addAttribute("page", this.questionBankService.pageMethod(pageable, temp_qbList));
				getSpecificSchool(model, temp_qbList);
			}

		}

	}

	/**
	 * ????????????????????????????????????
	 */
	public void getSpecificSchool(ModelMap model, List<QuestionBank> list) {
		List<QuestionBank> questionbankList = list;
		// ????????????????????????????????????
		List<School> schoolList = new ArrayList<>();
		// ?????????????????????????????????????????????????????????
		List<Integer> numberList = new ArrayList<>();
		for (int i = 0; i < questionbankList.size(); i++) {
			String specificSchool = questionbankList.get(i).getSpecific_school();
			int publicLevel = questionbankList.get(i).getPublic_level();
			if (specificSchool != null && !specificSchool.equals("") && publicLevel ==1) {
				if (specificSchool.contains(",")) {
					String[] temp = specificSchool.split(",");
					for (int j = 0; j < temp.length; j++) {
						schoolList.add(schoolService.find(Long.parseLong(temp[j])));
					}
					numberList.add(temp.length);
				} else {
					schoolList.add(schoolService.find(Long.parseLong(specificSchool)));
					numberList.add(1);
				}
			}

		}
		model.addAttribute("schoolList", schoolList);
		model.addAttribute("numberList", numberList);
	}

	/**
	 * ?????????????????????
	 */
	@ModelAttribute
	public void initCourse(ModelMap model) {
		// ???????????????
		if (systemUserService.getCurrentUser().getRoleId() == AdminEnum.authorityEnum.superManager.getValue()) {
			List<Course> allCourseList = courseService.findAll();
			SortUtil.sort(allCourseList,true,"courseName");
			List<Course> courseList = new ArrayList<>();
			// ???????????????
			boolean flag = true;
			for (int i = 0; i < allCourseList.size(); i++) {
				for (int j = 0; j < courseList.size(); j++) {
					if (courseList.get(j).getCourseName().equals(allCourseList.get(i).getCourseName())) {
						flag = false;
					}
				}
				if (flag) {
					courseList.add(allCourseList.get(i));
				}
				flag = true;
			}
			model.addAttribute("courseList", courseList);

		}
		// ???????????????
		else if (systemUserService.getCurrentUser().getRoleId() == EnumConstants.authorityEnum.manager.getValue()) {
			List<College> collegeList = collegeService.findByIdMany(systemUserService.getCurrentUser().getSchoolId());
			List<Course> courseList = new ArrayList<>();
			// ???????????????
			boolean flag = true;
			for (int i = 0; i < collegeList.size(); i++) {
				List<Course> temp_courseList = courseService.findByInMany(collegeList.get(i).getId().intValue());
				for (int j = 0; j < temp_courseList.size(); j++) {
					for (int k = 0; k < courseList.size(); k++) {
						if (courseList.get(k).getCourseName().equals(temp_courseList.get(j).getCourseName())) {
							flag = false;
						}
					}
					if (flag) {
						courseList.add(temp_courseList.get(j));
					}
					flag = true;
				}

			}
			model.addAttribute("courseList", courseList);

		} else {
			// ????????????
			Teacher temp = new Teacher();
			temp.setUsername(systemUserService.getCurrentUserName());
			temp.setSchoolId(systemUserService.getCurrentUser().getSchoolId());
			Teacher teacher = teacherService.findByUserName(temp);
			College college = teacher.getCollege();
			model.addAttribute("courseList", courseService.findByInMany(college.getId().intValue()));
		}
	}

	/**
	 * ?????????????????????????????????
	 */
	public String getChapterName(String chapter) {
		// ???????????????
		String chapterName = "???1???";
		for (int i = 20; i > 0; i--) {
			if (chapter.contains(String.valueOf(i))) {
				chapterName = "???" + i + "???";
				return chapterName;
			}
		}
		return chapterName;
	}

	/**
	 * ?????????????????????????????????
	 */
	public String setChapterName(String chapter) {
		String chapterName = "";
		for (int i = 20; i > 0; i--) {
			if (chapter.contains(String.valueOf(i))) {
				if (i < 10) {
					chapterName = "CP0" + i;
				} else {
					chapterName = "CP" + i;
				}
				return chapterName;
			}
		}
		return chapterName;
	}

}
