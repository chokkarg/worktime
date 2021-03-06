package eu.vranckaert.worktime.json.endpoint.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;

import eu.vranckaert.worktime.dao.ProjectDao;
import eu.vranckaert.worktime.dao.TaskDao;
import eu.vranckaert.worktime.dao.TimeRegistrationDao;
import eu.vranckaert.worktime.json.base.request.RegisteredServiceRequest;
import eu.vranckaert.worktime.model.Project;
import eu.vranckaert.worktime.model.Service;
import eu.vranckaert.worktime.model.ServicePlatform;
import eu.vranckaert.worktime.model.Session;
import eu.vranckaert.worktime.model.Task;
import eu.vranckaert.worktime.model.TimeRegistration;
import eu.vranckaert.worktime.model.User;
import eu.vranckaert.worktime.security.dao.ServiceDao;
import eu.vranckaert.worktime.security.dao.SessionDao;
import eu.vranckaert.worktime.security.dao.UserDao;
import eu.vranckaert.worktime.security.exception.ServiceNotAllowedException;
import eu.vranckaert.worktime.security.service.SecurityChecker;
import eu.vranckaert.worktime.security.utils.KeyGenerator;

@Path("setup")
public class SetupEndpoint {
	@Inject
	private SecurityChecker securityChecker;
	
	@Inject
	private ServiceDao serviceDao;
	
	@Inject
	private ProjectDao projectDao;
	
	@Inject
	private TaskDao taskDao;
	
	@Inject
	private SessionDao sessionDao;
	
	@Inject
	private TimeRegistrationDao timeRegistrationDao;
	
	@Inject
	private UserDao userDao;
	
	@GET
	@Path("hello")
	@Produces(MediaType.TEXT_PLAIN)
	public String sayHello() {
		return "Hello World";
	}
	
//	@GET
//	@Path("setupService")
//	@Produces(MediaType.TEXT_PLAIN)
//	public String setupService() {
//		// Create a setup service...
//		Service serviceTest = new Service();
//		serviceTest.setAppName("WorkTime for Android");
//		serviceTest.setContact("dirkvranckaert@gmail.com");
//		serviceTest.setPlatform(ServicePlatform.ANDROID);
//		serviceTest.setServiceKey(KeyGenerator.getNewKey());
//		serviceDao.persist(serviceTest);
//		
//		return "Service created!";
//	}
	
	@GET
	@Path("removeProjectTasksTimeRegistrationsForUser")
	@Produces(MediaType.TEXT_PLAIN)
	public String retrieveIdsForUser(@QueryParam("serviceKey") String serviceKey, @QueryParam("email") String email) {
		RegisteredServiceRequest request = new RegisteredServiceRequest() {};
		request.setServiceKey(serviceKey);
		
		try {
			securityChecker.checkService(request);
		} catch (ServiceNotAllowedException e) {
			return "Not Done...";
		}
		
		User user = userDao.findById(email);
		
		List<Project> projects = projectDao.findAll(user);
		List<Task> tasks = taskDao.findAll(user);
		List<TimeRegistration> timeRegistrations = timeRegistrationDao.findAll(user);
		
		for (TimeRegistration timeRegistration : timeRegistrations) {
			timeRegistrationDao.remove(timeRegistration);
		}
		
		for (Task task : tasks) {
			taskDao.remove(task);
		}
		
		for (Project project : projects) {
			projectDao.remove(project);
		}
		
		return "Done!";
	}
	
	@GET
	@Path("exportUsers")
	@Produces(MediaType.TEXT_PLAIN)
	public String exportUsers(@QueryParam("serviceKey") String serviceKey) {
		final String format = "yyyy-MM-dd hh:mm:ss.SSS";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		RegisteredServiceRequest request = new RegisteredServiceRequest() {};
		request.setServiceKey(serviceKey);
		
		try {
			securityChecker.checkService(request);
		} catch (ServiceNotAllowedException e) {
			return "Cannot export...";
		}
		
		String export = "";
		List<User> users = userDao.findAll();
		for (User user : users) {
			export += "insert into user(email, firstName, lastName, lastLoginDate, passwordHash, registrationDate, role) values('" 
						+ user.getEmail() + "', '" 
					    + user.getFirstName() + "', '" 
						+ user.getLastName() + "', '" 
					    + sdf.format(user.getLastLoginDate()) + "', '"
						+ user.getPasswordHash() + "', '"
						+ sdf.format(user.getRegistrationDate()) + "', '"
						+ user.getRole().toString() + "')\n";
		}
		
		return export;
	}
	
	@GET
	@Path("exportProjects")
	@Produces(MediaType.TEXT_PLAIN)
	public String exportProjects(@QueryParam("serviceKey") String serviceKey) {
		final String format = "yyyy-MM-dd hh:mm:ss.SSS";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		RegisteredServiceRequest request = new RegisteredServiceRequest() {};
		request.setServiceKey(serviceKey);
		
		try {
			securityChecker.checkService(request);
		} catch (ServiceNotAllowedException e) {
			return "Cannot export...";
		}

		String exportProjects = "";	
		
		List<Project> projects = projectDao.findAll();
		for (Project project : projects) {
			if (!getIgnoredAccounts().contains(project.getUser().getEmail())) {
				exportProjects += "insert into project(name, comment, defaultValue, finished, flags, projectOrder, syncKey, lastUpdated, userId) values("
						+ "'" + project.getName() + "', "
						+ "'" + project.getComment() + "', "
						+ "" + (project.isDefaultValue() ? 1 : 0) + ", "	
						+ "" + (project.isFinished() ? 1 : 0) + ", "
						+ "'" + project.getFlags() + "', "
						+ "" + project.getOrder() + ", "
						+ "'" + project.getSyncKey() + "', "
						+ "'" + sdf.format(project.getLastUpdated()) + "', "
						+ "'" + project.getUser().getEmail() + "'"
						+ ");\n";
			}
		}
		
		return "# Projects Export\n" + exportProjects;
	}
	
	@GET
	@Path("exportTasks")
	@Produces(MediaType.TEXT_PLAIN)
	public String exportTasks(@QueryParam("serviceKey") String serviceKey, @QueryParam("startAt") int startAt) {
		long startTime = new Date().getTime();
		
		final String format = "yyyy-MM-dd hh:mm:ss.SSS";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		RegisteredServiceRequest request = new RegisteredServiceRequest() {};
		request.setServiceKey(serviceKey);
		
		try {
			securityChecker.checkService(request);
		} catch (ServiceNotAllowedException e) {
			return "Cannot export...";
		}

		String exportTasks = "";
		int endAt = startAt;
		boolean allDone = false;
		
		List<Task> tasks = taskDao.findAll();
		for (int i=startAt; i<tasks.size(); i++) {
			Task task = tasks.get(i);
			if (task != null && task.getProject() != null && !getIgnoredAccounts().contains(task.getProject().getUser().getEmail())) {
				exportTasks += "insert into task(name, comment, finished, flags, taskOrder, syncKey, lastUpdated, projectId) select ";
				exportTasks += "'" + task.getName().replaceAll("'", "") + "', ";
				exportTasks += "'" + (StringUtils.isNotBlank(task.getComment()) ? task.getComment().replaceAll("'", "\\'") : "") + "', ";	
				exportTasks += "" + (task.isFinished() ? 1 : 0) + ", ";
				exportTasks += "'" + (StringUtils.isNotBlank(task.getFlags()) ? task.getFlags().replaceAll("'", "\\'") : "") + "', ";
				exportTasks += "" + task.getOrder() + ", ";
				exportTasks += "'" + (StringUtils.isNotBlank(task.getSyncKey()) ? task.getSyncKey().replaceAll("'", "\\'") : "") + "', ";
				exportTasks += "'" + sdf.format(task.getLastUpdated()) + "', ";
				exportTasks += "p.project_id from project p where p.name like '" + task.getProject().getName() + "' COLLATE utf8_bin and p.userId='" + task.getProject().getUser().getEmail() + "'";
				exportTasks += ";\n";
			}
			
			if (isOperationRunningForTooLong(startTime)) {
				endAt = i;
				break;
			}
			
			if (i==(tasks.size()-1)) {
				allDone = true;
			}
		}
		
		return "# Tasks Export (Start at " + startAt + ", ended at " + endAt + ", all done? " + allDone +")\n" + exportTasks;
	}
	
	@GET
	@Path("exportTimeRegistrations")
	@Produces(MediaType.TEXT_PLAIN)
	public String exportTimeRegistrations(@QueryParam("serviceKey") String serviceKey, @QueryParam("startAt") int startAt) {
		long startTime = new Date().getTime();
		
		final String format = "yyyy-MM-dd hh:mm:ss.SSS";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		RegisteredServiceRequest request = new RegisteredServiceRequest() {};
		request.setServiceKey(serviceKey);
		
		try {
			securityChecker.checkService(request);
		} catch (ServiceNotAllowedException e) {
			return "Cannot export...";
		}

		String exportTimeRegistrations = "";
		int endAt = startAt;
		boolean allDone = false;
		
		List<TimeRegistration> timeRegistrations = timeRegistrationDao.findAll();
		for (int i=startAt; i<timeRegistrations.size(); i++) {
			TimeRegistration timeRegistration = timeRegistrations.get(i);
			if (timeRegistration != null && timeRegistration.getTask() != null && timeRegistration.getTask().getProject() != null && !getIgnoredAccounts().contains(timeRegistration.getTask().getProject().getUser().getEmail())) {
				exportTimeRegistrations += "insert into timeRegistration(startTime, endTime, comment, flags, syncKey, lastUpdated, taskId) select ";
				exportTimeRegistrations += "'" + sdf.format(timeRegistration.getStartTime()) + "', ";
				exportTimeRegistrations += (timeRegistration.getEndTime() != null ? "'" + sdf.format(timeRegistration.getEndTime()) + "'" : "null") + ", ";
				exportTimeRegistrations += "'" + (StringUtils.isNotBlank(timeRegistration.getComment()) ? timeRegistration.getComment().replaceAll("'", "\\'") : "") + "', ";
				exportTimeRegistrations += "'" + (StringUtils.isNotBlank(timeRegistration.getFlags()) ? timeRegistration.getFlags().replaceAll("'", "\\'") : "") + "', ";
				exportTimeRegistrations += "'" + (StringUtils.isNotBlank(timeRegistration.getSyncKey()) ? timeRegistration.getSyncKey().replaceAll("'", "\\'") : "") + "', ";
				exportTimeRegistrations += "'" + sdf.format(timeRegistration.getLastUpdated()) + "', ";
				exportTimeRegistrations += "t.task_id from task t, project p where t.projectId = p.project_id and t.name like '" + timeRegistration.getTask().getName() + "' COLLATE utf8_bin and p.name like '" + timeRegistration.getTask().getProject().getName() + "' COLLATE utf8_bin and p.userId='" + timeRegistration.getTask().getProject().getUser().getEmail() + "'";
				exportTimeRegistrations += ";\n";
			}
			
			if (isOperationRunningForTooLong(startTime)) {
				endAt = i;
				break;
			}
			
			if (i==(timeRegistrations.size()-1)) {
				allDone = true;
			}
		}
		
		return "# Time Registrations Export (Start at " + startAt + ", ended at " + endAt + ", all done? " + allDone +")\n" + exportTimeRegistrations;
	}
	
	@GET
	@Path("exportSessions")
	@Produces(MediaType.TEXT_PLAIN)
	public String exportSessions(@QueryParam("serviceKey") String serviceKey, @QueryParam("startAt") int startAt) {
		long startTime = new Date().getTime();
		
		final String format = "yyyy-MM-dd hh:mm:ss.SSS";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		
		RegisteredServiceRequest request = new RegisteredServiceRequest() {};
		request.setServiceKey(serviceKey);
		
		try {
			securityChecker.checkService(request);
		} catch (ServiceNotAllowedException e) {
			return "Cannot export...";
		}

		String exportSessions = "";
		int endAt = startAt;
		boolean allDone = false;
		
		List<Session> sessions = sessionDao.findAll();
		for (int i=startAt; i<sessions.size(); i++) {
			Session session = sessions.get(i);
			if (session != null && !getIgnoredAccounts().contains(session.getUser().getEmail())) {
				exportSessions += "insert into session(creationDate, sessionKey, timesUsed, lastTimeUsed, platform, userId) select ";
				exportSessions += "'" + sdf.format(session.getCreationDate()) + "', ";
				exportSessions += "'" + session.getSessionKey() + "', ";
				exportSessions += session.getTimesUsed() + ", ";
				exportSessions += (session.getLastTimeUsed() != null ? "'" + sdf.format(session.getLastTimeUsed()) + "'" : "null") + ", ";
				exportSessions += "'" + (session.getPlatform() != null ? session.getPlatform().toString() : "") + "', ";
				exportSessions += "'" + session.getUser().getEmail() + "'";
				exportSessions += ";\n";
			}
			
			if (isOperationRunningForTooLong(startTime)) {
				endAt = i;
				break;
			}
			
			if (i==(sessions.size()-1)) {
				allDone = true;
			}
		}
		
		return "# Session Export (Start at " + startAt + ", ended at " + endAt + ", all done? " + allDone +")\n" + exportSessions;
	}
	
	private List<String> getIgnoredAccounts() {
		List<String> ignoredEmails = new ArrayList<String>();
		ignoredEmails.add("cjmbranco@yahoo.co.uk");
		ignoredEmails.add("dirkvranckaert@gmail.com");
		ignoredEmails.add("irving@mordormx.net");
		ignoredEmails.add("shay_nahum@yahoo.com");
		ignoredEmails.add("test@mail.com");
		return ignoredEmails;
	}
	
	private boolean isOperationRunningForTooLong(long startTime) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(startTime);
		cal.add(Calendar.SECOND, 20);
		
		long timeout = cal.getTimeInMillis();
		long now = new Date().getTime();
		
		if (now >= timeout) {
			return true;
		}
		
		return false;		
	}
}
